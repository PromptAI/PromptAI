package com.zervice.kbase.service;

import com.zervice.kbase.api.restful.pojo.ChatCriteria;
import com.zervice.kbase.api.restful.pojo.PageRequest;
import com.zervice.kbase.api.restful.pojo.RestChat;
import com.zervice.kbase.database.pojo.Chat;
import com.zervice.kbase.database.utils.PageResult;


public interface ChatService {

    /**
     * 保存chat
     * @param dbName
     * @param chat
     * @return
     */
    Chat save(String dbName, Chat chat);

    /**
     * 查询chat
     * @param dbName
     * @param criteria
     * @return
     */
    PageResult<RestChat> get(String dbName, ChatCriteria criteria, PageRequest pageRequest) throws Exception;

    /**
     * 查询chat
     * @param dbName db
     * @param id chatId
     * @return
     */
    Chat get(String dbName, String id);
}
