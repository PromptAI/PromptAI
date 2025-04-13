package com.zervice.kbase.database.pojo;

import cn.hutool.core.lang.Validator;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.zervice.common.utils.Base36;
import com.zervice.common.utils.Constants;
import com.zervice.kbase.api.restful.rbac.UserRolesBuilder;
import com.zervice.kbase.database.SecurityUtils;
import com.zervice.kbase.database.dao.UserDao;
import com.zervice.kbase.database.utils.DaoUtils;
import com.zervice.kbase.jwt.JwtPayLoad;
import com.zervice.kbase.jwt.JwtTokenUtil;
import lombok.*;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;

import java.sql.Connection;
import java.util.Locale;
import java.util.Set;


/**
 * The complete user JSON object will be
 *
 * {
 *     id: "u12df34",
 *     username: "def@cnn.com",
 *     password: "dasf09343",
 *     dept_id:"1",
 *     dept_name:"QA",
 *     status: "registered|active|suspended",
 *     lastAccessEpochMs: $epochInMs,     // last request received from the user
 *     properties: {
 *         createTime: #Millisecond,
 *         creator: "admin",
 *         email: "123@123.com"
 *         phone: "(001)8052346789"  // where the number inside braces is country code
 *     }
 * }
 */
@Log4j2
@EqualsAndHashCode(of = {"_id"})
@ToString
@Builder
public class User {

    /**
     * 系统操作的用户Id
     */
    public static final Long USER_ID_SYSTEM = 0L;
    /**
     * 默认密码
     */
    public static final String USER_DEFAULT_PWD = "promptai";

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @ToString
    public static class UserProp {
        public static final String CONFIG_LANGUAGE = "language";
        public static final String CONFIG_TIMEZONE = "timeZone";

        /** 来源： 注册 */
        public static final String FROM_REGISTRY = "registry";
        /** 来源：第三方登录 google.. */
        public static final String FROM_SSO = "sso";
        /** 来源：手动创建 */
        public static final String FROM_MANUAL_CREATE = "manual_create";

        String _avatar;
        long _createTime;
        long _creatorId;
        String _password;
        String _desc;
        /**
         * 用户的初始密码，更新密码后删掉改值
         */
        String _initPass;

        /**
         * user config
         * {
         * "language":"zh",
         * "timeZone":"UTC"
         * }
         */
        JSONObject _config;

        /**
         * stripe的客户id
         */
        private String _stripeCustomId;

        /**
         * 注册方式
         */
        @Builder.Default
        private String _from = FROM_REGISTRY;

        public UserProp() {
        }

        /**
         * 构建用户默认的配置信息
         * -  语言 英文(用户使用中国手机号的时候设置成中文)
         * -  时区 GMT+8
         */
        public static JSONObject defaultConfig(User user) {
            JSONObject config = new JSONObject();
            if (Validator.isMobile(user.getMobile())) {
                config.put(CONFIG_LANGUAGE, Locale.CHINESE.getLanguage());
            } else {
                config.put(CONFIG_LANGUAGE, Locale.ENGLISH.getLanguage());
            }
            config.put(CONFIG_TIMEZONE, Constants.ZONE_ID_GMT_8);
            return config;
        }

        public static UserProp parse(String properties) {
            return JSON.parseObject(properties, UserProp.class);
        }

        public static UserProp factory(String avatar) {

            return UserProp.builder().avatar(avatar)
                    .createTime(System.currentTimeMillis())
                    .build();
        }

        public static UserProp empty() {
            return factory(null);
        }

        public void updateConfig(String language, String timeZone) {
            if (_config == null) {
                _config = new JSONObject();
            }
            if (StringUtils.isNotBlank(language)) {
                _config.put("language", language);
            }
            if (StringUtils.isNotBlank(timeZone)) {
                _config.put("timeZone", timeZone);
            }
        }
    }


    @Getter
    @Setter
    long _id;

    @Getter
    @Setter
    String _username;


    @Getter
    @Setter
    Boolean _status;

    @Getter
    @Setter
    long _lastAccessEpochMs;

    @Getter
    @Setter
    Set<Long> _roles;

    @Getter
    @Setter
    String _externalAcctId;

    @Getter
    @Setter
    boolean _needResetPassword;

    @Getter
    @Setter
    @Builder.Default
    boolean _needCompleteAccountSetup = false;

    @Getter
    @Setter
    String _mobile;

    @Getter
    @Setter
    String _email;

    @Getter
    @Setter
    UserProp _properties;


    public long getCreateTime() {
        return _properties.getCreateTime();
    }

    @JsonProperty("id")
    public String getExternalId() {
        return getExternalId(_id);
    }

    public static String getExternalId(long id) {
        return "u" + Base36.encode(id);
    }

    public static long fromExternalId(String extId) {
        if (!extId.startsWith("u")) {
            throw new IllegalArgumentException("Invalid exernal user ID - " + extId);
        }

        return Base36.decode(extId.substring(1));
    }

    /**
     * called when create account phase 1 from accout utils. driven by adminportal
     */
    public static User factory(String username, String password, String email, String mobile) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(username));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(password));

        User user = User.builder()
                .username(username)
                .status(true).email(email).mobile(mobile)
                .lastAccessEpochMs(System.currentTimeMillis())
                .properties(UserProp.builder().build())
                .build();

        user.setPassword(password);
        return user;
    }

    /**
     * called by add user op to create user.
     */
    public static User factory(String username, String password,
                               String email, String mobile,
                               UserProp userProp) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(username));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(password));
        Preconditions.checkArgument(userProp != null);

        User user = User.builder()
                .username(username)
                .status(true)
                .mobile(mobile)
                .email(email)
                .lastAccessEpochMs(System.currentTimeMillis())
                .properties(userProp)
                .build();


        user.setPassword(password);
        return user;
    }

    /**
     * called by UserDao to instantiate a user with id
     */
    public static User createUserFromDao(long id, String username, String email, String mobile,
                                         Boolean status, long lastAccessEpochMs, String properties) {
        return User.builder()
                .id(id)
                .username(username).email(email)
                .mobile(mobile).status(status)
                .lastAccessEpochMs(lastAccessEpochMs)
                .properties(UserProp.parse(properties))
                .build();
    }

    /**
     *  always set the password as salted
     * @param password
     */
    public void setPassword(String password) {
        if (StringUtils.isBlank(password)) {
            return;
        }

        this._properties._password = SecurityUtils.getSaltedHash(password);
    }

    public JSONObject generateTokenWithRole(String dbName) throws Exception {
        JSONObject result = new JSONObject();
        String jwtToken = JwtTokenUtil.generate(JwtPayLoad.of(dbName, User.getExternalId(_id), _username));
        result.put("token", jwtToken);
        result.put("username", _username);
        result.put("user", UserRolesBuilder.buildUserWithRoles(dbName, _id));
        /**
         * Special audit log!!!
         */
        @Cleanup Connection conn = DaoUtils.getConnection(true);
        DaoUtils.recordAuditLog(conn, dbName, _username, "read", UserDao.TABLE_NAME, "", "User signed in");
        return result;
    }
}
