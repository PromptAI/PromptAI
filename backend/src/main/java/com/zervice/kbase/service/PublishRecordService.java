package com.zervice.kbase.service;

import com.zervice.kbase.AccountCatalog;
import com.zervice.kbase.database.dao.PublishRecordDao;
import com.zervice.kbase.database.dao.PublishedProjectDao;
import com.zervice.kbase.database.pojo.AgentTask;
import com.zervice.kbase.database.pojo.PublishRecord;
import com.zervice.kbase.database.pojo.PublishSnapshot;
import com.zervice.kbase.database.pojo.PublishedProject;
import com.zervice.kbase.database.utils.DaoUtils;
import lombok.Cleanup;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * handle release
 *
 * @author chenchen
 * @Date 2023/12/5
 */
@Log4j2
@Service
public class PublishRecordService {

    public void runStage(Long publishRecordId, String stageName, String publishedProjectId, String dbName) {
        PublishedProject lock = AccountCatalog.ensure(dbName).getPublishedProjects().get(publishedProjectId);
        try {
            synchronized (lock) {
                @Cleanup Connection conn = DaoUtils.getConnection(true);

                PublishRecord publishRecord = PublishRecordDao.get(conn, dbName, publishRecordId);
                if (publishRecord == null) {
                    LOG.error("[{}][handle stage start fail, publish record:{} not found]", dbName, publishRecordId);
                    return;
                }

                PublishRecord.Stage stage = publishRecord.getStage(stageName);
                if (stage == null) {
                    LOG.error("[{}][handle stage start fail, publish record:{} stage:{} not found]", dbName, publishRecordId, stageName);
                    return;
                }

                if (stage.isNotStart()) {
                    stage.start();
                    PublishRecordDao.updateProp(conn, dbName, publishRecord);
                    LOG.info("[{}][publish:{} stage:{} started]", dbName, publishRecordId, stageName);
                    return;
                }

                LOG.warn("[{}][publish:{} stage:{} can't be start with status:{}]", dbName, publishRecordId, stageName, stage.getStatus());
            }
        } catch (Exception e) {
            LOG.error("[{}][handle publish record:{} stage start with error:{}]", dbName, publishRecordId, e.getMessage(), e);
        }
    }
    /**
     * handle task end
     */
    public void afterTaskEnd(AgentTask task, String message, String dbName) throws Exception {
        String publishedProjectId = task.getPublishedProjectId();
        Long recordId = task.getProperties().getPublishRecordId();
        PublishedProject lock = AccountCatalog.ensure(dbName).getPublishedProjects().get(publishedProjectId);

        String stageName = PublishRecord.Stage.NAME_AGENT_TASK;
        synchronized (lock) {
            @Cleanup Connection conn = DaoUtils.getConnection(false);
            PublishRecord publishRecord = PublishRecordDao.get(conn, dbName, recordId);
            PublishRecord.Stage stage = publishRecord.getStage(stageName);

            // success run task
            if (task.isOk()) {
                stage.finish();

                // this task related model id
                Long modelId = task.getProperties().getRelatedModelId();
                if (modelId != null) {
                    publishRecord.getProperties().setRelatedModelId(modelId);
                }

                PublishRecordDao.update(conn, dbName, publishRecord);
                conn.commit();
                LOG.info("[{}][project published:{} success run stage:{} ]", dbName, recordId, stageName);

                // all stage finished
                if (publishRecord.isFinished()) {
                    publishRecord.success();
                    PublishRecordDao.update(conn, dbName, publishRecord);

                    _afterPublishSuccess(publishRecord, conn, dbName);
                    conn.commit();
                    LOG.info("[{}][project success published:{} end with task:{}]", dbName, recordId, task.getId());
                }

                return;
            }

            // error
            stage.failed(message);
            publishRecord.failed();
            PublishRecordDao.update(conn, dbName, publishRecord);
            conn.commit();
            LOG.info("[{}][project publish:{} failed while run agent task:{} with message:{}]", dbName, recordId, task.getId(), message);
        }
    }

    /**
     * handle snapshot end
     */
    public void afterSnapshotEnd(PublishSnapshot snapshot, String message, String dbName) throws Exception {
        String publishedProjectId = snapshot.getPublishedProjectId();
        Long recordId = snapshot.getProperties().getPublishRecordId();
        PublishedProject lock = AccountCatalog.ensure(dbName).getPublishedProjects().get(publishedProjectId);

        String stageName = PublishRecord.Stage.NAME_SNAPSHOT;
        synchronized (lock) {
            @Cleanup Connection conn = DaoUtils.getConnection(false);
            PublishRecord publishRecord = PublishRecordDao.get(conn, dbName, recordId);
            PublishRecord.Stage stage = publishRecord.getStage(stageName);

            // ok
            if (snapshot.isOk()) {
                stage.finish();
                publishRecord.getProperties().setRelatedSnapshotId(snapshot.getId());

                LOG.info("[{}][project published:{} success run stage:{} ]", dbName, recordId, stageName);
                PublishRecordDao.update(conn, dbName, publishRecord);
                conn.commit();

                // all stage finished
                if (publishRecord.isFinished()) {
                    publishRecord.success();
                    PublishRecordDao.update(conn, dbName, publishRecord);

                    _afterPublishSuccess(publishRecord, conn, dbName);

                    conn.commit();
                    LOG.info("[{}][project success published:{} end with snapshot:{}]", dbName, recordId, snapshot.getId());
                }
                return;
            }

            // error
            stage.failed(message);
            publishRecord.failed();
            PublishRecordDao.update(conn, dbName, publishRecord);
            conn.commit();
            LOG.info("[{}][project publish:{} failed while run snapshot:{} with message:{}]", dbName, recordId, snapshot.getId(), message);
        }
    }

    /**
     * update published project
     */
    private void _afterPublishSuccess(PublishRecord record, Connection conn, String dbName) throws Exception {
        String publishedProjectId = record.getPublishedProjectId();
        PublishedProject project = PublishedProjectDao.get(conn, dbName, publishedProjectId);
        project.setStatus(PublishedProject.STATUS_RUNNING);
        project.getProperties().setUpdateTime(System.currentTimeMillis());
        project.getProperties().setPublishingIds(Collections.emptyList());

        // refresh the latest published components
        List<String> publishedComponents = record.getProperties().getPublishRoots();
        if (CollectionUtils.isNotEmpty(publishedComponents)) {
            project.getProperties().setComponentIds(publishedComponents);
        }

        // refresh the latest model
        Long modelId = record.getProperties().getRelatedModelId();
        Long modelIdInProject = project.getProperties().getModelId();
        if (!Objects.equals(modelId, modelIdInProject)) {
            project.getProperties().setModelId(modelId);
            LOG.info("[{}][update published project:{} model to:{}]", dbName, project.getId(), modelId);
        }

        // refresh the latest snapshot
        Long snapshotId = record.getProperties().getRelatedSnapshotId();
        Long snapshotIdInProject = project.getProperties().getPublishSnapshotId();
        if (!Objects.equals(snapshotId, snapshotIdInProject)) {
            project.getProperties().setPublishSnapshotId(snapshotId);
            LOG.info("[{}][update published project:{} snapshot to:{}]", dbName, project.getId(), snapshotId);
        }

        PublishedProjectDao.updateStatusAndProp(conn, dbName, project);
        AccountCatalog.ensure(dbName).getPublishedProjects().onUpdate(project);
    }
}
