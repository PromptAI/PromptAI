package com.zervice.kbase.api.restful.rbac;

import com.zervice.common.pojo.common.Account;
import com.zervice.common.restful.exception.RestException;
import com.zervice.common.restful.exception.StatusCodes;
import com.zervice.kbase.AccountCatalog;
import com.zervice.kbase.api.restful.pojo.RestRole;
import com.zervice.kbase.api.restful.pojo.RestUser;
import com.zervice.kbase.database.dao.AccountDao;
import com.zervice.kbase.database.dao.RbacRoleDao;
import com.zervice.kbase.database.dao.UserDao;
import com.zervice.kbase.database.dao.UserRbacRoleDao;
import com.zervice.kbase.database.pojo.RbacRole;
import com.zervice.kbase.database.pojo.User;
import com.zervice.kbase.database.pojo.UserRbacRole;
import com.zervice.kbase.database.utils.DaoUtils;
import lombok.experimental.UtilityClass;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@UtilityClass
public class UserRolesBuilder {
    public static RestUser buildUserWithRoles(String dbName, long userId) throws Exception {
        List<RestRole> roleList = new ArrayList<>();
        Connection conn = null;
        com.zervice.kbase.database.pojo.User dbUser;
        try {
            conn = DaoUtils.getConnection(true);
            dbUser = UserDao.get(conn, dbName, userId);
            if (dbUser == null) {
                throw new RestException(StatusCodes.USER_NOT_EXISTS);
            }

            List<UserRbacRole> userRbacRoleList = UserRbacRoleDao.get(conn, dbName, dbUser.getId());
            if (userRbacRoleList.isEmpty()) {
                return toRestUser(dbUser, new ArrayList<>(), conn, dbName);
            }

            for (UserRbacRole userRbacRole : userRbacRoleList) {
                RbacRole rbacRole = RbacRoleDao.get(conn, dbName, userRbacRole.getRbacRoleId());
                if (rbacRole == null) {
                    throw new RestException(StatusCodes.ROLE_NOT_EXISTS);
                }
                roleList.add(new RestRole(rbacRole));
            }
            return toRestUser(dbUser, roleList, conn, dbName);

        } finally {
            DaoUtils.closeQuietly(conn);
        }

    }

    public static List<RestUser> buildUserWithRoles(String dbName, List<com.zervice.kbase.database.pojo.User> dbUserList)
            throws Exception {
        List<RestUser> userList = new ArrayList<>();
        for (com.zervice.kbase.database.pojo.User dbUser : dbUserList) {
            userList.add(buildUserWithRoles(dbName, dbUser.getId()));
        }
        return userList.stream().sorted(Comparator.comparingLong(r -> Long.parseLong(r.getId())))
                .collect(Collectors.toList());
    }

    public static RestUser buildUserWithRoles(String dbName, com.zervice.kbase.database.pojo.User dbUser) throws Exception {
        List<RestRole> roleList = new ArrayList<>();
        Connection conn = null;
        try {
            conn = DaoUtils.getConnection(true);
            List<UserRbacRole> userRbacRoleList = UserRbacRoleDao.get(conn, dbName, dbUser.getId());
            if (userRbacRoleList.isEmpty()) {
                return toRestUser(dbUser, new ArrayList<>(), conn, dbName);
            }

            for (UserRbacRole userRbacRole : userRbacRoleList) {
                RbacRole rbacRole = RbacRoleDao.get(conn, dbName, userRbacRole.getRbacRoleId());
                if (rbacRole == null) {
                    throw new RestException(StatusCodes.ROLE_NOT_EXISTS);
                }
                roleList.add(new RestRole(rbacRole));
            }
            return toRestUser(dbUser, roleList, conn, dbName);
        } finally {
            DaoUtils.closeQuietly(conn);
        }
    }

    private RestUser toRestUser(User user, List<RestRole> roleList, Connection conn, String dbName) throws Exception {
        User creator = null;
        if (user.getProperties().getCreatorId() > 0) {
            // maybe this is the current admin, the creator is not set for the current super admin
            // see com.zervice.kbase.database.helpers.DbInitializer._createTestingAccount
            creator = UserDao.get(conn, dbName, user.getProperties().getCreatorId());
        }
        String creatorName = creator == null ? null : creator.getUsername();


        //账号每events只保存了登录事件，根据这个判断是否是第一次登录
        Boolean firstLogin = _firstLogin(dbName);

        // query rest token
        long restToken = AccountDao.getRestToken(conn, Account.fromExternalId(dbName));

        Account account = AccountDao.getByDbName(conn, dbName);
        boolean featureEnable = account.featureEnabled();

        RestUser restUser = new RestUser(user, roleList, creatorName);
        restUser.setFirstLogin(firstLogin);
        restUser.setRestToken(restToken);
        restUser.setFeatureEnable(featureEnable);

        return restUser;
    }

    private boolean _firstLogin(String dbName) {
        AccountCatalog accountCatalog = AccountCatalog.guess(dbName);
        //账号每events只保存了登录事件，根据这个判断是否是第一次登录
        return accountCatalog.getEvents() == null || accountCatalog.getEvents().size() <= 1;
    }

}
