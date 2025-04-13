package com.zervice.broker.restful.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Stopwatch;
import com.zervice.broker.backend.KbChatService;
import com.zervice.broker.backend.KbMessageService;
import com.zervice.broker.backend.ProjectService;
import com.zervice.broker.backend.PublishedProjectService;
import com.zervice.broker.restful.filter.BrokerFilter;
import com.zervice.broker.service.impl.MessageService;
import com.zervice.common.agent.pojo.SimplePublishedProject;
import com.zervice.common.model.MessageRes;
import com.zervice.common.model.SendMessageModel;
import com.zervice.common.pojo.chat.ChatPojo;
import com.zervice.common.pojo.chat.MessageEvaluateModel;
import com.zervice.common.pojo.chat.MessageResComposer;
import com.zervice.common.pojo.chat.ProjectComponentBotPojo;
import com.zervice.common.pojo.common.EmptyResponse;
import com.zervice.common.utils.LayeredConf;
import com.zervice.common.utils.NetworkUtils;
import com.zervice.common.utils.ServletUtils;
import com.zervice.common.utils.TimeRecordHelper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.*;

/**
 * @author Peng Chen
 * @date 2022/7/5
 */
@Log4j2
@RestController
@RequestMapping("/api/message")
public class MessageController {
    private final KbMessageService _kbMessageService = KbMessageService.getInstance();
    private final KbChatService _kbChatService = KbChatService.getInstance();
    private final PublishedProjectService _publishedProjectService = PublishedProjectService.getInstance();

    /**
     * 用来处理消息
     */
    private ExecutorService _executorService = new ThreadPoolExecutor(10, 200,
            60L, TimeUnit.SECONDS,
            new SynchronousQueue<>());

    private Integer messageReplayTimeOut = LayeredConf.getInt("message.replay.time.out", 20);


    @PostMapping("file")
    public Object file(@RequestParam String chatId,
                       @RequestPart MultipartFile file,
                       HttpServletRequest request,
                       @RequestHeader(BrokerFilter.X_EXTERNAL_PUBLISHED_PROJECT_HEADER) String publishedProjectId,
                       @RequestHeader(BrokerFilter.X_EXTERNAL_PUBLISHED_PROJECT_TOKEN_HEADER) String token) throws Exception {
        String ip = NetworkUtils.getRemoteIP(request);
        SimplePublishedProject project = _publishedProjectService.get(publishedProjectId, token);
        String projectId = project.getProjectId();
        String account = project.getAccountDbName();
        return _kbMessageService.uploadFile(account, chatId, ip, projectId, file);
    }

    public static JSONArray replayFallback(String chatId, String account, String projectId, String publishedProjectId) {
        JSONObject fallbackRes = ProjectService.getInstance().fallback(projectId, chatId, publishedProjectId, account);
        String fallbackStr = fallbackRes.getString("fallback");
        LOG.info("[{}:{}][success get fallback from publishedProjectId:{}, fallback:{}",
                account, chatId, publishedProjectId, fallbackStr);
        // generate res
        List<Object> result = MessageResComposer.composeText(fallbackStr);
        return new JSONArray(result);
    }

    /**
     * 优先级：
     * 1、从chat: 来源于project setting
     * 2、从http: 浏览器环境/语言的header
     */
    private Locale _getLocale(ChatPojo chatPojo) {
        String localeFromChat = chatPojo.getProperties().getLocale();
        if (StringUtils.isNotBlank(localeFromChat)) {
            try {
                Locale locale = Locale.forLanguageTag(localeFromChat);
                if (locale != null) {
                    return locale;
                }
            } catch (Exception e) {
                LOG.info("[convert locale fail from:{}]", localeFromChat);
            }
        }

        return ServletUtils.getLocale();
    }

    @PutMapping("evaluate")
    public Object evaluate(@RequestBody @Validated MessageEvaluateModel evaluate,
                           @RequestHeader(BrokerFilter.X_EXTERNAL_PUBLISHED_PROJECT_HEADER) String publishedProjectId,
                           @RequestHeader(BrokerFilter.X_EXTERNAL_PUBLISHED_PROJECT_TOKEN_HEADER) String token) {
        SimplePublishedProject project = _publishedProjectService.get(publishedProjectId, token);
        _kbMessageService.evaluate(evaluate, project.getAccountDbName());
        return EmptyResponse.empty();
    }

    private JSONArray _replayError(String error) {
        // generate res
        JSONObject res = ProjectComponentBotPojo.buildTextRes(error);

        JSONArray result = new JSONArray();
        result.add(res);
        return result;
    }

