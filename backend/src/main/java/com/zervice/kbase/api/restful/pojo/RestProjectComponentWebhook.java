package com.zervice.kbase.api.restful.pojo;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.lang.Pair;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.zervice.common.restful.exception.RestException;
import com.zervice.common.restful.exception.StatusCodes;
import com.zervice.common.utils.HttpClientUtils;
import com.zervice.kbase.database.pojo.ProjectComponent;
import com.zervice.kbase.validator.error.ValidatorError;
import lombok.*;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;

import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Peng Chen
 * @date 2022/6/22
 */
@Log4j2
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RestProjectComponentWebhook extends RestBaseProjectComponent implements ProjectComponentAble {

    public static final String TYPE_NAME = ProjectComponent.TYPE_WEBHOOK;

    public static final String NORMAL_WEBHOOK = "normal";
    public static final String TALK2BITS_WEBHOOK = "talk2bits";

    public static final String REQUEST_HEADER_TYPE_NONE = "none";
    public static final String REQUEST_HEADER_TYPE_CUSTOM = "custom";

    /**
     * 网络请求
     */
    public static final String WEBHOOK_TYPE_WEB_REQUEST = "web_request";
    /**
     * python 代码
     */
    public static final String WEBHOOK_TYPE_PYTHON = "python_code";

    public RestProjectComponentWebhook(ProjectComponent component) {
        super(component);

        this._id = component.getId();
        this._type = component.getType();
        this._projectId = component.getProjectId();
        this._properties = component.getProperties();
        this._no = component.getProperties().getNo();

        JSONObject data = component.getProperties().getData();

        this._text = data.getString("text");
        this._url = data.getString("url");
        if (data.containsKey("data") && data.getJSONObject("data") != null) {
            this._data = data.getJSONObject("data").toJavaObject(Data.class);
            this._data.setValidatorError(_validatorError);
        } else {
            this._data = Data.empty();
        }

        if (data.containsKey("headers") && data.getJSONArray("headers") != null) {
            this._headers = data.getJSONArray("headers").toJavaList(Pair.class);
        }

        if (data.containsKey("params") && data.getJSONArray("params") != null) {
            this._params = data.getJSONArray("params").toJavaList(Pair.class);
        }

        if (data.containsKey("responseHandle") && data.getJSONObject("responseHandle") != null) {
            this._responseHandle = data.getJSONObject("responseHandle").toJavaObject(ResponseHandle.class);
        }

        this._requestHeaderType = data.getString("requestHeaderType");
        this._requestBody = data.getString("requestBody");
        this._requestBodyType = data.getString("requestBodyType");
        this._requestType = data.getString("requestType");
        this._responseType = data.getString("responseType");
        this._description = data.getString("description");
    }

    public static RestProjectComponentWebhook factory(String name, String code) {
        RestProjectComponentWebhook webhook = new RestProjectComponentWebhook();
        webhook.setType(TYPE_NAME);

        Data data = new Data();
        data.setWebhookType(WEBHOOK_TYPE_PYTHON);
        data.setCode(code);
        webhook.setText(name);
        webhook.setData(data);

        return webhook;
    }
    public static RestProjectComponentWebhook factory(String text, String url, String requestType,
                                                      String requestHeaderType, String requestBodyType,
                                                      String responseType, ResponseHandle responseHandle,
                                                      List<Pair> headers, String requestBody) {
        RestProjectComponentWebhook webhook = new RestProjectComponentWebhook();
        webhook.setText(text);
        webhook.setType(TYPE_NAME);
        webhook.setUrl(url);
        webhook.setRequestType(requestType);
        webhook.setRequestHeaderType(requestHeaderType);
        webhook.setRequestBodyType(requestBodyType);
        webhook.setResponseType(responseType);
        webhook.setResponseHandle(responseHandle);
        webhook.setHeaders(headers);
        webhook.setRequestBody(requestBody);

        Data data = new Data();
        data.setWebhookType(WEBHOOK_TYPE_WEB_REQUEST);
        webhook.setData(data);


        return webhook;
    }

    public boolean pythonCode() {
        return WEBHOOK_TYPE_PYTHON.equals(_data.getWebhookType());
    }

    /**
     * 是否是talk2bits的节点
     */
    public Boolean blnTalk2bits() {
        return _data != null && StringUtils.isNotBlank(_data.getTalk2bitsInput());
    }

    private String _id;

    private String _type;
    private String _projectId;

    private String _text;

    private String _url;

    private List<Pair> _headers;
    private List<Pair> _params;

    private Data _data;
    @JsonProperty("request_header_type")
    @JSONField(name = "request_header_type")
    private String _requestHeaderType;

    @JsonProperty("request_body_type")
    @JSONField(name = "request_body_type")
    private String _requestBodyType;

    @JsonProperty("request_body")
    @JSONField(name = "request_body")
    private String _requestBody;

    @JsonProperty("request_type")
    @JSONField(name = "request_type")
    private String _requestType;

    @JsonProperty("response_type")
    @JSONField(name = "response_type")
    private String _responseType;

    @JsonProperty("response_handle")
    @JSONField(name = "response_handle")
    private ResponseHandle _responseHandle;

    public List<Pair> responseHandleParse() {
        if (_responseHandle == null) {
            return null;
        }

        return _responseHandle.getParse();
    }

    private String _description;

    @JsonIgnore
    @JSONField(serialize = false, deserialize = false)
    private ProjectComponent.Prop _properties;

    @Override
    public String getParentId() {
        return null;
    }

    @Override
    public void setParentId(String parentId) {
    }

    @Builder
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Data {

        public static Data empty() {
            return Data.builder()
                    .build();
        }

        @Builder.Default
        private String _webhookType= WEBHOOK_TYPE_WEB_REQUEST;

        /**
         * python 代码
         */
        private String _code;

        /**
         * talk2bits发的link
         */
        private String _talk2bitsInput;

        private String _name;
        private String _description;
        private Integer _errorCode;

        private ValidatorError _validatorError;
    }

    @Builder
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResponseHandle {

        /**
         * key: variable name, value: jsonpath
         */
        private List<Pair> _parse;
        /**
         * bot utterance
         */
        private String _text;

        public static ResponseHandle factory(List<Pair> parse, String text, String errorMsg) {
            return ResponseHandle.builder()
                    .parse(parse)
                    .text(text)
                    .errorMsg(errorMsg)
                    .build();
        }

        @JsonProperty("error_msg")
        @JSONField(name = "error_msg")
        private String _errorMsg;
    }

    @Override
    public ProjectComponent toProjectComponent() {
        JSONObject data = new JSONObject();
        data.put("text", _text);
        data.put("url", _url);
        data.put("headers", _headers);
        data.put("params", _params);
        data.put("requestHeaderType", _requestHeaderType);
        data.put("requestBodyType", _requestBodyType);
        data.put("requestBody", _requestBody);
        data.put("requestType", _requestType);
        data.put("responseType", _responseType);
        data.put("responseHandle", _responseHandle);
        data.put("description", _description);
        data.put("data", _data);

       if (_properties == null) {
            _properties = ProjectComponent.Prop.empty();
        }
        _properties.setData(data);


        return ProjectComponent.builder()
                .id(_id).type(_type)
                .projectId(_projectId)
                .properties(_properties)
                .build();
    }

    public interface UrlParser{
        RestProjectComponentWebhook parse(String input);
    }

    /**
     * 输入是这样的，目前需要支持三个版本
     * V3: 直接是一个url
     * https://app.promptai.cn/ava/chatbot.app?name=Prompt+A
     *      * I&id=a1_p_c4c9iurlzdhc&token=YjhiZDRhOWMtNDhlMi00NWVjLTgxMWMtZGNjODU2NjRhYWRh&project=p_c
     *      * 4c9iurlzdhc
     *
     * V2 - config是base64编码后的query，需要先找到config，最后base64解码后提取结果
     * <script type="text/javascript" src="https://talk2bits.com/ava/chatbot.app?config=aW
     * Q9YTFfcF9jZnhxeGk0YXAxeGMmdG9rZW49T0RCaFlqVm1ZbUV0T0RNME9DMDBabVptTFdKaFpEa3RZV0ZrTV
     * dJNE56bGtOell6JnByb2plY3Q9cF9jZnhxeGk0YXAxeGMmZW5naW5lPWNoYXQmbmFtZT1UYWxrMkJpdHMmd2V
     * sY29tZT1JJTI3bSt5b3VyK2RvY3VtZW50K2Fzc2lzdGFudCUyQyt3aGF0K2RvK3lvdSt3YW50K3RvK2tub3crY
     * WJvdXQrdGhlK2RvY3VtZW50JTNG"></script>
     *
     * V1 - 直接从query param 提取出结果
     * <script type="text/javascript" src="https://app.promptai.cn/ava/chatbot.app?name=Prompt+A
     * I&id=a1_p_c4c9iurlzdhc&token=YjhiZDRhOWMtNDhlMi00NWVjLTgxMWMtZGNjODU2NjRhYWRh&project=p_c
     * 4c9iurlzdhc"></script>
     *
     */
    public static class Talk2BitsUrlParser implements UrlParser {
        private final Set<String> requiredParams = Set.of("id", "token", "project");
        private final Map<String, String> headerConverter = new HashMap<>();
        {
            headerConverter.put("project", "X-project-id");
            headerConverter.put("id", "x-published-project-id");
            headerConverter.put("token", "x-published-project-token");
        }

        /**
         * 从script标签中提取到src属性
         */
        private final Pattern pattern = Pattern.compile("<script.*?src=\"(.*?)\".*?>", Pattern.CASE_INSENSITIVE);

        private static Map<String, String> getParamFromUrlQuery(String query) {
            Map<String, String> parameters = new HashMap<>();
            for (String parameter : query.split("&")) {
                String[] pair = parameter.split("=");
                String name = URLDecoder.decode(pair[0], StandardCharsets.UTF_8);
                String value = URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
                parameters.put(name, value);
            }
            return parameters;
        }

        /**
         * 创建chat
         */
        private final String API_CREATE_CHAT = "/chat/api/chat";

        /**
         * 创建message
         */
        private final String API_SEND_MESSAGE = "/chat/api/message";
        private final String WEBHOOK_NAME = "_talk2bits";
        private final String REQUEST_BODY = "{\"message\":\"{latest_message}\",\"chatId\":\"{send_id}\",\"content\":\"{latest_message}\"}";
        private final String RESPONSE_HANDLER = "{\"error_msg\":\"Please try again later!\",\"parse\":[{\"key\":\"reply\",\"value\":\"$.answers[0].text\"}],\"text\":\"{reply}\"}";
        private String parseServerFromUrl(String url) {
            try {
                URL u = new URL(url);
                String protocol = u.getProtocol(); // 获取协议
                String host = u.getHost(); // 获取主机名
                int port = u.getPort(); // 获取端口号

                StringBuilder server = new StringBuilder(protocol + "://" + host);
                // 没有使用默认的端口号
                if (port != -1) {
                    server.append(":").append(port);
                }

                return server.toString();
            } catch (Exception e) {
                return null;
            }
        }
        private String parseQueryFromUrl(String url) {
            try {
                URL u = new URL(url);
                return u.getQuery();
            } catch (Exception e) {
                return null;
            }
        }

        private String parseUrl(String input) {
            if (input.startsWith("http")) {
                return input;
            }

            Matcher matcher = pattern.matcher(input);
            if (matcher.find()) {
                return matcher.group(1);
            }

            return null;
        }

        private Map<String, String> parseParamFromInput(String input) {
            String url = parseUrl(input);
            if (StringUtils.isBlank(url)) {
                throw new RestException(StatusCodes.Talk2BitsParseFail);
            }

            String server = parseServerFromUrl(url);
            if (StringUtils.isBlank(server)) {
                throw new RestException(StatusCodes.Talk2BitsParseFail);
            }

            String query = parseQueryFromUrl(url);
            if (StringUtils.isBlank(query)) {
                throw new RestException(StatusCodes.Talk2BitsParseFail);
            }

            Map<String, String> param = getParamFromUrlQuery(query);
            if (param.isEmpty()) {
                throw new RestException(StatusCodes.Talk2BitsParseFail);
            }

            // V2
            if (param.size() == 1 && param.containsKey("config")) {
                String config = param.get("config");
                config = Base64.decodeStr(config);
                param = getParamFromUrlQuery(config);
            }

            // 检查需要的参数
            for (String requiredParam : requiredParams) {
                if (!param.containsKey(requiredParam)) {
                    throw new RestException(StatusCodes.BadRequest);
                }
            }

            param.put("server", server);
            return param;
        }

        private boolean createChat(Map<String, String> param) {
            try {
                String server = param.get("server");

                HttpHeaders httpHeaders = new HttpHeaders();
                for (Map.Entry<String, String> entry : param.entrySet()) {
                    String key = entry.getKey();
                    if (headerConverter.containsKey(key)) {
                        String realHeaderKey = headerConverter.get(key);
                        httpHeaders.add(realHeaderKey, entry.getValue());
                    }
                }

                String url = server + API_CREATE_CHAT;

                HttpClientUtils.getJson(url, httpHeaders);
                return true;
            } catch (Exception e) {
                LOG.warn("[test talk 2 bits webhook fail:{}]", e.getMessage(), e);
                return false;
            }
        }

        private List<Pair> buildRequestHeader(Map<String ,String > headerMap) {
            List<Pair> headers = new ArrayList<>(headerMap.size());
            for (Map.Entry<String, String> h : headerMap.entrySet()) {
                String key = h.getKey();
                if (headerConverter.containsKey(key)) {
                    String realHeaderKey = headerConverter.get(key);
                    headers.add(new Pair<>(realHeaderKey, h.getValue()));

                }
            }

            return headers;
        }

        @Override
        public RestProjectComponentWebhook parse(String input) {
            // 1、解析出需要的参数
            Map<String, String> param = parseParamFromInput(input);

            // 2、检查配置是否可达
            boolean success = createChat(param);
            if (!success) {
                throw new RestException(StatusCodes.Talk2BitsNotReachable);
            }

            // 3、创建webhook
            String server = param.remove("server");
            String url = server + API_SEND_MESSAGE;

            List<Pair> headers = buildRequestHeader(param);
            ResponseHandle responseHandle = JSONObject.parseObject(RESPONSE_HANDLER, ResponseHandle.class);
            RestProjectComponentWebhook webhook = RestProjectComponentWebhook.factory(
                    WEBHOOK_NAME, url, "post", "custom",
                    "application/json", "custom", responseHandle, headers, REQUEST_BODY);

            RestProjectComponentWebhook.Data data = Data.builder()
                    .talk2bitsInput(input)
                    .build();
            webhook.setData(data);
            return webhook;
        }
    }

}
