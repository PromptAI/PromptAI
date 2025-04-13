package com.zervice.kbase.api.rpc;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.zervice.common.ding.DingTalkSender;
import com.zervice.common.restful.exception.RestException;
import com.zervice.common.restful.exception.StatusCodes;
import com.zervice.kbase.ServerInfo;
import com.zervice.kbase.api.BaseController;
import com.zervice.kbase.llm.LlmServiceHelper;
import com.zervice.kbase.llm.service.LlmService;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.*;

/**
 * Mica调用GPT
 *
 * @author chenchen
 * @Date 2023/8/16
 */
@Log4j2
@RestController
@RequestMapping("/rpc/rasa/")
public class RpcLLMController extends BaseController {

    @PostMapping("embedding")
    public Object embedding(@RequestBody JSONObject data,
                            @RequestHeader(RpcFilter.X_EXTERNAL_PROJECT_ID_HEADER) String projectId,
                            @RequestHeader(RpcFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName,
                            @RequestHeader(RpcFilter.X_EXTERNAL_PUBLISHED_PROJECT_ID_HEADER) String publishedProjectId) throws Exception {
        LOG.info("[{}][request embedding from gpt:{} with data:{}]", dbName, publishedProjectId, data.toString(SerializerFeature.PrettyFormat));

        LlmService llmService = LlmServiceHelper.choose(dbName);
        // request api
        try {
            return llmService.embedding(data);
        } catch (RestException re) {
            LOG.error("[{}][request embedding from gpt fail]", dbName);
            DingTalkSender.sendQuietly(String.format("[%s:%s][request embedding fail:%s]", ServerInfo.getName(), dbName, re.getMessage()));
            throw re;
        } catch (Exception e) {
            LOG.error("[{}][request embedding from gpt fail]", dbName);
            DingTalkSender.sendQuietly(String.format("[%s:%s][request embedding fail:%s]", ServerInfo.getName(), dbName, e.getMessage()));
            throw new RestException(StatusCodes.InternalError);
        }
    }
    @PostMapping("message")
    public Object message(@RequestBody JSONObject data,
                          @RequestHeader(RpcFilter.X_EXTERNAL_PROJECT_ID_HEADER) String projectId,
                          @RequestHeader(RpcFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName,
                          @RequestHeader(RpcFilter.X_EXTERNAL_PUBLISHED_PROJECT_ID_HEADER) String publishedProjectId) throws Exception {
        LOG.info("[{}][request gpt from gpt:{} with data:{}]", dbName, publishedProjectId, data.toString(SerializerFeature.PrettyFormat));

        LlmService llmService = LlmServiceHelper.choose(dbName);

        // request api
        try {
            return llmService.chat(data);
        } catch (RestException re) {
            LOG.error("[{}][request gpt from gpt fail]", dbName);
            DingTalkSender.sendQuietly(String.format("[%s:%s][request llm fail:%s]", ServerInfo.getName(), dbName, re.getMessage()));

            throw re;
        } catch (Exception e) {
            LOG.error("[{}][request gpt from gpt fail]", dbName);
            DingTalkSender.sendQuietly(String.format("[%s:%s][request llm fail:%s]", ServerInfo.getName(), dbName, e.getMessage()));

            throw new RestException(StatusCodes.InternalError);
        }
    }
}