package com.zervice.common.pojo.common;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.zervice.common.utils.Base36;
import lombok.*;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;

import java.util.TimeZone;

/**
 * Like other objects, we use an NoSQL approach to store
 * accounts in the MySQL table.
 * <p>
 * An account object has an id that is a 64-bit long generated using
 * the snowflake algorithm and global-wise unique.
 * <p>
 * The name is a single word string such as "cnn", "netflix". It's
 * case-insensitive.
 * <p>
 * Everything else is stored in a JSON object whose corresponding
 * table column is "properties". The JSON object has the following
 * format:
 * <p>
 * {
 * ver: 1,
 * status: "ready", // the value could be
 * // "ready"|"suspended"|"terminated"|"settingUp1"
 * // "settingUp1" means our admin finishes the
 * // initial setup and waits for the user enters
 * // more information to finish the entire setup
 * // process
 * createdAtEpochMs: 454549454, // when this account was created
 * createdByAdmin: "abc@zervice.us", // which internal employee initialize
 * // the creation of the account
 * createdByUser: "def@cnn.com",       // which account employee finishes
 * // the account setup
 * setupFinishedAtEpochMs: 5684054,    // when the account setup is finished
 * // by the account employee
 * timezone: "America/Los_Angeles"     // account's timezone
 * }
 * <p>
 * So the JSON object returned by RESTful API has the format
 * {
 * id: "a34344", // "a" + Base33(id)
 * name: "cnn",
 * properties: {
 * ver: 1,
 * status: "ready",
 * type: "trial"
 * createdAtEpochMs: 4095409584,
 * createdByAdmin: "abc@zervice.us",
 * createdByUser: "def@cnn.com",
 * setupFinishedAtEpochMs: 45453453,
 * timezone: "America/Los_Angeles"
 * }
 * }
 */
@EqualsAndHashCode
@ToString
@Log4j2
public class Account {
    public static final String SYS_ADMIN_EMAIL = "admin@promptai.us";
    public static final String PROP_API_RATE_LIMITER = "api_rate_limiter";
    public static final String PROP_CREATED_AT = "createdAtEpochMs";
    public static final String PROP_TYPE = "type";
    public static final String PROP_STATUS = "status";
    public static final String PROP_FULL_NAME = "fullName";
    public static final String PROP_MX_DOMAIN = "mxDomain";
    public static final String PROP_DOMAIN = "domain";
    public static final String PROP_CREATE_BY = "createdByUser";
    public static final String PROP_CREATE_BY_USERNAME = "createdByUserName";
    public static final String PROP_TIMEZONE = "timezone";
    public static final String PROP_REGISTRY_CODE = "registryCode";
    public static final String PROP_PASSCODE = "passcode";
    //活跃用户：true 非活跃用户：false
    public static final String PROP_ACTIVE = "active";
    //标记时间
    public static final String PROP_ACTIVE_UPDATE_TIME = "activeUpdateTime";
    //资源释放时间
    public static final String PROP_RELEASE_RESOURCE_TIME = "releaseResourceTime";
    //资源释放后是否需要通知
    public static final String PROP_RELEASE_RESOURCE_NEED_NOTIFY = "releaseResourceNeedNotify";
    //上次登录时间
    public static final String PROPERTY_LAST_LOGIN = "lastLogin";
    // 账户模式：normal / chat, 默认是 null，表示为normal模式
    public static final String PROPERTY_MODEL = "model";
    // 计划删除时间: 标记删除后，延迟一段时间进行物理删除
    public static final String PROPERTY_DELETE_TIME = "delete_time";
    // 是否启用新特性: true/ false
    public static final String PROPERTY_FEATURE_ENABLE = "feature_enable";
    // Rest token 告警通知的 limit
    public static final String PROPERTY_REST_TOKEN_NOTIFY_LIMIT = "rest_token_notify_limit";

    public static final String STATUS_READY = "ready";
    public static final String STATUS_SUSPENDED = "suspended";
    public static final String STATUS_TERMINATED = "terminated";
    public static final String STATUS_SETTINGUP1 = "settingUp1";
    public static final String STATUS_DELETED = "deleted";

