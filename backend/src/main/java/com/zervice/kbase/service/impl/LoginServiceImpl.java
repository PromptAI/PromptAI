package com.zervice.kbase.service.impl;

import cn.hutool.core.io.IoUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.RSA;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import com.zervice.common.pojo.common.Account;
import com.zervice.common.pojo.common.EmptyResponse;
import com.zervice.common.restful.exception.RestException;
import com.zervice.common.restful.exception.StatusCodes;
import com.zervice.common.utils.LayeredConf;
import com.zervice.common.utils.LoginUtils;
import com.zervice.common.utils.ServletUtils;
import com.zervice.kbase.AccountCatalog;
import com.zervice.kbase.ZBotConfig;
import com.zervice.kbase.ZBotRuntime;
import com.zervice.kbase.api.restful.AuthFilter;
import com.zervice.kbase.api.restful.controller.NotifyController;
import com.zervice.kbase.api.restful.pojo.LoginRequest;
import com.zervice.kbase.api.restful.pojo.RestUser;
import com.zervice.kbase.api.restful.rbac.UserRolesBuilder;
import com.zervice.kbase.database.SecurityUtils;
import com.zervice.kbase.database.dao.AccountDao;
import com.zervice.kbase.database.dao.PublishedProjectDao;
import com.zervice.kbase.database.dao.UserDao;
import com.zervice.kbase.database.pojo.AccessToken;
import com.zervice.kbase.database.pojo.PublishedProject;
import com.zervice.kbase.database.pojo.User;
import com.zervice.kbase.database.utils.DaoUtils;
import com.zervice.kbase.jwt.JwtPayLoad;
import com.zervice.kbase.jwt.JwtTokenUtil;
import com.zervice.kbase.service.LoginService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.Cleanup;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;

import static com.zervice.common.restful.exception.StatusCodes.*;

;
;

/**
 * login/logout
 */
@Log4j2
@Service
public class LoginServiceImpl implements LoginService {
    private final String privateKey = LayeredConf.getString(ZBotConfig.KEY_RSA_KEY, ZBotConfig.DEFAULT_RSA_KEY);

    /**
     * login
     *
     * @param request http session
     * @return user.name
     */
    @Override
    public Object login(HttpServletRequest request) throws Exception {
        Connection conn = null;
        try {
            conn = DaoUtils.getConnection(true);

            Object res = _tryLoginWithApiToken(request, conn);
            if (res != null) {
                return res;
            }

            // try login with info...
            String body = _getRequestBodyFromRequest(request);
            LoginRequest loginRequest = JSONObject.parseObject(body, LoginRequest.class);

            Set<String> trialAccounts = Set.of("trial@talk2bits.com", "trial@promptai.cn", "trial@promptai.us");
            // 先禁止这个账户登录 - 用于FastProject的api
            if (trialAccounts.contains(loginRequest.getUsername())) {
                LOG.info("[forbidden trial account:{} to login]", loginRequest.getUsername());
                throw new RestException(StatusCodes.USER_PASS_OR_NAME_FAILED);
            }

            if (LoginRequest.TYPE_SMS.equals(loginRequest.getType())) {
                return loginWithSms(loginRequest, request, conn);
            } else if (LoginRequest.TYPE_PASSCODE.equals(loginRequest.getType())) {
                return loginWithPass(loginRequest, request, conn);
            }
            if (LoginRequest.TYPE_EMAIL.equals(loginRequest.getType())) {
                return loginWithEmail(loginRequest, request, conn);
            }

            LOG.warn("[unsupported login type:{}]", loginRequest.getType());
            throw new RestException(UNSUPPORTED_LOGIN_TYPE, loginRequest.getType());
        } finally {
            DaoUtils.closeQuietly(conn);
        }
    }


