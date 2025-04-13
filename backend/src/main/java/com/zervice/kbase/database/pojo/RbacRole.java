package com.zervice.kbase.database.pojo;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.*;
import lombok.extern.log4j.Log4j2;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = "_id")
@ToString
@Builder
@Log4j2
@Getter @Setter
public class RbacRole {

    private long _id;

    /**
     * The role is readonly if the name start with sys
     * User can't create a new role if the name start with sys
     */
    private String _name;


    @Getter
    @Setter
    private String _display;

    private Boolean _status;

    private RbacRoleProp _properties;

    @ToString
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Getter
    @Setter
    public static class RbacRoleProp {
        private List<Permission> _permissions;
        private String _remark;
        private String _creator;
        private long _createTime;

        public static RbacRoleProp parse(String properties) {
            return JSON.parseObject(properties, RbacRoleProp.class);
        }

        public static RbacRoleProp factory(String creator, String remark, List<Permission> permissions) {
            return RbacRoleProp.builder()
                    .creator(creator)
                    .createTime(System.currentTimeMillis())
                    .remark(remark)
                    .permissions(permissions)
                    .build();
        }
    }

    @ToString
    @Getter
    @Setter
    public static class Permission {
        //
        // resource name, '*' for all resource
        private String _resource;

        //
        // operations, could be set to empty to indicate all operation allowed on this resource
        private List<String> _operations;

        //
        // default to false, so allow user to perform given actions
        private boolean _denyAccess = false;

        //
        // If the permission is allowed to certain objects in the resource, give them here. Only by ID for now
        private JSONObject _filter = new JSONObject();
    }

    public List<Permission> getPermissions() {
        return _properties.getPermissions();
    }

    public static RbacRole factory(String name, RbacRoleProp rbacRoleProp) {
        return RbacRole.builder()
                .name(name)
                .status(true)
                .properties(rbacRoleProp)
                .build();
    }

    /**
     * called by RbacRoleDao to instantiate a user with id
     */
    public static RbacRole createRbacRoleFromDao(long id, String name, String display, Boolean status, String properties) {
        return RbacRole.builder()
                .id(id)
                .name(name)
                .display(display)
                .status(status)
                .properties(RbacRoleProp.parse(properties))
                .build();
    }
}
