package com.zervice.common.model;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author xh
 * @date 2022/7/30
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AccountModel {

    /**
     * 租户name
     */
    private String _name;

    /**
     * 租户db name
     */
    private String _dbName;

    /**
     * 租户默认管理账号
     */
    private JSONArray _admins;

    /**
     * 系统默认管理账号
     */
    private String _admin;

//    private String _email;
//
//    private String _mobile;

    /**
     * 系统默认管理账号name
     */
    private String _adminName;
    /**
     * Company
     */
    private String _fullName;

    private String _timezone;

    private String _type;

    private JSONObject _prop;

    /**
     * 目前是内部使用，创建账号时设置的密码
     */
    private String _passcode;

    /**
     * 是否运行使用新特性
     */
    private Boolean _featureEnable;

    /**
     * 是否活跃
     */
    private Boolean _active;
}
