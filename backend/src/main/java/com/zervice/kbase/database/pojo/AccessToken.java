package com.zervice.kbase.database.pojo;

import com.zervice.common.utils.Base36;
import com.zervice.common.utils.IdGenerator;
import com.zervice.kbase.database.SecurityUtils;
import com.zervice.kbase.jwt.JwtTokenUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;


@ToString
public class AccessToken {
    private static final String _PREFIX = "at";

    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_SUSPEND = "suspend";

    @Getter @Setter
    Long _id;

    @Getter @Setter
    Long _userId;

    @Getter @Setter
    String _token;

    @Getter @Setter
    String _status;

    @Getter @Setter
    Long _createdOn;

    public boolean active() {
        return STATUS_ACTIVE.equals(_status);
    }

    public String toRpcToken() {
        return JwtTokenUtil.PREFIX + getExternalID(this._id) + this._token;
    }

    public static AccessToken factory(long userId) {
        AccessToken accessToken = new AccessToken();
        accessToken.setId(IdGenerator.generateId());
        accessToken.setUserId(userId);
        accessToken.setCreatedOn(System.currentTimeMillis());
        accessToken.setToken(SecurityUtils.generateCollectorAccessKey());
        accessToken.setStatus(AccessToken.STATUS_ACTIVE);
        return accessToken;
    }

    public static String getExternalID(long id) {
        return _PREFIX + Base36.encode(id);
    }

    public static long fromExternalID(String id) {
        if (id.startsWith(_PREFIX)) {
            return Base36.decode(id.substring(2));
        }

        throw new IllegalArgumentException("Invalid exrernal access token ID - " + id);
    }

}
