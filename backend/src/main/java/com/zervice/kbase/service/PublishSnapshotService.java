package com.zervice.kbase.service;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.zervice.common.ding.DingTalkSender;
import com.zervice.common.pojo.common.Account;
import com.zervice.common.restful.exception.RestException;
import com.zervice.common.restful.exception.StatusCodes;
import com.zervice.common.utils.AccountService;
import com.zervice.kbase.ServerInfo;
import com.zervice.kbase.database.dao.ProjectDao;
import com.zervice.kbase.database.dao.PublishSnapshotDao;
import com.zervice.kbase.database.pojo.*;
import com.zervice.kbase.database.utils.DaoUtils;
import lombok.Cleanup;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * published project release...
 *
 * @author chenchen
 * @Date 2023/12/4
 */
@Log4j2
@Service
public class PublishSnapshotService {

    private final Map<String , Map<Long /*snapshot_id*/, Future<?>/* handle_snapshot_task*/>> accountSnapshotMap = new ConcurrentHashMap<>();
    @Autowired
    private AccountService accountService;
    @Autowired
    private PublishRecordService recordService;
    private ProjectComponentService componentService = ProjectComponentService.getInstance();
    private ExecutorService executor = new ThreadPoolExecutor(1, 10,
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>());
    private  ScheduledExecutorService checkExecutors = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder()
            .setNameFormat("publish-snapshot-task-checker")
            .setDaemon(true)
            .build());

    {
        checkExecutors.scheduleAtFixedRate(this::_checkFuture, 0, 2, TimeUnit.SECONDS);
    }

    private void _checkFuture() {
        for (Map.Entry<String, Map<Long, Future<?>>> entry : accountSnapshotMap.entrySet()) {
            entry.getValue().values().removeIf(Future::isDone);
        }
    }

    public void init() {
        try {
            @Cleanup Connection conn = DaoUtils.getConnection(true);
            for (Account account : accountService.getAll()) {
                String dbName = account.getDbName();

                List<PublishSnapshot> running = PublishSnapshotDao.getByStatus(conn, dbName, PublishSnapshot.STATUS_RUNNING);
                if (running.isEmpty()) {
                    continue;
                }

                Map<Long, Future<?>> snapshots = new ConcurrentHashMap<>();
                accountSnapshotMap.put(dbName, snapshots);

                for (PublishSnapshot r : running) {
                    Future<?> task = run(r, dbName);
                    snapshots.put(r.getId(), task);
                }
            }
        } catch (Exception e) {
            LOG.error("[[init snapshot when start with error:{} ]", e.getMessage(), e);
            DingTalkSender.sendQuietly(String.format("[%s][init snapshot when start with error:%s]", ServerInfo.getName(), e.getMessage()));
        }
    }


    /**
     * create a snapshot and async run
     */
    public PublishSnapshot generate(PublishRecord publishRecord, long userId,
                                    String dbName, Connection conn) throws Exception {
        Long publishRecordId = publishRecord.getId();
        String projectId = publishRecord.getProjectId();
        String publishedProjectId = publishRecord.getPublishedProjectId();

        PublishSnapshot.Prop prop = PublishSnapshot.Prop.builder()
                .publishRecordId(publishRecordId).createBy(userId)
                .createBy(System.currentTimeMillis())
                .build();

        PublishSnapshot snapshot = PublishSnapshot.builder()
                .projectId(projectId)
                .publishedProjectId(publishedProjectId)
                .tag(PublishSnapshot.generateTag())
                .status(PublishSnapshot.STATUS_INIT)
                .properties(prop)
                .build();

        long id = PublishSnapshotDao.add(conn, dbName, snapshot);
        snapshot.setId(id);
        return snapshot;
    }


    /**
     * start a snapshot async
     */
    public synchronized Future<?> run(PublishSnapshot snapshot, String dbName) {
        Long id = snapshot.getId();

        // make account's snapshot already exists
        accountSnapshotMap.putIfAbsent(dbName, new ConcurrentHashMap<>());

        Map<Long, Future<?>> snapshots = accountSnapshotMap.get(dbName);

        // already running，return the task
        if (snapshots.containsKey(id)) {
            return snapshots.get(id);
        }

        // submit new task
        Future<?> future = executor.submit(() -> _run(snapshot, dbName));
        snapshots.put(id, future);
        return future;
    }

    /**
     * handle snapshot
     */
    public void _run(PublishSnapshot publishSnapshot, String dbName) {
        Long id = publishSnapshot.getId();
        Long publishRecordId = publishSnapshot.getProperties().getPublishRecordId();
        String publishedProjectId = publishSnapshot.getPublishedProjectId();

        // mark start
        recordService.runStage(publishRecordId, PublishRecord.Stage.NAME_SNAPSHOT, publishedProjectId, dbName);

        LOG.info("[{}][start to run snapshot:{}]", dbName, id);
        try {
            // not for run
            if (!PublishSnapshot.STATUS_INIT.equals(publishSnapshot.getStatus())) {
                LOG.error("[{}][run snapshot:{} with error status:{}]", dbName, id, publishSnapshot.getStatus());
                throw new RestException(StatusCodes.InternalError);
            }

            String projectId = publishSnapshot.getProjectId();

            // read project、component related
            PublishSnapshot.Snapshot snapshot = _buildSnapshot(projectId, dbName);
            publishSnapshot.getProperties().setSnapshot(snapshot);

            // mark ready
            publishSnapshot.ready();

            // update 2 db
            _updatePublishSnapshot2DB(publishSnapshot, dbName);
            LOG.info("[{}][success run snapshot:{} ]", dbName, id);
        } catch (Exception e) {
            // mark error
            publishSnapshot.error(e.getMessage());

            // update 2 db
            _updatePublishSnapshot2DB(publishSnapshot, dbName);
            LOG.error("[{}][run snapshot:{} with error:{}]", dbName, id, e.getMessage(), e);
        }

        try {
            String msg = publishSnapshot.getProperties().getLastMessage();
            recordService.afterSnapshotEnd(publishSnapshot, msg, dbName);
        } catch (Exception re) {
            LOG.error("[{}][handle snapshot:{} end with error:{}]", dbName, id, re.getMessage(), re);
        }
    }

    private void _updatePublishSnapshot2DB(PublishSnapshot snapshot, String dbName)  {
        try {
            @Cleanup Connection conn = DaoUtils.getConnection(true);
            PublishSnapshotDao.update(conn, dbName, snapshot);
        } catch (Exception e) {
            LOG.error("[{}][update snapshot:{} with error:{}]", dbName, snapshot.getId(), e.getMessage(), e);
            throw new RestException(StatusCodes.InternalError);
        }
    }


    /**
     * build snapshot with required project & components
     */
    private PublishSnapshot.Snapshot _buildSnapshot(String projectId, String dbName) throws Exception {
        @Cleanup Connection conn = DaoUtils.getConnection(true);
        Project project = ProjectDao.get(conn, dbName, projectId);
        List<ProjectComponent> projectComponents = componentService.getProjectComponentWithInternalEntity(projectId, conn, dbName);

        List<ProjectComponent> flows = projectComponents.stream()
                .filter(c -> ProjectComponent.TYPE_CONVERSATION.equals(c.getType()))
                .collect(Collectors.toList());


        return PublishSnapshot.Snapshot.builder()
                .project(project).components(flows)
                .components(projectComponents)
                .build();
    }

}
