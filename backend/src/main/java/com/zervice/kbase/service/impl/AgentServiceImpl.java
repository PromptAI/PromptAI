package com.zervice.kbase.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.google.common.collect.Maps;
import com.zervice.common.restful.exception.RestException;
import com.zervice.common.restful.exception.StatusCodes;
import com.zervice.common.utils.Constants;
import com.zervice.common.utils.HttpClientUtils;
import com.zervice.common.utils.ServletUtils;
import com.zervice.kbase.AccountCatalog;
import com.zervice.kbase.database.dao.AgentDao;
import com.zervice.kbase.database.dao.AgentTaskDao;
import com.zervice.kbase.database.dao.PublishedProjectDao;
import com.zervice.kbase.database.pojo.Agent;
import com.zervice.kbase.database.pojo.AgentTask;
import com.zervice.kbase.database.pojo.PublishRecord;
import com.zervice.kbase.database.pojo.PublishedProject;
import com.zervice.kbase.database.utils.DaoUtils;
import com.zervice.kbase.service.AgentClientService;
import com.zervice.kbase.service.AgentService;
import com.zervice.kbase.service.PublishRecordService;
import lombok.Cleanup;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.sql.Connection;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author Peng Chen
 * @date 2022/6/22
 */
@Log4j2
@Service
public class AgentServiceImpl implements AgentService {

    @Autowired
    private PublishRecordService recordService;
    private Map<String, Agent> _agentCache = new ConcurrentHashMap<>();
    private AgentClientService _agentClientService = AgentClientService.getInstance();

    private static final String _URI_RUN_TASK = "/api/task/run";
    private static final String _URI_CANCEL_TASK = "/api/task/cancel";
    private static final String _URI_TRANSFER_PROJECT = "/api/task/transfer/project";
    private static final String _URI_PUBLISH_PROJECT = "/api/publish/project";
    private static final String _URI_PU_URI_PUBLISH_PROJECT_PUBLISH = "/api/publish/project/publish";

    @Override
    public void registry(String agentId, JSONObject data) throws Exception {
        @Cleanup Connection conn = DaoUtils.getConnection(true);
        Agent agent = AgentDao.getById(conn, agentId);
        if (agent == null) {
            LOG.warn("[{}][agent registry fail, agent not found:{}]", Constants.COREDB, agentId);
            throw new RestException(StatusCodes.BadRequest, "not exists");
        }

        // update status
        if (agent.getStatus() != Agent.STATUS_ACTIVE) {
            LOG.info("[{}][update agent status from:{} to:{}]", Constants.COREDB, agent.getStatus(), Agent.STATUS_ACTIVE);
            agent.setStatus(Agent.STATUS_ACTIVE);

            _agentClientService.onAgentStatusUpdated(agent);
        }

        String publicUrl = data.getString(Constants.AGENT_PUBLIC_URL);
        String version = data.getString(Constants.AGENT_VERSION);
        String name = data.getString(Constants.AGENT_HOSTNAME);
        Boolean hasGpu = data.getBoolean(Constants.AGENT_HAS_GPU);
        Boolean canUserGpu = data.getBoolean(Constants.AGENT_CAN_USE_GPU);
        String ip = ServletUtils.getCurrentNoProxyIpWithoutError();

        // check essential params...
        _checkPublicUrl(publicUrl);

        agent.getProperties().setPublicUrl(publicUrl);
        agent.getProperties().setVersion(version);
        agent.getProperties().setLastConnectIn(System.currentTimeMillis());
        agent.getProperties().setName(name);
        agent.getProperties().setIp(ip);
        agent.getProperties().setHasGpu(hasGpu);
        agent.getProperties().setCanUseCpu(canUserGpu);

        // update cache
        _updateAgent(agent);

        AgentDao.update(conn, agent);

        LOG.warn("[agent:{} registry successfully from:{}]", agentId, ip);
    }

