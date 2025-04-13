package com.zervice.kbase.api.restful;

import com.zervice.common.utils.NetworkUtils;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;

@Log4j2
@WebFilter(urlPatterns = "/actuator/*", filterName = "actuator", asyncSupported = true)
public class ActuatorFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest hreq = (HttpServletRequest) request;
        /**
         * only accept local access for Account apis
         */
        String remoteIp = NetworkUtils.getRemoteIP(hreq);
        if (!NetworkUtils.isLocalAddr(remoteIp)) {
            LOG.warn("Forbid access for admin apis, remote={}", remoteIp);
            ((HttpServletResponse) response).sendError(HttpServletResponse.SC_UNAUTHORIZED,
                    "Only allow local access");
            return;
        }

        filterChain.doFilter(hreq, response);
    }
}
