package com.zervice.broker.restful.filter;

import com.zervice.common.filter.FilterConfigName;
import com.zervice.common.filter.MutableHttpServletRequest;
import com.zervice.common.utils.AccountExternalIdUtil;
import com.zervice.common.utils.NetworkUtils;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpMethod;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * broker filter
 * 从host中获取account name
 *
 * @author Peng Chen
 * @date 2020/3/7
 */
@Log4j2
@WebFilter(urlPatterns = "/*", filterName = "brokerFilter", asyncSupported = true)
public class BrokerFilter implements Filter {

    private Set<String> noAccountRequiredPath = new HashSet<>();
    private Set<String> onlyLocalAccessPath = new HashSet<>();
    public static final String X_EXTERNAL_ACCOUNT_NAME_HEADER = FilterConfigName.X_EXTERNAL_ACCOUNT_NAME_HEADER;
    public static final String X_EXTERNAL_ACCOUNT_ID_HEADER = FilterConfigName.X_EXTERNAL_ACCOUNT_ID_HEADER;
    public static final String X_EXTERNAL_PUBLISHED_PROJECT_HEADER = FilterConfigName.X_EXTERNAL_PUBLISHED_PROJECT_HEADER;
    public static final String X_EXTERNAL_PUBLISHED_PROJECT_TOKEN_HEADER = FilterConfigName.X_EXTERNAL_PUBLISHED_PROJECT_TOKEN_HEADER;
    public static final String HEADER_HOST = FilterConfigName.HEADER_HOST;
    public static final String TOKEN = FilterConfigName.HEADER_TOKEN;
    public static final String X_EXTERNAL_PROJECT_ID_HEADER = FilterConfigName.X_EXTERNAL_PROJECT_ID_HEADER;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        noAccountRequiredPath.add("/api/version");
        noAccountRequiredPath.add("/actuator/");
        noAccountRequiredPath.add("/api/message");
        noAccountRequiredPath.add("/api/chat");
    }

    /**
     * If this is an OPTION, simply return 200?
     *
     * @param request
     * @param response
     * @param filterChain
     * @throws IOException
     * @throws ServletException
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        if (request instanceof HttpServletRequest) {
            HttpServletRequest hreq = (HttpServletRequest) request;

            if (HttpMethod.OPTIONS.toString().equals(hreq.getMethod())) {
                filterChain.doFilter(hreq, response);
                return;
            }
            for (String noRequireAccountName : noAccountRequiredPath) {
                if (hreq.getServletPath().startsWith(noRequireAccountName)) {
                    filterChain.doFilter(hreq, response);
                    return;
                }
            }

            String servletPath = hreq.getServletPath();
            for (String path : onlyLocalAccessPath) {
                if (servletPath.startsWith(path)) {
                    /**
                     * only accept local access for Account apis
                     */
                    String remoteIp = NetworkUtils.getRemoteIP(hreq);
                    if (!NetworkUtils.isLocalAddr(remoteIp)) {
                        LOG.warn("Forbid access for admin apis, remote={}", remoteIp);
                        ((HttpServletResponse) response).sendError(HttpServletResponse.SC_UNAUTHORIZED,
                                "Only allow local access");
                        return;
                    } else {
                        filterChain.doFilter(hreq, response);
                        return;
                    }
                }
            }

            MutableHttpServletRequest mutableRequest = new MutableHttpServletRequest(hreq);

            //获取accountName
            String accountName = _getAccountInRequest(mutableRequest);
            if (StringUtils.isBlank(accountName)) {
                ((HttpServletResponse) response).sendError(HttpServletResponse.SC_UNAUTHORIZED,
                        "account cant'be found");
                return;
            }

            mutableRequest.putHeader(X_EXTERNAL_ACCOUNT_NAME_HEADER, accountName);


            filterChain.doFilter(mutableRequest, response);
            return;
        }

        ((HttpServletResponse) response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    /**
     * return accountId if existed;
     * If the accountName is blank or account not fount the null will be return
     */
    private String _getAccountInRequest(MutableHttpServletRequest httpRequest) {
        String accountName = httpRequest.getHeader(X_EXTERNAL_ACCOUNT_NAME_HEADER);
        if (StringUtils.isNotBlank(accountName)) {
            return accountName;
        }

        String host = httpRequest.getHeader(HEADER_HOST);
        if (StringUtils.isNotBlank(host)) {
            //不是ip，从host中取account
            return AccountExternalIdUtil.getAccountNameFromHost(host);
        }

        LOG.error("request.host is black {}", host);
        return null;
    }


}
