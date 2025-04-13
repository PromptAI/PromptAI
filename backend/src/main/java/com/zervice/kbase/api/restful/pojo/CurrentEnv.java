package com.zervice.kbase.api.restful.pojo;

import com.zervice.common.utils.ServletUtils;
import com.zervice.kbase.database.pojo.User;
import com.zervice.kbase.api.restful.AuthFilter;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 记录一些Audit 公共信息 比如当前的用户和ip
 */
@Getter
public class CurrentEnv {
    private long _userId = -1; // 当前登录的用户
    private String _userIp = ""; // 当前登录的用户ip
    private long _userTime = System.currentTimeMillis(); // 记录当前的时间
    private String _dbName;

    private CurrentEnv() {
        HttpServletRequest request = ServletUtils.getWebRequest();
        _userIp = request.getRemoteHost();
        String externalUid = request.getHeader(AuthFilter.X_EXTERNAL_USER_ID_HEADER);
        if (StringUtils.isEmpty(externalUid)) {
            throw new IllegalStateException("Can't find user id from request - " + request.getRequestURL());
        } else {
            _userId = User.fromExternalId(externalUid);
        }
        _dbName = request.getHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER);
        if (StringUtils.isEmpty(_dbName)) {
            throw new IllegalStateException("Can't find account from request - " + request.getRequestURL());
        }
    }

    public static CurrentEnv get() {
        return new CurrentEnv();
    }
}
