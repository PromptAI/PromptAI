package com.zervice.kbase.api.restful;

import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.zervice.common.ding.DingTalkSender;
import com.zervice.common.filter.FilterConfigName;
import com.zervice.common.filter.MutableHttpServletRequest;
import com.zervice.common.pojo.common.Account;
import com.zervice.common.restful.exception.RestException;
import com.zervice.common.restful.exception.StatusCodes;
import com.zervice.common.utils.AccountExternalIdUtil;
import com.zervice.common.utils.AccountService;
import com.zervice.common.utils.NetworkUtils;
import com.zervice.kbase.*;
import com.zervice.kbase.database.dao.AccountDao;
import com.zervice.kbase.database.pojo.AccessToken;
import com.zervice.kbase.database.utils.DaoUtils;
import com.zervice.kbase.jwt.JwtPayLoad;
import com.zervice.kbase.jwt.JwtTokenUtil;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.Cleanup;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;

import java.io.IOException;
import java.sql.Connection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Log4j2
@WebFilter(urlPatterns = "/api/*", filterName = "authenticator",asyncSupported = true)
public class AuthFilter implements Filter {
    public static final String LOGIN = "login";
    public static final String SUPPORT_LOGIN = "support_login";
    public static final String ACCOUNTID = "account";

    /**
     * /user login identity, could be email:xxx@somewhere.com or mobile:xxxxx
     */
    public static final String IDENTITY = "identity";
    public static final String ID_EMAIL = "email:";
    public static final String ID_MOBILE = "mobile:";

    private Set<String> ignoreAuthPath = new HashSet<>();
    private Set<String> onlyLocalAccessPath = new HashSet<>();

    // can access on non-master node non-httpget requests
    private Set<String> ignoreMasterCheckPath = new HashSet<>();

    public static final String X_EXTERNAL_ACCOUNT_ID_HEADER = FilterConfigName.X_EXTERNAL_ACCOUNT_ID_HEADER;
    public static final String X_EXTERNAL_ACCOUNT_NAME_HEADER = FilterConfigName.X_EXTERNAL_ACCOUNT_NAME_HEADER;
    public static final String X_EXTERNAL_USER_ID_HEADER = FilterConfigName.X_EXTERNAL_USER_ID_HEADER;
    public static final String X_EXTERNAL_API_TOKEN_HEADER = FilterConfigName.X_EXTERNAL_API_TOKEN_HEADER;
    public static final String X_EXTERNAL_API_TOKEN_ID_HEADER = FilterConfigName.X_EXTERNAL_API_TOKEN_ID_HEADER;
    public static final String HEADER_HOST = FilterConfigName.HEADER_HOST;
    public static final String X_HOST = "X-host";
    public static final String TOKEN = FilterConfigName.HEADER_TOKEN;

    private static final String _SYS_ADMIN_URI = "/api/sysadmin";

    private Set<String> noAccountRequiredPath = new HashSet<>();
    private Set<String> apiTokenSupportedPath = new HashSet<>();
    // fast api 专用，不需要验证token，在filer设置Account...
    private Set<String> fastProjectAPIPath = new HashSet<>();


    @Autowired
    private AccountService accountService;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        fastProjectAPIPath.add("/api/fast/project");

        noAccountRequiredPath.add("/api/version");
        noAccountRequiredPath.add("/api/convert/");
        noAccountRequiredPath.add("/api/yml/");
        noAccountRequiredPath.add("/actuator");
        noAccountRequiredPath.add("/api/blobs/get");
        noAccountRequiredPath.add("/api/blobs/group/qrcode");
        noAccountRequiredPath.add("/api/auth/login");
        noAccountRequiredPath.add("/api/auth/logout");
        noAccountRequiredPath.add("/api/auth/quick/signin");
        noAccountRequiredPath.add("/api/auth/reset/pass");
        noAccountRequiredPath.add("/api/auth/active");
        noAccountRequiredPath.add("/api/notify/register");
        noAccountRequiredPath.add("/api/notify/login");
        noAccountRequiredPath.add("/api/notify/forget/pwd");
        noAccountRequiredPath.add("/api/notify/pre/check");
        noAccountRequiredPath.add("/api/auth/code");
        noAccountRequiredPath.add("/api/auth/supportlogin");
        noAccountRequiredPath.add("/api/trial/apply");
        noAccountRequiredPath.add("/api/oauth/google");
        noAccountRequiredPath.add("/api/shared/project");
        noAccountRequiredPath.add("/api/stripe/create/session");
        noAccountRequiredPath.add("/api/template");

