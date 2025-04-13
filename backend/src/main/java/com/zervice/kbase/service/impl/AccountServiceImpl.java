package com.zervice.kbase.service.impl;

import cn.hutool.core.lang.Validator;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.zervice.common.model.AccountModel;
import com.zervice.common.pojo.common.Account;
import com.zervice.common.restful.exception.RestException;
import com.zervice.common.restful.exception.StatusCodes;
import com.zervice.common.utils.AccountService;
import com.zervice.common.utils.Constants;
import com.zervice.common.utils.IdGenerator;
import com.zervice.kbase.AccountCatalog;
import com.zervice.kbase.ZBotRuntime;
import com.zervice.kbase.api.restful.controller.UserController;
import com.zervice.kbase.database.dao.AccountDao;
import com.zervice.kbase.database.dao.UserDao;
import com.zervice.kbase.database.helpers.AccountCore;
import com.zervice.kbase.database.pojo.User;
import com.zervice.kbase.database.utils.DaoUtils;
import com.zervice.kbase.llm.LlmServiceHelper;
import com.zervice.kbase.queues.atomic.AccountActivationOp;
import com.zervice.kbase.service.AgentService;
import com.zervice.kbase.service.PublishedProjectService;
import jakarta.ws.rs.WebApplicationException;
import lombok.Cleanup;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Log4j2
@Service
public class AccountServiceImpl implements AccountService {

    @Autowired
    private AgentService agentService;
    @Autowired
    private PublishedProjectService publishedProjectService;

    @Override
    public Account updateStatus(JSONObject data) throws Exception {
        String status = data.getString("status");
        String extId = data.getString("id");
        long userId = data.getLong("userId");
        if (StringUtils.isEmpty(status) || StringUtils.isEmpty(extId)) {
            throw new RestException(StatusCodes.ACCOUNT_PARAMS_ERROR, "status and id required");
        }
        long id = Account.fromExternalId(extId);
        @Cleanup Connection conn = DaoUtils.getConnection();
        List<String> accountDisableStatus = Arrays.asList(Account.STATUS_SUSPENDED, Account.STATUS_TERMINATED);
        AccountDao.updateStatus(conn, id, status);
        if (accountDisableStatus.contains(status)) {
            String dbName = Account.getAccountDbName(id);
            _releaseAccountResource(userId, dbName);
        }
        AccountCatalog.initialize();
        return AccountDao.get(conn, id);
    }

    @Override
    public Boolean enoughToken(Connection conn, String dbName) throws Exception {
        return LlmServiceHelper.choose(dbName, conn).enoughToken(conn);
    }

    @Override
    public void checkEnoughToken(Connection conn, String dbName) throws Exception {
        if (!enoughToken(conn, dbName)) {
            throw new RestException(StatusCodes.RestTokenNotEnough);
        }
    }

    private void _releaseAccountResource(long userId, String dbName) throws Exception {
        LOG.info("[{}][begin to release account resource.]", dbName);
        //1.cancel agent task status in STATUS_SCHEDULE and STATUS_EXECUTING
        agentService.cancelAllTask(userId, dbName);

        //2.stop published project status in STATUS_DEPLOYING and STATUS_RUNNING
        publishedProjectService.release(userId, dbName);
        LOG.info("[{}][success release account resource.]", dbName);
    }

    /**
     * 获取所有account
     *
     * @return accounts
     */
    @Override
    public List<Account> getAll() {
        Connection conn = null;
        try {
            conn = DaoUtils.getConnection(true);
            return AccountDao.getAll(conn);
        } catch (Exception e) {
            LOG.error("[{}][Fail to get all accounts. error:{}]", Constants.COREDB, e.getMessage(), e);
            throw new RestException(StatusCodes.InternalError, "Fail to get all accounts");
        } finally {
            DaoUtils.closeQuietly(conn);
        }
    }

    /**
     * 通过account.is获取account
     *
     * @param acctExternalId account.id
     * @return account
     */
    @Override
    public Account get(String acctExternalId) {
        Connection conn = null;
        try {
            conn = DaoUtils.getConnection(true);
            Account account = AccountDao.get(conn, Account.fromExternalId(acctExternalId));
            if (account == null) {
                LOG.warn("[{}][Can't find account from external id {}]", Constants.COREDB, acctExternalId);
                throw new RestException(StatusCodes.BadRequest, "can't find account " + acctExternalId);
            }
            return account;
        } catch (Exception e) {
            LOG.error("[{}][Unknown error when get account {}]", Constants.COREDB, acctExternalId, e);
            throw new RestException(StatusCodes.InternalError, "Internal error when getting account " + acctExternalId);
        } finally {
            DaoUtils.closeQuietly(conn);
        }
    }

