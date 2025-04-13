package com.zervice.broker.agent;

import com.alibaba.fastjson2.JSONArray;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.zervice.broker.backend.BackendClient;
import com.zervice.broker.backend.BackendClientSelector;
import com.zervice.common.agent.AgentClient;
import com.zervice.common.restful.exception.RestException;
import com.zervice.common.restful.exception.StatusCodes;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.MutablePair;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Log4j2
public class AgentClientSelector {
    private BackendClient _mainKbClient;
    private volatile  List<AgentClient> _clients;
    private AgentClientSelectPolicy _policy;
    
    private static final AgentClientSelector _instance = new AgentClientSelector();

    public static AgentClientSelector getInstance() {
        return _instance;
    }

    public void init() {
        // 根据backendClient来读取对应的AgentClient信息
        _mainKbClient = BackendClientSelector.getMainBackendClient();
        _clients = getAllAgentClients(_mainKbClient);

        ThreadFactory factory = new ThreadFactoryBuilder()
                .setDaemon(true)
                .setNameFormat("sync-Agent-clients")
                .build();
        Executors.newSingleThreadScheduledExecutor(factory)
                .scheduleAtFixedRate(this::_syncAgentClientsFromBackend, 10, 30, TimeUnit.SECONDS);

        _policy = new SelectByRoundRobinStickyPolicy();
        LOG.info("Agent select policy {}", _policy.name());
    }

    public List<AgentClient> getAllAgents() {
        return _clients;
    }

    private void _syncAgentClientsFromBackend() {
        try {

            _clients = getAllAgentClients(_mainKbClient);
        }
        catch (Exception e) {
            LOG.error("Fail to sync Agent information form kb {}, error={}", _mainKbClient.getUrl(), e.getMessage(), e);
        }
    }

    public AgentClient select(String publishedProjectId) {
        try {
            return _policy.select(publishedProjectId);
        } catch (Exception e) {
            LOG.error("select agent error:{}", e.getMessage(), e);
            throw new RestException(StatusCodes.PublishedProjectNotPublish);
        }
    }

    private List<AgentClient> getAllAgentClients(BackendClient backendClient) {
        String resp = backendClient.getString("/rpc/agent/status");
        JSONArray array = JSONArray.parseArray(resp);
        List<AgentClient> clientList = array.toJavaList(AgentClient.class);
        if (clientList.isEmpty()) {
            LOG.warn("No Agent clients get from flow:{}", backendClient.getUrl());
        }

        return clientList;
    }

    public interface AgentClientSelectPolicy {
        AgentClient select(String publishedProjectId);

        String name();
    }

    private class SelectByRoundRobinStickyPolicy implements AgentClientSelectPolicy {
        private AtomicInteger index = new AtomicInteger();
        private Map<String, MutablePair<AgentClient, Long>> publishedProjectId2AccessMap = new HashMap<>();

        public SelectByRoundRobinStickyPolicy() {
            Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setDaemon(true).setNameFormat("expire-sessions").build())
                    .scheduleAtFixedRate(() -> expireSessions(), 10, 60, TimeUnit.MINUTES);
        }

        private synchronized void expireSessions() {
            publishedProjectId2AccessMap.forEach((s, p) -> {
                if (Math.abs(System.currentTimeMillis() - p.getRight()) > TimeUnit.MINUTES.toMillis(30)) {
                    publishedProjectId2AccessMap.remove(s);
                }
            });
        }

        @Override
        public AgentClient select(String publishedProjectId) {
            MutablePair<AgentClient, Long> clientAndAccessTime = publishedProjectId2AccessMap.get(publishedProjectId);
            if (clientAndAccessTime == null) {
                // new coming
                AgentClient client = getNextPossibleOkClient(publishedProjectId);
                clientAndAccessTime = MutablePair.of(client, System.currentTimeMillis());
                publishedProjectId2AccessMap.put(publishedProjectId, clientAndAccessTime);
            }
            else if (clientAndAccessTime.getLeft().getStatus() != AgentClient.STATUS.ACTIVE) {
                // the existed client is down...
                AgentClient client = getNextPossibleOkClient(publishedProjectId);
                LOG.warn("The session {} previous executed on {} but it's down, will execute on another Agent {}",
                        publishedProjectId, clientAndAccessTime.getLeft().getUrl(), client.getUrl()
                );
                clientAndAccessTime = MutablePair.of(client, System.currentTimeMillis());
                publishedProjectId2AccessMap.put(publishedProjectId, clientAndAccessTime);
                return client;
            }
            // aha a okay client
            clientAndAccessTime.setValue(System.currentTimeMillis());
            return clientAndAccessTime.getLeft();
        }



        private AgentClient getClientByProject(String projectId) {
            for (AgentClient client : _clients) {
                if (client.supportProject(projectId)) {
                    return client;
                }
            }

            return null;
        }

        private AgentClient getNextPossibleOkClient(String projectId) {
            AgentClient client = getClientByProject(projectId);
            if (client == null) {
                LOG.error("[no agent for project:{}]", projectId);
                throw new RestException(StatusCodes.AiProjectNoAgentAvailable);
            }

            return client;
        }

        @Override
        public String name() {
            return "roundrobin";
        }
    }

}
