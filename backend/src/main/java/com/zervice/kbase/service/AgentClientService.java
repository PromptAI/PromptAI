package com.zervice.kbase.service;

import com.zervice.common.agent.AgentClient;
import com.zervice.common.agent.pojo.SimplePublishedProject;
import com.zervice.common.pojo.common.Account;
import com.zervice.common.utils.Constants;
import com.zervice.kbase.AccountCatalog;
import com.zervice.kbase.cache.AccountPublishedProjects;
import com.zervice.kbase.database.dao.AccountDao;
import com.zervice.kbase.database.dao.AgentDao;
import com.zervice.kbase.database.pojo.Agent;
import com.zervice.kbase.database.pojo.PublishedProject;
import com.zervice.kbase.database.utils.DaoUtils;
import lombok.Cleanup;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author Peng Chen
 * @date 2022/7/7
 */
@Log4j2
@Component
@DependsOn({"daoUtils"})
public class AgentClientService {
    private AgentClientService() {
    }

    private static final Map<String, AgentClient> _agentCache = new ConcurrentHashMap<>(16);
    private static final AgentClientService _instance = new AgentClientService();


    public static AgentClientService getInstance() {
        return _instance;
    }

    public void init() {
        _loadAgentClient();
    }

    public void _loadAgentClient() {
        // init all agent clients
        List<Agent> agents = _allAgents();
        if (agents.isEmpty()) {
            LOG.warn("No agent exists");
            return;
        }


        for (int i = 0; i < agents.size(); i++) {
            Agent agent = agents.get(i);
            AgentClient.STATUS status = AgentClient.STATUS.valueOf(agent.getStatus());
            AgentClient agentClient = new AgentClient(agent.getId(), agent.getPublicUrl(), status);
            _agentCache.put(agent.getId(), agentClient);
        }

        //start schedule update status
        _startCheckActive();
    }

    private Map<String, Account> _loadAccountMap() {
        try {
            @Cleanup Connection conn = DaoUtils.getConnection(true);
            return AccountDao.getAll(conn).stream()
                    .collect(Collectors.toMap(Account::getAccountDbName, a -> a));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * kb 获取所有的agent
     */
    public AgentClient[] getAllAgentClients() {
        AgentClient[] clients = _agentCache.values().toArray(new AgentClient[0]);

        for (AgentClient client : clients) {
            // 先清理上一次的数据，因为set add all 不会替换新数据
            client.clearProjects();

            _loadProjects4AgentClient(client);
        }

        return clients;
    }

    private void _loadProjects4AgentClient(AgentClient client) {
        for (AccountCatalog catalog : AccountCatalog.getAllAccounts()) {
            AccountPublishedProjects projects = catalog.getPublishedProjects();

            Set<PublishedProject> publishedProjects = projects.getByAgentId(client.getId());
            if (publishedProjects.isEmpty()) {
                continue;
            }

            // convert to SamplePublishedProject
            List<SimplePublishedProject> sampleProjects = publishedProjects.stream()
                    .map(p -> new SimplePublishedProject(p.getId(), p.getProperties().getProjectId(),
                            p.getAgentId(), p.getStatus(), p.getToken(),
                            p.getDbName()))
                    .collect(Collectors.toList());

            client.addProjects(sampleProjects);
        }
    }

    public void onAgentRemoved(Agent agent) {
        _agentCache.remove(agent.getId());
    }

    public void onAgentStatusUpdated(Agent agent) {
        AgentClient.STATUS status = AgentClient.STATUS.valueOf(agent.getStatus());

        if (_agentCache.containsKey(agent.getId())) {
            AgentClient agentClient = _agentCache.get(agent.getId());
            agentClient.setStatus(status);
            agentClient.setUrl(agent.getPublicUrl());
            return;
        }

        // add in cache
        AgentClient agentClient = new AgentClient(agent.getId(), agent.getPublicUrl(), status);
        _agentCache.put(agent.getId(), agentClient);
    }

    private String _getAccountNameFromAccountId(String accountId) {
        return _loadAccountMap().get(accountId).getName();
    }


    public List<Agent> _allAgents() {
        try {
            @Cleanup Connection conn = DaoUtils.getConnection(true);
            return AgentDao.get(conn);
        } catch (Exception e) {
            LOG.error("[{}][load agents fail:{}]", Constants.COREDB, e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private void _startCheckActive() {
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            for (Map.Entry<String, AgentClient> entry : _agentCache.entrySet()) {
                AgentClient client = entry.getValue();
                AgentClient.STATUS status = client.checkActive();

                // status updated!
                if (status != null) {
                    _updateAgentStatus(client);
                }

            }
        }, 0, 60, TimeUnit.SECONDS);
    }

    private void _updateAgentStatus(AgentClient client) {
        try {
            @Cleanup Connection conn = DaoUtils.getConnection(true);
            Agent agent = AgentDao.getById(conn, client.getId());
            if (agent != null && agent.getStatus() != client.getStatus().ordinal()) {
                agent.setStatus(client.getStatus().ordinal());
                AgentDao.updateStatus(conn, agent);
            }
        } catch (Exception e) {
            LOG.error("[{}][update agent status fail. agentId:{}, error:{}]", Constants.COREDB, client.getId(), e.getMessage(), e);
        }
    }
}
