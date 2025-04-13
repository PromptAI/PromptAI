package com.zervice.broker.backend;

import cn.hutool.core.io.FileUtil;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import com.zervice.common.model.MessageModel;
import com.zervice.common.model.MessageRes;
import com.zervice.common.model.SendMessageModel;
import com.zervice.common.pojo.chat.MessageEvaluateModel;
import com.zervice.common.restful.exception.RestException;
import com.zervice.common.restful.exception.StatusCodes;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Map;

/**
 * @author chen
 * @date 2022/8/30
 */
@Log4j2
public class KbMessageService {

    private BackendClient _backendClient;
    private static final KbMessageService _instance = new KbMessageService();

    private static final String _URI_MESSAGE_EVALUATE = "/rpc/message";
    private static final String _URI_MESSAGE = "/rpc/message/evaluate";
    private static final String _URI_MESSAGE_FILE = "/rpc/message/file";


    public static KbMessageService getInstance() {
        return _instance;
    }

    private KbMessageService() {
    }

    public void init(BackendClient backendClient) {
        _backendClient = backendClient;
    }

    public JSONObject uploadFile(String account, String chatId, String ip,
                                 String projectId,  MultipartFile file) throws Exception {
        String originName = file.getOriginalFilename();
        try {
            // 这里将原始名称弄出来, MultipartFile 生成临时文件，在kb端读取的时候不要用MultipartFile,而是请求体的
            Map<String, Object> param = Maps.newHashMapWithExpectedSize(4);
            param.put("chatId", chatId);
            param.put("ip", ip);
            param.put("projectId", projectId);
            param.put("originName", originName);

            File tmp = FileUtil.createTempFile("tmp_message_file", file.getOriginalFilename(), true);
            file.transferTo(tmp);

            return _backendClient.postFile(account, _URI_MESSAGE_FILE, tmp, param);
        } catch (Exception ex) {
            LOG.error("[{}:{}][fail to save file:{} with error:{}]", account, chatId,
                    originName, ex.getMessage(), ex);
            throw new RestException(StatusCodes.BadRequest);
        }
    }

    public void saveMessage2Backend(String account, long startTime, String agentClientId, SendMessageModel message, MessageRes res) {
        MessageModel messageModel = MessageModel.toMessage(agentClientId, startTime, message, res);
        try {
            JSONObject result = _backendClient.postJson(account, _URI_MESSAGE_EVALUATE, messageModel);

            _afterMessageSaved(res, result);
        } catch (Exception ex) {
            LOG.error("[{}:{}][fail to save message:{} with error:{}]", account, message.getChatId(),
                    JSONObject.toJSONString(messageModel), ex.getMessage(), ex);
            throw new RestException(StatusCodes.BadRequest);
        }
    }

    public void evaluate(MessageEvaluateModel evaluate, String dbName) {
        _backendClient.putJson(dbName, _URI_MESSAGE, evaluate);
        LOG.info("[{}][success evaluate chat:{} with helpful:{}]", dbName, evaluate.getChatId(), evaluate.getHelpful());
    }

    private void _afterMessageSaved(MessageRes res, JSONObject message) {
        String id = message.getString("id");
        JSONObject properties = message.getJSONObject("properties");

        res.setId(id);
        res.setProperties(properties.toJavaObject(MessageRes.Prop.class));
    }

}
