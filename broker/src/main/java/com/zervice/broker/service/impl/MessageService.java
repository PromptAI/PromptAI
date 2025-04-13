package com.zervice.broker.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import com.zervice.broker.agent.AgentClientSelector;
import com.zervice.broker.backend.KbChatService;
import com.zervice.broker.backend.ProjectComponentService;
import com.zervice.broker.backend.ProjectService;
import com.zervice.broker.restful.controller.MessageController;
import com.zervice.common.agent.AgentClient;
import com.zervice.common.agent.pojo.SimplePublishedProject;
import com.zervice.common.model.MessageRes;
import com.zervice.common.model.SendMessageModel;
import com.zervice.common.pojo.chat.ChatPojo;
import com.zervice.common.pojo.chat.ProjectComponentEntityPojo;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author chen
 * @date 2023/3/9
 */
@Log4j2
public class MessageService implements com.zervice.broker.service.MessageService {

    private MessageService(){}

    @Getter
    private static MessageService _instance = new MessageService();

    private static ProjectService projectService = ProjectService.getInstance();
    private final KbChatService kbChatService = KbChatService.getInstance();
    private final ProjectComponentService projectComponentService = ProjectComponentService.getInstance();

    @Override
    public MessageRes message(SendMessageModel message, SimplePublishedProject project, String account) {
        AgentClient agentClient = AgentClientSelector.getInstance().select(project.getId());
        String chatId = message.getChatId();
        String publishedProjectId = project.getId();
        String projectId = project.getProjectId();

        JSONArray res;

        if (ChatPojo.INIT_MESSAGE.equals(message.getMessage())) {
            res = _initMicaChat(message, agentClient, account, projectId, publishedProjectId);
            return MessageRes.factory(res, MessageRes.OUTPUT_TYPE_WELCOME);
        }


        String micaRes = _requestMica(account, agentClient, message, project);
        if (MessageRes.EMPTY_RES.equals(message) || StringUtils.isBlank(micaRes)) {
            JSONArray fallback = MessageController.replayFallback(chatId, account, projectId, publishedProjectId);
            return MessageRes.factory(fallback, MessageRes.OUTPUT_TYPE_FALLBACK);
        }

        return MessageRes.factory(JSONArray.parseArray(micaRes), MessageRes.OUTPUT_TYPE_BOT);
    }

    /**
     * 如果是初始化会话，这里一并将该会话在MICA那边初始化
     */
    private JSONArray _initMicaChat(SendMessageModel messageModel, AgentClient agentClient,
                                    String account, String projectId, String publishedProjectId) {
        String chatId = messageModel.getChatId();
        String message = messageModel.getMessage();
        message = _appendSlotToMessage(message, chatId, account);
        String res = "";
        try {
            res = agentClient.sendMsg(chatId, message, publishedProjectId);
        } catch (Exception ex) {
            LOG.info("[{}:{}][init mica chat error. agent:{}, message:{}, error:{}]",
                    account, chatId, agentClient.getId(), message, ex.getMessage(), ex);
        }
        LOG.info("[{}:{}][finish init mica chat. agent:{}, message:{}, res:{}]",
                account, chatId, agentClient.getId(), message, res);

        if (MessageRes.EMPTY_RES.equals(res) || StringUtils.isBlank(res)) {
            String welcome = projectService.welcome(projectId, chatId, publishedProjectId, account);
            JSONObject resWelcome = new JSONObject();
            resWelcome.put("text", welcome);
            resWelcome.put("recipient_id", chatId);

            // 转成array
            res = JSONObject.toJSONString(Collections.singletonList(resWelcome));
        }
        return JSONArray.parseArray(res);
    }

    /**
     * dynamic append slot values
     *
     * @param message     input
     * @param chatId      chatId
     * @param accountName db
     * @return message with slot value
     */
    private String _appendSlotToMessage(String message, String chatId, String accountName) {
        try {
            ChatPojo chatPojo = kbChatService.searchChatWithEntity(accountName, chatId);
            if (chatPojo == null || chatPojo.getProperties().getEntities() == null) {
                LOG.warn("[{}:{}][fail to search chat entities]", accountName, chatId);
                return message;
            }

            /*
             * {"entityId":"entityValue"}
             */
            Map<String /*entity id*/, Object /* entity value*/> slots = chatPojo.getProperties().getSlots();
            if (slots == null || slots.isEmpty()) {
                return message;
            }

            Map<String /* entity id*/, ProjectComponentEntityPojo> entityMap = chatPojo.getProperties().getEntities().stream()
                    .collect(Collectors.toMap(ProjectComponentEntityPojo::getId, e -> e));

            Map<String /*entity name */, String /*entity value*/> slotNameAndValue = Maps.newHashMapWithExpectedSize(slots.size());
            for (Map.Entry<String, Object> entry : slots.entrySet()) {
                ProjectComponentEntityPojo e = entityMap.get(entry.getKey());
                if (e == null) {
                    LOG.warn("[{}:{}][failed to append slot:{} with value:{} to message:{}]",
                            accountName, chatId, entry.getKey(), entry.getValue(), message);
                    continue;
                }

                slotNameAndValue.put(e.getName(), entry.getValue().toString());
            }

            message = message + JSONObject.toJSONString(slotNameAndValue);

            LOG.info("[{}:{}][find {} slot value for message:{}]", accountName, chatId, slotNameAndValue, message);
            return message;
        } catch (Exception e) {
            LOG.error("[{}:{}][failed to append slot with message:{}]", accountName, chatId, message);
            return message;
        }
    }

    private String _requestMica(String account, AgentClient agentClient, SendMessageModel message,
                                SimplePublishedProject project) {
        String publishedProjectId = project.getId();
        String send2Mica = message.getMessage();
        String res = agentClient.sendMsg(message.getChatId(), send2Mica, publishedProjectId);

        LOG.info("[{}:{}][mica message:{}, res:{}]",
                account, message.getChatId(), send2Mica, JSONObject.toJSONString(res));
        return res;
    }
}