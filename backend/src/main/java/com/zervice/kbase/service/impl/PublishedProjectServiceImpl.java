package com.zervice.kbase.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.zervice.common.restful.exception.RestException;
import com.zervice.common.restful.exception.StatusCodes;
import com.zervice.kbase.AccountCatalog;
import com.zervice.kbase.api.restful.pojo.RestPublishRecord;
import com.zervice.kbase.api.restful.pojo.RestPublishedProject;
import com.zervice.kbase.database.dao.AgentTaskDao;
import com.zervice.kbase.database.dao.PublishRecordDao;
import com.zervice.kbase.database.dao.PublishedProjectDao;
import com.zervice.kbase.database.pojo.Agent;
import com.zervice.kbase.database.pojo.AgentTask;
import com.zervice.kbase.database.pojo.PublishRecord;
import com.zervice.kbase.database.pojo.PublishedProject;
import com.zervice.kbase.database.utils.DaoUtils;
import com.zervice.kbase.service.AgentService;
import com.zervice.kbase.service.AgentTaskService;
import com.zervice.kbase.service.PublishedProjectService;
import lombok.Cleanup;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Peng Chen
 * @date 2022/8/3
 */
@Log4j2
@Service
public class PublishedProjectServiceImpl implements PublishedProjectService {

    @Value("${published.project.recent.taskCount:5}")
    public Integer recentTaskCount;
    @Value("${published.record.recent.count:5}")
    public Integer recentRecordCount;

    @Autowired
    private AgentTaskService agentTaskService;
    @Autowired
    private AgentService agentService;


    @Override
    public RestPublishedProject get(String id, Connection conn, String dbName) throws Exception {
        PublishedProject project = PublishedProjectDao.get(conn, dbName, id);
        if (project == null) {
            return null;
        }

        List<RestPublishRecord> recentRecords = loadRecentRecords(id, conn, dbName);
        return new RestPublishedProject(project, null, recentRecords);
    }

    @Override
    public List<RestPublishRecord> loadRecentRecords(String publishedProjectId, Connection conn, String dbName) throws Exception {
        return PublishRecordDao.getByPublishedProjectIdOrderByIdDescWithLimit(conn, dbName, publishedProjectId, recentRecordCount)
                .stream()
                .map(p -> new RestPublishRecord(p, dbName))
                .collect(Collectors.toList());
    }

    @Override
    public RestPublishedProject stop(String publishedProjectId, long userId, String dbName) throws Exception {
        @Cleanup Connection conn = DaoUtils.getConnection(true);
        PublishedProject project = PublishedProjectDao.get(conn, dbName, publishedProjectId);
        if (project == null) {
            LOG.info("project:{} not publish", publishedProjectId);
            throw new RestException(StatusCodes.PublishedProjectNotPublish);
        }

        // 取消执行中、待执行的task、record
        _cancelPublishedProject(publishedProjectId, userId, conn, dbName);

        if (!PublishedProject.STATUS_NOT_RUNNING.equals(project.getStatus())) {

            // update project
            project.setStatus(PublishedProject.STATUS_NOT_RUNNING);
            project.getProperties().setUpdateTime(System.currentTimeMillis());
            project.getProperties().setUpdateBy(userId);
            // 清空模型
            project.getProperties().setModelId(null);
            PublishedProjectDao.updateStatusAndProp(conn, dbName, project);
            AccountCatalog.ensure(dbName).getPublishedProjects().onUpdate(project);

            // send stop cmd...
            _stopQuietly(project, conn, dbName);
            LOG.info("[{}][published project:{} stopped by user:{}]", dbName, publishedProjectId, userId);

            return new RestPublishedProject(project);
        }

        LOG.info("[{}][project:{} already stopped]", dbName, publishedProjectId);
        return new RestPublishedProject(project);
    }

