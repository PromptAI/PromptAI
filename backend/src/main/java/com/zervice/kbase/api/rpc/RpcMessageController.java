package com.zervice.kbase.api.rpc;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.net.URLEncodeUtil;
import com.alibaba.fastjson.JSONObject;
import com.zervice.common.model.MessageModel;
import com.zervice.common.pojo.chat.MessageEvaluateModel;
import com.zervice.common.pojo.common.EmptyResponse;
import com.zervice.common.utils.Constants;
import com.zervice.kbase.api.restful.AuthFilter;
import com.zervice.kbase.api.restful.pojo.RestBlob;
import com.zervice.kbase.cache.ChatCache;
import com.zervice.kbase.database.dao.ChatDao;
import com.zervice.kbase.database.dao.EvaluationDao;
import com.zervice.kbase.database.dao.MessageDao;
import com.zervice.kbase.database.pojo.Chat;
import com.zervice.kbase.database.pojo.CommonBlob;
import com.zervice.kbase.database.pojo.Evaluation;
import com.zervice.kbase.database.pojo.Message;
import com.zervice.kbase.database.utils.DaoUtils;
import com.zervice.kbase.service.BlobService;
import com.zervice.kbase.service.MessageService;
import lombok.Cleanup;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.sql.Connection;
import java.util.List;


/**
 * @author xh
 */
@Log4j2
@RestController
@RequestMapping("/rpc/message")
public class RpcMessageController {

    @Autowired
    private MessageService messageService;
    private BlobService blobService = BlobService.getInstance();

    @PostMapping
    public Object save(@RequestBody @Validated MessageModel messageModel,
                       @RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName) throws Exception {
        Message message = Message.fromMessageModel(messageModel.getId(),
                messageModel.getChatId(),
                JSONObject.toJSONString(messageModel.getDialog()),
                JSONObject.toJSONString(messageModel.getProperties()),
                messageModel.getTime());
        return messageService.save(dbName, message);
    }

    /**
     * 在对话中上传文件，以消息的形式存储在message中
     */
    @PostMapping("file")
    public Object file(@RequestPart("file") MultipartFile file,
                       @RequestParam("chatId") String chaId,
                       // 文件原始名称，不要从file读，broker 内部生成了一次临时文件
                       @RequestParam("originName") String originName,
                       @RequestParam("projectId") String projectId,
                       @RequestParam("ip") String ip,
                       @RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName) throws Exception {
        @Cleanup Connection conn = DaoUtils.getConnection(true);

        RestBlob blob = blobService.save(file.getInputStream(), originName, ip, CommonBlob.TYPE_MESSAGE_ATTACHMENT, conn, dbName);

        /**
         * 按照这个格式存储在message中
         *
         * {
         *      "text": "%7B%22name%22%3A%22API-quartz.pdf%22%2C%22type%22%3A%22pdf%22%2C%22href%22%3A%22%2Fapi%2Fblobs%2Fget%2Fa1_cqxbh1gmot8g%22%2C%22version%22%3A%220.0.1%22%7D",
         * }
         * text 存储的是json的url encode之后的内容
         * {
         *   "name": "API-quartz.pdf",
         *   "type": "pdf",
         *   "href": "/api/blobs/get/a1_cqxbh1gmot8g",
         *   "version": "0.0.1"
         * }
         */

        JSONObject attachmentText = new JSONObject();
        attachmentText.put("name", originName);
        attachmentText.put("type", FileUtil.extName(originName));
        attachmentText.put("href", "/api/blobs/get/" + blob.getId());
        attachmentText.put("version", "0.0.1");

        String text = URLEncodeUtil.encode(attachmentText.toJSONString());

        long now = System.currentTimeMillis();

        Message.Dialog dialog = Message.Dialog.builder()
                .input(Message.Input.builder()
                        .query(text).send(text).type(Message.INPUT_TYPE_FILE)
                        .sendTime(now)
                        .build())
                .output(Message.Output.builder().build())
                .build();

        Chat chat = ChatCache.getInstance().get(dbName,chaId);
        String scene = chat.getProperties().getScene() == null ? Chat.Prop.SCENE_DEBUG : chat.getProperties().getScene();

        Message.Prop prop = Message.Prop.builder()
                .projectId(projectId).ip(ip)
                .scene(scene)
                .build();

        Message message = Message.fromMessageModel(null,
                chaId,
                JSONObject.toJSONString(dialog),
                JSONObject.toJSONString(prop),
                now);
        LOG.info("[{}][success upload file:{} from chat:{}]", dbName, originName, chaId);
        return messageService.save(dbName, message);
    }

    @PutMapping("evaluate")
    public Object evaluate(@RequestBody @Validated MessageEvaluateModel evaluateModel,
                           @RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName) throws Exception {
        String chatId = evaluateModel.getChatId();
        String messageId = evaluateModel.getMessageId();

        LOG.info("[{}:{}][evaluate message:{} with:{}]", dbName, chatId, messageId, evaluateModel.getHelpful());

        Chat chat = ChatDao.get(dbName, chatId);
        if (chat == null) {
            LOG.warn("[{}]:[evaluate fail chat:{} not found]", dbName, chatId);
            return EmptyResponse.empty();
        }

        String projectId = chat.getProperties().getProjectId();

        Message message = MessageDao.get(dbName, messageId);
        if (message == null) {
            LOG.warn("[{}]:[evaluate fail message:{} not found]", dbName, messageId);
            return EmptyResponse.empty();
        }

        String rootComponentId = message.parseRootComponentId();
        rootComponentId = rootComponentId == null ? Constants.ROOT_COMPONENT_ID_UNKNOWN : rootComponentId;

        @Cleanup Connection conn = DaoUtils.getConnection(true);
        List<Evaluation> evaluations = EvaluationDao.getByMessageId(conn, dbName, messageId);
        if (CollectionUtils.isNotEmpty(evaluations)) {
            LOG.warn("[{}]:[message:{} already evaluated]", dbName, messageId);
            return EmptyResponse.empty();
        }

        Evaluation.Prop prop = Evaluation.Prop.builder()
                .createTime(System.currentTimeMillis())
                .build();

        Evaluation evaluation = Evaluation.builder()
                .messageId(messageId)
                .chatId(chatId)
                .helpful(evaluateModel.getHelpful())
                .componentId(rootComponentId)
                .properties(prop)
                .build();

        EvaluationDao.add(conn, dbName, evaluation);

        // record evaluate in chat
        _recordEvaluateInChat(chat, evaluateModel.getHelpful(), dbName);

        return EmptyResponse.empty();
    }

    /**
     * 将评价信息记录在Chat中,便于搜索
     *
     * @param chat
     * @param helpful
     */
    private void _recordEvaluateInChat(Chat chat, Integer helpful, String dbName) {
        chat.appendEvaluate(helpful);
        ChatDao.updateProp(dbName, chat.getId(), chat.getProperties());
    }


}
