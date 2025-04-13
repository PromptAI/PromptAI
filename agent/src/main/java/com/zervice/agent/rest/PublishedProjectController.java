package com.zervice.agent.rest;

import com.alibaba.fastjson.JSONObject;
import com.zervice.agent.published.project.PublishedProjectPojo;
import com.zervice.agent.service.PublishedProjectService;
import com.zervice.common.pojo.common.EmptyResponse;
import com.zervice.common.utils.Constants;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author Peng Chen
 * @date 2022/8/4
 */
@Log4j2
@RestController
@RequestMapping("/api/publish/project")
public class PublishedProjectController {

    @Autowired
    private PublishedProjectService publishedProjectService;

    @GetMapping("{publishedProjectId}")
    public Object get(@PathVariable String publishedProjectId,
                      @RequestParam(defaultValue = "true") Boolean report) {
        PublishedProjectPojo pojo = publishedProjectService.get(publishedProjectId, report);
        return pojo == null ? EmptyResponse.empty() : pojo;
    }

    /**
     * start new published project
     */
    @PostMapping
    public Object run(@RequestBody JSONObject body) {
        publishedProjectService.run(body);
        return EmptyResponse.empty();
    }

    @PostMapping("publish")
    public Object publish(@RequestPart MultipartFile file,
                          @RequestHeader(Constants.AGENT_PUBLISHED_PROJECT_HEADER) String publishedProjectId) {
        publishedProjectService.publish(file, publishedProjectId);
        return EmptyResponse.empty();
    }

    @DeleteMapping("{publishedProjectId}")
    public Object stop(@PathVariable String publishedProjectId) {
        publishedProjectService.stop(publishedProjectId);
        return EmptyResponse.empty();
    }
}
