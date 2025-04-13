package com.zervice.agent.service;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import com.zervice.agent.report.Reporters;
import com.zervice.agent.utils.ConfConstant;
import lombok.extern.log4j.Log4j2;

import java.util.Map;

/**
 * 向backend发送一些事件
 *
 * @author Peng Chen
 * @date 2022/6/15
 */
@Log4j2
public class ReportService {

    private String _publishedProjectId;

    public ReportService(String publishedProjectId) {
        this._publishedProjectId = publishedProjectId;
    }

    /**
     * 上报step信息
     */
    public void reportTaskStep(Long taskId, Boolean ok,
                               String step, Integer index,
                               String message, long elapsedInMillis) {
        Map<String, Object> param = Maps.newHashMapWithExpectedSize(7);
        param.put("taskId", taskId);
        param.put("ok", ok);
        param.put("step", step);
        param.put("index", index);
        param.put("message", message);
        param.put("agentId", ConfConstant.AGENT_ID);
        param.put("elapsed", elapsedInMillis);

        JSONObject jsonParam = new JSONObject(param);
        LOG.info("[{}:{}][report step with param:{}]", _publishedProjectId, taskId, jsonParam.toJSONString());

        Reporters.getTaskStepReporter().report(_publishedProjectId, jsonParam);
    }

    /**
     * 此信息与具体的step无关，常用于任务的开始和结束
     */
    public void reportTaskStep(Long taskId, String message) {
        reportTaskStep(taskId, Boolean.TRUE, null, null, message, 0);
    }

    /**
     * 上报错误信息
     */
    public void reportTaskResult(Long taskId, Boolean ok, String message) {
        Map<String, Object> param = Maps.newHashMapWithExpectedSize(3);
        param.put("taskId", taskId);
        param.put("ok", ok);
        param.put("message", message);

        JSONObject jsonParam = new JSONObject(param);
        LOG.info("[{}:{}][report result with param:{}]", _publishedProjectId, taskId, jsonParam.toJSONString());
        Reporters.getTaskResultReporter().report(_publishedProjectId, jsonParam);
    }

}
