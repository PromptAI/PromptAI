package com.zervice.kbase.api.rpc;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.zervice.common.pojo.common.EmptyResponse;
import com.zervice.kbase.AccountCatalog;
import com.zervice.kbase.api.BaseController;
import com.zervice.kbase.api.restful.AuthFilter;
import com.zervice.kbase.cache.AccountPublishedProjects;
import com.zervice.kbase.database.dao.AgentTaskDao;
import com.zervice.kbase.database.dao.PublishedProjectDao;
import com.zervice.kbase.database.pojo.AgentTask;
import com.zervice.kbase.database.pojo.PublishedProject;
import com.zervice.kbase.database.utils.DaoUtils;
import com.zervice.kbase.service.PublishedProjectService;
import lombok.Cleanup;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.sql.Connection;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Agent 调用的
 * @author Peng Chen
 * @date 2022/8/3
 */
@Log4j2
@RestController
@RequestMapping("/rpc/published/project")
public class RpcPublishedProjectController extends BaseController {

    @Autowired
    private PublishedProjectService publishedProjectService;

    /**
     * get all valid projects
     */
    @GetMapping
    public Object get(@RequestHeader(RpcFilter.X_EXTERNAL_AGENT_ID_HEADER) String agentId) {
        List<AccountCatalog> accounts = AccountCatalog.getAllAccounts();

        Set<PublishedProject> projects = new HashSet<>();
        for (AccountCatalog account : accounts) {
            AccountPublishedProjects p = account.getPublishedProjects();
            projects.addAll(p.getByAgentId(agentId));
        }

        return _toRest(projects);
    }

    private JSONArray _toRest(Set<PublishedProject> projects) {
        JSONArray res = new JSONArray(projects.size());
        for (PublishedProject p : projects) {
            JSONObject item = new JSONObject();
            item.put("data", p);
            res.add(item);
        }

        return res;
    }

    /**
     * 表明一个project成功启动了
     * 里面的参数:
     * {
     *   "ai.trainPath": "/data/asar/rebuild/data",
     *   "ai.agentId": "mcvwuyfdrdh6agate",
     *   "ai.logPath": "/data/asar/rebuild/logs",
     *   "ai.currentModel": "20220726-041357",
     *   "ai.publicUrl": "http://mcvwuyfdrdh6agate.ngrok.pcc.pub:8443",
     *   "ai.modelPath": "/data/asar/rebuild/models"
     *   "ai.imageId": "sha256:910e003ddf01963e3da17e6c37a3b98c7c57ca43fbd2d25de6da1b05058ee63b"
     * }
     */
    @PostMapping("started")
    public Object started(@RequestBody JSONObject data,
                          @RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName,
                          @RequestHeader(RpcFilter.X_EXTERNAL_PUBLISHED_PROJECT_ID_HEADER) String publishedProjectId) throws Exception {
        LOG.info("[{}][receive published project started:{}]", dbName, publishedProjectId);

        PublishedProject publishedProject = AccountCatalog.ensure(dbName).getPublishedProjects().get(publishedProjectId);

        // 内存中不存在，说明不用同步
        if (publishedProject == null) {
            LOG.warn("[{}][not found published project:{} in memory]", dbName, publishedProjectId);
            return _handleStared(data, publishedProjectId, dbName);
        }

        // 内存中存在，需要同步处理，防止丢失数据
        synchronized (publishedProject) {
            return _handleStared(data, publishedProjectId, dbName);
        }
    }

    private Object _handleStared(JSONObject data, String publishedProjectId, String dbName) throws Exception {
        LOG.info("[{}][handle published project started:{}]", dbName, publishedProjectId);

        AccountCatalog accountCatalog = AccountCatalog.ensure(dbName);
        @Cleanup Connection conn = DaoUtils.getConnection(true);
        PublishedProject project = PublishedProjectDao.get(conn, dbName, publishedProjectId);
        if (project == null) {
            LOG.warn("[{}][leaked published project:{} started]", dbName, publishedProjectId);
            return EmptyResponse.empty();
        }

        // 获取计划中或执行中的task
        List<AgentTask> schedulingOrRunningTasks = AgentTaskDao.getByStatusIn(conn, dbName, Arrays.asList(AgentTask.STATUS_SCHEDULE, AgentTask.STATUS_EXECUTING))
                .stream()
                .filter(t -> project.getId().equals(t.getPublishedProjectId()))
                .collect(Collectors.toList());

        // 如果有执行中或待执行的task，将状态标记为deploying
        String status = schedulingOrRunningTasks.isEmpty() ? PublishedProject.STATUS_RUNNING : PublishedProject.STATUS_DEPLOYING;

        project.setStatus(status);
        project.getProperties().setAi(data);
        PublishedProjectDao.updateStatusAndProp(conn, dbName, project);
        accountCatalog.getPublishedProjects().onUpdate(project);

        LOG.info("[{}][published project:{} success started]", dbName, JSONObject.toJSONString(project));
        return EmptyResponse.empty();
    }
}
