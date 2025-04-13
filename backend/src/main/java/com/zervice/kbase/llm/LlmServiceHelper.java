package com.zervice.kbase.llm;

import com.alibaba.fastjson.JSONObject;
import com.zervice.common.ding.DingTalkSender;
import com.zervice.common.restful.exception.RestException;
import com.zervice.common.restful.exception.StatusCodes;
import com.zervice.kbase.ServerInfo;
import com.zervice.kbase.database.dao.ConfigurationDao;
import com.zervice.kbase.database.pojo.Configuration;
import com.zervice.kbase.database.utils.DaoUtils;
import com.zervice.kbase.llm.pojo.LlmConfig;
import com.zervice.kbase.llm.service.LlmService;
import com.zervice.kbase.llm.service.impl.OpenAILlmService;
import lombok.Cleanup;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;

import java.sql.Connection;

/**
 * 决定具体调用的LLM服务
 * - 直接调用GPT
 * - 决定实现类。。
 *
 * @author chenchen
 * @Date 2023/10/13
 */
@Log4j2
public class LlmServiceHelper {


    public static JSONObject embedding(JSONObject data, String dbName) throws Exception {
        @Cleanup Connection conn = DaoUtils.getConnection(true);
        return embedding(data, dbName, conn);
    }
    public static JSONObject embedding(JSONObject data, String dbName, Connection conn) throws Exception {
        LlmService llmService = choose(dbName, conn);
        return llmService.embedding(data);
    }
    public static JSONObject chat(JSONObject data, String dbName, Connection conn) throws Exception {
        LlmService llmService = choose(dbName, conn);
        return llmService.chat(data);
    }

    public static LlmService choose(String dbName) throws Exception {
        @Cleanup Connection conn = DaoUtils.getConnection(true);
        return choose(dbName, conn);
    }

    public static boolean enoughToken(String dbName) {
        try {
            @Cleanup Connection conn = DaoUtils.getConnection(true);
            return choose(dbName, conn).enoughToken(conn);
        } catch (RestException e) {
            throw e;
        } catch (Exception e) {
            LOG.error("[{}][check enough token with error:{}]", dbName, e.getMessage(), e);
            throw new RestException(StatusCodes.InternalError);
        }
    }

    public static void requireEnoughToken(String dbName) {
        if (!enoughToken(dbName)) {
            throw new RestException(StatusCodes.RestTokenNotEnough);
        }
    }
    public static LlmService choose(String dbName, Connection conn) throws Exception {
        LlmConfig config = _config(dbName, conn);
        switch (config.getType()) {
            case LlmConfig.OPEN_AI:
                return new OpenAILlmService(config.getOpenai(), dbName);
            default:
                LOG.info("[{}][unknown llm server config:{}]", dbName, JSONObject.toJSONString(config));
                DingTalkSender.sendQuietly(String.format("[%s:%s][unknown llm server config:%s]", ServerInfo.getName(), dbName, JSONObject.toJSONString(config)));
                throw new RestException(StatusCodes.LlmUnknownConfig);
        }
    }

    /**
     * 获取系统的配置
     *
     */
    private static LlmConfig _config(String dbName, Connection conn) throws Exception {
        String config = ConfigurationDao.getString(conn, dbName, Configuration.LLM_CONFIG, null);

        // 使用系统的默认配置
        if (StringUtils.isBlank(config)) {
            return LlmConfig.defaultConfig();
        }

        try {
            return JSONObject.parseObject(config).toJavaObject(LlmConfig.class);
        } catch (Exception e) {
            LOG.error("[{}][parse llm config with error:{} from:{}, use default]", dbName, e.getMessage(), config, e);
            DingTalkSender.sendQuietly(String.format("[%s:%s][parse llm config with error:%s, from:%s]", ServerInfo.getName(), dbName, e.getMessage(), config));

            return LlmConfig.defaultConfig();
        }
    }
}
