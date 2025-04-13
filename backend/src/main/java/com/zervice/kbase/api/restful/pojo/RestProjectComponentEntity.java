package com.zervice.kbase.api.restful.pojo;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.zervice.common.utils.JSONUtils;
import com.zervice.common.utils.LayeredConf;
import com.zervice.kbase.database.pojo.ProjectComponent;
import com.zervice.kbase.validator.error.ValidatorError;
import lombok.*;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 全局的slot
 * @author Peng Chen
 * @date 2022/6/23
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestProjectComponentEntity extends RestBaseProjectComponent implements ProjectComponentAble {
    public static final String TYPE_NAME = ProjectComponent.TYPE_ENTITY;

    public static final String SLOT_TYPE_BOOLEAN = "boolean";
    public static final String SLOT_TYPE_STRING = "string";
    public static final String SLOT_TYPE_NUMBER = "number";
    public static final String SLOT_TYPE_ARRAY = "array";

    /**
     * 系统内置变量-前缀
     */
    public static String INTERNAL_ENTITY_PREFIX = LayeredConf.getString("internal.entity.prefix", "promptai.");

    public static final String DEFAULT_VALUE_TYPE_SET = "set";

    private String _id;
    private String _type;
    private String _projectId;

    private String _name;

    private String _description;

    private String _display;

    private Data _data;

    //    {
    //        slotType: 'string' | 'number' | 'array' | 'boolean',
    //        defaultValue: string,
    //        defaultValueType: 'set' | 'localStore' | 'sessionStore' | 'custom',
    //        defaultValueEnable: Boolean,
    //        enum: string[],
    //        enumEnable: Boolean
    //      }
    @Builder.Default
    private String _slotType = SLOT_TYPE_STRING;
    private String _defaultValue;
    private String _defaultValueType;
    @Builder.Default
    private Boolean _defaultValueEnable = Boolean.FALSE;

    /**
     * 对应 GPT 可选值
     */
    private List<Object> _enum;
    @Builder.Default
    private Boolean _enumEnable = Boolean.FALSE;

    /**
     * 直接引用改entity的bot/user/filed/gpts...
     * 这不存到数据库，查询时动态构造
     */
    private ProjectComponent.ComponentRelation _componentRelation;

    public RestProjectComponentEntity(String name, String projectId) {
        this._id = ProjectComponent.generateId(TYPE_NAME);
        this._name = name;
        this._type = TYPE_NAME;
        this._projectId = projectId;

        this._display = name;

        this._defaultValue = "";
        this._defaultValueEnable = false;
        this._defaultValueType = DEFAULT_VALUE_TYPE_SET;
        this._enum = List.of();
        this._enumEnable = false;
        this._slotType = SLOT_TYPE_STRING;
    }
    public RestProjectComponentEntity(ProjectComponent component) {
        super(component);

        this._id = component.getId();
        this._type = component.getType();
        this._projectId = component.getProjectId();
        this._properties = component.getProperties();
        this._no = component.getProperties().getNo();


        JSONObject data = component.getProperties().getData();

        this._name = data.getString("name");
        this._description = data.getString("description");
        this._display = data.getString("display");

        if (data.containsKey("data") && data.getJSONObject("data") != null) {
            this._data = data.getJSONObject("data").toJavaObject(Data.class);
        } else {
            this._data = Data.builder().build();
        }

        this._data.setValidatorError(_validatorError);

        // 如果未设置，就用 name
        this._display = StringUtils.isBlank(_display) ? _name : _display;

        this._slotType = JSONUtils.getOrDefault(data, "slotType", SLOT_TYPE_STRING);
        this._defaultValue = data.getString("defaultValue");
        this._defaultValueType = data.getString("defaultValueType");
        this._defaultValueEnable = JSONUtils.getOrDefault(data, "defaultValueEnable", false);
        this._enum = data.getJSONArray("enum") != null ? data.getJSONArray("enum").toJavaList(Object.class) : null;
        this._enumEnable = JSONUtils.getOrDefault(data, "enumEnable", false);
    }

    @JsonIgnore
    @JSONField(serialize = false, deserialize = false)
    private ProjectComponent.Prop _properties;

    /**
     * 判断是否为系统内置entity
     */
    public static boolean isInternalEntity(String entity) {
        if (StringUtils.isBlank(entity)) {
            return false;
        }

        return entity.startsWith(INTERNAL_ENTITY_PREFIX);
    }

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
        /**
         * 用户上传的词典
         */
        private Set<String> _dictionary;

        private ValidatorError _validatorError;
    }

    @Override
    public ProjectComponent toProjectComponent() {
        JSONObject data = new JSONObject();
        data.put("name", _name);
        data.put("description", _description);
        data.put("display", _display);

        data.put("slotType", _slotType);
        data.put("defaultValue", _defaultValue);
        data.put("defaultValueType", _defaultValueType);
        data.put("defaultValueEnable", _defaultValueEnable);
        data.put("enum", _enum);
        data.put("enumEnable", _enumEnable);

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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RestProjectComponentEntity entity = (RestProjectComponentEntity) o;
        return _id.equals(entity._id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(_id);
    }
}
