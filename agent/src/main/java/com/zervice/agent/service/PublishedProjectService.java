package com.zervice.agent.service;

import com.alibaba.fastjson.JSONObject;
import com.zervice.agent.published.project.PublishedProjectPojo;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author chen
 * @date 2022/9/13
 */
public interface PublishedProjectService {

    /**
     * 启动published project
     *
     * @param body param
     */
    void run(JSONObject body);

    /**
     * 调用 AI 服务进行发布
     * @param data
     * @param publishedProjectId
     */
    void publish(MultipartFile data, String publishedProjectId);

    /**
     * 上报published project
     */
    PublishedProjectPojo get(String publishedProjectId, Boolean report);

    /**
     * 停止 published project
     *
     * @param publishedProjectId id
     */
    void stop(String publishedProjectId);
}
