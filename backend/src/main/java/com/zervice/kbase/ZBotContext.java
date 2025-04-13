package com.zervice.kbase;

import com.zervice.kbase.api.restful.AuthFilter;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

@UtilityClass
public class ZBotContext {
    private final ThreadLocal<String> userName = new InheritableThreadLocal<>();

    private final ThreadLocal<String> _identity = new InheritableThreadLocal<>();

    public void setIdentity(String name) {
        _identity.set(name);
    }

    public String getIdentity() {
        String name = _identity.get();
        if (StringUtils.isEmpty(name)) {
            return AuthFilter.ID_EMAIL + "<anonym>";
        }

        return name;
    }

    public String resolveUser() {
        return resolveUser(getIdentity());
    }

    // helper to strip user email or mobile from identity
    public static String resolveUser(String identity) {
        if(identity.startsWith(AuthFilter.ID_EMAIL)) {
            return identity.substring(AuthFilter.ID_EMAIL.length());
        }
        else {
            return identity.substring(AuthFilter.ID_MOBILE.length());
        }
    }

    public void setUserName(String name) {
        userName.set(name);
    }

    public String getUserName() {
        String val = userName.get();
        if(StringUtils.isEmpty(val)) {
            return "system";
        }

        return val;
    }
}
