package com.zervice.kbase.service.impl;

import com.zervice.common.i18n.MessageUtils;
import com.zervice.common.model.MessageRes;
import com.zervice.common.utils.Constants;
import com.zervice.kbase.api.restful.pojo.ChatCriteria;
import com.zervice.kbase.api.restful.pojo.PageRequest;
import com.zervice.kbase.api.restful.pojo.RestChat;
import com.zervice.kbase.cache.ChatCache;
import com.zervice.kbase.database.dao.ChatDao;
import com.zervice.kbase.database.dao.ProjectComponentDao;
import com.zervice.kbase.database.pojo.Chat;
import com.zervice.kbase.database.pojo.Project;
import com.zervice.kbase.database.pojo.ProjectComponent;
import com.zervice.kbase.database.utils.DaoUtils;
import com.zervice.kbase.database.utils.PageResult;
import com.zervice.kbase.service.ChatService;
import lombok.Cleanup;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author xh
 */
@Service
public class ChatServiceImpl implements ChatService {


    @Override
    public Chat save(String dbName, Chat chat) {
        Chat dbChat = ChatDao.save(dbName, chat);
        ChatCache.getInstance().put(dbName,  dbChat);
        return dbChat;
    }

    @Override
    public PageResult<RestChat> get(String dbName, ChatCriteria criteria, PageRequest pageRequest) throws Exception {
        PageResult<Chat> page = ChatDao.get(dbName, criteria, pageRequest);

        @Cleanup Connection conn = DaoUtils.getConnection(true);

        // 转换成 id -> component map
        Map<String/*root component id*/, ProjectComponent /*root component*/> rootMap =
                ProjectComponentDao.getByTypes(conn, dbName, ProjectComponent.TYPE_ROOTS).stream()
                        .collect(Collectors.toMap(ProjectComponent::getId, c ->c));

        List<RestChat> chats = page.getData().stream()
                .map(chat -> {
                    List<RestChat.RootComponent> rootComponents = _convertChatRestRoots(chat, rootMap);
                    return new RestChat(chat, rootComponents, 0L);
                })
                .collect(Collectors.toList());
        return PageResult.of(chats, page.getTotalCount());
    }

    private List<RestChat.RootComponent> _convertChatRestRoots(Chat chat, Map<String, ProjectComponent> rootMap) {
        Set<String> rootComponentIds = chat.getProperties().getRootComponentIds();
        if (CollectionUtils.isEmpty(rootComponentIds)) {
            return Collections.emptyList();
        }

        return rootComponentIds.stream()
                .map(id -> {
                    // 默认回复专属的
                    if (id != null && id.startsWith(Constants.FALLBACK_BOT_ID)) {
                        // fallback-talk2bits 这后面是小类，默认方成text
                        String[] fallbackParts = id.split("-");
                        String type = Project.Prop.FALLBACK_TYPE_TEXT;
                        if (fallbackParts.length >= 2) {
                            type = fallbackParts[1];
                        }

                        return RestChat.RootComponent.builder()
                                .id(id).name(MessageUtils.get(Constants.I18N_FALLBACK_NAME)).type(type)
                                .build();
                    }

                    // llm /kbqa
                    if (MessageRes.OUTPUT_TYPE_KBQA.equals(id) || MessageRes.OUTPUT_TYPE_LLM.equals(id)) {
                        String name = MessageUtils.get(Constants.I18N_LLM_NAME);
                        return RestChat.RootComponent.builder()
                                .id(id).name(name)
                                .build();
                    }
                    ProjectComponent rootComponent = rootMap.get(id);
                    String name = rootComponent == null ? MessageUtils.get(Constants.I18N_DELETED) : rootComponent.parseName();
                    String type = rootComponent == null ? null : rootComponent.getType();
                    return RestChat.RootComponent.builder()
                            .id(id).name(name).type(type)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    public Chat get(String dbName, String id) {
        return ChatDao.get(dbName, id);
    }
}
