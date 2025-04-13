package com.zervice.common.utils;


import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Locale;

@Log4j2
public class ServletUtils {

    /**
     * nginx 代理后真正的IP地址放在header中
     */
    private final static String NGINX_REAL_IP_HEADER = "X-Forwarded-For";

    /**
     * get user real ip
     *
     * @return ip
     */
    public static String getCurrentIp() {
        HttpServletRequest request = getWebRequest();
        return NetworkUtils.getRemoteIP(request);
    }

    public static HttpServletRequest getWebRequest() {
        return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
    }

    /**
     * get user real ip no proxy
     *
     * @return ip
     */
    public static String getCurrentNoProxyIp() {
        String ip = getCurrentIp();
        return ip.split(",")[0];
    }

    /**
     * get user real ip no proxy
     *
     * @return ip
     */
    public static String getCurrentNoProxyIpWithoutError() {
        try {
            String ip = getCurrentIp();
            return ip.split(",")[0];
        } catch (Exception e) {
            return "0.0.0.0";
        }
    }

    public static Locale getLocale() {
        Locale locale = LocaleContextHolder.getLocale();
        if (StringUtils.isBlank(locale.getLanguage())) {
            locale = Locale.CHINA;
        }
        return locale;
    }
}