    /**
     * 查找Agent 策略
     * 1、账户所属的Agent
     * 2、系统提供的Agent
     */
    @Override
    public Agent selectAnAgent(Connection conn, String dbName) throws Exception {
        List<Agent> agents = AgentDao.get(conn);

        // 1、get agent from accounts
        List<Agent> accountAgents = agents.stream()
                .filter(a -> dbName.equals(a.getDbName()))
                .collect(Collectors.toList());
        for (Agent accountAgent : accountAgents) {
            if (accountAgent.getStatus() == Agent.STATUS_ACTIVE && accountAgent.getProperties().getDefault()) {
                return accountAgent;
            }
        }

        LOG.info("[{}][no active default agent exists, try public]", dbName);

        // 2、get public agents
        List<Agent> publicAgents = agents.stream()
                .filter(a -> Agent.PUBLIC_DBNAME.equals(a.getDbName()))
                .collect(Collectors.toList());
        for (Agent publicAgent : publicAgents) {
            if (publicAgent.getStatus() == Agent.STATUS_ACTIVE) {
                return publicAgent;
            }
        }

        throw new RestException(StatusCodes.AiAgentNotAvailable);
    }

    @Override
    public boolean available(Connection conn, String dbName) throws Exception {
        List<Agent> agents = AgentDao.get(conn);

        // 1、get agent from accounts
        List<Agent> accountAgents = agents.stream()
                .filter(a -> dbName.equals(a.getDbName()))
                .collect(Collectors.toList());
        for (Agent agent : accountAgents) {
            if (agent.getStatus() != Agent.STATUS_INSTALLING) {
                return true;
            }
        }

        // 2、get public agents
        List<Agent> publicAgents = agents.stream()
                .filter(a -> Agent.PUBLIC_DBNAME.equals(a.getDbName()))
                .collect(Collectors.toList());
        for (Agent agent : publicAgents) {
            if (agent.getStatus() != Agent.STATUS_INSTALLING) {
                return true;
            }
        }

        return false;
    }


    @Override
    public Agent getByAgentId(String agentId, Connection conn) throws Exception {
        Agent agent = AgentDao.getById(conn, agentId);
        if (agent == null) {
            throw new RestException(StatusCodes.AiAgentNotFound);
        }

        return agent;
    }

    @Override
    public void runTask(AgentTask task, String accountName, Connection conn) throws Exception {
        PublishedProject publishedProject = PublishedProjectDao.get(conn, accountName, task.getPublishedProjectId());
        if (publishedProject == null) {
            LOG.error("[{}][run task:{} on agent:{} fail, published project:{} not found]",
                    accountName, task.getId(), task.getAgentId(), task.getPublishedProjectId());
            throw new RestException(StatusCodes.PublishedProjectNotRunning);
        }

        Agent agent = AgentDao.get(conn, task.getAgentId());
        if (agent == null) {
            LOG.error("[{}][run agent task:{} fail, agent:{} not found]", accountName, task.getId(), task.getAgentId());
            throw new RestException(StatusCodes.AiAgentNotFound);
        }

        if (agent.getStatus() != Agent.STATUS_ACTIVE) {
            LOG.error("[{}][run agent:{} task fail, agent is busy:{}]", accountName, agent.getId(), agent.getStatus());
            throw new RestException(StatusCodes.AiAgentIsBusy);
        }

        // 这里只判断停用的，如果在deploying agent端会拦截任务
        if (PublishedProject.STATUS_NOT_RUNNING.equals(publishedProject.getStatus())) {
            LOG.error("[{}][run task:{} on agent:{} fail, published project:{} not ready]",
                    accountName, task.getId(), task.getAgentId(), task.getPublishedProjectId());
            throw new RestException(StatusCodes.PublishedProjectNotRunning);
        }

        _renderAiParam4Task(task, publishedProject, conn, accountName);

        String publicUrl = agent.getPublicUrl();
        if (StringUtils.isBlank(publicUrl)) {
            LOG.error("[{}][run agent:{} task fail, agent public url is black", accountName, agent.getId());
            throw new RestException(StatusCodes.AiAgentCanNotReach);
        }

        _runTask(agent, task, accountName);

        // 更新状态
        _afterTaskRun(task, accountName, conn);

        // 更新状态为部署中
        if (!PublishedProject.STATUS_DEPLOYING.equals(publishedProject.getStatus())) {
            publishedProject.setStatus(PublishedProject.STATUS_DEPLOYING);
            PublishedProjectDao.updateStatus(conn, accountName, publishedProject);
            AccountCatalog.ensure(accountName).getPublishedProjects().onUpdate(publishedProject);
        }

        _agentClientService.onAgentStatusUpdated(agent);
    }

