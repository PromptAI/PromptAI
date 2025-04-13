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
 * type Goto = {
 * id: string
 * parentId: string
 * type: 'goto',
 * data: {
 * linkId: string
 * },
 * relation: string[]
 * }
 *
 * @author Peng Chen
 * @date 2022/6/22
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RestProjectComponentGoto extends RestBaseProjectComponent implements ProjectComponentAble {

    public static final String TYPE_NAME = ProjectComponent.TYPE_GOTO;

    public static RestProjectComponentGoto newInstance(String projectId, String parentId, String rootComponentId,
                                                       String linkId) {
        RestProjectComponentGoto restGoto = new RestProjectComponentGoto();
        restGoto.setId(ProjectComponent.generateId(TYPE_NAME));
        restGoto.setProjectId(projectId);
        restGoto.setParentId(parentId);
        restGoto.setRootComponentId(rootComponentId);
        restGoto.setType(TYPE_NAME);

        Data data = new Data();
        data.setLinkId(linkId);

        restGoto.setData(data);

        return restGoto;
    }

    public RestProjectComponentGoto(ProjectComponent component) {
        super(component);

        this._id = component.getId();
        this._type = component.getType();
        this._projectId = component.getProjectId();
        this._rootComponentId = component.getRootComponentId();
        this._properties = component.getProperties();
        this._no = component.getProperties().getNo();

        JSONObject data = component.getProperties().getData();

        if (data.containsKey("relations") && data.getJSONArray("relations") != null) {
            this._relations = data.getJSONArray("relations").toJavaList(String.class);
        }

        if (data.containsKey("data") && data.getJSONObject("data") != null) {
            this._data = data.getJSONObject("data").toJavaObject(Data.class);

            this._data.setValidatorError(_validatorError);
        }

        _parentId = data.getString("parentId");
    }


    private String _id;

    private String _type;
    private String _projectId;
    private String _parentId;
    private String _rootComponentId;

    private Data _data;

    private List<String> _relations;


    /**
     * convert V2 - next label name
     */
    private String _next;

    @JsonIgnore
    @JSONField(serialize = false, deserialize = false)
    private ProjectComponent.Prop _properties;

    @Builder
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Data {
        private String _linkId;
        private String _name;
        private Integer _errorCode;

        private String _description;
        private ValidatorError _validatorError;
    }

    @Override
    public ProjectComponent toProjectComponent() {
        JSONObject data = new JSONObject();
        data.put("data", _data);
        data.put("relations", _relations);
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
}
