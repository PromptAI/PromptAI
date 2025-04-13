package com.zervice.kbase.service.impl.mica.pojo;

import cn.hutool.core.lang.Pair;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.zervice.kbase.api.restful.pojo.RestProjectComponentBot;
import com.zervice.kbase.api.restful.pojo.RestProjectComponentWebhook;
import com.zervice.kbase.database.pojo.ProjectComponent;
import lombok.*;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Builder
@Setter
@Getter
@Log4j2
@AllArgsConstructor
@NoArgsConstructor
public class Webhook {
    private String _name;
    private Set<String> _args;
    private String _body;
    private Set<String> _required;

    public static List<Webhook> parse(Map<String, Function> webhooks) {
        if (webhooks == null || webhooks.isEmpty()) {
            return List.of();
        }

        List<Webhook> result = new ArrayList<>(webhooks.size());
        for (String key : webhooks.keySet()) {
            Function item = webhooks.get(key);
            JSONArray args = item.getArgs();

            Set<String> argsSet = args != null && !args.isEmpty() ? new HashSet<>(args.toJavaList(String.class)) : Set.of();
            Set<String> required = Set.of();
            String body = item.getBody();
            result.add(Webhook.factory(key, body, argsSet, required));
        }

        return result;
    }

    /**
     * <pre>
     * {
     * 	"webhook1": {
     * 		"args": ["xx", "xx", "xx"],
     * 		"body": "string",
     * 		"required": ["xx", "xx", "xx"]
     *     },
     * 	"webhook2": {
     * 		"args": ["xx", "xx", "xx"],
     * 		"body": "string",
     * 		"required": ["xx", "xx", "xx"]
     *   }
     * }
     * </pre>
     *
     * @param webhooks
     * @return
     */
    public static List<Webhook> parse(JSONObject webhooks) {
        if (webhooks == null || webhooks.isEmpty()) {
            return List.of();
        }

        List<Webhook> result = new ArrayList<>(webhooks.size());
        for (String key : webhooks.keySet()) {
            JSONObject item = webhooks.getJSONObject(key);
            Set<String> args = new HashSet<>(item.getJSONArray("args").toJavaList(String.class));
            Set<String> required = new HashSet<>(item.getJSONArray("required").toJavaList(String.class));
            String body = item.getString("body");
            result.add(Webhook.factory(key, body, args, required));
        }

        return result;
    }

    public static Webhook factory(String name, String body, Set<String> args, Set<String> required) {
        return Webhook.builder()
                .name(name).body(body).args(args).required(required)
                .build();
    }

    public RestProjectComponentWebhook toComponent(String projectId) {
        String url = url();
        String requestType = requestType();

        RestProjectComponentWebhook w;
        if (StringUtils.isBlank(url) || StringUtils.isBlank(requestType)) {
            w = _parseToPythonWebhook();
        } else {
            w = _parseToEditableWebhook(url, requestType);
        }

        w.setParentId(projectId);
        w.setId(ProjectComponent.generateId(ProjectComponent.TYPE_WEBHOOK));
        return w;
    }

    /**
     * 由界面化编辑的 网络请求
     *
     * @param url         请求地址
     * @param requestType 请求方法
     */
    private RestProjectComponentWebhook _parseToEditableWebhook(String url, String requestType) {

        JSONObject headers = headers();
        String requestHeaderType = headers == null || headers.isEmpty() ?
                RestProjectComponentWebhook.REQUEST_HEADER_TYPE_NONE
                : RestProjectComponentWebhook.REQUEST_HEADER_TYPE_CUSTOM;

        String requestBody = StringUtils.isBlank(textBody()) ? params() : textBody();

        // 将 yml 使用的 type转换为 PromptAI使用的
        String responseType = RestProjectComponentBot.BotResponse.parseMicaResponseType(responseType());

        return RestProjectComponentWebhook.factory(
                _name, url, requestType, requestHeaderType, requestBodyType(), responseType,
                responseHandler().toJavaObject(RestProjectComponentWebhook.ResponseHandle.class),
                _toPairHeader(headers), requestBody
        );
    }

    /**
     * python代码的webhook，不做解析东多
     * @return
     */
    private RestProjectComponentWebhook _parseToPythonWebhook() {
        return RestProjectComponentWebhook.factory(_name, _body);
    }

    private List<Pair> _toPairHeader(JSONObject header) {
        if (header == null || header.isEmpty()) {

            return List.of();
        }

        List<Pair> pairs = new ArrayList<>(header.size());
        for (String key : header.keySet()) {
            pairs.add(Pair.of(key, header.get(key)));
        }

        return pairs;
    }

    public String url() {
        String regex = "url\\s*=\\s*\"([^\"]*)\"";

        // 编译正则表达式
        Pattern pattern = Pattern.compile(regex);

        // 创建匹配器
        Matcher matcher = pattern.matcher(_body);

        // 查找匹配项
        if (matcher.find()) {
            // 提取 url 值
            return matcher.group(1);
        }

        return null;
    }

    public String requestType() {
        // 正则表达式匹配
        String regex = "request_type\\s*=\\s*\"([^\"]*)\"";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(_body);

        if (matcher.find()) {
            return matcher.group(1);

        }
        return null;
    }

    public String requestBodyType() {
        // 正则表达式匹配
        String regex = "request_body_type\\s*=\\s*\"([^\"]*)\"";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(_body);

        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    public String responseType() {
        String regex = "response_type\\s*=\\s*\"([^\"]*)\"";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(_body);

        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    public JSONObject headers() {
        // 正则表达式匹配
        String regex = "headers\\s*=\\s*\"([^\"]*)\"";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(_body);

        if (matcher.find()) {
            String headerStr = matcher.group(1);
            try {
                return JSONObject.parseObject(headerStr);
            } catch (Exception e) {
                LOG.error("[parse headers:{} with error:{}]", headerStr, e.getMessage(), e);
                return new JSONObject();
            }
        }
        return new JSONObject();
    }

    public String params() {
        String regex = "params\\s*=\\s*\"([^\"]*)\"";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(_body);

        if (matcher.find()) {
            return matcher.group(1);

        }
        return "";
    }

    /**
     * 请求体为 text类型
     */
    public String textBody() {
        // 正则表达式匹配
        String regex = "text\\s*=\\s*\"([^\"]*)\"";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(_body);

        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    public JSONObject responseHandler() {
        // 正则表达式，严格匹配 response_handle 的值
        // 正则表达式匹配 response_handle = 所在行的完整内容
        String regex = ".*response_handle\\s*=\\s*(.+)$";
        Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(_body);

        // 如果找到匹配，提取并返回
        if (matcher.find()) {
            String responseHandle = matcher.group(1).trim();
            try {
                return JSONObject.parseObject(responseHandle);
            } catch (Exception e) {
                LOG.error("[parse response handler:{} with error:{}]", responseHandle, e.getMessage(), e);
                return new JSONObject();
            }
        }

        return new JSONObject();
    }
}