    private void _afterTaskRun(AgentTask task, String dbName, Connection conn) throws Exception {
        String oldProperties = JSONObject.toJSONString(task.getProperties());
        Integer oldStatus = task.getStatus();

        // success send task
        task.executing();

        int updateRow = AgentTaskDao.updateStatusAndProp(conn, dbName, task, oldStatus, oldProperties);
        if (updateRow > 0) {
            LOG.info("[{}][success run task:{}]", dbName, task.getId());
        } else {
            LOG.info("[{}][do not update task:{} after run, task already updated]", dbName, task.getId());
        }

        // mark stage start
        Long recordId = task.getProperties().getPublishRecordId();
        String publishedProjectId = task.getPublishedProjectId();
        recordService.runStage(recordId, PublishRecord.Stage.NAME_AGENT_TASK, publishedProjectId, dbName);
    }

    /**
     * create and run a new published project
     *
     * @param data published project of json
     * @return res
     */
    @Override
    public String runPublishedProject(Agent agent, String data, String publishedProjectId) {
        Map<String, String> param = Maps.newHashMapWithExpectedSize(1);
        param.put("data", data);

        String url = agent.getPublicUrl() + _URI_PUBLISH_PROJECT;
        return HttpClientUtils.postJson(url, param, _createHeader(agent.getId(), publishedProjectId)).toJSONString();
    }

    @Override
    public void publish(String data, Agent agent, PublishedProject publishedProject, String dbName) {
        Map<String, String> param = Maps.newHashMapWithExpectedSize(1);
        param.put("data", data);

        String url = agent.getPublicUrl() + _URI_PU_URI_PUBLISH_PROJECT_PUBLISH;
        String res = HttpClientUtils.postJson(url, param, _createHeader(agent.getId(), publishedProject.getId())).toJSONString();
        LOG.info("[{}][publish:{} with result:{}]", dbName, publishedProject.getId(), res);
    }

    @Override
    public void publish(File data, Agent agent, PublishedProject publishedProject, String dbName) {
        String url = agent.getPublicUrl() + _URI_PU_URI_PUBLISH_PROJECT_PUBLISH;
        String res = HttpClientUtils.postFile(url, data, "file", Map.of(), _createHeader(agent.getId(), publishedProject.getId())).toJSONString();
        LOG.info("[{}][publish:{} with result:{}]", dbName, publishedProject.getId(), res);
    }

    @Override
    public String stopPublishedProject(Agent agent, String publishedProjectId, boolean rmContainer) {
        String path = agent.getPublicUrl() + _URI_PUBLISH_PROJECT + "/" + publishedProjectId + "?rmContainer=" + rmContainer;
        return HttpClientUtils.deleteJSON(path, "{}", _createHeader(agent.getId(), publishedProjectId)).toJSONString();
    }

    @Override
    public void get(Agent agent, String publishedProjectId) {
        // 不要求上报的连接
//        String path = agent.getPublicUrl() + _URI_PUBLISH_PROJECT + "/" + publishedProjectId + "?report=false";
        String path = agent.getPublicUrl() + _URI_PUBLISH_PROJECT + "/" + publishedProjectId;
        String res = HttpClientUtils.getJson(path, _createHeader(agent.getId(), publishedProjectId)).toJSONString();
        LOG.info("[{}][report project with result:{}]", publishedProjectId, res);
    }

    @Override
    public void cancelQuietly(AgentTask task, String accountName, Connection conn) throws Exception {
        Agent agent = AgentDao.get(conn, task.getAgentId());
        if (agent == null) {
            LOG.error("[{}][cancel agent task:{} fail, agent:{} not found]", accountName, task.getId(), task.getAgentId());
            return;
        }

        String publicUrl = agent.getPublicUrl();
        if (StringUtils.isBlank(publicUrl)) {
            LOG.error("[{}][run agent:{} task fail, agent public url is black]", accountName, agent.getId());
            return;
        }

        try {
            _cancelTask(agent, task, accountName);
        } catch (Exception e) {
            LOG.warn("[{}][cancel task:{} fail on agent:{} and published project:{}]", accountName,
                    task.getId(), agent.getId(), task.getProperties());
        }
    }