    public static final String TYPE_TRIAL = "trial";
    public static final String TYPE_NORMAL = "normal";
    private static final int _EVENTS_MAX_SIZE = 10;

    public static final String TIMEZONE_GMT8 = "GMT+8";

    private static final String[] _VALID_STATUS = new String[]{
            STATUS_READY, STATUS_SETTINGUP1, STATUS_SUSPENDED, STATUS_TERMINATED
    };


    @Getter
    @Setter
    long _id;

    @Getter
    @Setter
    @NonNull
    String _name;

    @Getter
    @Setter
    @NonNull
    String _dbName;

    @Getter
    @Setter
    Long _restToken;

    @Getter
    @Setter
    JSONArray _events;

    @Getter
    @Setter
    JSONArray _notes;

    @Getter
    @Setter
    JSONObject _properties;

    @JsonProperty("id")
    public String getExternalId() {
        return getExternalId(_id);
    }

    public static String getExternalId(long id) {
        return "a" + Base36.encode(id);
    }

    public static long fromExternalId(String extId) {
        if (!extId.startsWith("a")) {
            throw new IllegalArgumentException("Invalid exernal account ID - " + extId);
        }

        return Base36.decode(extId.substring(1));
    }

    @JsonIgnore
    public boolean featureEnabled() {
        return Boolean.TRUE.equals(_properties.getBoolean(PROPERTY_FEATURE_ENABLE));
    }

    @JsonIgnore
    public Long getRestTokenNotifyLimit() {
        return _properties.getLong(Account.PROPERTY_REST_TOKEN_NOTIFY_LIMIT);
    }

    public void setRestTokenNotifyLimit(Long limit) {
        _properties.put(Account.PROPERTY_REST_TOKEN_NOTIFY_LIMIT, limit);
    }


    /**
     * Generate the db name of the account from its id. The db name is
     *   'a' + Base36Encoding(id)
     */
    @JsonIgnore
    public String getAccountDbName() {
        return getExternalId();
    }

    public static String getAccountDbName(long id) {
        return getExternalId(id);
    }

