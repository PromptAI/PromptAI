package com.zervice.kbase.llm.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.zervice.common.ding.DingTalkSender;
import com.zervice.common.restful.exception.RestException;
import com.zervice.common.restful.exception.StatusCodes;
import com.zervice.common.utils.HttpClientUtils;
import com.zervice.kbase.ServerInfo;
import com.zervice.kbase.llm.pojo.OpenAILlmConfig;
import com.zervice.kbase.llm.pojo.openai.OpenAIEmbedding;
import com.zervice.kbase.llm.pojo.openai.OpenAIMessage;
import com.zervice.kbase.llm.service.LlmService;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;

import java.io.InputStream;
import java.sql.Connection;

/**
 * @author chenchen
 * @Date 2023/10/16
 */
@Log4j2
public class OpenAILlmService implements LlmService {
    private final static String _SERVER = "https://api.openai.com";
    private final static String _URI_EMBEDDING = "/v1/embeddings";
    private final static String _URI_CHAT_COMPLETIONS = "/v1/chat/completions";
    private final OpenAILlmConfig _config;
    private final String _dbName;

    public OpenAILlmService(OpenAILlmConfig config, String dbName) {
        this._config = config;
        this._dbName = dbName;

        if (_config == null || StringUtils.isBlank(config.getApiKey())) {
            throw new RestException(StatusCodes.LlmInvalidOpenAIConfig);
        }
    }

    @Override
    public InputStream chatStream(JSONObject data) {
        throw new UnsupportedOperationException();
    }

    private static HttpHeaders _headers(String openAIKey) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Authorization", "Bearer " + openAIKey);
        httpHeaders.add("Content-Type", "application/json");
        return httpHeaders;
    }

    /**
     * OepnAI不检查剩余token
     * @return
     * @throws Exception
     */
    @Override
    public Boolean enoughToken(Connection conn) throws Exception {
        return Boolean.TRUE;
    }


    @Override
    public JSONObject chat(JSONObject data) {
        String url = _SERVER + _URI_CHAT_COMPLETIONS;
        LOG.info("[{}][chat completions with data:{}]", _dbName, data.toJSONString());
        try {
            JSONObject result = HttpClientUtils.postJson(url, data, _headers(_config.getApiKey()));
            LOG.info("[{}][request gpt with result:{}]", _dbName, result);
            return result;
        } catch (Exception e) {
            LOG.error("[{}][chat completions with error:{} with data:{}]", _dbName, e.getMessage(), data, e);
            DingTalkSender.sendQuietly(String.format("[%s:%s][chat completions with error:%s with data:%s]", ServerInfo.getName(), _dbName, e.getMessage(), data));

            throw e;
        }
    }

    @Override
    public JSONObject embedding(JSONObject data) {
        String url = _SERVER + _URI_EMBEDDING;
        LOG.info("[{}][embedding with data:{}]", _dbName, data);
        try {
            OpenAIEmbedding embeddingReq = OpenAIEmbedding.factory(data.getString("input"));
            JSONObject embedding = HttpClientUtils.postJson(url, embeddingReq.toJSON(), _headers(_config.getApiKey()));
            return embedding;
        } catch (Exception e) {
            LOG.error("[{}][embedding with error:{} with data:{}]", _dbName, e.getMessage(), data, e);
            DingTalkSender.sendQuietly(String.format("[%s:%s][embedding with error:%s with data:%s]", ServerInfo.getName(), _dbName, e.getMessage(), data));

            throw e;
        }
    }

    /**
     * test openai token
     * @param config
     * @param msg
     * @param dbName
     */
    public static void test(OpenAILlmConfig config, String msg, String dbName) {
        OpenAIMessage message = OpenAIMessage.Gpt3(msg);
        String url = _SERVER + _URI_CHAT_COMPLETIONS;
        JSONObject chatRes = HttpClientUtils.postJson(url, message.toJson(), _headers(config.getApiKey()));
        LOG.info("[{}][success test openai key with result:{}]", dbName, chatRes);
    }
}
