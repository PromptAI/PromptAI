package com.zervice.agent.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.zervice.agent.published.project.PublishedProjectManager;
import com.zervice.agent.published.project.PublishedProjectPojo;
import com.zervice.agent.service.PublishedProjectService;
import com.zervice.common.restful.exception.RestException;
import com.zervice.common.restful.exception.StatusCodes;
import com.zervice.common.utils.MultipartFileUtil;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

/**
 * @author chen
 * @date 2022/9/13
 */
@Log4j2
@Service
public class PublishedProjectServiceImpl implements PublishedProjectService {
    private PublishedProjectManager _publishedTaskManager = PublishedProjectManager.getInstance();

    @Override
    public void run(JSONObject body) {
        PublishedProjectPojo publishedProject = PublishedProjectPojo.parse(body);
        _publishedTaskManager.run(publishedProject);
    }

    @Override
    public void publish(MultipartFile file, String publishedProjectId) {
        PublishedProjectPojo publishedProjectPojo = _publishedTaskManager.getProject(publishedProjectId);
        if (publishedProjectPojo == null) {
            LOG.error("[{}][publish data error, project not exist]", publishedProjectId);
            throw new RestException(StatusCodes.PublishedProjectNotRunning);
        }

        // take out data and request to publish
        File file1 = MultipartFileUtil.toFile("/tmp/" + file.getOriginalFilename(), file);
       try {
           publishedProjectPojo.aiClient().publish(file1);
       } finally {
           file1.delete();
       }

    }

    @Override
    public PublishedProjectPojo get(String publishedProjectId, Boolean report) {
        return _publishedTaskManager.get(publishedProjectId, report);
    }

    @Override
    public void stop(String publishedProjectId) {
        _publishedTaskManager.stop(publishedProjectId);
    }
}
