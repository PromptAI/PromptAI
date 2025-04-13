package com.zervice.kbase.api.rpc;

import com.google.common.base.Stopwatch;
import com.zervice.common.ShareConfig;
import com.zervice.common.ding.DingTalkSender;
import com.zervice.common.filter.FilterConfigName;
import com.zervice.common.filter.MutableHttpServletRequest;
import com.zervice.common.pojo.common.Account;
import com.zervice.common.utils.AccountExternalIdUtil;
import com.zervice.common.utils.AccountService;
import com.zervice.common.utils.Constants;
import com.zervice.common.utils.LayeredConf;
import com.zervice.kbase.AccountCatalog;
import com.zervice.kbase.ServerInfo;
import com.zervice.kbase.ZBotRuntime;
import com.zervice.kbase.database.dao.AccountDao;
import com.zervice.kbase.database.dao.AgentDao;
import com.zervice.kbase.database.dao.ProjectDao;
import com.zervice.kbase.database.dao.PublishedProjectDao;
import com.zervice.kbase.database.pojo.AccessToken;
import com.zervice.kbase.database.pojo.Agent;
import com.zervice.kbase.database.pojo.Project;
import com.zervice.kbase.database.pojo.PublishedProject;
import com.zervice.kbase.database.utils.DaoUtils;
import com.zervice.kbase.jwt.JwtTokenUtil;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Cleanup;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.sql.Connection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * rpc auth filter
 * 暴露给broker的相关接口，使用rpc作为请求uri前缀
 *
 * @author Peng Chen
 * @date 2020/3/7
 */
@Log4j2
@WebFilter(urlPatterns = "/rpc/*", filterName = "rpcAuthenticator", asyncSupported = true)
public class RpcFilter implements Filter {

    public static final String TOKEN = "Authorization";

    public static final String X_EXTERNAL_ACCOUNT_NAME_HEADER = FilterConfigName.X_EXTERNAL_ACCOUNT_NAME_HEADER;
    public static final String X_EXTERNAL_ACCOUNT_ID_HEADER = FilterConfigName.X_EXTERNAL_ACCOUNT_ID_HEADER;
    public static final String X_EXTERNAL_PUBLISHED_PROJECT_ID_HEADER = FilterConfigName.X_EXTERNAL_PUBLISHED_PROJECT_HEADER;
    public static final String X_EXTERNAL_PROJECT_ID_HEADER = FilterConfigName.X_EXTERNAL_PROJECT_ID_HEADER;
    public static final String X_EXTERNAL_AGENT_ID_HEADER = Constants.AGENT_ID_HEADER;
    public static final String X_EXTERNAL_AGENT_AK_HEADER = Constants.AGENT_AK_HEADER;

    @Autowired
    private AccountService accountService;

    private String authCode = LayeredConf.getString(ShareConfig.KEY_RPC_AUTH_CODE, ShareConfig.DEFAULT_AUTH_CODE);

    private Set<String> noAccountRequiredPath = new HashSet<>();
    private Set<String> ignoreAuthPath = new HashSet<>();
    private Set<String> publishedProjectRequiredPath = new HashSet<>();
    private Set<String> agentPath = new HashSet<>();
    private Set<String> agentWithPublishedProjectIdPath = new HashSet<>();

    private Set<String> accessTokenRequired = new HashSet<>();

    /** no account but the auth code required */
    private Set<String> authCodeRequired = new HashSet<>();

    private Set<String> llmPath = new HashSet<>();

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        noAccountRequiredPath.add("/rpc/llm/chat/variables");
        noAccountRequiredPath.add("/rpc/gaode/weather");
        noAccountRequiredPath.add("/rpc/echo");
        noAccountRequiredPath.add("/rpc/trial/apply");
        noAccountRequiredPath.add("rpc/fallback/token/validate");
        noAccountRequiredPath.add("/rpc/stripe/event");
        noAccountRequiredPath.add("/rpc/install/install_agent.sh");
        noAccountRequiredPath.add("/rpc/license/auth");
        // aio gpt apis
        noAccountRequiredPath.add("/rpc/aio");
        noAccountRequiredPath.add("/rpc/report");
        noAccountRequiredPath.add("/rpc/survey");

        // broker 请求agent信息，需要验证header
        authCodeRequired.add("/rpc/kbqa");
        authCodeRequired.add("/rpc/chat");
        authCodeRequired.add("/rpc/message");
        authCodeRequired.add("/rpc/project/");
        authCodeRequired.add("/rpc/agent/status");
        authCodeRequired.add("/rpc/project/component/");
        authCodeRequired.add("/rpc/agent/published/project/");

        // agent auth  & check published project id
        agentWithPublishedProjectIdPath.add("/rpc/published/project/started");
        agentWithPublishedProjectIdPath.add("/rpc/model");
        agentWithPublishedProjectIdPath.add("/rpc/agent/task");

