package com.zervice.kbase.api.restful.controller;

import com.zervice.kbase.AccountCatalog;
import com.zervice.kbase.api.restful.AuthFilter;
import com.zervice.kbase.database.dao.AccessTokenDao;
import com.zervice.kbase.database.pojo.AccessToken;
import com.zervice.kbase.database.pojo.User;
import com.zervice.kbase.database.utils.DaoUtils;
import lombok.Cleanup;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author chenchen
 * @Date 2023/9/7
 */
@Log4j2
@RestController
@RequestMapping("/api/access/token")
public class AccessTokenController {

    @PostMapping
    public Object create(@RequestHeader(AuthFilter.X_EXTERNAL_USER_ID_HEADER) String uid,
                         @RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName) throws Exception {
        long userId = User.fromExternalId(uid);
        AccountCatalog accountCatalog = AccountCatalog.ensure(dbName);

        @Cleanup Connection conn = DaoUtils.getConnection(true);
        AccessToken accessToken = AccessToken.factory(userId);
        AccessTokenDao.add(conn, dbName, accessToken);

        accountCatalog.getAccessTokens().add(accessToken);

        LOG.info("[{}][user:{} create new access token:{}]", dbName, userId, accessToken.getId());

        Map<String, String> result = new HashMap<>();
        result.put("externalId", AccessToken.getExternalID(accessToken.getId()));
        result.put("token", accessToken.getToken());
        result.put("rpc", accessToken.toRpcToken());
        return result;
    }
}
