package com.zervice.kbase.ai.convert.pojo;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.lang.Pair;
import com.alibaba.fastjson.JSONObject;
import com.zervice.common.restful.exception.RestException;
import com.zervice.common.restful.exception.StatusCodes;
import com.zervice.kbase.ai.convert.PublishContext;
import com.zervice.kbase.ai.convert.VariablesHelper;
import com.zervice.kbase.api.restful.pojo.RestBaseProjectComponent;
import com.zervice.kbase.api.restful.pojo.RestProjectComponentBot;
import com.zervice.kbase.api.restful.pojo.RestProjectComponentWebhook;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * webhook -> Python function
 *
 * @author chenchen
 * @Date 2024/9/12
 */
@Log4j2
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Python {

    private static final String _WEBHOOK_TEMPLATE = "templates/ai/webhook.py";

    /**
     * 内置填充变量的webhook
     */
    private static final String _CONTEXT_VARIABLES_WEBHOOK_TEMPLATE = "templates/ai/context_variables.py";

    private static final String NAME_PREFIX = "py_";

    /**
     * 系统占用
     */
    public static final String CONTEXT_VARIABLES_NAME = "context_variables";

    /**
     * webhook name
     */
    private String _name;

    /**
     * python code
     */
    private String _body;

    /**
     * 使用的变量
     */
    private Set<String> _required;

    /**
     * 1、使用的变量
     * 2、产生的变量
     */
    private Set<String> _args;

    public Python(RestProjectComponentWebhook webhook, PublishContext context) {
        // 防止名称出现中文、空格等特殊符号，这里使用 name 的hash
        this._name = buildPythonName(webhook.getText());
        this._body = _buildBody(webhook, context);

        // webhook 使用的变量
        this._required = _parseRequired(webhook);

        // 这一行必须要在 _required 之后，因为_args 会添加 _required 的值
        this._args = _parseArgs(webhook);
    }

    public Python(String name, String body, Set<String> args) {
        this._name = name;
        this._body = body;

        this._args = args;
        this._required = Set.of();
    }

    public static String buildPythonName(String name) {
//        return NAME_PREFIX + MD5.create().digestHex16(name);
        return name;
    }

    public static List<Python> buildPythons(PublishContext context) {
        Set<String> usedWebhookIds = _filterUsedWebhooks(context);
        return usedWebhookIds.stream()
                .map(webhookId -> {
                    RestProjectComponentWebhook webhook = new RestProjectComponentWebhook(context.get(webhookId));
                    return new Python(webhook, context);
                })
                .collect(Collectors.toList());
    }

    /**
     * 将需要填充的变量转换为 webhook,调用promptAI的接口获取变量值，最后使用webhook的mappings填充变量
     *
     * @param name      webhook名称
     * @param body      请求体:请勿使用chatId、accountId、projectId作为变量名，这个三个变量已被系统占用。
     *                  可以通过${sender}、${accountName}、${projectId}来获取当前对话对应的变量值。
     * @param variables 需要渲染的变量：变量名 -> jsonpath
     * @param url       请求地址: backend 提供的接口，通过chatId获取自动填充的变量
     * @param headers   header
     * @return Python
     */
    public static Python contextVariables(String name,
                                          JSONObject body,
                                          List<Pair> variables,
                                          String url,
                                          Map<String, String> headers,
                                          String dbName) {
        headers.put("Content-Type", "application/json");

        // llmChat 支持的内置变量
        // 以下三个变量名修改后，需要修改对应接口
        JSONObject internalVariables = new JSONObject(true);
        internalVariables.put("chatId", "${sender}");
        internalVariables.put("accountName", "${accountName}");
        internalVariables.put("publishedProjectId", "${projectId}");

        // 合并变量,internalVariables优先级更高，如果body中有重名的变量，则会被覆盖
        if (body == null) {
            body = internalVariables;
        } else {
            body.putAll(internalVariables);
        }

        JSONObject headersJson = new JSONObject(true);
        headersJson.putAll(headers);

        // build response handle:text & error message could be empty text
        RestProjectComponentWebhook.ResponseHandle handle = RestProjectComponentWebhook.ResponseHandle.factory(variables, "", "");
        //build python context
        Map<String, Object> pythonContextMap = new PythonContext()
                .put("functionName", name)
                .put("url", url)
                .put("headers", headersJson)
                .put("requestBody", body.toJSONString())
                .put("responseHandle", JSONObject.toJSONString(handle))
                .build();

        // 需要赋值的变量，放在args中
        Set<String> args = variables.stream()
                .map(p -> p.getKey().toString())
                .collect(Collectors.toSet());

        String webhookBody = _body(_CONTEXT_VARIABLES_WEBHOOK_TEMPLATE, pythonContextMap, dbName);
        return new Python(name, webhookBody, args);
    }

    private static String _body(String template, Map<String, Object> webhookContext, String dbName) {
        // read template file
        Resource resource = new ClassPathResource(template);
        try {
            String content = IoUtil.read(resource.getInputStream(), StandardCharsets.UTF_8);

            for (Map.Entry<String, Object> entry : webhookContext.entrySet()) {
                Object value = entry.getValue();
                if (value == null) {
                    value = "";
                }

                content = content.replace(entry.getKey(), value.toString());
            }

            LOG.info("[{}][success build webhook:{}]", dbName, content);
            return content;
        } catch (Exception e) {
            LOG.error("[{}][build webhook with error:{}]", dbName, e.getMessage(), e);
            throw new RestException(StatusCodes.InternalError);
        }
    }

    private String _buildBody(RestProjectComponentWebhook webhook, PublishContext context) {
        if (webhook.pythonCode()) {
            return webhook.getData().getCode();
        }

         return _body(_WEBHOOK_TEMPLATE, _prepareBodyContent(webhook, context), context.dbName());
    }

    private Map<String, Object> _prepareBodyContent(RestProjectComponentWebhook webhook, PublishContext context) {
        String bodyText = "text/plain".equals(webhook.getRequestBodyType()) ? webhook.getRequestBody() : "";

        JSONObject headers = new JSONObject();
        if (CollectionUtils.isNotEmpty(webhook.getHeaders())) {
            for (Pair header : webhook.getHeaders()) {
                headers.put(header.getKey().toString(), header.getValue());
            }
        }

        // append content-type header if not exists
        if ("application/json".equals(webhook.getRequestBodyType()) && !headers.containsKey("Content-Type")) {
            headers.put("Content-Type", "application/json");
        }

        PythonContext pythonContext = new PythonContext()
                .put("functionName", _name)
                .put("url", webhook.getUrl())
                // 未使用
                .put("label", "")
                .put("headers", headers)
                .put("requestBody", webhook.getRequestBody() == null ? "{}" : webhook.getRequestBody())
                .put("requestBodyText", bodyText)
                .put("requestType", webhook.getRequestType())
                .put("requestBodyType", webhook.getRequestBodyType())
                .put("responseType", RestProjectComponentBot.BotResponse.convert2MicaResponseType(webhook.getResponseType()))
                .put("responseHandle", webhook.getResponseHandle() != null ? JSONObject.toJSONString(webhook.getResponseHandle()) : null)
                .put("errorMsg", webhook.getResponseHandle() != null ? webhook.getResponseHandle().getErrorMsg() : null);
        return pythonContext.build();
    }

    private static Set<String> _filterUsedWebhooks(PublishContext context) {
        Set<String> usedWebhookIds = new HashSet<>(16);
        for (RestBaseProjectComponent root : context.getRoots()) {
            List<String> webhooks = PublishContext.flatChildren(root).stream()
                    // filter bots with webhook response
                    .filter(component -> {
                        String type = component.getType();

                        // 非 bot
                        if (!type.equals(RestProjectComponentBot.TYPE_NAME)) {
                            return false;
                        }

                        RestProjectComponentBot bot = (RestProjectComponentBot) component;
                        for (RestProjectComponentBot.BotResponse response : bot.getData().getResponses()) {
                            if (RestProjectComponentBot.BotResponse.TYPE_WEBHOOK.equals(response.getType())) {
                                return true;
                            }
                        }

                        return false;
                    })
                    // map to webhook id
                    .flatMap(b -> {
                        RestProjectComponentBot bot = ((RestProjectComponentBot) b);
                        return bot.getData().getResponses().stream()
                                .filter(response -> RestProjectComponentBot.BotResponse.TYPE_WEBHOOK.equals(response.getType()))
                                .map(response -> response.getContent().getString("id"));

                    })
                    .collect(Collectors.toList());

            usedWebhookIds.addAll(webhooks);
        }

        return usedWebhookIds;
    }

    public JSONObject toPublishPython() {
        JSONObject python = new JSONObject(true);
        python.put("name", _name);
        python.put("body", _body);
        python.put("args", _args);
        python.put("required", _required);
        return python;
    }

    /**
     * webhook 生成的变量
     * @param webhook
     * @return
     */
    private Set<String > _newArgs(RestProjectComponentWebhook webhook) {
        // response handler
        // 这里是需要创建的变量，需要从require中排除
        if (webhook.getResponseHandle() != null && CollectionUtils.isNotEmpty(webhook.getResponseHandle().getParse())) {
           return webhook.getResponseHandle().getParse().stream()
                    .map(p -> p.getKey().toString())
                    .collect(Collectors.toSet());

        }

        return new HashSet<>();
    }
     /**
     * 使用到的变量
     */
    private Set<String> _parseArgs(RestProjectComponentWebhook webhook) {
        if (webhook.pythonCode()) {
            return Set.of();
        }

        Set<String> args = new HashSet<>(16);

        // 新生成的
        args.addAll(_newArgs(webhook));

        // 会用到的
        args.addAll(_required);

        return args;
    }

    /**
     * 使用到的变量
     */
    private Set<String> _parseRequired(RestProjectComponentWebhook webhook) {
        if (webhook.pythonCode()) {
            return Set.of();
        }
        Set<String> args = new HashSet<>(16);

        // url
        String url = webhook.getUrl();
        args.addAll(VariablesHelper.parse(url));

        // request body
        String body = webhook.getRequestBody();
        if (StringUtils.isNotBlank(body)) {
            args.addAll(VariablesHelper.parse(body));
        }

        return args;
    }

    public static class PythonContext {
        private final Map<String, Object> _context;


        public PythonContext() {
            this._context = new HashMap<>(16);
        }

        public PythonContext put(String key, Object value) {
            String TEMPLATE = "${%s}";
            _context.put(String.format(TEMPLATE, key), value);
            return this;
        }

        public Map<String, Object> build() {
            return _context;
        }

    }

}