    @PostMapping
    public Object message(@RequestBody @Validated SendMessageModel message,
                          HttpServletRequest request,
                          @RequestHeader(BrokerFilter.X_EXTERNAL_PUBLISHED_PROJECT_HEADER) String publishedProjectId,
                          @RequestHeader(BrokerFilter.X_EXTERNAL_PUBLISHED_PROJECT_TOKEN_HEADER) String token) throws Exception {
        long startTime = System.currentTimeMillis();
        SimplePublishedProject project = _publishedProjectService.get(publishedProjectId, token);

        String projectId = project.getProjectId();
        String ip = NetworkUtils.getRemoteIP(request);
        String scene = message.getScene();
        message.setIp(ip);
        message.setProjectId(projectId);
        message.setPublishedProjectId(publishedProjectId);

        String account = project.getAccountDbName();

        _preHandleMessage(message);

        // 确保后续是有chat存在
        ChatPojo chatPojo = _kbChatService.createChatIfNotExist(message.getChatId(), projectId, publishedProjectId, scene, account, request);

        String chatId = message.getChatId();
        String input = message.getMessage();
        String agentId = project.getAgentId();

        // 从http线程获取到local, 传递到处理message的线程
        Locale locale = _getLocale(chatPojo);

        LOG.info("[{}:{}][message:{} received]", account, chatId, input);
        Stopwatch stopwatch = Stopwatch.createStarted();
        try {
            Future<MessageRes> future = _executorService.submit(() -> {
                TimeRecordHelper.init();
                MessageService messageService =MessageService.getInstance();
                MessageRes res = messageService.setLocal(locale).message(message, project, account);
                messageService.postMessage(res);
                return res;
            });

            MessageRes res = future.get(messageReplayTimeOut, TimeUnit.SECONDS);

            // fallback
            if (res == null) {
                JSONArray fallbackRes = replayFallback(chatId, account, projectId, publishedProjectId);
                res = MessageRes.factory(fallbackRes, MessageRes.OUTPUT_TYPE_FALLBACK);
            }

            long elapsed = stopwatch.elapsed(TimeUnit.MILLISECONDS);
            _saveMessage2Backend(account, startTime, agentId, message, res);

            LOG.info("[{}:{}][success send message:{} with reply:{}, elapsed:{}]", account, chatId, input, res, elapsed);
            res.getProperties().setEmbedding(null);
            _setTime4Res(startTime, res);

            if (JSONObject.toJSONString(res).contains("\"rootType\":\"conversation\"")) {
                chatPojo.setLastInFlow(true);
            }

            return res;
        } catch (Exception ex) {
            MessageRes res = _saveError(message, startTime, agentId,ex.getMessage() + "\n" + ex.getClass(), account);
            long elapsed = stopwatch.elapsed(TimeUnit.MILLISECONDS);

            LOG.error("[{}:{}][send message error:{} agentClient:{}, message:{}, elapsed:{}]", account, chatId,
                    ex.getMessage(), agentId, JSONObject.toJSONString(message), elapsed, ex);
            _setTime4Res(startTime, res);
            return res;
        }
    }

    private void _setTime4Res(long startTime, MessageRes res) {
        MessageRes.Prop prop = res.getProperties();
        prop.setQueryTime(startTime);
        prop.setAnswerTime(System.currentTimeMillis());
    }

    private MessageRes _saveError(SendMessageModel message, long startTime, String agentId, String error, String account) {
        JSONArray errorRes = _replayError(error);
        MessageRes messageRes = MessageRes.factory(errorRes, MessageRes.OUTPUT_TYPE_ERROR);
        _saveMessage2Backend(account, startTime, agentId, message, messageRes);
        return messageRes;
    }

    private void _saveMessage2Backend(String account, long startTime, String agentClientId, SendMessageModel message, MessageRes res) {
        _kbMessageService.saveMessage2Backend(account, startTime, agentClientId, message, res);
    }

    /**
     * 处理特殊字符，防止攻击
     */
    private void _preHandleMessage(SendMessageModel message) {
        String msg = message.getMessage();
        String content = message.getContent();

        msg = _handleSpecialMessage(msg);
        content = _handleSpecialMessage(content);

        message.setMessage(msg);
        message.setContent(content);
    }

    private String _handleSpecialMessage(String input) {
        // Chat GPT的Prompt攻击
        Set<String> specialStr = Set.of("<|im_end|>", "<|im_start|>");
        for (String s : specialStr) {
            if (input.indexOf(s) > 0) {
                input = input.replace(s, " ");
            }
        }
        return input;
    }
}
