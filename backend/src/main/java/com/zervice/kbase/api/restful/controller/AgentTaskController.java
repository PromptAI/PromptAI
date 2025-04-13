package com.zervice.kbase.api.restful.controller;

import com.zervice.common.pojo.common.EmptyResponse;
import com.zervice.common.restful.exception.RestException;
import com.zervice.common.restful.exception.StatusCodes;
import com.zervice.kbase.api.BaseController;
import com.zervice.kbase.api.restful.AuthFilter;
import com.zervice.kbase.api.restful.pojo.RestAgentTask;
import com.zervice.kbase.database.dao.AgentTaskDao;
import com.zervice.kbase.database.pojo.AgentTask;
import com.zervice.kbase.database.pojo.PublishedProject;
import com.zervice.kbase.database.pojo.User;
import com.zervice.kbase.database.utils.DaoUtils;
import com.zervice.kbase.service.AgentService;
import lombok.Cleanup;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.sql.Connection;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Peng Chen
 * @date 2022/6/16
 */
@Log4j2
@RequestMapping("/api/agent/task")
@RestController
public class AgentTaskController extends BaseController {

    @Autowired
    private AgentService agentService;

    @GetMapping("running")
    public Object running(@RequestParam(required = false) String componentId,
                          @RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName,
                          @RequestHeader(AuthFilter.X_EXTERNAL_USER_ID_HEADER) String uid) throws Exception {
        String publishedProjectId = PublishedProject.generateId(dbName, PublishedProject.TEST_PROJECT_ID_SUFFIX);
        @Cleanup Connection conn = DaoUtils.getConnection(true);

        // 这里查询预约和执行中的
        List<AgentTask> runningTasks = AgentTaskDao.getByStatusIn(conn, dbName, Arrays.asList(AgentTask.STATUS_EXECUTING,
                AgentTask.STATUS_SCHEDULE));

        return runningTasks.stream()
                .filter(t -> {
                    boolean matchPublishedProjectId = t.getPublishedProjectId().equals(publishedProjectId);
                    boolean matchComponentId = componentId == null || t.getProperties().getComponentIds() == null || t.getProperties().getComponentIds().contains(componentId);
                    return matchPublishedProjectId && matchComponentId;
                })
                .map(t -> new RestAgentTask(t, dbName))
                .collect(Collectors.toList());
    }

    @PutMapping("cancel/{taskId}")
    public Object cancel(@PathVariable Long taskId,
                         @RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName,
                         @RequestHeader(AuthFilter.X_EXTERNAL_USER_ID_HEADER) String uid) throws Exception {
        long userId = User.fromExternalId(uid);
        @Cleanup Connection conn = DaoUtils.getConnection(true);
        AgentTask task = AgentTaskDao.get(conn, dbName, taskId);
        if (task == null) {
            throw new RestException(StatusCodes.AiAgentTaskNotFound, "task not found");
        }

        if (task.getStatus() != AgentTask.STATUS_SCHEDULE && task.getStatus() != AgentTask.STATUS_EXECUTING) {
            throw new RestException(StatusCodes.AiAgentTaskNotExecutingOrSchedule, "task not in EXECUTING or SCHEDULE");
        }

        agentService.cancelQuietly(task, dbName, conn);

        task.cancel(userId);
        AgentTaskDao.updateStatusAndProp(conn, dbName, task);

        return EmptyResponse.empty();
    }

    @GetMapping("{id}")
    public Object get(@PathVariable Long id,
                      @RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName,
                      @RequestHeader(AuthFilter.X_EXTERNAL_USER_ID_HEADER) String uid) throws Exception {
        @Cleanup Connection conn = DaoUtils.getConnection(true);
        AgentTask agentTask = AgentTaskDao.get(conn, dbName, id);
        if (agentTask == null) {
            LOG.info("[{}][task not found:{}]", dbName, id);
            throw new RestException(StatusCodes.AiAgentTaskNotFound, "not found");
        }

        return new RestAgentTask(agentTask, dbName);
    }
}
