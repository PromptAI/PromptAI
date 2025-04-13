package com.zervice.kbase.api.restful.controller;

import com.zervice.common.i18n.MessageUtils;
import com.zervice.common.pojo.common.EmptyResponse;
import com.zervice.common.restful.exception.RestException;
import com.zervice.common.restful.exception.StatusCodes;
import com.zervice.common.utils.Constants;
import com.zervice.kbase.api.BaseController;
import com.zervice.kbase.api.restful.AuthFilter;
import com.zervice.kbase.api.restful.pojo.PageRequest;
import com.zervice.kbase.api.restful.pojo.RestAgent;
import com.zervice.kbase.database.criteria.AgentCriteria;
import com.zervice.kbase.database.dao.AgentDao;
import com.zervice.kbase.database.dao.PublishedProjectDao;
import com.zervice.kbase.database.pojo.Agent;
import com.zervice.kbase.database.pojo.PublishedProject;
import com.zervice.kbase.database.pojo.User;
import com.zervice.kbase.database.utils.DaoUtils;
import com.zervice.kbase.database.utils.PageResult;
import com.zervice.kbase.service.AgentClientService;
import com.zervice.kbase.service.AgentService;
import lombok.Cleanup;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.sql.Connection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Peng Chen
 * @date 2022/6/16
 */
@Log4j2
@RestController
@RequestMapping("/api/agent")
public class AgentController extends BaseController {

    @Autowired
    private AgentService agentService;
    private AgentClientService clientService = AgentClientService.getInstance();

    @PostMapping
    public Object save(@RequestBody @Validated RestAgent agent,
                       @RequestHeader(AuthFilter.X_EXTERNAL_USER_ID_HEADER) String uid,
                       @RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName) throws Exception {
        long userId = User.fromExternalId(uid);

        @Cleanup Connection conn = DaoUtils.getConnection(true);

        // 初始化安装，如果有了，直接返回,无需创建
        if (agent.getInit() != null && agent.getInit()) {
            List<Agent> dbAgents = AgentDao.getByDbName(conn, dbName);
            if (CollectionUtils.isNotEmpty(dbAgents)) {
                return new RestAgent(dbAgents.get(0));
            }
        }

        Agent dbAgent = Agent.factory(dbName, userId);

        // first one as default
        List<Agent> dbAgents = AgentDao.getByDbName(conn, dbName);
        if (dbAgents.isEmpty()) {
            dbAgent.setDefault(true);
        }

        AgentDao.add(conn, dbAgent);

        return new RestAgent(dbAgent);
    }

    @GetMapping("available")
    public Object available(@RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName) throws Exception {
        @Cleanup Connection conn = DaoUtils.getConnection();
        boolean available = agentService.available(conn, dbName);
        return Map.of("available", available);
    }

    @PutMapping("default/{agentId}")
    public Object defaultAgent(@PathVariable String agentId,
                               @RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName) throws Exception {

        _defaultAgent(agentId, dbName);
        return EmptyResponse.empty();
    }

    private static void _defaultAgent(String agentId, String dbName) throws Exception{
        @Cleanup Connection conn = DaoUtils.getConnection(true);
        List<Agent> accountAgents = AgentDao.getByDbName(conn, dbName);
        Optional<Agent> defaultAgent = accountAgents.stream()
                .filter(a -> agentId.equals(a.getId()))
                .findAny();

        if (defaultAgent.isEmpty()) {
            throw new RestException(StatusCodes.AiAgentNotFound);
        }

        for (Agent agent : accountAgents) {
            boolean needUpdate2Db;
            if (agentId.equals(agent.getId())) {
                needUpdate2Db = agent.setDefault(true);
            } else {
                needUpdate2Db = agent.setDefault(false);
            }

            if (needUpdate2Db) {
                AgentDao.updateProp(conn, agent);
            }
        }

    }

    @GetMapping
    public Object get(AgentCriteria criteria, PageRequest pageRequest,
                      @RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName,
                      @RequestHeader(AuthFilter.X_EXTERNAL_USER_ID_HEADER) String uid) throws Exception {
        criteria.setDbName(dbName);

        @Cleanup Connection conn = DaoUtils.getConnection(true);
        PageResult<Agent> page = AgentDao.get(conn, criteria, pageRequest);

        // query running projects
        Map<String, List<PublishedProject>> agentPublishedProject = PublishedProjectDao.getAll(conn, dbName).stream()
                .collect(Collectors.groupingBy(PublishedProject::getAgentId));

        List<RestAgent> rests = page.getData().stream()
                .map(a -> {
                    Integer running = agentPublishedProject.getOrDefault(a.getId(), Collections.emptyList()).size();
                    return new RestAgent(a, running);
                })
                .collect(Collectors.toList());
        return PageResult.of(rests, page.getTotalCount());
    }

    @DeleteMapping("{agentId}")
    public Object delete(@PathVariable String agentId,
                         @RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName,
                         @RequestHeader(AuthFilter.X_EXTERNAL_USER_ID_HEADER) String uid) throws Exception {
        @Cleanup Connection conn = DaoUtils.getConnection(true);

        Agent agent = AgentDao.get(conn, dbName, agentId);
        if (agent == null) {
            throw new RestException(StatusCodes.AiAgentNotFound);
        }

        if (agent.getStatus() == Agent.STATUS_INSTALLING) {
            AgentDao.delete(conn, agentId);
            // init memory cache
            clientService.init();

            resetDefaultAgent(conn, dbName);
            return EmptyResponse.empty();
        }

        String cmd = MessageUtils.getMessage(Constants.I18N_DELETE_AGENT_CMD);
        return Map.of("cmd", String.format(cmd, agent.getProperties().getName()));
    }

    /**
     * 默认的agent删除了等情况重新选择一个Agent作为默认的
     */
    public static void resetDefaultAgent(Connection conn, String dbName) {
        try {
            List<Agent> agents = AgentDao.getByDbName(conn, dbName);
            if (agents.isEmpty()) {
                return;
            }

            // exists a default agent
            Optional<Agent> defaultAgent = agents.stream()
                    .filter(a -> a.getProperties().getDefault())
                    .findAny();
            if (defaultAgent.isPresent()) {
                return;
            }

            if (agents.size() == 1) {
                _defaultAgent(agents.get(0).getId(), dbName);
                return;
            }

            Optional<Agent> activeAgent = agents.stream()
                    .filter(a -> a.getStatus() == Agent.STATUS_ACTIVE)
                    .findAny();
            if (activeAgent.isPresent()) {
                _defaultAgent(activeAgent.get().getId(), dbName);
                return;
            }

            // not active... use first
            _defaultAgent(agents.get(0).getId(), dbName);
        } catch (Exception e) {
            LOG.error("[{}][reset default agent error]", dbName);
        }
    }
}