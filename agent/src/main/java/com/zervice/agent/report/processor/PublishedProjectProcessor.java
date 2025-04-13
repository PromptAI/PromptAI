package com.zervice.agent.report.processor;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.zervice.agent.service.BackendClient;
import lombok.AllArgsConstructor;

/**
 * 上报 Published Project
 * @author chen
 * @date 2022/9/1
 */
@AllArgsConstructor
public class PublishedProjectProcessor implements Processor{

    public BackendClient _backendClient;

    @Override
    public void process(String publishedProjectId, Object data) throws Exception {
        if (data instanceof JSON) {
            _backendClient.publishedProjectStarted((JSONObject) data, publishedProjectId);
            return;
        }

        throw new RuntimeException("Unsupported encoding");
    }
}