    private Object loginWithSms(LoginRequest loginRequest, HttpServletRequest request, Connection conn) throws Exception {
        String mobile = loginRequest.getUsername();
        HttpSession session = request.getSession();

        AccountCatalog account = AuthFilter.resolveAccountCatalog(request, "", mobile);
        if (account == null) {
            LOG.warn("[user:{} login by sms fail, account not found]", mobile);
            throw new RestException(StatusCodes.ACCOUNT_NOT_EXISTS);
        }
        if (accountSuspend(account.getDBName())) {
            throw new RestException(StatusCodes.ACCOUNT_SUSPEND);
        }

        String dbName = account.getDBName();
        User signInUser = account.findUserByMobile(mobile);
        if (signInUser == null) {
            LOG.warn("[{}][user:{} login by sms fail, user not found]", dbName, mobile);

            throw new RestException(StatusCodes.USER_NOT_EXISTS);
        }

        // let's check the code is correct
        NotifyController.verifySecInfo(session, mobile, loginRequest.getCode());

        signInUser.setLastAccessEpochMs(System.currentTimeMillis());
        UserDao.updateLastAccess(conn, dbName, signInUser);

        _userSignedIn(session, conn, account, signInUser.getUsername(),
                signInUser.getEmail(), signInUser.getMobile());

        //create jwt token
        return _generateLoginRes(signInUser, dbName, session);
    }

    private Object loginWithEmail(LoginRequest loginRequest, HttpServletRequest request, Connection conn) throws Exception {


        String email = loginRequest.getUsername();
        HttpSession session = request.getSession();

        AccountCatalog account = AuthFilter.resolveAccountCatalog(request, email, "");
        if (account == null) {
            LOG.warn("[user:{} login by email fail, account not found]", email);
            throw new RestException(StatusCodes.ACCOUNT_NOT_EXISTS);
        }
        if (accountSuspend(account.getDBName())) {
            throw new RestException(StatusCodes.ACCOUNT_SUSPEND);
        }

        String dbName = account.getDBName();
        User signInUser = account.findUserByEmail(email);
        if (signInUser == null) {
            LOG.warn("[{}][user:{} login by email fail, user not found]", dbName, email);

            throw new RestException(StatusCodes.USER_NOT_EXISTS);
        }

        // let's check the code is correct
        NotifyController.verifySecInfo(session, email, loginRequest.getCode());

        signInUser.setLastAccessEpochMs(System.currentTimeMillis());
        UserDao.updateLastAccess(conn, dbName, signInUser);

        _userSignedIn(session, conn, account, signInUser.getUsername(),
                signInUser.getEmail(), signInUser.getMobile());

        //create jwt token
        return _generateLoginRes(signInUser, dbName, session);
    }

    private Object loginWithPass(LoginRequest loginRequest, HttpServletRequest request, Connection conn) throws Exception {
        String email = loginRequest.getUsername();
        String mobile = loginRequest.getUsername();

        HttpSession session = request.getSession();

        AccountCatalog account = AuthFilter.resolveAccountCatalog(request, email, mobile);
        if (account == null) {
            LOG.warn("can't find account by email:{} and mobile:{}", email, mobile);
            throw new RestException(StatusCodes.USER_PASS_OR_NAME_FAILED);
        }
        if (accountSuspend(account.getDBName())) {
            throw new RestException(StatusCodes.ACCOUNT_NOT_EXISTS);
        }
        String dbName = account.getDBName();
        if (LoginUtils.isChineseMobile(email)) {
            mobile = email;
            email = "";
        }

        User signInUser;
        if (!StringUtils.isEmpty(email)) {
            signInUser = account.findUserByEmail(email);
        } else {
            signInUser = account.findUserByMobile(mobile);
        }

        if (signInUser == null) {
            LOG.error("[{}][User not exists. email:{}", dbName, email);
            throw new RestException(StatusCodes.USER_PASS_OR_NAME_FAILED);
        }

        // refresh from db
        signInUser = UserDao.get(conn, dbName, signInUser.getId());

        // return 401 when password does not match.
        String password;
        try {
            RSA rsa = new RSA(privateKey, null);
            password = new String(rsa.decrypt(loginRequest.getPassword(), KeyType.PrivateKey));
        } catch (Exception e) {
            LOG.error("[{}][user signing in decrypt password error - {}@{}]", dbName, e.getMessage(), dbName);
            throw new RestException(StatusCodes.USER_PASS_OR_NAME_FAILED);
        }

        if (!SecurityUtils.verifyPassword(password, signInUser.getProperties().getPassword())) {
            throw new RestException(StatusCodes.USER_PASS_OR_NAME_FAILED);
        }

        signInUser.setLastAccessEpochMs(System.currentTimeMillis());
        UserDao.updateLastAccess(conn, dbName, signInUser);

        _userSignedIn(session, conn, account, signInUser.getUsername(),
                signInUser.getEmail(), signInUser.getMobile());

        //create jwt token
        return _generateLoginRes(signInUser, dbName, session);
    }

