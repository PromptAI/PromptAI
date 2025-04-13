package com.zervice.common.utils;

import cn.hutool.core.net.NetUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Log4j2
public class NetworkUtils {

    public static String getRemoteIP(HttpServletRequest request) {
        if (StringUtils.isEmpty(request.getHeader("x-forwarded-for"))) {
            return request.getRemoteAddr();
        }
        return request.getHeader("x-forwarded-for");
    }

    public static String getRemoteScheme(HttpServletRequest request) {
        String scheme = request.getHeader("X-Forwarded-Proto");
        LOG.info("[get scheme:{} from http request]", scheme);
        if (StringUtils.isEmpty(scheme)) {
            return request.getScheme();
        }
        return scheme;
    }

    public static String getRemoteHost(HttpServletRequest request) {
        String host = request.getHeader("X-Forwarded-Server");
        LOG.info("[get host:{} from http request]", host);
        if (StringUtils.isEmpty(host)) {
            return request.getRemoteAddr();
        }
        return host;
    }

    public static String getRemotePort(HttpServletRequest request) {
        String remotePort = request.getHeader("X-Forwarded-Port");
        LOG.info("[get remote port:{} from http request]", remotePort);
        if (StringUtils.isEmpty(remotePort)) {
            return request.getRemotePort() + "";
        }
        return remotePort;
    }

    public static boolean isLocalAddr(String addr) {
        try {
            InetAddress netAddr = InetAddress.getByName(addr);
            return netAddr.isAnyLocalAddress() || netAddr.isLoopbackAddress()
                    || netAddr.isLinkLocalAddress() || NetUtil.isInnerIP(addr);
        } catch (UnknownHostException e) {
            LOG.warn("Unknown host {}", addr, e);
            return false;
        }
    }


}
