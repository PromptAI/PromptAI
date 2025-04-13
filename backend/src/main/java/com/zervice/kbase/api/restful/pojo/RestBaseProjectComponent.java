package com.zervice.kbase.api.restful.pojo;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.zervice.common.i18n.MessageUtils;
import com.zervice.common.utils.ServletUtils;
import com.zervice.kbase.database.pojo.ProjectComponent;
import com.zervice.kbase.validator.error.ValidatorError;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * @author chen
 * @date 2022/9/5
 */
@JsonIgnoreProperties({"properties", "userClick"})
public abstract class RestBaseProjectComponent implements ProjectComponentAble {

    public static final String DISPLAY_USER_CLICK = "user_click";
    public static final String DISPLAY_USER_INPUT = "user_input";

    public abstract String getType();

    public abstract String getId();

    public abstract String getParentId();

    public abstract void setParentId(String parentId);
    @Setter
    @Getter
    protected ValidatorError _validatorError;

    public RestBaseProjectComponent(){}

    @JSONField(serialize = false, deserialize = false)
    public abstract ProjectComponent.Prop getProperties();

    @Setter
    @Getter
    private String _rootComponentId;

    public RestBaseProjectComponent(ProjectComponent component) {

        JSONObject data = component.getProperties().getData().getJSONObject("data");
        if (data != null) {
            JSONObject error = data.getJSONObject(ProjectComponent.Prop.VALIDATOR_ERROR_KEY);
            if (error != null) {
                this._validatorError = error.toJavaObject(ValidatorError.class);
                this._validatorError.buildError();
            }
        }

    }

    @Getter
    @Setter
    protected Integer _no = 0;

    @JsonIgnore
    @JSONField(serialize = false, deserialize = false)
    protected List<RestBaseProjectComponent> _children = new ArrayList<>();

    @JsonIgnore
    @JSONField(serialize = false, deserialize = false)
    public void addChild(RestBaseProjectComponent child) {
        _children.add(child);
    }

    public List<RestBaseProjectComponent> getChildren() {
        return _children;
    }

    /**
     * convert V2 - link 的 label
     */
    @Setter@Getter
    protected String _label;

    @lombok.Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ValidInfo {
        private Integer _errorCode;
        private String _errorMessage;
    }

    @Builder
    @lombok.Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Data {
        private String _name;
        private String _description;
        private Integer _errorCode;
    }

    public String getMsgByStatus(Integer statusCodes) {
        return MessageUtils.getExceptionMessage("error." + statusCodes, ServletUtils.getLocale());
    }

    /**
     * build tree
     *
     * @param rootComponentId 根节点id
     * @param parentComponent 父节点id
     * @param allComponents   project下所有节点
     */
    public static void filterChild(String rootComponentId, RestBaseProjectComponent parentComponent,
                                   List<RestBaseProjectComponent> allComponents) {
        String parentId = parentComponent.getId();
        for (RestBaseProjectComponent sub : allComponents) {
            if (parentId.equals(sub.getParentId()) && !parentId.equals(sub.getId())) {

                // 绑定根节点id
                sub.setRootComponentId(rootComponentId);

                // 给bot关联userid
                if (sub instanceof RestProjectComponentBot) {
                    RestProjectComponentBot bot = (RestProjectComponentBot) sub;

                    if (ProjectComponent.TYPE_USER.equals(parentComponent.getType())) {
                        bot.setRelatedUserId(parentId);
                    } else if (ProjectComponent.TYPE_BOT.equals(parentComponent.getType())) {
                        RestProjectComponentBot parentBot = (RestProjectComponentBot) parentComponent;
                        bot.setRelatedUserId(parentBot.getRelatedUserId());
                    }
                }

                parentComponent.addChild(sub);
                filterChild(rootComponentId, sub, allComponents);
            }
        }
    }

    public boolean isUserClick() {
        if (ProjectComponent.TYPE_USER.equals(this.getType())) {
            RestProjectComponentUser user = (RestProjectComponentUser) this;
            return DISPLAY_USER_CLICK.equals(user.getData().getDisplay());
        }
        return false;
    }
}
