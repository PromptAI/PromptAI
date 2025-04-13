package com.zervice.agent.service;

import cn.hutool.core.thread.ThreadUtil;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import com.zervice.agent.report.Reporters;
import com.zervice.agent.utils.ConfConstant;
import com.zervice.agent.utils.EnvUtil;
import com.zervice.common.restful.exception.RestException;
import com.zervice.common.restful.exception.StatusCodes;
import com.zervice.common.utils.Constants;
import com.zervice.common.utils.HttpClientUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * report task result to Backend
 * - step result
 * - task result
 *
 * @author Peng Chen
 * @date 2022/6/16
 */
@Log4j2
public class BackendClient {

    private static BackendClient _instance = new BackendClient();

    public static BackendClient getInstance() {
        return _instance;
    }

    private BackendClient(){}

    private String _backendUrl = EnvUtil.getEnvOrProp("SERVER_URL","http://127.0.0.1");


    private final String _PUBLISHED_PROJECT_ID_HEADER = Constants.AGENT_PUBLISHED_PROJECT_HEADER;

    private final String _REPORT_TASK_STEP_URI = "/rpc/agent/task/report/step";
    private final String _REPORT_TASK_RESULT_URI = "/rpc/agent/task/report/result";
    private final String _PUBLISHED_TASK_START_UP_URI = "/rpc/published/project/started";
    private final String _PUBLISHED_TASK_URI = "/rpc/published/project";
    private final String _AGENT_REGISTRY_URI = "/rpc/agent/registry";
    private final String _AGENT_PROXY_CONFIG = "/rpc/agent/p";
    private final HttpHeaders _DEFAULT_HTTP_HEADERS = new HttpHeaders();
    {
        _DEFAULT_HTTP_HEADERS.add(Constants.AGENT_ID_HEADER, ConfConstant.AGENT_ID);
        _DEFAULT_HTTP_HEADERS.add(Constants.AGENT_AK_HEADER, ConfConstant.AGENT_AK);
    }


    /**
     * 向backend注册
     */
    public void registry() {
        new Thread(() -> {
            // 如果发现是401，那么表示在系统中已删除了该agent，那么就不应该重复请求后段进行注册。
            // 这里为什么重试多次？ 防止后段重启等原因导致非预期返回的401
            int retryTimes4Unauthorized = 100;
            while (true) {
                try {
                    _registry();
                    break;
                }
                catch (Exception e) {
                    String errorMessage = e.getMessage();
                    if (errorMessage.contains("401")) {
                        if (retryTimes4Unauthorized > 0) {
                            retryTimes4Unauthorized--;

                            LOG.error("[unauthorized when registry, left retry:{}]", retryTimes4Unauthorized);
                            ThreadUtil.sleep(TimeUnit.SECONDS.toMillis(20));
                            continue;
                        }

                        LOG.error("[unauthorized when registry, stop retry:{}]", retryTimes4Unauthorized);

                        // stop all reporters
                        Reporters.stop();
                        return;
                    }

                    LOG.error("registry to backend fail:{}, try next time", errorMessage);
                    ThreadUtil.sleep(TimeUnit.SECONDS.toMillis(20));
                }
            }
        }).start();
    }

    private void _registry() {
        Map<String, Object> params = _buildRegistryParam();
        HttpHeaders httpHeaders = _defaultHeader();
        LOG.info("start registry with param:{}, headers:{}", JSONObject.toJSONString(params), httpHeaders.keySet());
        JSONObject result = HttpClientUtils.postJson(_backendUrl + _AGENT_REGISTRY_URI, params, httpHeaders);
        LOG.info("success registry with result:{}", result);
    }

    private Map<String, Object> _buildRegistryParam() {
        Map<String, Object> params = Maps.newHashMapWithExpectedSize(16);
        params.put(Constants.AGENT_PUBLIC_URL, ConfConstant.PUBLIC_URL);
        params.put(Constants.AGENT_CAN_USE_GPU, ConfConstant.AI_CAN_USE_GPU);
        params.put(Constants.AGENT_VERSION, ConfConstant.VERSION);
        params.put(Constants.AGENT_HOSTNAME, ConfConstant.HOSTNAME);
        return params;
    }

