package com.zervice.kbase.queues.atomic.operations;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Preconditions;
import com.zervice.common.pojo.common.Account;
import com.zervice.kbase.AccountCatalog;
import com.zervice.kbase.api.restful.pojo.RestRole;
import com.zervice.kbase.api.restful.pojo.RestUser;
import com.zervice.kbase.database.dao.AccountDao;
import com.zervice.kbase.database.dao.UserDao;
import com.zervice.kbase.database.dao.UserRbacRoleDao;
import com.zervice.kbase.database.pojo.AtomicTask;
import com.zervice.kbase.database.pojo.User;
import com.zervice.kbase.database.utils.DaoUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Log4j2
@AtomicOperation(CreateUserOp.TYPE)
public class CreateUserOp {
    public static final String TYPE = "createUser";
    private static final long _ONE_DAY_MILLIS = 24 * 60 * 60 * 1000;

    public static void process(AtomicTask task) {
        final long acctId = task.getAcctId();
        String dbName = Account.getAccountDbName(acctId);
        final AccountCatalog account = AccountCatalog.of(acctId);
        final JSONObject data = task.getData();

        Connection conn = null;

        try {
            RestUser userRest = JSON.parseObject(data.getString("data"), RestUser.class);
            /*
            if (userRest.getRoles() == null || userRest.getRoles().size() == 0) {
                LOG.warn().param("userId", userRest.getId()).message("Should have at least one role");
                throw new IllegalArgumentException("Should have at least one role");
            }
            */

            conn = DaoUtils.getConnection(false);

            createUser(conn, account, userRest);
        } catch (IllegalArgumentException e) {
            DaoUtils.rollbackQuietly(conn);
            LOG.error("[{}][Invalid task data:{}, e:{}]",dbName, data.toString(), e.getMessage(), e);
            task.setException(e);
        } catch (Exception e) {
            DaoUtils.rollbackQuietly(conn);
            LOG.error("Unable to handle this task because of exception:{}",dbName, e.getMessage(), e);
            task.setException(e);
        } finally {
            DaoUtils.closeQuietly(conn);
        }
    }

    public static User createUser(Connection conn, AccountCatalog account, RestUser userRest) throws Exception {
        return createUser(conn, account, userRest, false);
    }

    public static User createUser(Connection conn, AccountCatalog account, RestUser userRest, boolean quick) throws Exception {
        final long acctId = account.getId();
        final String dbName = account.getDBName();

        String lowerEmail = userRest.getEmail().toLowerCase();
        if(!StringUtils.isEmpty(lowerEmail)) {
            if(account.findUserByEmail(lowerEmail) != null) {
                throw new IllegalArgumentException("The user email already exists - " + lowerEmail);
            }

            AccountCatalog acct2 = AccountCatalog.getUserAccountByEmail(lowerEmail);
            if(acct2 != null) {
                LOG.error("User email:{} already exists in another account:{}", lowerEmail, acct2.getName());
                throw new IllegalArgumentException("The user email already token - " + lowerEmail);
            }
        }

        String mobile = userRest.getMobile();
        if(!StringUtils.isEmpty(mobile)) {
            if (account.findUserByMobile(mobile) != null) {
                throw new IllegalArgumentException("The user mobile already exists - " + mobile);
            }

            AccountCatalog acct2 = AccountCatalog.getUserAccountByMobile(mobile);
            if(acct2 != null) {
                LOG.error("User mobile:{} already exists in another account:{}", mobile, acct2.getName());
                throw new IllegalArgumentException("The user mobile already token - " + mobile);
            }
        }

        Account dbAccount = AccountDao.get(conn, account.getId());
        if(dbAccount == null) {
            throw new IllegalStateException("Cannot load account information");
        }
        // generate an api key
        User dbUser = userRest.toDbObject();

//        if(ZBotRuntime.isStandalone()) {
//            dbUser.setStatus(User.STATUS_ACTIVE);
//        } else {
//            dbUser.setStatus(User.STATUS_REGISTERED);
//        }
        dbUser.setStatus(true);

        dbUser.setId(UserDao.addReturnId(conn, dbName, dbUser));
        userRest.setId(User.getExternalId(dbUser.getId()));
        // update user-role.
        Set<RestRole> externalRoles = userRest.getRoles();
        Set<Long> roleIds = new HashSet<>();

        if(externalRoles != null) {
            for (RestRole role : externalRoles) {
                long roleId = Long.parseLong(role.getId());
                if (!account.getAcm().getRoleMap().containsKey(roleId)) {
                    throw new IllegalArgumentException("role " + role + " not exist");
                }
                UserRbacRoleDao.add(conn, dbName, dbUser.getId(), roleId, new JSONObject());
                roleIds.add(roleId);
            }
        }

        conn.commit();

        // send invitation via email, only if user is created with an email
//        if(!quick && !StringUtils.isEmpty(lowerEmail)) {
//            JwtPayLoad payLoad = JwtPayLoad.of(Account.getExternalId(account.getId()), User.getExternalId(dbUser.getId()), dbUser.getUsername());
//            String jwtToken = JwtTokenUtil.generate(payLoad);
//
//            if (!ZPRuntime.isStandalone()) {
//                Email email = EmailTemplates.buildInvitationEmail(dbAccount, jwtToken);
//                email.addTo(dbUser.getEmail());
//
//                account.getEmailService().queueEmail(email, new JSONObject());
//            }
//        }

        // update AccountControlBlock
        List<Pair<Long, JSONObject>> roleParams = new ArrayList<>();
        for (RestRole role : externalRoles) {
            JSONObject roleParam = new JSONObject();
            long id = StringUtils.isBlank(role.getId()) ? -1 : Long.parseLong(role.getId());
            UserRbacRoleDao.add(conn, dbName, dbUser.getId(), id, roleParam);
            roleParams.add(new MutablePair<>(id, roleParam));
        }

        account.onUpdateUser(dbUser, roleParams);

        // update extra props and cache to acb before return
        dbUser.setRoles(roleIds);

        return dbUser;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    static public class Builder {
        RestUser _dataRest = null;

        public Builder userData(RestUser j) {
            _dataRest = j;
            return this;
        }

        public AtomicTask build() {
            Preconditions.checkArgument(_dataRest != null);

            AtomicTask t = new AtomicTask();
            t.setId(0);
            t.setEnqueuedAtEpochMs(System.currentTimeMillis());
            t.setDequeuedAtEpochMs(0);
            t.setStatus(AtomicTask.Status.READY);

            JSONObject j = new JSONObject();
            j.put("ver", 1);
            j.put("type", TYPE);
            j.put("data", JSON.toJSONString(_dataRest));
            t.setData(j);

            return t;
        }
    }

}
