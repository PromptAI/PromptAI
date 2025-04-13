package com.zervice.kbase.api.restful.controller;

import com.zervice.kbase.api.BaseController;
import com.zervice.kbase.api.restful.AuthFilter;
import com.zervice.kbase.api.restful.pojo.ChatCriteria;
import com.zervice.kbase.api.restful.pojo.PageRequest;
import com.zervice.kbase.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
public class ChatController extends BaseController {

    @Autowired
    private ChatService chatService;

    @GetMapping()
    public Object get(ChatCriteria critical,
                      PageRequest pageRequest,
                      @RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName) throws Exception {
        return chatService.get(dbName, critical, pageRequest);
    }
}
