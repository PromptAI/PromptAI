package com.zervice.kbase.api.restful.pojo;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.zervice.kbase.database.pojo.ProjectComponent;
import com.zervice.kbase.validator.error.ValidatorError;
import lombok.*;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;

/**
 * @author Peng Chen
 * @date 2022/6/22
 */
@Getter
@Setter
@NoArgsConstructor
public class RestProjectComponentConversation extends RestBaseProjectComponent {

    public static final String TYPE_NAME = ProjectComponent.TYPE_CONVERSATION;

    public static RestProjectComponentConversation newInstance(String projectId, String name, String type, String desc) {
        RestProjectComponentConversation c = new RestProjectComponentConversation();
        c.setId(ProjectComponent.generateId(TYPE_NAME));
        c.setProjectId(projectId);
        c.setType(TYPE_NAME);

        Data data = new Data();
        data.setName(name);
        data.setDescription(desc);
        data.setType(type);

        c.setData(data);

        return c;
    }

    public RestProjectComponentConversation(ProjectComponent component) {
        super(component);

        this._id = component.getId();
        this._type = component.getType();
        this._projectId = component.getProjectId();
        this._properties = component.getProperties();
        this._no = component.getProperties().getNo();

        JSONObject data = component.getProperties().getData();

        if (data.containsKey("data") && data.getJSONObject("data") != null) {
            this._data = data.getJSONObject("data").toJavaObject(Data.class);
        } else {
            _data = Data.builder().build();
        }

        this._data.setValidatorError(_validatorError);

        if (data.containsKey("relations") && data.getJSONArray("relations") != null) {
            this._relations = data.getJSONArray("relations").toJavaList(String.class);
        }
        if (data.containsKey(ProjectComponent.Prop.READY_KEY) && data.getBoolean(ProjectComponent.Prop.READY_KEY) != null) {
            Boolean isReady = data.getBoolean(ProjectComponent.Prop.READY_KEY);
            _data.setIsReady(isReady);
        }
    }

    private String _id;

    private String _type;

    private String _projectId;

    private Data _data;

    private List<String> _relations;

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

    @Override
    public ProjectComponent toProjectComponent() {
        JSONObject data = new JSONObject();
        data.put("data", _data);
        data.put("relations", _relations);
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

    @Builder
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Data {
        /**
         * 类型- flow Agent，可以有 兼容所有 flow 类型
         */
        public static final String TYPE_FLOW_AGENT = "flowAgent";

        /**
         * 类型- llm Agent, 当前 flow 有且仅有一个 GPT子节点
         */
        public static final String TYPE_LLM_AGENT = "llmAgent";

        private String _name;

        private String _description;
        private Integer _errorCode;

        @Builder.Default
        private Boolean _isReady = false;

        /**
         * 在link节点生效时，配置是否默认展示
         * （如果link节点过多，那么满屏都是按钮。
         * 这里配置是否"收起来",
         * true表示收起来，false or null 表示在外面）
         */
        @Builder.Default
        private Boolean _hidden = false;

        /**
         * 前端依据这个值是否可编辑 hidden
         */
        private Boolean _canEditorHidden;

        /**
         * 分支引导语，如果有就在命中flow节点就fake一个bot res
         */
        private String _welcome;

        @Builder.Default
        private String _type = TYPE_FLOW_AGENT;

        /**
         * 创建 llmAgent时，暂存 gpt 节点的 Prompt
         */
        private String _prompt;

        private ValidatorError _validatorError;
    }

    @Override
    public RestProjectComponentConversation clone() {
        return JSONObject.parseObject(JSONObject.toJSONString(this), RestProjectComponentConversation.class);
    }

    public void clearChildren() {
        _children.clear();
    }
}
