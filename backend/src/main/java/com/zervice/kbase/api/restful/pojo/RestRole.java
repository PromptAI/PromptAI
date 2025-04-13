package com.zervice.kbase.api.restful.pojo;

import com.zervice.kbase.database.pojo.RbacRole;
import com.zervice.kbase.api.restful.group.Insert;
import com.zervice.kbase.api.restful.group.Update;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(of = "_id")
@ToString
public class RestRole {

    @Getter
    @Setter
    @DecimalMin(value = "1", message = "id is required and must be higher than 0", groups = Update.class)
    private String _id;

    @Getter
    @Setter
    @NotBlank(message = "name is required", groups = {Insert.class, Update.class})
    private String _name;

    @Getter
    @Setter
    private Boolean _status;

    @Getter
    @Setter
    private String _remark;

    @Getter
    @Setter
    private String _creator;

    @Getter
    @Setter
    private long _createTime;

    @Getter
    @Setter
    private List<RbacRole.Permission> _permissions = new ArrayList<>();

    public RestRole() {

    }

    public RestRole(RbacRole rbacRole) {
        this._id = String.valueOf(rbacRole.getId());
        this._name = rbacRole.getName();
        this._status = rbacRole.getStatus();
        this._remark = rbacRole.getProperties().getRemark();
        this._creator = rbacRole.getProperties().getCreator();
        this._createTime = rbacRole.getProperties().getCreateTime();
        this._permissions.addAll(rbacRole.getProperties().getPermissions());
    }
}
