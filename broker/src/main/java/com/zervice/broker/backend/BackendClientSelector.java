package com.zervice.broker.backend;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.zervice.common.utils.LayeredConf;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.MutablePair;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Log4j2
@Component
public class BackendClientSelector {

    private static List<BackendClient> _clients;
    private static BackendClientSelectPolicy _policy;

    public static void init(String authCode) {
        String[] allEndpoints = LayeredConf.getStringArray("flow.service.url");
        if (allEndpoints.length == 0) {
            throw new IllegalArgumentException("No valid flow service url provided ");
        }
        _clients = new ArrayList<>();
        for (int i = 0; i < allEndpoints.length; i++) {
            _clients.add(new BackendClient(allEndpoints[i], authCode));
            LOG.info("Build flow prc client {}, url={}", i, allEndpoints[i]);
        }

        _policy = new BackendClientSelector.SelectByRoundRobinStickyPolicy();
        LOG.info("flow select policy {}", _policy.name());
    }

    public static BackendClient getAnyRpcClient() {
        return getNextPossibleOkClient();
    }

    public static BackendClient getMainBackendClient() {
        return _clients.get(0);
    }

    public static BackendClient getRpcClient(String sessionId) {
        return _policy.select(sessionId);
    }

    private static AtomicInteger index = new AtomicInteger(0);

    private static BackendClient getNextPossibleOkClient() {
        // select at most
        BackendClient client = null;
        for (int i = 0; i < _clients.size(); i++) {
            client = _clients.get(index.incrementAndGet() % _clients.size());
            if (client.getStatus() == BackendClient.STATUS.ACTIVE) {
                break;
            }
        }
        if (client == null) {
            LOG.error("All flow endpoints are down select the first one {}", _clients.get(0));
            client = _clients.get(0);
        }
        return client;
    }

    /**
     * backend select policy
     */
    public interface BackendClientSelectPolicy {

        /**
         * select by session id
         *
         * @param sessionId session id
         * @return client
         */
        BackendClient select(String sessionId);

        /**
         * get policy name
         *
         * @return policy name
         */
        String name();
    }


    private static class SelectByRoundRobinStickyPolicy implements BackendClientSelectPolicy {
        private AtomicInteger index = new AtomicInteger();
        private Map<String, MutablePair<BackendClient, Long>> sessionId2AccessMap = new HashMap<>();

        public SelectByRoundRobinStickyPolicy() {
            ThreadFactory factory = new ThreadFactoryBuilder()
                    .setDaemon(true)
                    .setNameFormat("expire-flow-client-sessions")
                    .build();

            Executors.newSingleThreadScheduledExecutor(factory)
                    .scheduleAtFixedRate(this::_expireSessions, 10, 60, TimeUnit.MINUTES);
        }

        private synchronized void _expireSessions() {
            sessionId2AccessMap.forEach((s, p) -> {
                if (Math.abs(System.currentTimeMillis() - p.getRight()) > TimeUnit.MINUTES.toMillis(30)) {
                    sessionId2AccessMap.remove(s);
                }
            });
        }

        @Override
        public synchronized BackendClient select(String sessionId) {
            MutablePair<BackendClient, Long> clientAndAccessTime = sessionId2AccessMap.get(sessionId);
            if (clientAndAccessTime == null) {
                // new coming
                BackendClient client = getNextPossibleOkClient();
                clientAndAccessTime = MutablePair.of(client, System.currentTimeMillis());
                sessionId2AccessMap.put(sessionId, clientAndAccessTime);
            } else if (clientAndAccessTime.getLeft().getStatus() != BackendClient.STATUS.ACTIVE) {
                // the existed client is down...
                BackendClient client = getNextPossibleOkClient();
                LOG.warn("The session {} previous executed on {} but it's down, will execute on another flow {}",
                        sessionId, clientAndAccessTime.getLeft().getUrl(), client.getUrl()
                );
                clientAndAccessTime = MutablePair.of(client, System.currentTimeMillis());
                sessionId2AccessMap.put(sessionId, clientAndAccessTime);
                return client;
            }
            // aha a okay client
            clientAndAccessTime.setValue(System.currentTimeMillis());
            return clientAndAccessTime.getLeft();
        }

        @Override
        public String name() {
            return "roundrobin";
        }

    }
}