    private String _getRequestBodyFromRequest(HttpServletRequest request) throws Exception {
        return IoUtil.readUtf8(request.getInputStream());
    }

    @Override
    public Object loginWithUser(User signInUser, AccountCatalog account, Connection conn) throws Exception {
        HttpServletRequest request = ServletUtils.getWebRequest();
        String dbName = account.getDBName();
        HttpSession session = request.getSession();
        _userSignedIn(session, conn, account, signInUser.getUsername(),
                signInUser.getEmail(), signInUser.getMobile());

        //create jwt token
        return _generateLoginRes(signInUser, dbName, session);
    }

    /**
     * _tryLoginWithApiToken
     */
    private Object _tryLoginWithApiToken(HttpServletRequest request, Connection conn) throws Exception {
        String dbName = request.getHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER);
        String accessTokenId = request.getHeader(AuthFilter.X_EXTERNAL_API_TOKEN_ID_HEADER);
        String token = request.getHeader(AuthFilter.X_EXTERNAL_API_TOKEN_HEADER);
        if (StringUtils.isBlank(accessTokenId) || StringUtils.isBlank(token) || StringUtils.isBlank(dbName)) {
            return null;
        }

        AccountCatalog catalog = AccountCatalog.ensure(dbName);

        long tokenId = AccessToken.fromExternalID(accessTokenId);

        // this token already checked by AuthFilter
        // here just find user and generate token...
        AccessToken accessToken = catalog.getAccessTokens().getOriginToken(tokenId);
        if (accessToken == null) {
            LOG.error("[:{}][login with access token fail, ac not found with id:{}]", dbName, tokenId);
            throw new RestException(StatusCodes.Unauthorized);
        }

        User user = catalog.getUser(accessToken.getUserId());
        if (user == null) {
            LOG.error("[:{}][login with access token fail, user not found with id:{}]", dbName, tokenId);
            throw new RestException(StatusCodes.Unauthorized);
        }

