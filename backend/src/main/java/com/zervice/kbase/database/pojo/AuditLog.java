package com.zervice.kbase.database.pojo;

import com.alibaba.fastjson.JSONObject;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

/**
 * Audit log save changes to a table
 */
@EqualsAndHashCode
@ToString
@Getter
@Setter
@Log4j2
public class AuditLog {
    public static final String ACCESS_READ = "read";
    public static final String ACCESS_CREATE = "create";
    public static final String ACCESS_UPDATE = "update";
    public static final String ACCESS_DELETE = "delete";

    long _id;

    // who made the access
    String _user;

    // access type
    String _access;

    // table accessed
    String _table;

    // sql to execute
    String _sql;

    // describe the access, default to empty, a message if the access is failed, etc.
    String _message;

    String _ip;

    Long _time;

    // other stuff ...
    JSONObject _properties;
}
