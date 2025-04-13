package com.zervice.kbase.api.restful.pojo;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.zervice.kbase.database.pojo.ProjectComponent;
import com.zervice.kbase.validator.error.ValidatorError;
import lombok.*;

import java.util.List;

/**
 * Gpt Node
 *
 * @author Peng Chen
 * @Date 2023/11/14
 */
@Getter
@Setter
@NoArgsConstructor

public class RestProjectComponentGpt extends RestBaseProjectComponent {

    public static final String TYPE_NAME = ProjectComponent.TYPE_GPT;
    private String _id;
    private String _type;
    private String _projectId;
    private String _parentId;
    private String _rootComponentId;
    private Data _data;
    private List<String> _relations;
    @JsonIgnore
    @JSONField(serialize = false, deserialize = false)
    private ProjectComponent.Prop _properties;

    public static RestProjectComponentGpt newInstance(String projectId, String parentId, String rootComponentId,
                                                      String name, String prompt, String description,
                                                      List<Slot> slots,
                                                      List<FunctionCalling> functionCallings) {

        RestProjectComponentGpt gpt = new RestProjectComponentGpt();
        gpt.setId(ProjectComponent.generateId(TYPE_NAME));
        gpt.setRootComponentId(rootComponentId);
        gpt.setParentId(parentId);
        gpt.setProjectId(projectId);
        gpt.setType(TYPE_NAME);

        Data data = new Data();
        data.setName(name);
        data.setPrompt(prompt);
        data.setDescription(description);
        data.setSlots(slots);
        data.setFunctionCalling(functionCallings);

        gpt.setData(data);
        return gpt;
    }

    public RestProjectComponentGpt(ProjectComponent component) {
        super(component);

        this._id = component.getId();
        this._type = component.getType();
        this._projectId = component.getProjectId();
        this._rootComponentId = component.getRootComponentId();
        this._properties = component.getProperties();
        this._no = component.getProperties().getNo();


        JSONObject data = component.getProperties().getData();

        if (data.containsKey("data") && data.getJSONObject("data") != null) {
            this._data = data.getJSONObject("data").toJavaObject(Data.class);

            this._data.setValidatorError(_validatorError);
        }

        if (data.containsKey("relations") && data.getJSONArray("relations") != null) {
            this._relations = data.getJSONArray("relations").toJavaList(String.class);
        }

        if (data.containsKey("parentId")) {
            this._parentId = data.getString("parentId");
        }
    }

    @Override
    public ProjectComponent toProjectComponent() {
        JSONObject data = new JSONObject();
        data.put("data", this._data);
        data.put("relations", this._relations);
        data.put("parentId", this._parentId);
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

    @Builder
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Data {
        private String _name;

        private String _description;

        private String _prompt;

        private Integer _errorCode;

        private List<Slot> _slots;

        private List<FunctionCalling> _functionCalling;

        private ValidatorError _validatorError;
    }

    @Builder
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FunctionCalling {

        public static FunctionCalling factory(String name, String code) {
            return FunctionCalling.builder()
                    .name(name).code(code)
                    .build();
        }

        /**
         * function name from the code
         */
        private String _name;

        /**
         * python function code
         */
        private String _code;
    }

    @Builder
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Slot {

        public static Slot factory(String slotId, String type) {
            return Slot.builder()
                    .slotId(slotId).type(type)
                    .build();
        }
        private String _slotId;

        private String _type;
    }
}