    public static boolean isValidStatus(String status) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(status));
        for (int i = 0; i < _VALID_STATUS.length; i++) {
            if (status.equals(_VALID_STATUS[i])) {
                return true;
            }
        }

        return false;
    }

    /**
     * return true only when account property is status ready.
     */
    public boolean isAccountReady() {
        String status = StringUtils.EMPTY;
        try {
            status = _properties.getString("status");
        } catch (Exception e) {
            LOG.error("Cannot get account properties due to exception - " + getAccountDbName(), e);
        }

        return StringUtils.equalsIgnoreCase(status, STATUS_READY);
    }

    public String getStatus() {
        return _properties.getString(PROP_STATUS);
    }

    public boolean isDeleted() {
        return STATUS_DELETED.equals(getStatus());
    }


    public void addEvent(long timestamp, String event) {

        if (_events == null) {
            _events = new JSONArray();
        }

        JSONObject e = new JSONObject();
        e.put("timeEpoch", timestamp);
        e.put("event", event);

        _events.add(0, e);

        if (_events.size() > _EVENTS_MAX_SIZE) {
            _events.subList(_EVENTS_MAX_SIZE, _events.size()).clear();
        }
    }

    public void addNote(long timestamp, String note) {
        if (_notes == null) {
            _notes = new JSONArray();
        }

        JSONObject e = new JSONObject();
        e.put("timeEpoch", timestamp);
        e.put("note", note);

        _notes.add(0, note);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(JSONObject props) {
        return new Builder(props);
    }

    public boolean isSuspend() {
        String status = _properties.getString("status");
        if (STATUS_SUSPENDED.equals(status)) {
            return true;
        }
        return false;
    }

    public static class Builder {
        long id;
        String name;
        String dbName;
        String events;
        String notes;
        JSONObject jprops;


        Builder() {
            jprops = new JSONObject();
        }

        Builder(JSONObject props) {
            this.jprops = props;
        }


        public Account build() {
            Preconditions.checkState(!Strings.isNullOrEmpty(name) &&
                    id > 0 &&
                    jprops.containsKey("status"));

            jprops.put("ver", 1);
            //默认是活跃账号
            long now = System.currentTimeMillis();
            jprops.put(PROP_ACTIVE, true);
            jprops.put(PROP_ACTIVE_UPDATE_TIME, now);
            jprops.put(PROPERTY_LAST_LOGIN, now);

            Account a = new Account();
            a.setId(this.id);
            a.setName(this.name);
            a.setProperties(jprops);
            a.setDbName(StringUtils.isEmpty(dbName) ? Account.getExternalId(id) : dbName);
            a.setEvents(StringUtils.isEmpty(events) ? new JSONArray() : JSONArray.parseArray(events));
            a.setNotes(StringUtils.isEmpty(notes) ? new JSONArray() : JSONArray.parseArray(notes));
            a.setProperties(jprops);


            return a;
        }

        public Builder id(long id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            Preconditions.checkArgument(!Strings.isNullOrEmpty(name));
            this.name = name;
            return this;
        }

        // if empty, use external ID (so id shall be valid)
        public Builder dbName(String name) {
            this.dbName = name;
            return this;
        }

        public Builder events(String events) {
            this.events = events;
            return this;
        }

        public Builder notes(String notes) {
            this.notes = notes;
            return this;
        }


        public Builder status(String status) {
            Preconditions.checkArgument(!Strings.isNullOrEmpty(status));
            if (!isValidStatus(status)) {
                throw new IllegalArgumentException("Unknown status " + status);
            }

            this.jprops.put("status", status);
            return this;
        }

        public Builder createdAtEpochMs(long tm) {
            Preconditions.checkArgument(tm > 0);
            jprops.put("createdAtEpochMs", tm);
            return this;
        }

        public Builder createdByAdmin(String admin) {
            Preconditions.checkArgument(!Strings.isNullOrEmpty(admin));
            jprops.put("createdByAdmin", admin);
            return this;
        }

        public Builder createdByUser(String user) {
            Preconditions.checkArgument(!Strings.isNullOrEmpty(user)
                    //admin is not an email, it would create account fail
                    // && EmailValidator.getInstance().isValid(user)
            );
            jprops.put("createdByUser", user);
            return this;
        }

        public Builder setupFinishedAtEpochMs(long tm) {
            Preconditions.checkArgument(tm > 0);
            jprops.put("setupFinishedAtEpochMs", tm);
            return this;
        }

        public Builder timezone(String tz) {
            Preconditions.checkArgument(!Strings.isNullOrEmpty(tz));
            TimeZone timeZone = TimeZone.getTimeZone(tz);
            Preconditions.checkArgument(timeZone != null);
            jprops.put("timezone", tz);
            return this;
        }
    }

    /**
     * 是否活跃账户
     * 如果为 null ，返回false
     * @return
     */
    public boolean active() {
        Boolean active = _properties.getBoolean(PROP_ACTIVE);
        if (active == null) {
            return false;
        }

        return active;
    }

    /**
     * active :是否是活跃用户
     * needNotify :是否需要发送通知
     */
    public void updateActive(boolean active) {
        _properties.put(PROP_ACTIVE, active);
        _properties.put(PROP_ACTIVE_UPDATE_TIME, System.currentTimeMillis());
    }

    /**
     * 是否需要通知
     * @param releaseResourceTime
     */
    public void updateReleaseResourceTime(long releaseResourceTime, boolean needNotify) {
        _properties.put(PROP_RELEASE_RESOURCE_TIME, releaseResourceTime);
        _properties.put(PROP_RELEASE_RESOURCE_NEED_NOTIFY, needNotify);
    }

    public boolean isTrialAccount() {
        return TYPE_TRIAL.equals(_properties.getString(Account.PROP_TYPE));
    }

    /**
     * 先将系统标记为删除，然后在milleSeconds 进行物理删除
     */
    public void delete() {
        long now = System.currentTimeMillis();
        // update account Name = xxx_deleted_now
        this._name = this._name + "_deleted_" + now;
        _properties.put(PROP_STATUS, STATUS_DELETED);
        _properties.put(PROPERTY_DELETE_TIME, now);
    }

}
