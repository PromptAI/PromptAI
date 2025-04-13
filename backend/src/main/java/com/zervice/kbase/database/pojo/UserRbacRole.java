package com.zervice.kbase.database.pojo;

import lombok.*;
import com.alibaba.fastjson.JSONObject;

/**
 * Represents an association between a user and a rbac role. A user may have multiple roles.
 */
@ToString
public class UserRbacRole {
    @Setter @Getter
    long _id;

    @Setter @Getter
    long _userId;

    @Setter @Getter
    long _rbacRoleId;

    //
    // Auxiliary field, currently not used, just keep it for future purpose, TBD.
    @Setter @Getter
    JSONObject _params;
}
