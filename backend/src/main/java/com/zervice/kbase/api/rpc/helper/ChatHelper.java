package com.zervice.kbase.api.rpc.helper;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.zervice.common.SpringContext;
import com.zervice.kbase.AccountCatalog;
import com.zervice.kbase.api.restful.pojo.RestProjectComponentConversation;
import com.zervice.kbase.cache.ChatCache;
import com.zervice.kbase.database.dao.ProjectComponentDao;
import com.zervice.kbase.database.dao.ProjectDao;
import com.zervice.kbase.database.dao.PublishSnapshotDao;
import com.zervice.kbase.database.pojo.*;
import com.zervice.kbase.database.utils.DaoUtils;
import com.zervice.kbase.service.ProjectComponentService;
import com.zervice.kbase.service.ProjectService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Cleanup;
import lombok.NoArgsConstructor;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;

import java.sql.Connection;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 对Chat相关的快照支持
 *
 * @author chenchen
 * @Date 2023/12/7
 */
@Log4j2
@UtilityClass
public class ChatHelper {


    /**
     * snapshot过期时间
     */
    private final Integer expirationInSec = 30 * 60;

    private Map<String, Map<Long, PublishSnapshotWrapper>> accountSnapshot = new ConcurrentHashMap<>();

    private ProjectComponentService _componentService = ProjectComponentService.getInstance();

    public void init() {
        _scheduleRemoveExpireSnapshotTask();
    }
    private void _scheduleRemoveExpireSnapshotTask() {
        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setDaemon(true)
                .setNameFormat("cleanup-expire-snapshot")
                .build();

        Executors.newSingleThreadScheduledExecutor(threadFactory).scheduleAtFixedRate(() -> {
            try {
                for (Map.Entry<String, Map<Long, PublishSnapshotWrapper>> accountSnapshotCache : accountSnapshot.entrySet()) {
                    accountSnapshotCache.getValue()
                            .entrySet()
                            .removeIf(accountChat -> accountChat.getValue().expired(expirationInSec));
                }
            } catch (Exception e) {
                LOG.error("Fail to remove expired snapshots", e);
            }
        }, 10, expirationInSec, TimeUnit.SECONDS);
    }

    /**
     * 获取project - before create chat
     */
    public Project project(String scene, String projectId, String publishedProjectId, String dbName) throws Exception {
        PublishSnapshot snapshot = _snapshotFromScene(scene, publishedProjectId, dbName);
        return _project(snapshot, projectId, dbName);
    }

    /**
     * 获取project - after chat created
     */
    public Project project(String chatId, String projectId, String dbName) throws Exception {
        PublishSnapshot snapshot = _snapshotFromChat(chatId, dbName);
        return _project(snapshot, projectId, dbName);
    }

    /**
     * Qdrant搜索的时候用的projectId，
     * - 如果有快照，返回该快照关联的projectId
     * - 否则，返回参数给到的projectId
     */
    public String qdrantProjectId(String chatId, String projectId, String dbName) throws Exception {
        String qdrantProjectId = projectId;

        // try from snapshot
        PublishSnapshot snapshot = _snapshotFromChat(chatId, dbName);
        if (snapshot != null) {
            qdrantProjectId = snapshot.getProperties().getSnapshot().getQdrantProjectId();
        }

        return qdrantProjectId == null ? projectId : qdrantProjectId;
    }


    public List<ProjectComponent> getByType(String type, String projectId, String chatId, String dbName) throws Exception {
        PublishSnapshot snapshot = _snapshotFromChat(chatId, dbName);
        if (snapshot != null) {
            PublishSnapshot.Snapshot s = snapshot.getProperties().getSnapshot();
            return s.getComponents().stream()
                    .filter(p -> type.equals(p.getType()))
                    .collect(Collectors.toList());
        }

        // try from db
        @Cleanup Connection conn = DaoUtils.getConnection(true);
        return ProjectComponentDao.getByProjectIdAndType(conn, dbName, projectId, type);
    }