    @Override
    public void cancelAllTask(long userId, String dbName) {
        Connection conn = null;
        List<AgentTask> tasks = new ArrayList<>();
        try {
            conn = DaoUtils.getConnection();
            List<Integer> status = Arrays.asList(AgentTask.STATUS_EXECUTING, AgentTask.STATUS_SCHEDULE);
            tasks = AgentTaskDao.getByStatusIn(conn, dbName, status);
            for (AgentTask task : tasks) {
                task.cancel(userId);
                AgentTaskDao.updateStatusAndProp(conn, dbName, task);
            }
        } catch (Exception ex) {
            LOG.error("[{}][cancel task failed.error:{}]", dbName, ex.getMessage());
        } finally {
            DaoUtils.closeQuietly(conn);
        }
        for (AgentTask task : tasks) {
            try {
                //防止connection 超时
                @Cleanup Connection connection = DaoUtils.getConnection(true);
                cancelQuietly(task, dbName, connection);
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void uninstall(Agent agent,Connection conn) throws Exception{
        AgentDao.delete(conn, agent.getId());
        AgentClientService.getInstance().onAgentRemoved(agent);
    }

    @Async
    @Override
    public void releaseQuietly(PublishedProject project) throws Exception {
        try {
            //防止connection 超时，调用rpc的时候重新获取一个connection
            @Cleanup Connection conn = DaoUtils.getConnection(true);
            Agent agent = getByAgentId(project.getAgentId(), conn);
            stopPublishedProject(agent, project.getId(), true);
        } catch (Exception ignored) {}
    }

    private synchronized void _updateAgent(Agent agent) {
        _agentCache.put(agent.getId(), agent);
    }

    public synchronized Agent _putAgent2Cache(String agentId) throws Exception {
        @Cleanup Connection conn = DaoUtils.getConnection(true);
        Agent agent = AgentDao.getById(conn, agentId);
        _agentCache.put(agentId, agent);
        return agent;
    }

    private void _runTask(Agent agent, AgentTask task, String accountName) {
        String url = agent.getPublicUrl() + _URI_RUN_TASK;
        JSONObject res = HttpClientUtils.postJson(url, task, _createHeader(agent.getId(), task.getPublishedProjectId()));
        LOG.info("[{}][run {} agent:{} task:{} with result:{}]", accountName, agent.getDbName(), agent.getId(), task.getId(), res);
    }

    private void _cancelTask(Agent agent, AgentTask task, String accountName) {
        String url = agent.getPublicUrl() + _URI_CANCEL_TASK;

        Map<String, Object> param = Maps.newHashMapWithExpectedSize(1);
        param.put("id", task.getId());

        JSONObject res = HttpClientUtils.postJson(url, param, _createHeader(agent.getId(), task.getPublishedProjectId()));
        LOG.info("[{}][cancel {} agent:{} task:{} with result:{}]", accountName, agent.getDbName(), agent.getId(), task.getId(), res);
    }

    private HttpHeaders _createHeader(String zpAgentId, String publishedProjectId) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(Constants.AGENT_ID_HEADER, zpAgentId);
        httpHeaders.add(Constants.AGENT_PUBLISHED_PROJECT_HEADER, publishedProjectId);
        return httpHeaders;
    }

    private void _renderAiParam4Task(AgentTask task, PublishedProject publishedProject, Connection conn, String dbName) throws Exception {
        // render ai param
        Map<String, String> aiParamMap = Maps.newHashMapWithExpectedSize(16);

        publishedProject.fillAiParam(aiParamMap);

        if (aiParamMap.isEmpty()) {
            LOG.error("[{}][empty ai param for task:{}]", dbName, task.getId());
            throw new RestException(StatusCodes.InternalError);
        }

        LOG.info("[{}:{}][render task with ai param:{}]", dbName, task.getId(), JSONObject.toJSONString(aiParamMap, SerializerFeature.PrettyFormat));

        task.renderSteps(aiParamMap);

        AgentTaskDao.update(conn, dbName, task);
    }

    private void _checkPublicUrl(String publicUrl) {
        String healthUri = "/api/health";
        if (StringUtils.isBlank(publicUrl) || !HttpClientUtils.reachable(publicUrl + healthUri)) {
            throw new RestException(StatusCodes.BadRequest, "publicUrl not reachable");
        }
    }

}
