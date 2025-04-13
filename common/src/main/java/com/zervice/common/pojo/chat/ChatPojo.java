package com.zervice.common.pojo.chat;


import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Log4j2
public class ChatPojo {

    public static final String INIT_MESSAGE = "/init";

    /**
     * 标记最后一轮对话后是否在flow里面
     */
    @Builder.Default
    @JsonIgnore
    @JSONField(serialize = false, deserialize = false)
    private Boolean _lastInFlow = false;

    private void _decodeProperties(JSONObject res) {
        JSONObject properties = res.getJSONObject("properties");
        if (properties != null) {
            this._properties = properties.toJavaObject(Prop.class);
        }
    }


    private void _decodeComponents(JSONObject res) {
        if (res.containsKey("components")) {
            JSONArray components = res.getJSONArray("components");
            if (components != null) {
                for (int i = 0; i < components.size(); i++) {
                    _components.add(components.getJSONObject(i));
                }
            }
        }
    }

    private String _id;

    private Long _visitTime;

    private Prop _properties;

    public void refreshLastReadTime() {
        _lastReadTime = System.currentTimeMillis();
    }

    public boolean expired(Integer expirationInSec) {
        return System.currentTimeMillis() - _lastReadTime > TimeUnit.SECONDS.toMillis(expirationInSec);
    }

    /**
     * 最后访问时间
     */
    @JsonIgnore
    @JSONField(serialize = false, deserialize = false)
    private Long _lastReadTime;

    public ChatPojo(JSONObject res) {
        this._id = res.getString("id");
        this._visitTime = res.getLong("visitTime");

        this._components = new ArrayList<>();
        this._lastInFlow = false;

        _decodeComponents(res);
        _decodeProperties(res);

        _build();

        refreshLastReadTime();
    }

    @JsonIgnore
    @JSONField(serialize = false, deserialize = false)
    private BaseComponentPojo _rootComponent;

    /**
     * debug会话驱动的flow components
     */
    @Builder.Default
    private List<JSONObject> _components = new ArrayList<>();

    private Boolean _lastHandleViaFaqNlp;

    public boolean existEntities() {
        return _properties._entities != null;
    }

    /**
     * 如果能自驱动,那么build所需的的结构
     */
    private void _build() {
        if (CollectionUtils.isEmpty(_components)) {
            return;
        }
        List<BaseComponentPojo> convertedComponents = _components.stream()
                .map(c -> BaseComponentPojo.convert(c, _id))
                .collect(Collectors.toList());

        // 目前只支持一个flow类型的root
        String rootType = ProjectComponentConversationPojo.NAME;

        Optional<BaseComponentPojo> rootOp = convertedComponents.stream()
                .filter(c -> rootType.equals(c.getType()))
                .findFirst();
        if (rootOp.isEmpty()) {
            LOG.warn("[{}][fail to build chat, root component not found:{} when build chat components]", _id, rootType);
            return;
        }

        _rootComponent = rootOp.get();
        ProjectComponentConversationPojo conversation = (ProjectComponentConversationPojo) _rootComponent;

        BaseComponentPojo.buildComponentTree(conversation, convertedComponents);

        LOG.info("[{}][success build chat]", _id);
    }

    public List<BaseComponentPojo> next(String message) {
        if (_rootComponent == null) {
            return null;
        }
        List<BaseComponentPojo> result = new ArrayList<>();
        ProjectComponentConversationPojo root = (ProjectComponentConversationPojo) _rootComponent;
        List<BaseComponentPojo> next = root.getChildren();

        if (INIT_MESSAGE.equals(message)) {
            for (BaseComponentPojo c : next) {
                result.add(c);
                terminate(result, c.getChildren());
            }

            return result;
        }

        BaseComponentPojo pojo = _match(message, next);
        if (pojo != null) {
            terminate(result, pojo.getChildren());
            return result;
        }

        return null;
    }

    private BaseComponentPojo _match(String message, List<BaseComponentPojo> next) {
        if (CollectionUtils.isEmpty(next)) {
            return null;
        }
        for (BaseComponentPojo c : next) {
            if (message.contains(c.getId())) {
                return c;
            }
            if (c instanceof ProjectComponentUserPojo) {
                ProjectComponentUserPojo user = (ProjectComponentUserPojo) c;
                List<String> examples = user.getData().getExamples().stream()
                        .map(ProjectComponentUserPojo.Example::getText)
                        .collect(Collectors.toList());
                if (CollectionUtils.isNotEmpty(examples)) {
                    for (String e : examples) {
                        if (message.equalsIgnoreCase(e)) {
                            return c;
                        }
                    }
                }
            }

            BaseComponentPojo matched = _match(message, c.getChildren());
            if (matched != null) {
                return matched;
            }
        }

        return null;
    }

    /**
     * 生成Res的可以是bot/option
     */
    private void terminate(List<BaseComponentPojo> result, List<BaseComponentPojo> next) {
        if (CollectionUtils.isEmpty(next)) {
            return;
        }

        for (BaseComponentPojo c : next) {
            if (c.isUserClick()) {
                result.add(c);
                continue;
            }

            // 机器节点还可以是机器节点
            if (c.getType().equals(ProjectComponentBotPojo.NAME)) {
                result.add(c);
                terminate(result, c.getChildren());
            }
        }
    }

    @JsonIgnore
    @JSONField(serialize = false, deserialize = false)
    public boolean canSelfDriven() {
        return CollectionUtils.isNotEmpty(_components);
    }


    @Builder
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Prop {
        private String _ip;
        private String _projectId;
        private String _publishedProjectId;

        private String _componentId;

        /**
         * project可以控制下级节点是否生成按钮
         */
        private List<ButtonPojo> _buttons;

        /**
         * 是否选择展示节点信息（conversations）
         */
        private String _showSubNodesAsOptional;

        /**
         * 项目欢迎语
         */
        private String _welcome;

        /**
         * 默认回答
         */
        private String _fallback;

        /**
         * 对话时Project的entities,为变量回复列表提供支持
         */
        private List<ProjectComponentEntityPojo> _entities;

        /**
         * project locale
         */
        private String _locale;

        /**
         * 发布的roots
         */
        private List<String> _publishedComponents;

        private JSONObject _chatBotSettings;

        /**
         * 创建对话时需要填充到上下文的slot - 从外部系统来
         */
        private Map<String, Object> _slots;

        /**
         * 需要存储的变量 - 从外部系统来
         */
        private Map<String, Object> _variables;
    }
}