    private void _cancelPublishedProject(String publishedProjectId, long userId, Connection conn, String dbName) throws Exception {
        // 查询正在执行或待执行的task
        List<Integer> status = Arrays.asList(AgentTask.STATUS_EXECUTING, AgentTask.STATUS_SCHEDULE);
        List<AgentTask> notStoppedTasks = AgentTaskDao.getByPublishedProjectIdAndStatusIn(conn, dbName, publishedProjectId, status);

        for (AgentTask task : notStoppedTasks) {
            // 如果是执行中的任务，先让agent停止，这里忽略报错
            if (task.getStatus() == AgentTask.STATUS_EXECUTING) {
                try {
                    agentService.cancelQuietly(task, dbName, conn);
                } catch (Exception ignored) {
                }
            }

            task.cancel(userId);
            AgentTaskDao.updateStatusAndProp(conn, dbName, task);
        }

        _cancelPublishRecord(publishedProjectId, conn, dbName);
    }

    /**
     * 判定运行的任务处于失败
     * @param publishedProjectId
     * @param conn
     * @param dbName
     * @throws Exception
     */
    private void _cancelPublishRecord(String publishedProjectId, Connection conn, String dbName) throws Exception {
        String status = PublishRecord.STATUS_RUNNING;
        List<PublishRecord> runningRecords = PublishRecordDao.getByPublishedProjectIdAndStatus(conn, dbName, publishedProjectId, status);
        for (PublishRecord r : runningRecords) {
            r.failed();
            PublishRecordDao.update(conn, dbName, r);
        }
    }

    private PublishedProject _createPublishedProjectIfNotExist(String projectId, String publishedProjectId, Agent agent, long userId,
                                                               String publishModel, Connection conn, String dbName) throws Exception {
        PublishedProject dbProject = PublishedProjectDao.get(conn, dbName, publishedProjectId);
        if (dbProject == null) {
            // init new
            dbProject = PublishedProject.factory(projectId, publishedProjectId, agent.getId(),
                    PublishedProject.STATUS_DEPLOYING, publishModel, null, userId);
            PublishedProjectDao.add(conn, dbName, dbProject);

            // add to cache...
            AccountCatalog.ensure(dbName).getPublishedProjects().add(dbProject);
        }
        return dbProject;
    }

    /**
     * 部署project时需要训练模型，此时 published project还没准备好
     *
     * @return
     */
    @Override
    public PublishedProject preparePublishedProject(long userId, String projectId, String publishedProjectId,
                                                    String publishModel,  String dbName) throws Exception {
        @Cleanup Connection conn = DaoUtils.getConnection(true);
        Agent agent = null;
        PublishedProject project = PublishedProjectDao.get(conn, dbName, publishedProjectId);
        if (project == null) {
            agent = agentService.selectAnAgent(conn, dbName);
            project = _createPublishedProjectIfNotExist(projectId, publishedProjectId, agent,
                    userId, publishModel, conn, dbName);
        } else {
            agent = agentService.getByAgentId(project.getAgentId(), conn);
        }

        // 之前有个bug，导致debug时里面的ProjectId为debug,这里纠正一下
        if (projectId != null && !projectId.equals(project.getProperties().getProjectId())) {
            project.getProperties().setProjectId(projectId);
            PublishedProjectDao.updateProp(conn, dbName, project);
            AccountCatalog.ensure(dbName).getPublishedProjects().onUpdate(project);
        }

        boolean isRunning = hasRunningOrSchedulingTask(publishedProjectId, conn, dbName);
        if (isRunning) {
            LOG.error("[{}] project is running, publishedProjectId:{}", dbName, publishedProjectId);
            throw new RestException(StatusCodes.AiProjectIsBusy);
        }

        // 之前是停止或者不存在的情况，在内存中不存在PublishedProject
        // 确保后续逻辑正确，这里先加入内存
        PublishedProject publishedProject = AccountCatalog.ensure(dbName).getPublishedProjects().get(publishedProjectId);
        if (publishedProject == null) {
            publishedProject = project;
            AccountCatalog.ensure(dbName).getPublishedProjects().onUpdate(project);
        }

        // 存在project，此时将project的AI数据清空，保证agent在准备好project之后再执行task
        publishedProject.getProperties().setAi(new JSONObject());

        // 启动容器
        // 注意：不要在这之后更新PublishedProject到db，这会导致RpcPublishedProjectController#started保存的数据丢失
        try {
            agentService.runPublishedProject(agent, JSONObject.toJSONString(project), publishedProjectId);
            LOG.info("[{}][success run published project:{} on agent:{}]", dbName, publishedProjectId, agent.getId());
        } catch (Exception ex) {
            LOG.error("[{}][agent can't reach. failed run debug published project on agentId:{} , projectId:{} ,error:{}]",
                    dbName, agent.getId(), project.getId(), ex.getMessage(), ex);
            throw new RestException(StatusCodes.AiAgentCanNotReach);
        }

        return project;
    }


