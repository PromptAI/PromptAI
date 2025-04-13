package com.zervice.kbase.api.restful.controller;

import com.zervice.kbase.api.BaseController;
import com.zervice.kbase.api.restful.AuthFilter;
import com.zervice.kbase.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/message")
public class MessageController extends BaseController {

    @Autowired
    private MessageService messageService;

    @GetMapping("{chatId}")
    public Object get(@PathVariable String chatId,
                      @RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName) throws Exception {
        return messageService.get(dbName, chatId);
    }
}
