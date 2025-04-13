package com.zervice.kbase.api.rpc;

import com.alibaba.fastjson.JSONObject;
import com.zervice.common.agent.pojo.SimplePublishedProject;
import com.zervice.common.pojo.common.EmptyResponse;
import com.zervice.kbase.AccountCatalog;
import com.zervice.kbase.api.BaseController;
import com.zervice.kbase.api.restful.controller.AgentController;
import com.zervice.kbase.database.dao.AgentDao;
import com.zervice.kbase.database.dao.PublishedProjectDao;
import com.zervice.kbase.database.pojo.Agent;
import com.zervice.kbase.database.pojo.PublishedProject;
import com.zervice.kbase.database.pojo.User;
import com.zervice.kbase.database.utils.DaoUtils;
import com.zervice.kbase.service.AgentClientService;
import com.zervice.kbase.service.AgentService;
import com.zervice.kbase.service.PublishedProjectService;
import lombok.Cleanup;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.sql.Connection;
import java.util.List;

/**
 * Broker调用的
 *
 * @author Peng Chen
 * @date 2022/6/16
 */
@Log4j2
@RestController
@RequestMapping("/rpc/agent")
public class RpcAgentController extends BaseController {
    @Autowired
    private AgentService agentService;
    @Autowired
    private PublishedProjectService publishedProjectService;
    private AgentClientService clientService = AgentClientService.getInstance();

    @PostMapping("registry")
    public Object registry(@RequestBody String body,
                           @RequestHeader(RpcFilter.X_EXTERNAL_AGENT_ID_HEADER) String agentId) throws Exception {
        LOG.debug("[agent:{}, registry:{}]", agentId, body);

        agentService.registry(agentId, JSONObject.parseObject(body));

        return EmptyResponse.empty();
    }

    @DeleteMapping("uninstall")
    public Object uninstall(@RequestHeader(RpcFilter.X_EXTERNAL_AGENT_ID_HEADER) String agentId) throws Exception{
        LOG.info("[{}][try uninstall agent]", agentId);
        @Cleanup Connection conn = DaoUtils.getConnection(true);
        Agent agent = AgentDao.get(conn, agentId);
        if (agent == null) {
            LOG.warn("[{}][failed to uninstall agent, not found]", agentId);
            return EmptyResponse.empty();
        }

        if (!agent.isPrivate()) {
            LOG.warn("[{}][don't uninstall public agent]", agentId);
            return EmptyResponse.empty();
        }

        LOG.info("[{}][start uninstall agent for account:{}]", agentId, agent.getDbName());

        String dbName = agent.getDbName();

        // 1、clear published project
        List<PublishedProject> publishedProjects = PublishedProjectDao.getByAgentId(conn, dbName, agentId);
        for (PublishedProject p : publishedProjects) {
            publishedProjectService.release(p, User.USER_ID_SYSTEM, conn, dbName);
        }

        // 2、uninstall agent
        agentService.uninstall(agent,conn);

        AgentController.resetDefaultAgent(conn, dbName);
        LOG.info("[{}][finish uninstall agent]", agentId);
        return EmptyResponse.empty();
    }

    @GetMapping("published/project/{id}")
    public Object publishedProject(@PathVariable String id) throws Exception {
        String dbName = PublishedProject.getDbNameFromId(id);
        if (StringUtils.isBlank(dbName) || AccountCatalog.guess(dbName) == null) {
            LOG.warn("[could not get dbName from published project id:{}]", id);
            return EmptyResponse.empty();
        }

        @Cleanup Connection conn = DaoUtils.getConnection(true);
        PublishedProject project = PublishedProjectDao.get(conn, dbName, id);
        if (project == null) {
            LOG.warn("[{}][could not found published project id:{}]", dbName, id);
            return EmptyResponse.empty();
        }

        return new SimplePublishedProject(project.getId(), project.getProperties().getProjectId(),
                project.getAgentId(), project.getStatus(), project.getToken(), dbName);
    }

    /**
     * 获取所有的
     */
    @GetMapping("status")
    public Object status() {
        return clientService.getAllAgentClients();
    }
}
