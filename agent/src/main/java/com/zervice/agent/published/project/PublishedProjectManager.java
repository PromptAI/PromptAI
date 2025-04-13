package com.zervice.agent.published.project;

import cn.hutool.core.io.FileUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.google.common.collect.Maps;
import com.zervice.agent.ai.AiClient;
import com.zervice.agent.report.Reporters;
import com.zervice.agent.service.BackendClient;
import com.zervice.agent.utils.ConfConstant;
import com.zervice.common.utils.Constants;
import lombok.extern.log4j.Log4j2;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 这里在内存中缓存published projects,同时需要持久化到一个本地文件,便于Agent重启启动后快速恢复运行
 * 1、重启后先请求backend获取最新的projects，如果失败，再load本地持久化文件，按照配置准备好Ai container
 * 2、一旦container启动完成后，调用Backend api，告知当前Published 已启动
 * 3、如果发现没有引用的Published task,stop掉以释放资源.
 *
 * @author Peng Chen
 * @date 2022/8/4
 */
@Log4j2
public class PublishedProjectManager {

    /**
     * cache project data...
     */
    private final String PROJECT_DATA_FILE;

    private final String _CONF_NAME = "published_project.conf";

    private static PublishedProjectManager _instance = new PublishedProjectManager();

    private Map<String, PublishedProjectPojo> _projectCaches = new ConcurrentHashMap<>();

    private BackendClient _backendClient = BackendClient.getInstance();

    private PublishedProjectManager() {
        PROJECT_DATA_FILE = ConfConstant.AGENT_BASE_DIR + File.separator + _CONF_NAME;
    }

    public static PublishedProjectManager getInstance() {
        return _instance;
    }

    public void init() {
        // 从backed获取最新的 projects
        // 这里可能会获取失败，后续计划周期性获取
        boolean success = _loadPublishedProjectsFromBackend();
        if (!success) {
            // 从配置文件文件获取
            _loadPublishedProjectsFromFile();
        }
    }

    private boolean _loadPublishedProjectsFromBackend() {
        try {
            String res = _backendClient.loadPublishedProjects();
            JSONArray projects = JSONArray.parseArray(res);
            for (int i = 0; i < projects.size(); i++) {
                JSONObject item = projects.getJSONObject(i);
                PublishedProjectPojo project = PublishedProjectPojo.parse(item);
                _projectCaches.put(project.getId(), project);
                _onProjectCachesUpdated();
            }

            return true;
        } catch (Exception e) {
            LOG.error("load published projects from backend fail:{}", e.getMessage(), e);
            return false;
        }
    }

    public PublishedProjectPojo get(String publishedProjectId, Boolean report) {
        PublishedProjectPojo pojo = _projectCaches.get(publishedProjectId);
        if (pojo == null) {
            return null;
        }

        // report
        if (report) {
            _reportProject(pojo);
        }
        return pojo;
    }

    public void run(PublishedProjectPojo projectPojo) {
        LOG.info("[run published project:{}]", JSONObject.toJSONString(projectPojo));
        if (_projectCaches.containsKey(projectPojo.getId())) {
            PublishedProjectPojo pojo = _projectCaches.get(projectPojo.getId());
            _reportProject(pojo);
        }

        _initProject(projectPojo);
    }

    private void _initProject(PublishedProjectPojo projectPojo) {
        AiClient aiClient = new AiClient(projectPojo);
        // set manager and client to project
        projectPojo.setAiClient(aiClient);

        // report to backend
        _reportProject(projectPojo);

        _projectCaches.put(projectPojo.getId(), projectPojo);
        LOG.info("[project cache refreshed:{}]", projectPojo.getId());
        _onProjectCachesUpdated();
    }

    public void stop(String publishedProjectId) {
        // 刷新本地缓存文件
        _onProjectCachesUpdated();
    }


    private void _reportProject(PublishedProjectPojo pojo) {
        Map<String, Object> params = Maps.newHashMapWithExpectedSize(6);
        params.put(Constants.AGENT_ID, ConfConstant.AGENT_ID);
        JSONObject jsonParam = new JSONObject(params);

        Reporters.getPublishedProjectReporter().report(pojo.getId(), jsonParam);
    }



    public PublishedProjectPojo getProject(String publishedProjectId) {
        PublishedProjectPojo project = _projectCaches.getOrDefault(publishedProjectId, null);

        // 刷新最后访问时间
        if (project != null) {
            project.refreshLastVisitTime();
        }

        return project;
    }

    private void _loadPublishedProjectsFromFile() {
        File projectFile = new File(PROJECT_DATA_FILE);
        if (!projectFile.exists()) {
            _clearProjectFile();
            return;
        }

        String content = "";
        try {
            content = FileUtil.readString(projectFile, StandardCharsets.UTF_8);
            JSONObject json = JSONObject.parseObject(content);
            if (!json.isEmpty()) {
                _projectCaches = json.toJavaObject(new TypeReference<Map<String, PublishedProjectPojo>>() {
                }.getType());

                LOG.info("success load projects from file:{} ", JSONObject.toJSONString(json, true));
            }
        } catch (Exception e) {
            LOG.error("init projects from file:{} with error:{}", content, e.getMessage(), e);
            LOG.error("clear conf file:{}", PROJECT_DATA_FILE);
            _clearProjectFile();
        }
    }

    public void stop() {

    }
    private synchronized void _storeProject2File() {
        String taskData = JSONObject.toJSONString(_projectCaches, true);
        FileUtil.writeString(taskData, PROJECT_DATA_FILE, StandardCharsets.UTF_8);
    }

    private void _clearProjectFile() {
        LOG.debug("clear project caches");
        FileUtil.writeString("{}", PROJECT_DATA_FILE, StandardCharsets.UTF_8);
    }

    public void _onProjectCachesUpdated() {
        _storeProject2File();
    }

}