        LOG.info("[{}][user:{} success login by access token]", dbName, user.getId());
        return _generateLoginRes(user, dbName, request.getSession());
    }

    private Map<String, Object> _generateLoginRes(User signInUser, String dbName, HttpSession session) throws Exception {

        String jwtToken = JwtTokenUtil.generate(JwtPayLoad.of(dbName, User.getExternalId(signInUser.getId()), signInUser.getUsername()));
        session.setAttribute(AuthFilter.TOKEN, jwtToken);
        Map<String, Object> result = Maps.newHashMapWithExpectedSize(3);
        RestUser user = UserRolesBuilder.buildUserWithRoles(dbName, signInUser.getId());
        result.put("user", user);
        result.put("token", jwtToken);
        result.put("tokenExpireAt", (System.currentTimeMillis() + JwtTokenUtil.EXPIRE_MILES));

        // default account markup
        if (ZBotRuntime.DEFAULT_EXTERNAL_ACCOUNT_ID.equals(dbName)) {
            result.put("defaultAccount", true);
        }

        return result;
    }

    /**
     * logout
     *
     * @param httpSession session
     * @return LoginResponse.empty
     */
    @Override
    public EmptyResponse logout(HttpSession httpSession) {
        //remove token
        httpSession.removeAttribute(AuthFilter.TOKEN);
        httpSession.removeAttribute(AuthFilter.LOGIN);
        httpSession.removeAttribute(AuthFilter.ACCOUNTID);
        httpSession.removeAttribute(AuthFilter.SUPPORT_LOGIN);
        httpSession.removeAttribute(AuthFilter.IDENTITY);
        httpSession.removeAttribute(AuthFilter.ID_EMAIL);
        httpSession.removeAttribute(AuthFilter.ID_MOBILE);

        return EmptyResponse.empty();
    }

    /**
     * get user info
     *
     * @param id     user id
     * @param dbName database name
     * @return user info
     */
    @Override
    public RestUser info(long id, String dbName) throws Exception {
        Connection conn = null;

        try {
            conn = DaoUtils.getConnection(true);

            User user = UserDao.get(conn, dbName, id);
            if (user == null) {
                throw new RestException(StatusCodes.NotFound, "user not exist");
            }

            return UserRolesBuilder.buildUserWithRoles(dbName, user.getId());
        } finally {
            DaoUtils.closeQuietly(conn);
        }
    }

    @Override
    public Object refreshToken(HttpServletRequest request, long userId, String dbName) throws Exception {
        @Cleanup Connection conn = DaoUtils.getConnection(true);
        User signUser = UserDao.get(conn, dbName, userId);
        if (signUser != null) {
            LOG.info("[{}][user:{} refresh token]", dbName, userId);
            return _generateLoginRes(signUser, dbName, request.getSession());
        }

        throw new RestException(Unauthorized);
    }

    private void _userSignedIn(HttpSession session, Connection conn, AccountCatalog account,
                               String user, String email, String mobile) throws Exception{
        long accountId = account.getId();

        // record an event in Account
        try {
            Account a = AccountDao.get(conn, accountId);

            // 限制不活跃账户登录开关
            boolean inactiveAccountLogin = LayeredConf.getBoolean("login.inactive.account", true);
            if (a != null) {
                if (!inactiveAccountLogin && !Boolean.TRUE.equals(a.getProperties().get(Account.PROP_ACTIVE))) {
                    LOG.info("[{}][not allow inactive account login]", account.getDBName());
                    throw new RestException(NotAllowInactiveAccountLogin);
                }

                long now = System.currentTimeMillis();
                a.addEvent(now, user + " (" + email + ") signed in!");
                a.updateActive(true);
                a.getProperties().put(Account.PROPERTY_LAST_LOGIN, now);
                a.getProperties().put(Account.PROP_ACTIVE, true);
                AccountDao.update(conn, a);

                // debug被回收后，再次登录进行训练，debug有概率出现再次被回收
                // 这里刷新下debug的 更新时间
                _refreshDebugAgentUpdateTime(conn, a.getDbName());
            }
        } catch (SQLException sqlException) {
            // ignore
        }


        DaoUtils.recordAuditLog(conn, account.getDBName(), user, "read", UserDao.TABLE_NAME, "", "User signed in");
        setLoginSession(session, accountId, email, mobile);
    }

    private void _refreshDebugAgentUpdateTime(Connection conn, String dbName) throws Exception {
        String debugId = PublishedProject.generateDebugPublishId(dbName);
        PublishedProject debug = PublishedProjectDao.get(conn, dbName, debugId);
        if (debug != null) {
            debug.getProperties().setUpdateTime(System.currentTimeMillis());
            PublishedProjectDao.updateProp(conn, dbName, debug);
            AccountCatalog.ensure(dbName).getPublishedProjects().onUpdate(debug);
        }
    }

    /**
     * User could either login with email or mobile. We just set according to the identity user used
     */
    public static void setLoginSession(HttpSession session, long accountID, String email, String mobile) {
        session.setAttribute(AuthFilter.LOGIN, true);
        session.setAttribute(AuthFilter.ACCOUNTID, accountID);

        if (!StringUtils.isEmpty(email)) {
            session.setAttribute(AuthFilter.IDENTITY, AuthFilter.ID_EMAIL + email);
        } else {
            session.setAttribute(AuthFilter.IDENTITY, AuthFilter.ID_MOBILE + mobile);
        }
    }

    private boolean accountSuspend(String dbName) {
        return AuthFilter.accountSuspend(dbName);
    }
}
