package com.zervice.kbase.cache;

import com.google.common.base.Strings;
import com.zervice.kbase.AccountCatalog;
import com.zervice.kbase.database.dao.AccessTokenDao;
import com.zervice.kbase.database.pojo.AccessToken;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
/**
 * Manage per-customer access tokens, saved in AccountCatalog ...
 */
@Log4j2
public class AccountAccessTokens extends AccountCache {

    public final static String NAME = "AccessTokens";
    public static final int AUTH_MODE = 2;
    public AccountAccessTokens(AccountCatalog catalog) {
        super(catalog, NAME);
    }

    private final ConcurrentHashMap<Long, AccessToken> _tokens = new ConcurrentHashMap<>();


    public void add(AccessToken accessToken) {
        _tokens.put(accessToken.getId(), accessToken);
    }

    public void remove(long accessId) {
        _tokens.remove(accessId);
    }

    public int size() {
        return _tokens.size();
    }

    public String getToken(long accessId) {
        AccessToken token = _tokens.getOrDefault(accessId, null);
        return (token == null || StringUtils.isBlank(token.getToken())) ? "" : token.getToken();
    }

    public AccessToken getOriginToken(long accessId) {
        return _tokens.getOrDefault(accessId, null);
    }

    public boolean validate(long accessId, String inputToken) {
        if(StringUtils.isEmpty(inputToken)) {
            LOG.debug("[Input access token is empty with accessId:{}]", accessId);
            return false;
        }

        AccessToken token = _tokens.get(accessId);
        if(token == null || Strings.isNullOrEmpty(token.getToken())) {
            LOG.debug("[Token not found from id:{}]", accessId);
            return false;
        }

        if(StringUtils.compare(token.getToken(), inputToken) != 0) {
            LOG.debug("[access token invalid with id:{}, token:{}]", accessId, inputToken);
            return false;
        }

        return token.active();
    }


    @Override
    protected void _initialize(Connection conn) {
        try {
            List<AccessToken> accessTokens = AccessTokenDao.getAllActive(conn, _account.getDBName());
            for (AccessToken accessToken: accessTokens) {
                _tokens.put(accessToken.getId(), accessToken);
            }
        } catch (SQLException e) {
            LOG.error("cannot init access token cache e:{}", e.getMessage(), e);
        }
    }

    @Override
    protected void _reload(Connection conn) {
        //nothing to do
    }
}
