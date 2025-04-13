package com.zervice.kbase.api.restful.controller;

import com.zervice.common.pojo.common.EmptyResponse;
import com.zervice.common.utils.Maps;
import com.zervice.kbase.api.BaseController;
import com.zervice.kbase.api.restful.AuthFilter;
import com.zervice.kbase.api.restful.pojo.RestPublishedProject;
import com.zervice.kbase.database.pojo.PublishedProject;
import com.zervice.kbase.database.utils.DaoUtils;
import com.zervice.kbase.service.PublishedProjectService;
import lombok.Cleanup;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.sql.Connection;

/**
 * @author Peng Chen
 * @date 2022/8/15
 */
@Log4j2
@RestController
@RequestMapping("api/published/project")
public class PublishedProjectController extends BaseController {

    @Autowired
    private PublishedProjectService publishedProjectService;

    /**
     * 账号下的debug容器是否准备ok
     */
    @GetMapping("/debug/status")
    public Object getDebugStatus(@RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName) throws Exception {
        @Cleanup Connection conn = DaoUtils.getConnection(true);
        String debugPublishedProjectId = PublishedProject.generateDebugPublishId(dbName);
        RestPublishedProject publishedProject = publishedProjectService.get(debugPublishedProjectId, conn, dbName);
        if (publishedProject != null && PublishedProject.STATUS_RUNNING.equals(publishedProject.getStatus())) {
            return Maps.of("ok", "true");
        }
        return EmptyResponse.empty();
    }

    @GetMapping("{id}")
    public Object get(@PathVariable String id,
                      @RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName) throws Exception {
        // this is a project id，convert it to a published project id
        if (!id.startsWith(dbName)) {
            id = PublishedProject.generateId(dbName, id);
            LOG.debug("[{}][convert project id to published project id:{}]", dbName, id);
        }
        @Cleanup Connection conn = DaoUtils.getConnection(true);

        // 如果没有发布，前端期望收到EmptyResponse而不是报错
        RestPublishedProject publishedProject = publishedProjectService.get(id, conn, dbName);
        return publishedProject == null ? EmptyResponse.empty() : publishedProject;
    }
}