    public PublishedProject publishedProject(String publishedProjectId, String dbName) {
        return AccountCatalog.ensure(dbName).getPublishedProjects().get(publishedProjectId);
    }

    public List<ProjectComponent> children(String id, String chatId, String dbName) throws Exception {
        PublishSnapshot snapshot = _snapshotFromChat(chatId, dbName);
        if (snapshot != null) {
            PublishSnapshot.Snapshot s = snapshot.getProperties().getSnapshot();
            ProjectComponent rootComponent = s.getById(id);
            if (rootComponent == null) {
                return null;
            }

            return _componentService.loadChildren(rootComponent, s.getComponents());
        }

        return _componentService.loadChildren(id, dbName);
    }

    public List<ProjectComponent> getDeployedComponents(String chatId, String publishedProjectId, String dbName) throws Exception {
        List<String> publishedRootIds = _publishedRoots(publishedProjectId, dbName);
        if (publishedRootIds.isEmpty()) {
            return Collections.emptyList();
        }

        PublishSnapshot snapshot = _snapshotFromChat(chatId, dbName);
        if (snapshot != null) {
            PublishSnapshot.Snapshot s = snapshot.getProperties().getSnapshot();

            List<ProjectComponent> publishedRoots = s.getByIds(publishedRootIds);
            List<ProjectComponent> children = new ArrayList<>();
            for (ProjectComponent root : publishedRoots) {
                List<ProjectComponent> components = _componentService.loadChildren(root, s.getComponents());
                if (CollectionUtils.isNotEmpty(components)) {
                    children.addAll(components);
                }
            }

            return children;
        }

        @Cleanup Connection conn = DaoUtils.getConnection(true);

        return projectService().getDeployedComponents(publishedProjectId, conn, dbName);
    }

    public ProjectComponent component(String chatId, String id, String dbName) throws Exception{
        PublishSnapshot snapshot = _snapshotFromChat(chatId, dbName);
        if (snapshot != null) {
            PublishSnapshot.Snapshot s = snapshot.getProperties().getSnapshot();
            return s.getById(id);
        }

        @Cleanup Connection conn = DaoUtils.getConnection(true);
        return ProjectComponentDao.get(conn, dbName, id);
    }
    public List<ProjectComponent> components(String chatId, String projectId, String dbName) throws Exception {
        PublishSnapshot snapshot = _snapshotFromChat(chatId, dbName);
        if (snapshot != null) {
            PublishSnapshot.Snapshot s = snapshot.getProperties().getSnapshot();
            return s.getComponents();
        }

        @Cleanup Connection conn = DaoUtils.getConnection(true);
        return _componentService.getProjectComponentWithInternalEntity(projectId, conn, dbName);
    }

    public List<ProjectComponent> components(String chatId, Collection<String> ids, String dbName) throws Exception {
        PublishSnapshot snapshot = _snapshotFromChat(chatId, dbName);
        if (snapshot != null) {
            PublishSnapshot.Snapshot s = snapshot.getProperties().getSnapshot();

            return s.getByIds(ids);
        }

        @Cleanup Connection conn = DaoUtils.getConnection(true);
        return ProjectComponentDao.getByIds(conn, dbName, new ArrayList<>(ids));
    }

    public Map<String, ProjectComponent> componentMap(String chatId, List<String> ids, String dbName) throws Exception{
        PublishSnapshot snapshot = _snapshotFromChat(chatId, dbName);
        if (snapshot != null) {
            PublishSnapshot.Snapshot s = snapshot.getProperties().getSnapshot();

            List<ProjectComponent> components = s.getByIds(ids);
            return components.stream()
                    .collect(Collectors.toMap(ProjectComponent::getId, c -> c));
        }


        @Cleanup Connection conn = DaoUtils.getConnection(true);

       return ProjectComponentDao.getByIds(conn, dbName, ids).stream()
                .collect(Collectors.toMap(ProjectComponent::getId, c -> c));
    }