    @Override
    public boolean hasRunningOrSchedulingTask(String id, Connection conn, String dbName) throws Exception {
        return agentTaskService.hasRunningOrSchedulingTask(id, conn, dbName);
    }

    @Override
    public boolean hasRunningTask(String id, Connection conn, String dbName) throws Exception {
        return agentTaskService.hasRunningTask(id, conn, dbName);
    }

    /**
     * 清理账户的资源
     *  - 发布的项目
     *
     * @param userId by user
     * @param dbName dbName
     */
    @Override
    public void release(long userId, String dbName) throws Exception {
        @Cleanup Connection conn = DaoUtils.getConnection(true);
        // 1、 query all publish projects
        List<PublishedProject> publishedProjects = PublishedProjectDao.getAll(conn, dbName);

        // 2、release
        for (PublishedProject publishedProject : publishedProjects) {
            _releaseQuietly(publishedProject, userId, conn, dbName);
        }
    }

    /**
     * 当项目删除时清理对应的发布项目数据
     */
    @Override
    public void release(String projectId, long userId, Connection conn, String dbName) throws Exception {
        // 删除对应的published project、释放资源
        String publishedProjectId = PublishedProject.generateId(dbName, projectId);
        PublishedProject publishedProject = PublishedProjectDao.get(conn, dbName, publishedProjectId);
        if (publishedProject != null) {
            _releaseQuietly(publishedProject, userId, conn, dbName);
        }
    }

    @Override
    public void release(PublishedProject publishedProject, long userId, Connection conn, String dbName) throws Exception {
        _releaseQuietly(publishedProject, userId, conn, dbName);
    }

    @Override
    public void quickRelease(Connection conn, String dbName) throws Exception {
        List<PublishedProject> publishedProjects = PublishedProjectDao.getAll(conn, dbName);
        if (CollectionUtils.isEmpty(publishedProjects)) {
            return;
        }

        try {
            for (PublishedProject project : publishedProjects) {
                agentService.releaseQuietly(project);
            }

        } catch (Exception e) {
            LOG.error("[{}][quick release published project fail:{}]", dbName, e.getMessage(), e);
        }
    }

    /**
     * 清理单个发布项目
     *  - 取消task
     *  - 清理容器
     *  - 从db删除记录
     *  - 从缓存删除记录
     */
    private void _releaseQuietly(PublishedProject publishedProject, long userId,
                                 Connection conn, String dbName) throws Exception {
        // 1、取消task
        _cancelPublishedProject(publishedProject.getId(), userId, conn, dbName);
        String publishedProjectId = publishedProject.getId();

        // 2、清理容器
        agentService.releaseQuietly(publishedProject);

        // 3、删除db中的记录
        PublishedProjectDao.delete(conn, dbName, publishedProjectId);

        // 4、删除缓存中的记录
        AccountCatalog.ensure(dbName).getPublishedProjects().remove(publishedProjectId);
        LOG.info("[{}][published project:{} recycled when delete project]", dbName, publishedProject);
    }

    private void _stopQuietly(PublishedProject project, Connection conn, String dbName) throws Exception {
        try {
            Agent agent = agentService.getByAgentId(project.getAgentId(), conn);
            agentService.stopPublishedProject(agent, project.getId(), false);
        } catch (Exception ignored) {
            LOG.warn("[{}][stop published project:{} fail:{}]", dbName, project.getId(), ignored.getMessage(), ignored);
        }
    }
}