    /**
     * 创建account by  mobile
     *
     * @param accountModel data
     * @return account
     */
    @Override
    public Account create(AccountModel accountModel) throws Exception {
        @Cleanup Connection conn = DaoUtils.getConnection(true);
        Account account;

        //check create params
        if (!checkCreateAccountParam(accountModel.getName(), accountModel.getDbName(), accountModel.getAdmin())) {
            throw new RestException(StatusCodes.BadRequest, "can't create account. name and dbName and mobile can't be null or empty !");
        }
        account = AccountDao.getByName(conn, accountModel.getName());
        if (account != null) {
            LOG.error("[{}][can't create account which name or dbName exists. name:{},dbName:{}]",
                    Constants.COREDB, accountModel.getName(), accountModel.getDbName());
            throw new RestException(StatusCodes.ACCOUNT_ALREADY_EXISTS, "can't create account. name or dbName exists!");
        }

        String userName = StringUtils.isEmpty(accountModel.getAdminName()) ? accountModel.getAdmin() : accountModel.getAdminName();
        String company = StringUtils.isEmpty(accountModel.getFullName()) ? "AweSome Company" : accountModel.getFullName();
        String createBy = StringUtils.isNotBlank(accountModel.getAdmin()) ? accountModel.getAdmin() : Account.SYS_ADMIN_EMAIL;
        JSONObject prop = new JSONObject();
        prop.put(Account.PROP_CREATE_BY, createBy);
        prop.put(Account.PROP_CREATED_AT, System.currentTimeMillis());
        prop.put(Account.PROP_FULL_NAME, company);
        prop.put(Account.PROP_CREATE_BY_USERNAME, userName);
        prop.put(Account.PROP_PASSCODE, accountModel.getPasscode());
        prop.put("from", User.UserProp.FROM_MANUAL_CREATE);
        if (StringUtils.isNotEmpty(accountModel.getTimezone())) {
            prop.put(Account.PROP_TIMEZONE, accountModel.getTimezone());
        } else {
            prop.put(Account.PROP_TIMEZONE, Account.TIMEZONE_GMT8);
        }
        if (StringUtils.isNotEmpty(accountModel.getType())) {
            prop.put(Account.PROP_TYPE, parseAccountType(accountModel.getType()));
        }

        JSONArray admins = accountModel.getAdmins();
        if (admins == null) {
            admins = new JSONArray();
        }

        account = _createAccount(accountModel.getName(), accountModel.getDbName(), accountModel.getAdmin(), userName, admins, prop);
        return account;

    }

    /**
     * 删除
     *
     * @param acctExternalId account.id
     */
    @Override
    public void delete(String acctExternalId) {
        Connection conn = null;
        try {
            long acctId = Account.fromExternalId(acctExternalId);
            AccountCatalog a = AccountCatalog.of(acctId);

            conn = DaoUtils.getConnection(false);

            // 快速回收资源 - 这是异步操作的，不用担心conn超时
            publishedProjectService.quickRelease(conn, acctExternalId);

            AccountDao.delete(conn, acctId);
            DaoUtils.dropDb(conn, acctExternalId);
            conn.commit();

            if (a != null) {
                AccountCatalog.onDeleteAccount(a);
                AccountCatalog.initialize();
            }
        } catch (WebApplicationException e) {
            DaoUtils.rollbackQuietly(conn);
            throw e;
        } catch (Exception e) {
            LOG.error("[{}][Fail to delete db for account {}]", Constants.COREDB, acctExternalId, e);
            DaoUtils.rollbackQuietly(conn);
            throw new RestException(StatusCodes.InternalError);
        } finally {
            DaoUtils.closeQuietly(conn);
        }
    }

    @Override
    public Account getByName(String accountName) {
        return this.getAll().stream().filter(a -> accountName.equalsIgnoreCase(a.getName())).findFirst().orElse(null);
    }

