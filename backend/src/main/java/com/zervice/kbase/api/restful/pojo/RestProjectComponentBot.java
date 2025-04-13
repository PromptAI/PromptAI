package com.zervice.kbase.api.restful.pojo;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.zervice.common.i18n.MessageUtils;
import com.zervice.common.utils.Constants;
import com.zervice.kbase.api.restful.pojo.mica.ConditionPojo;
import com.zervice.kbase.api.restful.pojo.mica.SlotPojo;
import com.zervice.kbase.database.pojo.ProjectComponent;
import com.zervice.kbase.utils.StrUtil;
import com.zervice.kbase.validator.error.ValidatorError;
import lombok.*;
import org.apache.commons.collections4.CollectionUtils;

import java.util.*;

/**
 * @author Peng Chen
 * @date 2022/6/22
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestProjectComponentBot extends RestBaseProjectComponent {
    public static final int MAX_BOT_RESPONSE_SIZE = 512;

    public static final String TYPE_NAME = ProjectComponent.TYPE_BOT;

    public static RestProjectComponentBot newInstance(String projectId, String parentId, String rootComponentId,
                                                      String text, List<ConditionPojo> conditionPojos) {
        return newInstance(projectId, parentId, rootComponentId, BotResponse.factory(text), conditionPojos);
    }

    public static RestProjectComponentBot newInstance(String projectId, String parentId, String rootComponentId,
                                                      RestProjectComponentWebhook webhook, List<ConditionPojo> conditionPojos) {
        return newInstance(projectId, parentId, rootComponentId, BotResponse.factory(webhook), conditionPojos);
    }
    public static RestProjectComponentBot newInstance(String projectId, String parentId, String rootComponentId, String text) {
        return newInstance(projectId, parentId, rootComponentId, BotResponse.factory(text), null);
    }

    public static RestProjectComponentBot newInstance(String projectId, String parentId, String rootComponentId, RestProjectComponentWebhook webhook) {
        return newInstance(projectId, parentId, rootComponentId, BotResponse.factory(webhook), null);
    }

    public static RestProjectComponentBot newInstance(String projectId, String parentId, String rootComponentId,
                                                      BotResponse response, List<ConditionPojo> conditionPojos) {
        RestProjectComponentBot bot = new RestProjectComponentBot();
        bot.setId(ProjectComponent.generateId(TYPE_NAME));
        bot.setRootComponentId(rootComponentId);
        bot.setParentId(parentId);
        bot.setProjectId(projectId);
        bot.setType(TYPE_NAME);

        Data data = new Data();
        data.setResponses(List.of(response));
        data.setConditions(conditionPojos);

        bot.setData(data);

        return bot;
    }

    public RestProjectComponentBot(ProjectComponent component) {
        super(component);

        this._id = component.getId();
        this._type = component.getType();
        this._projectId = component.getProjectId();
        this._rootComponentId = component.getRootComponentId();
        this._properties = component.getProperties();
        this._no = component.getProperties().getNo();

        JSONObject data = component.getProperties().getData();
        this._parentId = data.getString("parentId");

        if (data.containsKey("data") && data.getJSONObject("data") != null) {
            this._data = data.getJSONObject("data").toJavaObject(Data.class);

            this._data.setValidatorError(_validatorError);
        }

        if (data.containsKey("relations") && data.getJSONArray("relations") != null) {
            this._relations = data.getJSONArray("relations").toJavaList(String.class);
        }

        if (data.containsKey("keyword") && data.getJSONArray("keyword") != null) {
            this._relations = data.getJSONArray("keyword").toJavaList(String.class);
        }
    }

    /**
     * 是否使用了 Entity
     *
     * @param entityId entityID
     * @return
     */
    public boolean entityInUse(String entityId) {
        if (entityId == null) {
            return false;
        }

        List<SlotPojo> setSlots = _data.getSetSlots();

        if (CollectionUtils.isNotEmpty(setSlots)) {
            for (SlotPojo s : setSlots) {
                if (entityId.equals(s.getSlotId())) {
                    return true;
                }
            }
        }

        return false;
    }

    public static RestProjectComponentBot factory(String parentId, String rootComponentId, String projectId) {
        Data data = Data.textFactory(MessageUtils.get("default.bot.utter"));
        return RestProjectComponentBot.builder()
                .id(ProjectComponent.generateId(TYPE_NAME))
                .parentId(parentId).rootComponentId(rootComponentId)
                .projectId(projectId).type(TYPE_NAME)
                .data(data)
                .build();
    }

    private String _id;
    private String _type;
    private String _projectId;
    private String _rootComponentId;

    private String _parentId;

    private Data _data;

    private List<String> _relations;

    private List<String> _keyword;

    private String _relatedUserId;

    @JsonIgnore
    @JSONField(serialize = false, deserialize = false)
    public boolean matchAnswer(String answer) {
        if (CollectionUtils.isEmpty(_data._responses)) {
            return false;
        }

        for (BotResponse r : _data._responses) {
            String text = r.getContent().getString("text");
            if (StrUtil.containsAnyIgnoreCase(text, answer)) {
                return true;
            }
        }

        return false;
    }

    @JsonIgnore
    @JSONField(serialize = false, deserialize = false)
    private ProjectComponent.Prop _properties;

    /**
     * bot 更新对应的 global时调用，copy下data
     * 这里 set_slots/conditions 不更新到 global
     *
     * @return
     */
    public Data copyData() {
        JSONObject data = JSONObject.parseObject(JSONObject.toJSONString(_data));

        data.put("setSlots", new JSONArray());
        data.put("conditions", new JSONArray());

        return data.toJavaObject(Data.class);
    }

    public boolean onlyTextAnswer() {
        if (CollectionUtils.isEmpty(_data._responses)) {
            return false;
        }

        for (RestProjectComponentBot.BotResponse response : _data.getResponses()) {
            if (!RestProjectComponentBot.BotResponse.TYPE_TEXT.equals(response.getType())) {
                return false;
            }
        }

        return true;
    }

    /**
     * 将text 的回答拿出来，目前用于FAQ的embedding
     */
    public String filterTextAnswer() {
        StringBuilder result = new StringBuilder();
        if (CollectionUtils.isEmpty(_data._responses)) {
            return result.toString();
        }

        for (RestProjectComponentBot.BotResponse response : _data.getResponses()) {
            if (RestProjectComponentBot.BotResponse.TYPE_TEXT.equals(response.getType())) {
                result.append(response.getContent().getString("text"));
            }
        }

        return result.toString();
    }

    /**
     * 从global更新data,这里保留自己的set_slots/conditions
     *
     * @param dataFromGlobal from global bot
     */
    public void pasteData(Data dataFromGlobal) {
        List<SlotPojo> slotPojos = Collections.emptyList();
        if (CollectionUtils.isNotEmpty(_data._setSlots)) {
            slotPojos = JSONArray.parseArray(JSONObject.toJSONString(_data._setSlots)).toJavaList(SlotPojo.class);
        }

        List<ConditionPojo> conditions = Collections.emptyList();
        if (CollectionUtils.isNotEmpty(_data._conditions)) {
            conditions = JSONArray.parseArray(JSONObject.toJSONString(_data._conditions)).toJavaList(ConditionPojo.class);
        }

        Integer errorCode = null;
        if (_data._errorCode != null) {
            errorCode = _data._errorCode;
        }

        Data data = JSONObject.parseObject(JSONObject.toJSONString(dataFromGlobal)).toJavaObject(Data.class);
        data.setConditions(conditions);
        data.setSetSlots(slotPojos);
        data.setErrorCode(errorCode);

        _data = data;
    }

    /**
     * 造一个假的bot,目前的意图是生成faq的结束提示语
     * 其他场景使用请仔细修改此方法。
     *
     * @param id
     * @param parentId
     * @param text
     * @return
     */
    public static RestProjectComponentBot generate(String id, String parentId, String projectId, String text) {
        Data data = Data.textFactory(text);
        return RestProjectComponentBot.builder()
                .id(id).type(TYPE_NAME).parentId(parentId)
                .projectId(projectId).data(data)
                .build();
    }

    /**
     * 多轮中变量回答列表，适配银联的多轮需求, 目前支持entity的and匹配.
     * - 后期要升级or，可以在EntityReply增加一个属性：operator  ： and /or, entity间仅支持一种操作
     */
    @Builder
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EntityReply {

        /**
         * 包含的回复
         *  for now we support text、image & attachment
         */
        private List<BotResponse> _responses;

        /**
         * 选择的entity
         *  entityId -> value...
         */
        private Map<String, String> _entities;

        /**
         * 前端需要的id
         */
        private String _id;
    }

    @Builder
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Data {
        private String _name;
        private String _description;
        private Integer _errorCode;

        private List<BotResponse> _responses;

        /**
         * response执行后 是否需要手动将某个slot置为某值
         */
        private List<SlotPojo> _setSlots;

        /**
         * 当某个slot有值/无值时执行这个response
         */
        private List<ConditionPojo> _conditions;

        /**
         * 变量回复列表
         */
        private List<EntityReply> _entityReplies;

        public static Data textFactory(String response) {
            String[] texts = StrUtil.split(response, MAX_BOT_RESPONSE_SIZE);
            List<BotResponse> botResponses = new ArrayList<>();
            for (int i = 0; i < texts.length; i++) {
                JSONObject content = new JSONObject();
                content.put("text", texts[i]);
                BotResponse botResponse = BotResponse.builder()
                        .type(BotResponse.TYPE_TEXT)
                        .id(UUID.randomUUID().toString().replace("-", ""))
                        .content(content)
                        .build();
                botResponses.add(botResponse);
            }
            return Data.builder()
                    .responses(botResponses)
                    .build();
        }

        private ValidatorError _validatorError;
    }

    @Builder
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BotResponse {
        public static final String TYPE_TEXT = "text";
        public static final String TYPE_IMAGE = "image";
        public static final String TYPE_WEBHOOK = "webhook";
        public static final String TYPE_ATTACHMENT = "attachment";
        public static final String TYPE_ACTION = "action";

        public static final String WEBHOOK_MICA_RESPONSE_TYPE_CUSTOM = "mapping";
        /* 不处理 */
        public static final String WEBHOOK_MICA_RESPONSE_TYPE_NOT_HANDLE = "not";
        /* 原始响应 */
        public static final String WEBHOOK_MICA_RESPONSE_TYPE_DIRECT = "direct";

        // java 端使用

        /* 忽略状态码 */
        public static final String WEBHOOK_RESPONSE_TYPE_IGNORE_HTTP_CODE = "ignore_http_code";
        /* 忽略响应 */
        public static final String WEBHOOK_RESPONSE_TYPE_IGNORE_RESPONSE = "ignore_response";
        /* 原始响应 */
        public static final String WEBHOOK_RESPONSE_TYPE_ORIGIN_RESPONSE = "origin_response";
        /* 自定义处理 */
        public static final String WEBHOOK_RESPONSE_TYPE_CUSTOM = "custom";

        public static final String WEBHOOK_REQUEST_TYPE_JSON = "application/json";
        public static final String WEBHOOK_REQUEST_TYPE_FORM = "multipart/form-data";
        public static final String WEBHOOK_REQUEST_TYPE_TEXT = "text/plain";

        /**
         * 转换响应类型
         *   java -> mica
         *
         * @param responseType java 端使用的响应类型
         * @return mica 端使用的响应类型
         */
        public static String convert2MicaResponseType(String responseType) {
            switch (responseType) {
                // 自定义处理
                // 忽略状态码
                case WEBHOOK_RESPONSE_TYPE_IGNORE_HTTP_CODE:
                case WEBHOOK_RESPONSE_TYPE_CUSTOM:
                    return WEBHOOK_MICA_RESPONSE_TYPE_CUSTOM;
                // 原始响应
                case WEBHOOK_RESPONSE_TYPE_ORIGIN_RESPONSE:
                    return WEBHOOK_MICA_RESPONSE_TYPE_DIRECT;
                // 忽略响应
                case WEBHOOK_RESPONSE_TYPE_IGNORE_RESPONSE:
                    return WEBHOOK_MICA_RESPONSE_TYPE_NOT_HANDLE;
                default:
                    throw new IllegalArgumentException("unsupported java webhook type:" + responseType);
            }
        }

        /**
         * 转换响应类型
         *   mica -> java
         *
         * @param responseType mica 端使用的响应类型
         * @return java 端使用的响应类型
         */
        public static String parseMicaResponseType(String responseType) {
            switch (responseType) {
                // 自定义处理
                case WEBHOOK_MICA_RESPONSE_TYPE_CUSTOM:
                    return WEBHOOK_RESPONSE_TYPE_CUSTOM;
                // 原始响应
                case WEBHOOK_MICA_RESPONSE_TYPE_DIRECT:
                    return WEBHOOK_RESPONSE_TYPE_ORIGIN_RESPONSE;
                // 忽略响应
                case WEBHOOK_MICA_RESPONSE_TYPE_NOT_HANDLE:
                    return WEBHOOK_RESPONSE_TYPE_IGNORE_RESPONSE;
                default:
                    throw new IllegalArgumentException("unsupported mica webhook type:" + responseType);
            }
        }

        private String _id;
        private String _type;

        /**
         * 回复的 delay， 单位：毫秒，默认：500
         */
        @Builder.Default
        private Long _delay = 500L;
        private JSONObject _content;

        public static BotResponse factory(RestProjectComponentWebhook webhook) {
            JSONObject content = JSONObject.parseObject(JSONObject.toJSONString(webhook));
            return BotResponse.builder()
                    .id(webhook.getId())
                    .type(TYPE_WEBHOOK)
                    .delay(500L)
                    .content(content)
                    .build();
        }

        /**
         * 构建一个全新的 text
         * @param text text
         * @return BotResponse
         */
        public static BotResponse factory(String text) {
            JSONObject content = new JSONObject();
            content.put("text", text);
            return BotResponse.builder()
                    .id(UUID.randomUUID().toString().replace("-", ""))
                    .type(TYPE_TEXT)
                    .delay(500L)
                    .content(content)
                    .build();
        }


        @JsonIgnore
        @JSONField(serialize = false, deserialize = false)
        public boolean isWebhook() {
            return TYPE_WEBHOOK.equals(this._type);
        }
    }

    @Override
    public ProjectComponent toProjectComponent() {
        _checkAndSetDelay();

        _data.setValidatorError(_validatorError);
        JSONObject data = new JSONObject();
        data.put("data", _data);
        data.put("relations", _relations);
        data.put("keyword", _keyword);
        data.put("parentId", _parentId);
        if (_properties == null) {
            _properties = ProjectComponent.Prop.empty();
        }
        _properties.setData(data);


        return ProjectComponent.builder()
                .id(_id).type(_type)
                .projectId(_projectId)
                .rootComponentId(_rootComponentId)
                .properties(_properties)
                .build();

    }

    private void _checkAndSetDelay() {
        if (CollectionUtils.isNotEmpty(_data.getResponses())) {
            for (BotResponse res : _data.getResponses()) {
                // 如果没有设置或小于0，设置为0，不延迟
                if (res.getDelay() == null || res.getDelay() < 0) {
                    res.setDelay(Constants.BOT_REPLY_DELAY_DEFAULT);
                    continue;
                }

                // 超过了最大值，设置为默认值
                if (res.getDelay() > Constants.BOT_REPLY_DELAY_MAX) {
                    res.setDelay(Constants.BOT_REPLY_DELAY_DEFAULT);
                }
            }
        }
    }
}