        // agent 请求的path,需要
        // 1、校验 agent & ak；2、set dbName
        agentPath.add("/rpc/agent");
        agentPath.add("/rpc/published/project");

        llmPath.add("/rpc/rasa");

        // 通过access token进行认证
        // access:externalId+token
        accessTokenRequired.add("/rpc/v2");
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

            MutableHttpServletRequest mutableRequest = new MutableHttpServletRequest(hreq);

            String servletPath = hreq.getServletPath();
            Stopwatch stopwatch = Stopwatch.createStarted();

            try {
                for (String noRequireAccountName : noAccountRequiredPath) {
                    if (hreq.getServletPath().startsWith(noRequireAccountName)) {
                        filterChain.doFilter(hreq, response);
                        return;
                    }
                }

                for (String requiredAuthCodePath : authCodeRequired) {
                    if (hreq.getServletPath().startsWith(requiredAuthCodePath)) {
                        String reqAuthCode = mutableRequest.getHeader(TOKEN);
                        if (!authCode.equals(reqAuthCode)) {
                            ((HttpServletResponse) response).sendError(HttpServletResponse.SC_UNAUTHORIZED,
                                    "auth fail");
                            return;
                        }

                        filterChain.doFilter(mutableRequest, response);
                        return;
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
                        }
                        else {
                            String accountExternalId = AccountExternalIdUtil.getAccountInRequest(mutableRequest, accountService);
                            if (StringUtils.isBlank(accountExternalId)) {
                                LOG.error("Account can't be found");
                                ((HttpServletResponse) response).sendError(HttpServletResponse.SC_UNAUTHORIZED,
                                        "account cant'be found");
                                return;
                            }
                            mutableRequest.putHeader(FilterConfigName.X_EXTERNAL_ACCOUNT_ID_HEADER, accountExternalId);
                        }

                        filterChain.doFilter(mutableRequest, response);
                        return;
                    }
                }


                String accountExternalId;
                for (String path : agentWithPublishedProjectIdPath) {
                    if (servletPath.startsWith(path)) {
                        boolean pass = authAgent(mutableRequest);
                        if (!pass) {
                            ((HttpServletResponse) response).sendError(HttpServletResponse.SC_UNAUTHORIZED,
                                    "forbidden access:" + servletPath);
                            return;
                        }

                        accountExternalId = getAccountFromAgentId(mutableRequest);
                        if (StringUtils.isEmpty(accountExternalId)) {
                            ((HttpServletResponse) response).sendError(HttpServletResponse.SC_UNAUTHORIZED,
                                    "account id can't found via agent id");
                            return;
                        }
                        mutableRequest.putHeader(FilterConfigName.X_EXTERNAL_ACCOUNT_ID_HEADER, accountExternalId);
                        filterChain.doFilter(mutableRequest, response);
                        return;
                    }
                }

                for (String path : agentPath) {
                    if (servletPath.startsWith(path)) {
                        boolean pass = authAgent(mutableRequest);
                        if (!pass) {
                            ((HttpServletResponse) response).sendError(HttpServletResponse.SC_UNAUTHORIZED,
                                    "forbidden access:" + servletPath);
                            return;
                        }

                        filterChain.doFilter(mutableRequest, response);
                        return;
                    }
                }

                for (String path : llmPath) {
                    if (servletPath.startsWith(path)) {
                        boolean pass = authLlm(mutableRequest);
                        if (!pass) {
                            ((HttpServletResponse) response).sendError(HttpServletResponse.SC_UNAUTHORIZED,
                                    "forbidden access:" + servletPath);
                            return;
                        }

                        filterChain.doFilter(mutableRequest, response);
                        return;
                    }
                }

                for (String requiredAccessTokenPath : accessTokenRequired) {
                    if (hreq.getServletPath().startsWith(requiredAccessTokenPath)) {
                        if (!authAccessToken(mutableRequest)) {
                            ((HttpServletResponse) response).sendError(HttpServletResponse.SC_UNAUTHORIZED,
                                    "auth fail");
                            return;
                        }

                        filterChain.doFilter(mutableRequest, response);
                        return;
                    }
                }

