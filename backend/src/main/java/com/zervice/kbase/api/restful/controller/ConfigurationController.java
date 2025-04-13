package com.zervice.kbase.api.restful.controller;

import com.alibaba.fastjson.JSONObject;
import com.zervice.common.pojo.common.EmptyResponse;
import com.zervice.common.restful.exception.RestException;
import com.zervice.common.restful.exception.StatusCodes;
import com.zervice.kbase.AccountCatalog;
import com.zervice.kbase.api.BaseController;
import com.zervice.kbase.api.restful.AuthFilter;
import com.zervice.kbase.api.restful.pojo.PageRequest;
import com.zervice.kbase.api.restful.pojo.RestConfiguration;
import com.zervice.kbase.api.restful.pojo.RestLlmConfig;
import com.zervice.kbase.api.restful.util.PageStream;
import com.zervice.kbase.database.dao.ConfigurationDao;
import com.zervice.kbase.database.pojo.Configuration;
import com.zervice.kbase.database.pojo.User;
import com.zervice.kbase.database.utils.DaoUtils;
import com.zervice.kbase.llm.pojo.LlmConfig;
import com.zervice.kbase.llm.pojo.OpenAILlmConfig;
import com.zervice.kbase.llm.service.impl.OpenAILlmService;
import com.zervice.kbase.rbac.RBACConstants;
import lombok.Cleanup;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.sql.Connection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * system configuration
 *
 * @author Peng Chen
 * @date 2020/6/17
 */
@Log4j2
@RequestMapping(value = "/api/settings/configurations")
@RestController
public class ConfigurationController extends BaseController {

    @GetMapping
    public Object get(@RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName,
                      @RequestHeader(AuthFilter.X_EXTERNAL_USER_ID_HEADER) String uid,
                      PageRequest pageRequest) throws Exception {
        long userId = User.fromExternalId(uid);

        AccountCatalog accountCatalog = AccountCatalog.ensure(dbName);
        accountCatalog.getAcm().checkAccess(userId, RBACConstants.RESOURCE_CONFIGURATIONS, RBACConstants.OPERATION_GET);

        @Cleanup Connection conn = DaoUtils.getConnection();

        List<Configuration> configurations = ConfigurationDao.get(conn, dbName);

        int totalCount = configurations.size();

        configurations = PageStream.of(Configuration.class, pageRequest, configurations.stream());

        List<RestConfiguration> restConfigurations = configurations.stream()
                .map(RestConfiguration::new)
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>(2);
        result.put("totalElements", totalCount);
        result.put("contents", restConfigurations);

        return result;
    }

    @PutMapping
    public Object update(@RequestBody @Validated RestConfiguration configuration,
                       @RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName,
                       @RequestHeader(AuthFilter.X_EXTERNAL_USER_ID_HEADER) String uid) throws Exception{
        long userId = User.fromExternalId(uid);

        AccountCatalog accountCatalog = AccountCatalog.ensure(dbName);
        accountCatalog.getAcm().checkAccess(userId, RBACConstants.RESOURCE_CONFIGURATIONS, RBACConstants.OPERATION_UPDATE);

        @Cleanup Connection conn = DaoUtils.getConnection(false);

        Configuration dbConfig = ConfigurationDao.getByName(conn, dbName, configuration.getName());
        if (dbConfig == null) {
            LOG.info("[{}][update configuration fail, configuration not found] Configuration.name={}",
                    dbName, configuration.getName());
            throw new RestException(StatusCodes.CONFIGURATION_NOT_EXISTS, configuration.getName());
        }

        ConfigurationDao.update(conn, dbName, configuration.getName(), configuration.getValue());

        conn.commit();
        return EmptyResponse.empty();
    }

    @GetMapping("llm")
    public Object llm(@RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName,
                      @RequestHeader(AuthFilter.X_EXTERNAL_USER_ID_HEADER) String uid) throws Exception {
        @Cleanup Connection conn = DaoUtils.getConnection(true);
        String config = ConfigurationDao.getString(conn, dbName, Configuration.LLM_CONFIG, null);
        if (StringUtils.isBlank(config)) {
            return new RestLlmConfig(LlmConfig.defaultConfig());
        }

        try {
            LlmConfig llmConfig = JSONObject.parseObject(config).toJavaObject(LlmConfig.class);
            return new RestLlmConfig(llmConfig);
        } catch (Exception e) {
            LOG.error("[{}][parse llm config with error:{} from:{}, use default]", dbName, e.getMessage(), config, e);
            return new RestLlmConfig(LlmConfig.defaultConfig());
        }
    }

    @PutMapping("llm")
    public Object llm(@RequestBody LlmConfig data,
                      @RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName,
                      @RequestHeader(AuthFilter.X_EXTERNAL_USER_ID_HEADER) String uid) throws Exception{
        @Cleanup Connection conn = DaoUtils.getConnection(true);
        Configuration configuration = ConfigurationDao.getByName(conn, dbName, Configuration.LLM_CONFIG);


        switch (data.getType()) {
            case LlmConfig.OPEN_AI:
                _beforeCheckOpenaiKey(configuration, data, dbName);
                _checkOpenAIConfig(data.getOpenai(), dbName);
                break;
            default:
                throw new RestException(StatusCodes.LlmUnknownConfig);
        }


        if (configuration == null) {
            ConfigurationDao.add(conn, dbName, Configuration.LLM_CONFIG, JSONObject.toJSONString(data));
        } else {
            ConfigurationDao.update(conn, dbName, Configuration.LLM_CONFIG, JSONObject.toJSONString(data));
        }

        LOG.info("[{}][user:{} success update llm config]", dbName, User.fromExternalId(uid));
        return data;
    }

    /**
     * 解决没有更新key的情况。前端提交的key没有变化时是脱敏后的。这种情况将原始的key拿出来进行替换，进行后续流程
     */
    private void _beforeCheckOpenaiKey(Configuration configuration, LlmConfig data, String dbName) {
        // 判断是否有更改
        if (configuration != null) {
            try {
                LlmConfig llmConfig = JSONObject.parseObject(configuration.getValue()).toJavaObject(LlmConfig.class);
                // 从未设置openai key时，这可能是null
                if (llmConfig.getOpenai() != null && llmConfig.getOpenai().getApiKey() != null) {
                    String encrypted = new RestLlmConfig(llmConfig).getOpenai().getApiKey();
                    String restKey = data.getOpenai().getApiKey();

                    // 没有更新,将原始的key替换进去进行验证
                    if (Objects.equals(restKey, encrypted)) {
                        data.getOpenai().setApiKey(llmConfig.getOpenai().getApiKey());
                    }
                }
            } catch (Exception e) {
                LOG.info("[{}][check openai key in db failed:{}]", dbName, e.getMessage(), e);
            }
        }
    }

    private void _checkOpenAIConfig(OpenAILlmConfig config, String dbName) {
        if (config == null) {
            throw new RestException(StatusCodes.LlmInvalidOpenAIConfig);
        }

        try {
            OpenAILlmService.test(config, "hello", dbName);
        } catch (Exception e) {
            LOG.error("[{}][test openai config with error:{}]", dbName, e.getMessage(), e);
            throw new RestException(StatusCodes.LlmInvalidOpenAIConfig);
        }
    }
}
