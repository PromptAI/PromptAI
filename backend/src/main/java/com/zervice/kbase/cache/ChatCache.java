package com.zervice.kbase.cache;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.zervice.kbase.database.dao.ChatDao;
import com.zervice.kbase.database.pojo.Chat;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;

import java.util.concurrent.TimeUnit;

@Log4j2
public class ChatCache {
    private static final String SEPARATOR = "_";
    private static final long CHAT_ID_MAXIMUM_SIZE = 10000;

    @Getter
    private static final ChatCache _instance = new ChatCache();

    private ChatCache() {
    }

    @Getter
    private LoadingCache<String, Chat> _cache = CacheBuilder.newBuilder()
            .expireAfterAccess(60 * 30, TimeUnit.SECONDS).maximumSize(CHAT_ID_MAXIMUM_SIZE)
            .build(new CacheLoader<String, Chat>() {
                @Override
                public Chat load(@NotNull String key) {
                    return null;
                }
            });

    public Chat get(String dbName, String chatId) {
        String id = _buildUniqueKey(dbName, chatId);
        Chat chat = _cache.getIfPresent(id);
        if (chat == null) {
            chat = ChatDao.get(dbName, chatId);
            put(dbName, chat);
        }
        return chat;
    }

    public void put(String dbName, Chat chat) {
        if (StringUtils.isEmpty(dbName)) {
            throw new IllegalArgumentException();
        }
        String id = _buildUniqueKey(dbName, chat.getId());
        _cache.put(id, chat);
    }

    private String _buildUniqueKey(String dbName, String chatId) {
        return dbName + SEPARATOR + chatId;
    }
}
