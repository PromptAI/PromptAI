package com.zervice.kbase.database.helpers;

import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.zervice.common.pojo.common.Account;
import com.zervice.kbase.AccountCatalog;
import com.zervice.kbase.database.dao.AccountDao;
import com.zervice.kbase.database.dao.ConfigurationDao;
import com.zervice.kbase.database.dao.RbacRoleDao;
import com.zervice.kbase.database.pojo.Configuration;
import com.zervice.kbase.database.pojo.RbacRole;
import com.zervice.kbase.database.utils.DaoUtils;
import com.zervice.kbase.rbac.RBACConstants;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;

import java.sql.Connection;
import java.sql.SQLException;

@Log4j2
public class AccountCore {
    /**
     * 1.create account db
     * 2.add account info to accounts
     * 3.add configuration
     */
    public static Account createAccountPhase1(@NonNull Connection conn,
                                              long acctId,
                                              String acctName,
                                              String dbName,    // if empty, we would use external ID
                                              String creator,
                                              @NonNull JSONObject props) throws SQLException {
        Preconditions.checkArgument(acctId > 0 &&
                !Strings.isNullOrEmpty(acctName));

        LOG.info("[create new databases for account:{}, id:{}]", acctName, acctId);


        if (StringUtils.isEmpty(dbName)) {
            dbName = Account.getExternalId(acctId);
        }

        try {
            DbInitializer.createAccountDb(conn, acctId, dbName);

            Account acct = Account.newBuilder(props)
                    .id(acctId)
                    .name(acctName)
                    .dbName(dbName)
                    .status(Account.STATUS_SETTINGUP1)
                    .createdAtEpochMs(System.currentTimeMillis())
                    .createdByUser(creator)
                    .build();

            AccountDao.add(conn, acct);

            // set the user's email or mobile as the account owner!!!
            ConfigurationDao.add(conn, dbName, Configuration.ACCOUNT_OWNER, creator);

            return acct;
        } catch (Exception e) {
            DaoUtils.dropDbQuietly(conn, dbName);
            throw e;
        }
    }

    /**
     * 1. init admin user rbac info
     * 2. cache account info and resources info
     * Second part of account creation to
     * 1. initialize key data
     *    TODO: separate table creation and initialization and call initializer here for each table ...
     * 2. other stuff ...
     */
    public static AccountCatalog createAccountPhase2(@NonNull Connection conn, long accountId) throws SQLException {
        Account account = AccountDao.get(conn, accountId);

        String dbName = account.getDbName();
        // create roles
        RbacRole adminRole = new RbacRole();
        adminRole.setName(RBACConstants.SYS_ADMIN);
        adminRole.setStatus(true);
        adminRole.setDisplay("System Administrator");
        adminRole.setProperties(RbacRole.RbacRoleProp.parse(RBACConstants.ROLE_SITE_ADMIN));
        RbacRoleDao.add(conn, dbName, adminRole);

        RbacRole userRole = new RbacRole();
        userRole.setName(RBACConstants.SYS_USER);
        userRole.setStatus(true);
        userRole.setDisplay("System User");
        userRole.setProperties(RbacRole.RbacRoleProp.parse(RBACConstants.ROLE_SITE_USER));
        RbacRoleDao.add(conn, dbName, userRole);

        AccountDao.updateStatus(conn, account.getId(), Account.STATUS_READY);
        account.getProperties().put(Account.PROP_STATUS, Account.STATUS_READY);

        return AccountCatalog.build(conn, account);
    }

}