    public List<ProjectComponent> getByRootComponentIdAndTypeWithLimit(String rootId, String type, Integer limit,
                                                                       String chatId, String dbName) throws Exception {
        PublishSnapshot snapshot = _snapshotFromChat(chatId, dbName);
        if (snapshot != null) {
            PublishSnapshot.Snapshot s = snapshot.getProperties().getSnapshot();

            // search matched children
            List<ProjectComponent> children = s.getComponents().stream()
                    .filter(c -> rootId.equals(c.getRootComponentId()) && type.equals(c.getType()))
                    .collect(Collectors.toList());

            // if limited
            if (limit > 0) {
               children = children.stream()
                        .limit(limit)
                        .collect(Collectors.toList());
            }

            return children;
        }

        @Cleanup Connection conn = DaoUtils.getConnection(true);
        return ProjectComponentDao.getByRootComponentIdAndTypeWithLimit(conn, dbName, rootId, type, limit);
    }

    /**
     * contains system internal entities
     */
    public Set<ProjectComponent> projectEntities(String chatId, String projectId, String dbName) throws Exception {
        PublishSnapshot snapshot = _snapshotFromChat(chatId, dbName);
        return _projectEntities(snapshot, projectId, dbName);
    }
    /**
     * contains system internal entities
     */
    public Set<ProjectComponent> projectEntities(String scene, String projectId, String publishedProjectId, String dbName) throws Exception {
        PublishSnapshot snapshot = _snapshotFromScene(scene, publishedProjectId, dbName);
        return _projectEntities(snapshot, projectId, dbName);
    }

    private Set<ProjectComponent> _projectEntities(PublishSnapshot snapshot, String projectId, String dbName) throws Exception {
        if (snapshot != null) {
            PublishSnapshot.Snapshot s = snapshot.getProperties().getSnapshot();

            return s.getComponents().stream()
                    .filter(c -> ProjectComponent.TYPE_ENTITY.equals(c.getType()))
                    .collect(Collectors.toSet());
        }

        @Cleanup Connection conn = DaoUtils.getConnection(true);

        // load projects
        List<ProjectComponent> projectEntities = ProjectComponentDao.getByProjectIdAndType(conn, dbName, projectId, ProjectComponent.TYPE_ENTITY);
        return new HashSet<>(projectEntities);
    }

    /**
     * deployd conversation
     *
     * @param publishedProjectId
     * @param dbName
     * @return
     * @throws Exception
     */
    public List<RestProjectComponentConversation> deployedConversations(String scene, String publishedProjectId, String dbName) throws Exception {
        List<String> publishedRoots = _publishedRoots(publishedProjectId, dbName);
        if (publishedRoots.isEmpty()) {
            return Collections.emptyList();
        }

        // read from snapshot
        PublishSnapshot snapshot = _snapshotFromScene(scene, publishedProjectId, dbName);
        if (snapshot != null) {
            PublishSnapshot.Snapshot s = snapshot.getProperties().getSnapshot();

            return s.getByIds(publishedRoots).stream()
                    .filter(c -> ProjectComponent.TYPE_CONVERSATION.equals(c.getType()))
                    .map(RestProjectComponentConversation::new)
                    .collect(Collectors.toList());
        }

        // read from db
        @Cleanup Connection conn = DaoUtils.getConnection(true);
        return projectService().getDeployedConversations(publishedProjectId, conn, dbName);
    }

    private List<String> _publishedRoots(String publishedProjectId, String dbName) {
        PublishedProject publishedProject = AccountCatalog.ensure(dbName).getPublishedProjects().get(publishedProjectId);
        if (publishedProject == null) {
            return Collections.emptyList();
        }
        List<String> roots = publishedProject.getProperties().getComponentIds();
        if (roots == null) {
            return Collections.emptyList();
        }
        return roots;
    }

