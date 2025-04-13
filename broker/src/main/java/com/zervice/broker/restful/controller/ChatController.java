package com.zervice.broker.restful.controller;

import com.zervice.broker.backend.KbChatService;
import com.zervice.broker.backend.PublishedProjectService;
import com.zervice.broker.restful.filter.BrokerFilter;
import com.zervice.broker.restful.pojo.CreateChatReq;
import com.zervice.common.agent.pojo.SimplePublishedProject;
import lombok.extern.log4j.Log4j2;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

/**
 * @author Peng Chen
 * @date 2022/7/5
 */
@Log4j2
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final KbChatService _kbChatService = KbChatService.getInstance();
    private final PublishedProjectService _publishedProjectService = PublishedProjectService.getInstance();

    @GetMapping("settings")
    public Object settings(@RequestHeader(value = BrokerFilter.X_EXTERNAL_PUBLISHED_PROJECT_HEADER) String publishedProjectId,
                           @RequestParam(required = false) String scene,
                           @RequestHeader(BrokerFilter.X_EXTERNAL_PUBLISHED_PROJECT_TOKEN_HEADER) String token) throws Exception {
        //TODO clear me for test purpose
        if (scene == null) {
            scene = "debug";
        }

        SimplePublishedProject project = _publishedProjectService.get(publishedProjectId,token);
        String dbName = project.getAccountDbName();
        return _kbChatService.settings(project.getId(), project.getProjectId(), scene, dbName);
    }

    @PostMapping
    public Object apply(HttpServletRequest request,
                        @RequestBody @Validated CreateChatReq chatReq,
                        @RequestHeader(value = BrokerFilter.X_EXTERNAL_PUBLISHED_PROJECT_HEADER) String publishedProjectId,
                        @RequestHeader(BrokerFilter.X_EXTERNAL_PUBLISHED_PROJECT_TOKEN_HEADER) String token) {
        SimplePublishedProject project = _publishedProjectService.get(publishedProjectId, token);
        String dbName = project.getAccountDbName();
        String projectId = project.getProjectId();

        chatReq.setProjectId(projectId);
        chatReq.setPublishedProjectId(publishedProjectId);

        return _kbChatService.apply(chatReq, request, dbName);
    }
}
