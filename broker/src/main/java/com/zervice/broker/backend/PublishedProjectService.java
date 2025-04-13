package com.zervice.broker.backend;

import com.alibaba.fastjson.JSONObject;
import com.zervice.broker.agent.AgentClientSelector;
import com.zervice.common.agent.AgentClient;
import com.zervice.common.agent.pojo.SimplePublishedProject;
import com.zervice.common.restful.exception.RestException;
import com.zervice.common.restful.exception.StatusCodes;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;

/**
 * @author chen
 * @date 2023/3/10
 */
@Log4j2
public class PublishedProjectService {

    private final AgentClientSelector agentClientSelector = AgentClientSelector.getInstance();

    private PublishedProjectService(){}


    private final String URI_PUBLISHED_PROJECT = "/rpc/agent/published/project/";

    private BackendClient _backendClient;
    public void init(BackendClient backendClient) {
        _backendClient = backendClient;
    }

    @Getter
    private static PublishedProjectService _instance = new PublishedProjectService();

    /**
     * 从内存获取，获取不道到就从kb查询..
     */
    public SimplePublishedProject get(String publishedProjectId, String token) {
        SimplePublishedProject project = null;
        try {
            AgentClient agentClient = agentClientSelector.select(publishedProjectId);
            project = agentClient.getProject(publishedProjectId);
        } catch (Exception e) {
            LOG.warn("[get project fail from agent Client, try query now:{}]", publishedProjectId);
        }

        // try from kb
        if (project == null) {
            project = query(publishedProjectId);
        }

        if (project == null) {
            LOG.error("[{}][publishedProject not exist]", publishedProjectId);
            throw new RestException(StatusCodes.ChatProjectNotFound);
        }

        if (!project.validToken(token)) {
            LOG.error("[validate publishedProject:{} fail with token:{}]", publishedProjectId, token);
            throw new RestException(StatusCodes.Forbidden);
        }

        return project;
    }


    /**
     * 主动查询 project..
     * @param publishedProjectId
     * @return
     */
    public SimplePublishedProject query(String publishedProjectId) {
        String url = URI_PUBLISHED_PROJECT + "/" + publishedProjectId;

        String result = null;
        try {
            result = _backendClient.getString(url);
            if (StringUtils.isBlank(result)) {
                return null;
            }
            JSONObject data = JSONObject.parseObject(result);
            if (data.isEmpty()) {
                return null;
            }
            return data.toJavaObject(SimplePublishedProject.class);
        } catch (Exception e) {
            LOG.error("[query simple published project error:{} result:{}]", e.getMessage(), result, e);
            return null;
        }
    }
}
