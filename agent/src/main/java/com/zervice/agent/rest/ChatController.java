package com.zervice.agent.rest;

import com.alibaba.fastjson.JSONObject;
import com.zervice.agent.published.project.PublishedProjectPojo;
import com.zervice.agent.published.project.PublishedProjectManager;
import com.zervice.common.restful.exception.RestException;
import com.zervice.common.restful.exception.StatusCodes;
import com.zervice.common.utils.Constants;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.*;

/**
 * @author Peng Chen
 * @date 2022/6/20
 */
@Log4j2
@RequestMapping("api/chat")
@RestController
public class ChatController {

    private final PublishedProjectManager PROJECT_MANAGER = PublishedProjectManager.getInstance();

    @PostMapping("message")
    public Object message(@RequestBody String data,
                          @RequestHeader(Constants.AGENT_PUBLISHED_PROJECT_HEADER) String projectId) {
        PublishedProjectPojo project = PROJECT_MANAGER.getProject(projectId);
        if (project == null) {
            LOG.info("invalid published project:{}", projectId);
            throw new RestException(StatusCodes.BadRequest, "not project available");
        }

        return project.aiClient().chat(JSONObject.parseObject(data));
    }
}