        apiTokenSupportedPath.add("/api/auth/login");

        //联系我们留言
        noAccountRequiredPath.add("/rpc/contact/comment");

        ignoreAuthPath.add("/api/common/files");
        ignoreAuthPath.add("/api/version");
        ignoreAuthPath.add("/api/settings/users/avatar");
//        ignoreAuthPath.add("/api/agent/install");

        // let's ignore for demo pages ...
        ignoreAuthPath.add("/demo");

        // 登录相关的可以在非master上使用
        ignoreMasterCheckPath.add("/api/auth/");

        onlyLocalAccessPath.add("/api/accounts");
        onlyLocalAccessPath.add("/actuator"); // metrics
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
        Stopwatch stopwatch = Stopwatch.createStarted();

        if (request instanceof HttpServletRequest) {
            HttpServletRequest hreq = (HttpServletRequest) request;

            try {
                if (HttpMethod.OPTIONS.toString().equals(hreq.getMethod())) {
                    filterChain.doFilter(hreq, response);
                    return;
                }

                String servletPath = hreq.getServletPath();

                // 判断是不是master
                if (!HttpMethod.GET.toString().equals(hreq.getMethod())) {
                    if (!Environment.isMaster()) {
                        boolean ignored = false;
                        for (String path : ignoreMasterCheckPath) {
                            if (servletPath.startsWith(path)) {
                                ignored = true;
                                break;
                            }
                        }
                        if (!ignored) {
                            throw new RestException(StatusCodes.MUST_ON_MASTER_NODE, hreq.getRequestURI());
                        }
                    }
                }

                MutableHttpServletRequest mutableRequest = new MutableHttpServletRequest(hreq);

                // access by api token
                boolean accessByApiToken = _accessByApiToken(mutableRequest, response, filterChain);
                if (accessByApiToken) {
                    return;
                }

                for (String noRequireAccountName : noAccountRequiredPath) {
                    if (hreq.getServletPath().startsWith(noRequireAccountName)) {
                        filterChain.doFilter(hreq, response);
                        return;
                    }
                }

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

                for (String item : ignoreAuthPath) {
                    if (servletPath.startsWith(item)) {
                        /**
                         * !!! if we are deployed in standalone mode, we shall not do the check!
                         * 获取当前请求的Account属性
                         */
                        if (ZBotRuntime.isStandalone()) {
                            mutableRequest.putHeader(FilterConfigName.X_EXTERNAL_ACCOUNT_ID_HEADER, ZBotRuntime.DEFAULT_EXTERNAL_ACCOUNT_ID);
                        } else {
                            String accountExternalId = AccountExternalIdUtil.getAccountInRequest(mutableRequest, accountService);
                            if (StringUtils.isBlank(accountExternalId)) {
                                LOG.error("Account can't be found");
                                ((HttpServletResponse) response).sendError(HttpServletResponse.SC_UNAUTHORIZED,
                                        "account can't be found");
                                return;
                            }
                            mutableRequest.putHeader(FilterConfigName.X_EXTERNAL_ACCOUNT_ID_HEADER, accountExternalId);
                        }

                        filterChain.doFilter(mutableRequest, response);
                        return;
                    }
                }

                //set aid and uid into request
                String token = mutableRequest.getHeader(FilterConfigName.HEADER_TOKEN);
                if (StringUtils.isEmpty(token)) {
                    ((HttpServletResponse) response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "No valid token found");
                    return;
                }

                try {
                    //check token and get payload
                    JwtPayLoad payLoad = JwtTokenUtil.get(token);

                    ZBotContext.setUserName(payLoad.getUser());

                    //user id
                    mutableRequest.putHeader(FilterConfigName.X_EXTERNAL_USER_ID_HEADER, payLoad.getUid());

                    //accountId
                    String externalAccountId = payLoad.getAid();
                    if (accountSuspend(externalAccountId)) {
                        LOG.error("Account suspend:{} ", externalAccountId);
                        ((HttpServletResponse) response).sendError(HttpServletResponse.SC_UNAUTHORIZED);
                        return;
                    }
                    Account account = accountService.get(externalAccountId);

                    if (account == null) {
                        LOG.error("Account not found - " + externalAccountId);
                        throw new RestException(StatusCodes.Unauthorized, "Account invalid");
                    }

                    mutableRequest.putHeader(FilterConfigName.X_EXTERNAL_ACCOUNT_ID_HEADER, account.getExternalId());
                    filterChain.doFilter(mutableRequest, response);
                    return;
                } catch (RestException e) {
                    if (e.getStatus().getHttpStatusCode() == HttpServletResponse.SC_UNAUTHORIZED) {
                        LOG.error("Reject request with reason - " + e.getStatus().getInternalStatusCode());
                        ((HttpServletResponse) response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        return;
                    }


                    LOG.warn("Caught unknown exception for handling servlet - " + servletPath, e);
                    // re-throw the exception so other logic can take care of ...
                    throw e;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } finally {
                int status = ((HttpServletResponse) response).getStatus();
                stopwatch.stop();
                long usedTimeInMs = stopwatch.elapsed(TimeUnit.MILLISECONDS);

                //print log if a servlet cost more than 30 seconds
                if (usedTimeInMs > 30_000) {
                    LOG.warn(String.format(
                            "Detect VERY SLOW request. (method=%s, servlet=%s, time=%ds, code=%d)",
                            hreq.getMethod(), hreq.getServletPath(), usedTimeInMs / 1000, status
                    ));
                    DingTalkSender.sendQuietly(String.format(
                            "[%s][Detect VERY SLOW request. (method=%s, servlet=%s, time=%ds, code=%d)]",
                            ServerInfo.getName(), hreq.getMethod(), hreq.getServletPath(), usedTimeInMs / 1000, status));
                }
            }
        } else {
            LOG.error("Unexpected request - " + request.toString());
        }
        ((HttpServletResponse) response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    public static boolean accountSuspend(String dbName) {
        try {
            @Cleanup Connection conn = DaoUtils.getConnection();
            Account account = AccountDao.getByDbName(conn, dbName);
            if (account != null && (account.isSuspend() || account.isDeleted())) {
                LOG.info("account suspend or deleted dbname:{}", dbName);
                return true;
            }
            return false;
        } catch (Exception ex) {
            LOG.error("account status valid failed . error:{}", ex.getMessage(), ex);
            return false;
        }
    }

    /**
     * if request access by api token
     */
    private boolean _accessByApiToken(MutableHttpServletRequest mutableRequest, ServletResponse response, FilterChain filterChain)
            throws IOException, ServletException {
        String servletPath = mutableRequest.getServletPath();
        boolean apiTokenSupport = apiTokenSupportedPath.stream().anyMatch(p -> p.startsWith(servletPath));
        String apiToken = mutableRequest.getHeader(X_EXTERNAL_API_TOKEN_HEADER);
        String apiTokenId = mutableRequest.getHeader(X_EXTERNAL_API_TOKEN_ID_HEADER);

        if (apiTokenSupport && StringUtils.isNotBlank(apiToken) && StringUtils.isNotBlank(apiTokenId)) {
            AccountCatalog accountCatalog = _findAccountCatalogByAiToken(apiTokenId, apiToken);
            mutableRequest.putHeader(FilterConfigName.X_EXTERNAL_ACCOUNT_ID_HEADER, accountCatalog.getDBName());
            filterChain.doFilter(mutableRequest, response);
            return true;
        }

        return false;
    }

    private static AccountCatalog _findAccountCatalogByAiToken(String accessId, String apiToken) {
        for (AccountCatalog a : AccountCatalog.getAllAccounts()) {
            if (a.getAccessTokens().validate(AccessToken.fromExternalID(accessId), apiToken)) {
                return a;
            }
        }

        LOG.error("no account match api:{}, token:{} ", accessId, apiToken);
        throw new RestException(StatusCodes.Unauthorized);
    }

    static public AccountCatalog resolveAccountCatalog(HttpServletRequest hreq, String email, String mobile) {
        /**
         * The first step is to determine the account of the user
         * If we have host header or X-host header, we would try to use it as account name
         * to resolve account, if failed, we would fall on domain name (the part after '@' in
         * user email to find account)
         */
        AccountCatalog account = resolveAccountCatalog(hreq);

        if (account == null) {
            if (!StringUtils.isEmpty(email)) {
                if (AccountCatalog.isGlobalAdminAccount(email)) {
                    String host = hreq.getHeader(X_HOST);
                    if (StringUtils.isEmpty(host)) {
                        host = hreq.getHeader("host");
                    }
                    String acctName = AccountExternalIdUtil.getAccountNameFromHost(host);
                    if (!StringUtils.isEmpty(acctName)) {
                        AccountCatalog accountCatalog = AccountCatalog.guess(acctName);
                        if (accountCatalog != null) {
                            return accountCatalog;
                        }
                    }
                }

                if (!ZBotRuntime.usingSubdomains()) {
                    // only if we are not using subdomains ...
                    account = AccountCatalog.getUserAccountByEmail(email);
                    if (account != null) {
                        return account;
                    }
                }

                // last resolve, not actually necessary
//                String domain = email.substring(email.indexOf('@') + 1);
//                LOG.info("Try resolve account using user email ... domain:{}",domain);
//                account = AccountCatalog.findByMxDomain(domain);

                // this might be a mobile number!?? as here, user may input in mobile # instead of email
                account = AccountCatalog.getUserAccountByMobile(email);
                if (account != null) {
                    return account;
                }
            } else if (!StringUtils.isEmpty(mobile)) {
                account = AccountCatalog.getUserAccountByMobile(mobile);
                if (account != null) {
                    return account;
                }
            }
        }

        return account;
    }


    public static AccountCatalog resolveAccountCatalog(HttpServletRequest hreq) {
        if (ZBotRuntime.isStandalone()) {
            return AccountCatalog.of(ZBotRuntime.DEFAULT_EXTERNAL_ACCOUNT_ID);
        }

        HttpSession session = hreq.getSession(false);
        if (session != null) {
            Object val = session.getAttribute(ACCOUNTID);
            if (val != null) {
                return AccountCatalog.of((Long) val);
            }
        }

        String externalAccountId = hreq.getHeader(X_EXTERNAL_ACCOUNT_ID_HEADER);
        try {
            if (Strings.isNullOrEmpty(externalAccountId)) {
                /**
                 * the host might just be IP addresses! in such cases, we would try x-host instead
                 * Or better, if x-host exists, we would try it first, then default host header
                 */
                String host = hreq.getHeader(X_HOST);
                if (StringUtils.isEmpty(host)) {
                    host = hreq.getHeader("host");
                }

                if (ZBotRuntime.usingSubdomains()) {
                    String acctName = AccountExternalIdUtil.getAccountNameFromHost(host);
                    if (StringUtils.isEmpty(acctName)) {
                        return null;
                    }

                    // the acctName might be an external Id or a name ...
                    return AccountCatalog.guess(acctName);
                }

                return null;
            } else {
                //support change account name
                return AccountCatalog.guess(externalAccountId);
            }
        } catch (Exception e) {
            LOG.warn("Error searching account, externalAccountId:{}, xHost:{}, host:{}, e:{}",
                    externalAccountId, hreq.getHeader(X_HOST), hreq.getHeader(HEADER_HOST),
                    e.getMessage(), e);

            return null;
        }
    }


}

