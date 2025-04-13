package com.zervice.kbase.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.zervice.kbase.api.restful.pojo.RestEvaluation;
import com.zervice.kbase.api.restful.pojo.RestMessage;
import com.zervice.kbase.cache.ChatCache;
import com.zervice.kbase.database.dao.ChatDao;
import com.zervice.kbase.database.dao.EvaluationDao;
import com.zervice.kbase.database.dao.MessageDao;
import com.zervice.kbase.database.pojo.Chat;
import com.zervice.kbase.database.pojo.Message;
import com.zervice.kbase.database.utils.DaoUtils;
import com.zervice.kbase.service.MessageService;
import lombok.Cleanup;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author xh
 */
@Log4j2
@Service
public class MessageServiceImpl implements MessageService {
    private final ChatCache _chatCache = ChatCache.getInstance();

    @Override
    public Message save(String dbName, Message message) throws Exception {
        LOG.debug("[{}:{}][begin save message]", dbName, message.getChatId());
        Chat chat = _chatCache.get(dbName, message.getChatId());
        if (!message.isInitMessage() && chat != null) {
            //if not init message  and cache hasMessage is false,so we need update chat has message state to true
            boolean updated = false;
            if (!chat.getProperties().getHasMessage()) {
                chat.getProperties().setHasMessage(true);
                updated = true;
            }

            String rootComponentId = message.parseRootComponentId();
            if (StringUtils.isNotBlank(rootComponentId) && !chat.hasRootComponentId(rootComponentId)) {
                chat.addRootComponentId(rootComponentId);
                updated = true;
            }

            JSONObject validSlots = message.parseValidSlots();
            if (validSlots != null && !validSlots.isEmpty()) {
                chat.refreshLatestFilledSlots(validSlots);
                updated = true;
            }

            // 将fallback作为存在rootComponentIds，便于用户浏览回话有无fallback
            String fallback = message.parseFallback();
            if (StringUtils.isNotBlank(fallback) && !chat.hasRootComponentId(fallback)) {
                chat.addRootComponentId(fallback);
                updated = true;
            }

            String llmOrKbqa = message.parseLlmOrKbqaType();
            if (StringUtils.isNotBlank(llmOrKbqa) && !chat.hasRootComponentId(llmOrKbqa)) {
                chat.addRootComponentId(llmOrKbqa);
                updated = true;
            }

            if (updated) {
                ChatDao.updateProp(dbName, chat.getId(), chat.getProperties());
            }
        }

        String scene = chat == null ? Chat.Prop.SCENE_DEBUG : chat.getProperties().getScene();
        // 是否可评价
        message.getProperties().setCanEvaluate(true);
        message.getProperties().setScene(scene);

        // parse roots
        Set<String> roots = message.parseRoots();
        message.getProperties().setRoots(roots);

        Message msg = MessageDao.save(dbName, message);
        LOG.info("[{}:{}][success save message:{}]", dbName, message.getChatId(), message.getId());
        return msg;
    }


    @Override
    public List<RestMessage> get(String dbName, String chatId) throws Exception {
        List<Message> messages = MessageDao.getByChatId(dbName, chatId);

        @Cleanup Connection conn = DaoUtils.getConnection(true);
        Map<String /* message id*/, RestEvaluation> evaluationMap = EvaluationDao.getByChatId(conn, dbName, chatId).stream()
                .map(RestEvaluation::new)
                .collect(Collectors.toMap(RestEvaluation::getMessageId, e -> e));

        return messages.stream()
                .map(m -> new RestMessage(m, evaluationMap.get(m.getId())))
                .collect(Collectors.toList());
    }
}