    @Override
    public Account update(String accountExtId, AccountModel account) {
        Connection conn = null;
        Account dbAccount;
        try {
            // can only update name、email、mobile、accountType
            conn = DaoUtils.getConnection(false);
            dbAccount = AccountDao.get(conn, Account.fromExternalId(accountExtId));
            if (dbAccount == null) {
                LOG.error("[{}] account not exists. accountExtId:{}", Constants.COREDB, accountExtId);
                throw new RestException(StatusCodes.BadRequest);
            }
            if (StringUtils.isNotEmpty(account.getName())) {
                dbAccount.setName(account.getName());
            }
            if (StringUtils.isNotEmpty(account.getType())) {
                dbAccount.getProperties().put(Account.PROP_TYPE, parseAccountType(account.getType()));
            }
            if (StringUtils.isNotEmpty(account.getTimezone())) {
                dbAccount.getProperties().put(Account.PROP_TIMEZONE, account.getTimezone());
            }
            if (account.getFeatureEnable() != null) {
                dbAccount.getProperties().put(Account.PROPERTY_FEATURE_ENABLE, account.getFeatureEnable());
            }
            if (account.getActive() != null) {
                dbAccount.getProperties().put(Account.PROP_ACTIVE, account.getActive());
                dbAccount.getProperties().put(Account.PROPERTY_LAST_LOGIN, System.currentTimeMillis());
            }

            dbAccount.getProperties().put("updateTime", System.currentTimeMillis());
            AccountDao.update(conn, dbAccount);
            conn.commit();
            AccountCatalog.initialize();
        } catch (Exception exception) {
            LOG.error("[{}] update accounts failed. error:{}", Constants.COREDB, exception.getMessage());
            DaoUtils.rollbackQuietly(conn);
            throw new RestException(StatusCodes.BadRequest, "update account fail.");
        } finally {
            DaoUtils.closeQuietly(conn);
        }
        return dbAccount;
    }

    private Account _createAccount(String name, String dbName, String admin, String username, JSONArray admins, JSONObject properties) throws Exception {
        // by default, we create the first account using this method
        long accountId = ZBotRuntime.SYSMANAGE_ACCOUNTID;
        Set<String> adminNames = admins.stream().map(Object::toString).collect(Collectors.toSet());
        adminNames.add(admin);

        for (String mobile : adminNames) {
            if (AccountCatalog.checkUserIsReachable(mobile)) {
                throw new RestException(StatusCodes.USER_ALREADY_EXISTS, mobile);
            }
        }

        AccountCatalog acct = AccountCatalog.of(ZBotRuntime.SYSMANAGE_ACCOUNTID);
        if (acct != null) {
            // throw new RestException(StatusCodes.Forbidden, "Account already created");
            accountId = IdGenerator.generateId();
            LOG.warn("[{}][Account ID 1 already taken, create new account ...]", Constants.COREDB);
        }

        if (StringUtils.isEmpty(dbName)) {
            dbName = Account.getExternalId(accountId);
        }

        // 账号来源
        String from = properties.getString("from");

        LOG.info("[{}][Try create new account:{}, name:{}]", Constants.COREDB, accountId, name);

        @Cleanup Connection conn = DaoUtils.getConnection(false);
        Account dbAccount = AccountCore.createAccountPhase1(conn, accountId, name, dbName, admin, properties);

        LOG.info("[{}][Create resources for initial account ...]", Constants.COREDB);
        AccountCatalog account = AccountCore.createAccountPhase2(conn, accountId);

        // enqueue the first CO task to get job done
        LOG.info("[{}][Create initial admin ...]", Constants.COREDB);

        String passcode = properties.getString(Account.PROP_PASSCODE);
        passcode = StringUtils.isBlank(passcode) ? User.USER_DEFAULT_PWD : passcode;
        for (String adminName : adminNames) {

            String email = "";
            String mobile = "";
            if (Validator.isEmail(adminName)) {
                email = adminName;
            }
            if (Validator.isMobile(adminName)) {
                mobile = adminName;
            }
            if (StringUtils.isBlank(email) && StringUtils.isBlank(adminName)) {
                LOG.error("[{}] invalid mobile and email. admin:{}", dbName, adminName);
                throw new RestException(StatusCodes.INVALID_USER_MOBILE_OR_EMAIL);
            }
            if (admin.equals(adminName)) {
                adminName = username;
            }
            User user = AccountActivationOp.activateAccountQuick(account, conn, email, mobile, adminName, "", from, "");

            String avatar = UserController.generateImageIfNotExist(user.getUsername(), user.getProperties().getAvatar(), conn, account.getDBName());
            user.getProperties().setAvatar(avatar);

            user.setPassword(passcode);
            UserDao.updateProperties(conn, account.getDBName(), user);
            account.updateUserProperties(user);

            LOG.info("[{}][Create admin user with account:{}]", account.getDBName(), adminName);
        }

        conn.commit();
        return dbAccount;
    }

    private boolean checkCreateAccountParam(String name, String dbName, String admin) {
        if (StringUtils.isBlank(name)) {
            LOG.error("[{}][can't create account which name is null or empty}]", dbName);
            return false;
        }
        if (StringUtils.isBlank(admin)) {
            LOG.error("[{}][can't create account which admin is null or empty}]", dbName);
            return false;
        }
        return true;
    }

    private String parseAccountType(String accountType) {

        switch (accountType) {
            case Account.TYPE_TRIAL:
                break;
            case Account.TYPE_NORMAL:
                break;
            default:
                LOG.error("[{}] not support account type. accountType:{}", Constants.COREDB, accountType);
                throw new RestException(StatusCodes.BadRequest, "not support account type");
        }

        return accountType;
    }

}