    /**
     * after chat created
     */
    private PublishSnapshot _snapshotFromChat(String chatId, String dbName) throws Exception{
        Chat chat = ChatCache.getInstance().get(dbName, chatId);

        // read from db
        if (!chat.readFromSnapshot()) {
            return null;
        }

        String publishedProjectId = chat.getProperties().getPublishedProjectId();
        return _snapshot(publishedProjectId, dbName);
    }

    /**
     * before create chat
     */
    private PublishSnapshot _snapshotFromScene(String scene, String publishedProjectId, String dbName) throws Exception{
        if (!Chat.readFromSnapshot(scene)) {
            return null;
        }

        return _snapshot(publishedProjectId, dbName);
    }

    private PublishSnapshot _snapshot(String publishedProjectId,String dbName) throws Exception {
        PublishedProject publishedProject = AccountCatalog.ensure(dbName).getPublishedProjects().get(publishedProjectId);
        if (publishedProject == null) {
            return null;
        }

        Long snapshotId = publishedProject.getProperties().getPublishSnapshotId();
        PublishSnapshot snapshot = snapshotId == null ? null : _loadSnapshotIfExits(snapshotId, dbName);
        if (snapshot == null) {
            LOG.info("[{}-Debug][failed read:{} from:{}]", dbName, "snapshot", "cache");
        } else {
            LOG.info("[{}-Debug][success read:{} from:{}]", dbName, "snapshot", "cache");
        }

        return snapshot;
    }

    private PublishSnapshot _loadSnapshotIfExits(Long snapshotId, String dbName) throws Exception {
        accountSnapshot.putIfAbsent(dbName, new ConcurrentHashMap<>());
        Map<Long, PublishSnapshotWrapper> snapshotMap = accountSnapshot.get(dbName);
        if (snapshotMap.containsKey(snapshotId)) {
            LOG.info("[{}-Debug][read:{} from:{}]", dbName, "snapshot", "cache");
            return snapshotMap.get(snapshotId).get();
        }

        synchronized (snapshotMap) {
            if (snapshotMap.containsKey(snapshotId)) {
                return snapshotMap.get(snapshotId).get();
            }

            @Cleanup Connection conn = DaoUtils.getConnection(true);
            PublishSnapshot snapshot = PublishSnapshotDao.get(conn, dbName, snapshotId);
            if (snapshot != null) {
                snapshotMap.put(snapshotId, PublishSnapshotWrapper.wrap(snapshot));
                LOG.info("[{}-Debug][read:{} from:{}]", dbName, "snapshot", "db");
            } else {
                LOG.error("[{}-Debug][failed toread:{} from:{} --id:{}]", dbName, "snapshot", "db", snapshotId);
            }
        }

        return snapshotMap.get(snapshotId).get();
    }

    private Project _project(PublishSnapshot snapshot, String projectId, String dbName) throws Exception {
        if (snapshot != null) {
            LOG.info("[{}-Debug][read:{} from:{}]", dbName, "project", "snapshot");
            return snapshot.getProperties().getSnapshot().getProject();
        }

        LOG.info("[{}-Debug][read:{} from:{}]", dbName, "project", "db");
        // try from db
        @Cleanup Connection conn = DaoUtils.getConnection(true);
        return ProjectDao.get(conn, dbName, projectId);
    }


    private ProjectService projectService() {
        return SpringContext.getAppContext().getBean(ProjectService.class);
    }

    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PublishSnapshotWrapper {

        private PublishSnapshot _snapshot;
        private Long _lastAccessAt;

        public static PublishSnapshotWrapper wrap(PublishSnapshot snapshot) {
            return PublishSnapshotWrapper.builder()
                    .snapshot(snapshot).lastAccessAt(System.currentTimeMillis())
                    .build();
        }

        public PublishSnapshot get() {
            _lastAccessAt = System.currentTimeMillis();
            return _snapshot;
        }

        public boolean expired(Integer aliveInSec) {
            long now = System.currentTimeMillis();
            return now - _lastAccessAt > TimeUnit.SECONDS.toMillis(aliveInSec);
        }
    }
}
