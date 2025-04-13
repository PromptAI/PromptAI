package com.zervice.kbase.service;

import com.zervice.kbase.api.restful.pojo.RestMessage;
import com.zervice.kbase.database.pojo.Message;

import java.util.List;


public interface MessageService {
    /**
     * 保存message
     *
     * @param message
     * @param dbName
     * @return
     */
    Message save(String dbName, Message message) throws Exception;


    /**
     * 查询message
     */
    List<RestMessage> get(String dbName, String chatId) throws Exception;
}