                accountExternalId = AccountExternalIdUtil.getAccountInRequest(mutableRequest, accountService);
                mutableRequest.putHeader(FilterConfigName.X_EXTERNAL_ACCOUNT_ID_HEADER, accountExternalId);
                filterChain.doFilter(mutableRequest, response);
                return;
            } finally {
                int status = ((HttpServletResponse) response).getStatus();
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
        }

        ((HttpServletResponse) response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    private boolean authLlm(MutableHttpServletRequest mutableHttpServletRequest) {
        String dbName = mutableHttpServletRequest.getHeader(X_EXTERNAL_ACCOUNT_ID_HEADER);
        String projectId = mutableHttpServletRequest.getHeader(X_EXTERNAL_PROJECT_ID_HEADER);
        String publishedProjectId = mutableHttpServletRequest.getHeader(X_EXTERNAL_PUBLISHED_PROJECT_ID_HEADER);

        String uri = mutableHttpServletRequest.getRequestURI();
        if (StringUtils.isBlank(dbName) || StringUtils.isBlank(projectId) || StringUtils.isBlank(publishedProjectId)) {
            LOG.warn("not found db:{} projectId:{} and publishedProjectId:{} from header with uri:{}", dbName, projectId, publishedProjectId, uri);
            return false;
        }

        try {
            @Cleanup Connection conn = DaoUtils.getConnection(true);
            Account account = AccountDao.getByDbName(conn, dbName);
            if (account == null) {
                LOG.error("[{}][account not found]", dbName);
                return false;
            }

            Project project = ProjectDao.get(conn, dbName, projectId);
            if (project == null) {
                LOG.error("[{}][project:{} not found]", dbName, projectId);
                return false;
            }

            return true;
        } catch (Exception e) {
            LOG.error("auth llm with error :{}  with uri:{}", e.getMessage(), uri, e);
            return false;
        }
    }

    /**
     * 通过账户下的access token进行数据访问。
     * - 在header中提取token
     * - 安装长度拆分出 tokenid、token （tokenid长度为14）
     * - 进行校验，然后获取db放入header..
     */
    private boolean authAccessToken(MutableHttpServletRequest mutableRequest) {
        String uri = mutableRequest.getRequestURI();
        String token = mutableRequest.getHeader(TOKEN);
        if (StringUtils.isBlank(token)) {
            LOG.warn("[no access token found in:{}]", uri);
            return false;
        }

        // 先去掉前缀
        String accessIdAndToken = token.replace(JwtTokenUtil.PREFIX, "");

        //  token= AccessExternalId+token
        // id 14位，后续的是token
        String externalTokenId = accessIdAndToken.substring(0, 14);
        String accessToken = accessIdAndToken.substring(14);

        Long accessTokenId;
        try {
            accessTokenId = AccessToken.fromExternalID(externalTokenId);
        } catch (Exception e) {
            LOG.error("[invalid access token id:{} from header token:{}]", externalTokenId, token);
            return false;
        }

        List<AccountCatalog> accountCatalogs = AccountCatalog.getAllAccounts();
        for (AccountCatalog account : accountCatalogs) {
            if (account.getAccessTokens().validate(accessTokenId, accessToken)) {
                mutableRequest.putHeader(FilterConfigName.X_EXTERNAL_ACCOUNT_ID_HEADER, account.getDBName());
                LOG.info("[{}][visit uri:{} from access token:{}]", account.getDBName(), uri, accessTokenId);
                return true;
            }
        }

        LOG.warn("[invalid access token visit:{}]", uri);
        return false;
    }
    private boolean authAgent(MutableHttpServletRequest mutableRequest) {
        String agentId = mutableRequest.getHeader(X_EXTERNAL_AGENT_ID_HEADER);
        String agentAk = mutableRequest.getHeader(X_EXTERNAL_AGENT_AK_HEADER);
        String uri = mutableRequest.getRequestURI();
        if (StringUtils.isBlank(agentId) || StringUtils.isBlank(agentAk)) {
            LOG.warn("not found agent id 、ak from header with uri:{}", uri);
            return false;
        }

        try {
            @Cleanup Connection conn = DaoUtils.getConnection(true);
            Agent agent = AgentDao.getById(conn, agentId);
            if (agent == null) {
                LOG.warn("agent:{} not found  from header with uri:{}", agentId, uri);
                return false;
            }

            if (!agentAk.endsWith(agent.getAk())) {
                LOG.warn("agent:{} ak:{} not mutch from header with uri:{}", agentId, agentAk, uri);
                return false;
            }

            return true;
        } catch (Exception e) {
            LOG.error("auth agent with error :{}  with uri:{}", e.getMessage(), uri, e);
            return false;
        }
    }
    private String getAccountFromAgentId(MutableHttpServletRequest mutableRequest) {
        String publishedProjectId = mutableRequest.getHeader(X_EXTERNAL_PUBLISHED_PROJECT_ID_HEADER);
        String uri = mutableRequest.getRequestURI();

        if (StringUtils.isBlank(publishedProjectId)) {
            LOG.warn("not found publishedProjectId from header with uri:{}", uri);
            return null;
        }

        try {
            @Cleanup Connection conn = DaoUtils.getConnection(true);
            String dbName = PublishedProject.getDbNameFromId(publishedProjectId);
            AccountCatalog account = AccountCatalog.ensure(dbName);

            if (account.getPublishedProjects().exits(publishedProjectId)) {
                return dbName;
            }

            PublishedProject project = PublishedProjectDao.get(conn, dbName, publishedProjectId);
            if (project != null) {
                return dbName;
            }

            // find from db
            LOG.warn("published project:{} not found in db:{}", publishedProjectId, dbName);
            return null;
        } catch (Exception e) {
            LOG.error("get account from agent fail:{}  with uri:{}", e.getMessage(), uri, e);
            return null;
        }
    }
}
