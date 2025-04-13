package com.zervice.kbase.api.restful.pojo;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Strings;
import com.zervice.common.utils.LoginUtils;
import com.zervice.kbase.AccountCatalog;
import com.zervice.kbase.api.restful.group.Insert;
import com.zervice.kbase.api.restful.group.Update;
import com.zervice.kbase.database.SecurityUtils;
import com.zervice.kbase.database.pojo.User;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

;

@EqualsAndHashCode(of = "_id")
@ToString
public class RestUser {

    @Getter
    @Setter
    @DecimalMin(value = "1", message = "id is required and must be higher than 0", groups = {Update.class})
    private String _id;

    @Getter
    @Setter
    @NotBlank(message = "username is required", groups = {Insert.class, Update.class})
    private String _username;

    @Getter
    @Setter
    private String _avatar;

    @Getter
    @Setter
//    @NotBlank(message = "phone is required", groups = {Insert.class, Update.class})
    private String _mobile;

    @Getter
    @Setter
//    @NotBlank(message = "email is required", groups = {Insert.class, Update.class})
//    @Email(message = "email is error format", groups = {Insert.class, Update.class})
    private String _email;

    /**
     * 暂时可以不传,moren
     */
    @Getter
    @Setter
//    @Size(min = 1)
//    @NotEmpty(message = "role required")
    private Set<RestRole> _roles = new HashSet<>();

    @Getter
    @Setter
//    @NotNull(message = "status is required", groups = Update.class)
    private Boolean _status = Boolean.TRUE;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @NotBlank(message = "passwd is required", groups = Insert.class)
    private String password;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String file;

    /**
     * If this user is the owner of this account. An owner must be an admin
     * This cannot be changed by UI, as owner email is saved in account.configurations
     */
    @Setter
    @Getter
    private boolean _owner = false;

    @Setter
    @Getter
    private boolean _admin = false;


    @Getter
    @Setter
    private Long _createTime;

    @Getter
    @Setter
    private Long _lastAccessedAt;

    @Getter
    @Setter
    private String _creator;

    @Getter
    @Setter
    private String _desc;
    @Getter
    @Setter
    private String _initPass;
    @Getter
    @Setter
    private JSONObject _config;
    @Getter
    @Setter
    private Boolean _firstLogin;

    @Setter@Getter
    private Long _restToken;

    @Setter@Getter
    private String _from;

    @Setter@Getter
    private Boolean _featureEnable;
    public RestUser() {

    }

    public RestUser(User dbUser, List<RestRole> roleList, String creator) {
        this._id = String.valueOf(dbUser.getId());
        this._username = dbUser.getUsername();
        this._mobile = dbUser.getMobile();
        this._email = dbUser.getEmail();
        this._roles.addAll(roleList);
        this._status = dbUser.getStatus();
        this._createTime = dbUser.getProperties().getCreateTime();
        this._avatar = dbUser.getProperties().getAvatar();
        this._creator = creator;
        this._desc = dbUser.getProperties().getDesc();
        this._initPass = dbUser.getProperties().getInitPass();
        this._config = dbUser.getProperties().getConfig();
        this._from = dbUser.getProperties().getFrom();
        // 这标记下是否展示修改密码的窗口，如果是sso登录，则不显示密码登录窗口
        if (_from == null) {
            _from = User.UserProp.FROM_REGISTRY;
        }
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPassword() {
        return password;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public RestUser(User dbUser, long acctId) {
        this._id = User.getExternalId(dbUser.getId());
        this._username = dbUser.getUsername();
        this._email = (null == dbUser.getEmail()) ? "" : dbUser.getEmail().toLowerCase();
        this._mobile = (null == dbUser.getMobile()) ? "" : dbUser.getMobile();
        this._lastAccessedAt = dbUser.getLastAccessEpochMs();

        this._status = dbUser.getStatus();


        // set the soft state here
        AccountCatalog acct = AccountCatalog.of(acctId);
        if (acct != null) {
            // the first user's email or phone shall be the owner
            _owner = StringUtils.equalsIgnoreCase(_email, acct.getOwner())
                    || StringUtils.equalsIgnoreCase(_mobile, acct.getOwner());

            // if this user is an admin
            _admin = acct.isAccountAdmin(dbUser);
        }

        if (dbUser.getRoles() != null) {
            this._roles = acct.getAcm().getUserRoleList(dbUser.getId()).stream().map(RestRole::new).collect(Collectors.toSet());

        }
    }

    public User toDbObject() {
        String email = LoginUtils.normalizeEmail(_email);
        String mobile = LoginUtils.normalizePhone(_mobile);
        User user = User.builder()
                .username(_username)
                .email(StringUtils.isEmpty(email) ? null : email)
                .mobile(StringUtils.isEmpty(mobile) ? null : mobile)
                .status(_status)
                .build();

        if (_config == null) {
            _config = User.UserProp.defaultConfig(user);
        }
        User.UserProp prop = User.UserProp.builder()
                .password(Strings.isNullOrEmpty(password) ? "" : SecurityUtils.getSaltedHash(password))
                .config(_config)
                .createTime(System.currentTimeMillis())
                .desc(_desc)
                .avatar(_avatar)
                .from(_from)
                .build();
        user.setProperties(prop);

        if (!Strings.isNullOrEmpty(_id)) {
            user.setId(User.fromExternalId(_id));
        }
        return user;
    }

}