    /**
     * 上报report step log
     *
     * @param jsonParam param
     */
    public void reportTaskStep(JSONObject jsonParam, String publishedProjectId) {

        try {
            HttpClientUtils.postJson(_backendUrl + _REPORT_TASK_STEP_URI, jsonParam, _createHeader(publishedProjectId));
        } catch (Exception e) {
            LOG.error("[{}][report task fail:{}]", publishedProjectId, e.getMessage());
            throw e;
        }
    }

    public JSONObject proxyConfig(String type) {
        try {
            return HttpClientUtils.getJson(_backendUrl + _AGENT_PROXY_CONFIG + "?type=" + type, _defaultHeader());
        } catch (Exception e) {
            LOG.error("[load proxy config:{} with error:{}]", type, e.getMessage());
            throw e;
        }
    }

    /**
     * 上报report task result
     *
     * @param jsonParam param
     */
    public void reportTaskResult(JSONObject jsonParam, String publishedProjectId) {
        try {
            HttpClientUtils.postJson(_backendUrl + _REPORT_TASK_RESULT_URI, jsonParam, _createHeader(publishedProjectId));
        } catch (Exception e) {
            LOG.error("[{}][report task fail:{}]", publishedProjectId, e.getMessage());
            throw e;
        }
    }

    /**
     * 上报published project 信息
     *
     * @param jsonParam param
     */
    public void publishedProjectStarted(JSONObject jsonParam, String publishedProjectId) {
        try {
            HttpClientUtils.postJson(_backendUrl + _PUBLISHED_TASK_START_UP_URI, jsonParam, _createHeader(publishedProjectId));
            LOG.info("[{}][report published project started with param:{} ]", publishedProjectId, jsonParam);
        } catch (HttpClientErrorException.Unauthorized e) {
            LOG.error("[{}][report published project fail:{},ignore Unauthorized]", publishedProjectId, e.getMessage(), e);
        } catch (Exception e) {
            LOG.error("[{}][report published project fail:{}]", publishedProjectId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 获取当前Agent下所有的published tasks
     * [
     *     {
     *         "data": {
     *             "agentId": "mcvwuyfdrdh6adebug",
     *             "dbName": "a1",
     *             "id": "a1_debug",
     *             "properties": {
     *                 "ai": {
     *                     "ai.trainPath": "/Users/chen/.ai/train/a1",
     *                     "ai.agentId": "mcvwuyfdrdh6adebug",
     *                     "ai.logPath": "/Users/chen/.ai/log/a1",
     *                     "ai.modelPath": "/Users/chen/.ai/model/a1"
     *                 },
     *                 "createBy": "0",
     *                 "createTime": "0",
     *                 "projectId": "debug",
     *                 "updateBy": "0",
     *                 "updateTime": "0"
     *             },
     *             "status": "deploying",
     *             "token": "YTI5Yjk4YjAtOWMzYy00YjMwLWE1OGEtN2M1ZmU3ZGU0M2Y0"
     *         }
     *     }
     * ]
     * @return projects
     */
    public String loadPublishedProjects() {
        try {
            String res = HttpClientUtils.getString(_backendUrl + _PUBLISHED_TASK_URI, _defaultHeader());
            LOG.info("success load all published projects:{}", res);
            return res;
        } catch (Exception e) {
            LOG.error("load published project fail:{}", e.getMessage(), e);
            throw new RestException(StatusCodes.InternalError);
        }
    }

    private HttpHeaders _defaultHeader() {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.addAll(_DEFAULT_HTTP_HEADERS);
        return httpHeaders;
    }

    private HttpHeaders _createHeader(String publishedProjectId) {
        HttpHeaders httpHeaders = _defaultHeader();
        httpHeaders.add(_PUBLISHED_PROJECT_ID_HEADER, publishedProjectId);
        return httpHeaders;
    }
}
