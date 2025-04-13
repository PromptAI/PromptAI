package com.zervice.kbase.api.restful.controller;

import com.zervice.common.pojo.common.EmptyResponse;
import com.zervice.common.restful.exception.RestException;
import com.zervice.common.restful.exception.StatusCodes;
import com.zervice.kbase.api.restful.AuthFilter;
import com.zervice.kbase.api.restful.pojo.RestPublishRecord;
import com.zervice.kbase.database.dao.PublishRecordDao;
import com.zervice.kbase.database.pojo.PublishRecord;
import com.zervice.kbase.database.pojo.PublishedProject;
import com.zervice.kbase.database.pojo.User;
import com.zervice.kbase.database.utils.DaoUtils;
import com.zervice.kbase.service.PublishedProjectService;
import lombok.Cleanup;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.sql.Connection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author chenchen
 * @Date 2024/10/8
 */
@Log4j2
@RestController
@RequestMapping("/api/publish/record")
public class PublishRecordController {


    @Autowired
    private PublishedProjectService publishedProjectService;

    @GetMapping("running")
    public Object running(@RequestParam(required = false) String componentId,
                          @RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName,
                          @RequestHeader(AuthFilter.X_EXTERNAL_USER_ID_HEADER) String uid) throws Exception {
        String publishedProjectId = PublishedProject.generateId(dbName, PublishedProject.TEST_PROJECT_ID_SUFFIX);
        @Cleanup Connection conn = DaoUtils.getConnection(true);

        // 这里查询执行中的
        List<PublishRecord> runningTasks = PublishRecordDao.getByPublishedProjectIdAndStatus(conn, dbName, publishedProjectId, PublishRecord.STATUS_RUNNING);

        return runningTasks.stream()
                .filter(r -> {
                    List<String> publishRoots = r.getProperties().getPublishRoots();

                    boolean matchPublishedProjectId = r.getPublishedProjectId().equals(publishedProjectId);
                    boolean matchComponentId = componentId == null || CollectionUtils.isEmpty(publishRoots) || publishRoots.contains(componentId);
                    return matchPublishedProjectId && matchComponentId;
                })
                .map(t -> new RestPublishRecord(t, dbName))
                .collect(Collectors.toList());
    }

    @PutMapping("cancel/{publishRecordId}")
    public Object cancel(@PathVariable Long publishRecordId,
                         @RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName,
                         @RequestHeader(AuthFilter.X_EXTERNAL_USER_ID_HEADER) String uid) throws Exception {
        long userId = User.fromExternalId(uid);
        @Cleanup Connection conn = DaoUtils.getConnection(true);
        PublishRecord record = PublishRecordDao.get(conn, dbName, publishRecordId);
        if (record == null) {
            throw new RestException(StatusCodes.AiAgentTaskNotFound, "record not found");
        }

        if (PublishRecord.STATUS_RUNNING.equals(record.getStatus())) {
            throw new RestException(StatusCodes.AiAgentTaskNotExecutingOrSchedule, "record not in EXECUTING");
        }

        record.cancel(userId);
        PublishRecordDao.update(conn, dbName, record);

        // published project stop
        publishedProjectService.stop(record.getPublishedProjectId(), userId, dbName);

        return EmptyResponse.empty();
    }

    @GetMapping("{id}")
    public Object get(@PathVariable Long id,
                      @RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName,
                      @RequestHeader(AuthFilter.X_EXTERNAL_USER_ID_HEADER) String uid) throws Exception {
        @Cleanup Connection conn = DaoUtils.getConnection(true);
        PublishRecord publishRecord = PublishRecordDao.get(conn, dbName, id);
        if (publishRecord == null) {
            LOG.info("[{}][publishRecord not found:{}]", dbName, id);
            throw new RestException(StatusCodes.AiAgentTaskNotFound, "not found");
        }

        return new RestPublishRecord(publishRecord, dbName);
    }
}
