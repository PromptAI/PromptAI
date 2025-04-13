package com.zervice.kbase.api.restful.controller;

import com.zervice.common.pojo.common.Account;
import com.zervice.common.pojo.common.EmptyResponse;
import com.zervice.common.restful.exception.RestException;
import com.zervice.common.restful.exception.StatusCodes;
import com.zervice.common.utils.AccountService;
import com.zervice.kbase.AccountCatalog;
import com.zervice.kbase.api.restful.AuthFilter;
import com.zervice.kbase.database.dao.AccountDao;
import com.zervice.kbase.database.dao.UserDao;
import com.zervice.kbase.database.pojo.User;
import com.zervice.kbase.database.utils.DaoUtils;
import com.zervice.kbase.service.LoginService;
import lombok.Cleanup;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpSession;
import java.sql.Connection;

/**
 * @author chen
 * @date 2023/4/4 15:59
 */
@Log4j2
@RestController
@RequestMapping("/api/user/accounts")
public class UserAccountController {

    @Autowired
    private AccountService accountService;

    @Autowired
    private LoginService loginService;

    /**
     * 这里先做标记删除，防止用户回来查看账单，世纪删除时间目前配置的半年后
     * 调用这个api后，可以继续重新注册...
     */
    @DeleteMapping
    public Object delete(@RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName,
                         HttpSession session,
                         @RequestHeader(AuthFilter.X_EXTERNAL_USER_ID_HEADER) String uid) throws Exception {
        AccountCatalog accountCatalog = AccountCatalog.guess(dbName);
        if (accountCatalog == null) {
            throw new RestException(StatusCodes.ACCOUNT_NOT_AVAILABLE);
        }

        @Cleanup Connection conn = DaoUtils.getConnection(true);
        long userId = User.fromExternalId(uid);
        User user = UserDao.get(conn, dbName, userId);
        if (user == null) {
            throw new RestException(StatusCodes.USER_NOT_EXISTS);
        }

        String owner = accountCatalog.getOwner();
        if (!owner.equals(user.getEmail()) && !owner.equals(user.getMobile())) {
            LOG.error("[{}][user:{} delete account fail, not the owner!!]", dbName, userId);
            throw new RestException(StatusCodes.NotAccountOwner);
        }

        Account account = AccountDao.get(conn, Account.fromExternalId(dbName));

        Long restToken = AccountDao.getRestToken(conn, Account.fromExternalId(dbName));

        try {
            // 1、mark account 2 delete
            account.delete();
            AccountDao.update(conn, account);
            LOG.info("[{}][account marked deleted]", dbName);

            // 2、delete account catalog
            AccountCatalog.onDeleteAccount(accountCatalog);

            // 4、logout
            loginService.logout(session);
            LOG.info("[{}][the account's owner:{} deleted account with rest token:{}]", dbName, owner, restToken);

            // refresh catalog
            AccountCatalog.initialize();
            return EmptyResponse.empty();
        } catch (RestException e) {
            LOG.error("[{}][the account's owner:{} delete account with error:{}]", dbName, owner, e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            LOG.error("[{}][the account's owner:{} delete account with error:{}]", dbName, owner, e.getMessage(), e);
            throw new RestException(StatusCodes.InternalError);
        }
    }
}
