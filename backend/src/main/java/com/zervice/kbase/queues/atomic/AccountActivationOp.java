package com.zervice.kbase.queues.atomic;


import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Preconditions;
import com.zervice.common.pojo.common.Account;
import com.zervice.common.utils.JSONUtils;
import com.zervice.kbase.AccountCatalog;
import com.zervice.kbase.api.restful.pojo.RestRole;
import com.zervice.kbase.api.restful.pojo.RestUser;
import com.zervice.kbase.database.dao.RbacRoleDao;
import com.zervice.kbase.database.pojo.AtomicTask;
import com.zervice.kbase.database.pojo.RbacRole;
import com.zervice.kbase.database.pojo.User;
import com.zervice.kbase.database.utils.DaoUtils;
import com.zervice.kbase.rbac.RBACConstants;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;

import java.sql.Connection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This is the very first CO to be executed for a customer. It would finish the 2nd phase creation operation, etc.
 */
@Log4j2
@AtomicOperation(AccountActivationOp.TYPE)
public class AccountActivationOp {
    public static final String TYPE = "activateAccount";

    public static void process(AtomicTask task) {
        final long acctId = task.getAcctId();
        final AccountCatalog account = AccountCatalog.of(acctId);
        if (account == null) {
            LOG.error("Activating invalid account - " + acctId);
            task.setException(new IllegalArgumentException("Account invalid"));
            return;
        }

        final JSONObject data = task.getData().getJSONObject("data");
        boolean quickMode = JSONUtils.getOrDefault(data, "mode", false);
        JSONArray admins = data.getJSONArray("admins");
        if (admins == null || admins.size() == 0) {
            // admins cannot be empty!
            LOG.error("[{}][Empty admin list for account - {}]", account.getDBName(), account.getName());
            task.setException(new IllegalArgumentException("No ADMINS"));
            return;
        }
        JSONArray users = data.getJSONArray("users");

        // special case, for quick signin, user just provides a mobile #, we use the mobile # for account creation
        String adminMobile = "";
        String adminEmail = "";
        String adminName = "";
        String from = "";
        if (quickMode) {
            // must have just one element in admins!!!!

            // is this a JSON object? We could pass a JSON object like
            // {
            //    username: xxx,
            //    phone: xxx,
            //    email: xxx
            //  }
            Object tmp = admins.get(0);
            if (tmp instanceof JSONObject) {
                JSONObject admin = (JSONObject) tmp;
                adminName = JSONUtils.getOrDefault(admin, "username", "");
                adminMobile = JSONUtils.getOrDefault(admin, "phone", "");
                adminEmail = JSONUtils.getOrDefault(admin, "email", "");
                from = JSONUtils.getOrDefault(admin, "from", "");
            } else {
                try {
                    Long.parseLong(admins.getString(0));
                    adminMobile = admins.getString(0);
                } catch (Exception e) {
                    adminEmail = admins.getString(0);
                }
            }

            admins = new JSONArray();
        }

        Connection conn = null;
        try {
            conn = DaoUtils.getConnection(false);

            if (quickMode) {
                activateAccountQuick(account, conn, adminEmail, adminMobile, adminName, "", from, "");
            } else {
                activateAccount(account, conn, admins, users);
            }

            conn.commit();
        } catch (IllegalArgumentException e) {
            DaoUtils.rollbackQuietly(conn);
            LOG.error("[{}][Invalid task data:{}, e:{}]", account.getDBName(), data.toString(), e.getMessage(), e);
            task.setException(e);
        } catch (Exception e) {
            DaoUtils.rollbackQuietly(conn);
            LOG.error("[{}][Unable to handle this task because of exception:{}]", account.getDBName(), e.getMessage(), e);
            task.setException(e);
        } finally {
            DaoUtils.closeQuietly(conn);
        }
    }

    public static void activateAccount(AccountCatalog account, Connection conn, JSONArray admins, JSONArray users) throws Exception {
        long acctId = account.getId();

        // for each user, queue a CreateUserCo
        LOG.info("[{}][Creating users for {}]", account.getDBName(), account.getName());
        AtomicTaskQueues atomicTaskQueues = AtomicTaskQueues.getInstance();

        if (admins.size() > 0) {
            // get the roles for admin
            RbacRole rr = RbacRoleDao.get(conn, account.getDBName(), RBACConstants.SYS_ADMIN);
            Set<RestRole> adminRoles = new HashSet<>(Collections.singletonList(new RestRole(rr)));
            _createUsers(conn, atomicTaskQueues, acctId, admins, adminRoles);
        }

        if (users != null) {
            RbacRole rr = RbacRoleDao.get(conn, account.getDBName(), RBACConstants.SYS_USER);
            Set<RestRole> userRoles = new HashSet<>(Collections.singletonList(new RestRole(rr)));
            _createUsers(conn, atomicTaskQueues, acctId, users, userRoles);
        }
    }

    public static User activateAccountQuick(AccountCatalog account, Connection conn, String adminEmail, String adminMobile,
                                            String username, String picUrl, String from, String desc) throws Exception {
        // get the roles for admin
        RbacRole rr = RbacRoleDao.get(conn, account.getDBName(), RBACConstants.SYS_ADMIN);
        Set<RestRole> adminRoles = new HashSet<>(Collections.singletonList(new RestRole(rr)));

        // for each user, queue a CreateUserCo
        LOG.info("[{}][Quick creating users for account:{}, email:{}, mobile:{} ]", account.getDBName(), account.getName(), adminEmail, adminMobile);

        // the admin phone # shall be correct
        return _createUserSync(conn, account, username, adminEmail, adminMobile, adminRoles, picUrl, from, desc);
    }

    private static void _createUsers(Connection conn, AtomicTaskQueues atomicTaskQueues, long acctId, JSONArray users, Set<RestRole> roles) throws Exception {
        for (int i = 0; i < users.size(); i++) {
            String email = users.getString(i);

            RestUser userRest = new RestUser();
            userRest.setUsername(email);
            userRest.setEmail(email);

            LOG.info("[{}][Queue user creation for user:{}", Account.getAccountDbName(acctId), email);

            userRest.setRoles(roles);
            AtomicTask t = CreateUserOp.newBuilder()
                    .userData(userRest)
                    .build();

            t.setAcctId(acctId);
            atomicTaskQueues.putWithCommit(conn, acctId, t);
        }
    }

    /**
     * Create admin account in synchronous way instead of using atomic queue ...
     */
    private static User _createUserSync(Connection conn, AccountCatalog account, String name,
                                        String email, String mobile, Set<RestRole> roles,
                                        String picUrl, String from, String desc) throws Exception {
        Preconditions.checkArgument(!StringUtils.isEmpty(email) || !StringUtils.isEmpty(mobile));
        LOG.info("[{}][Queue quick synchronous user creation, emial:{}, mobile:{}]", account.getDBName(), email, mobile);

        String username = StringUtils.isEmpty(name) ? mobile : name;
        if (StringUtils.isEmpty(username) && !StringUtils.isEmpty(email)) {
            username = email.substring(0, email.indexOf('@'));
            if (StringUtils.isEmpty(username)) {
                username = email;
            }
        }

        RestUser userRest = new RestUser();
        userRest.setUsername(username);
        userRest.setEmail(email);
        userRest.setMobile(mobile);
        userRest.setAvatar(picUrl);

        userRest.setRoles(roles);
        userRest.setDesc(desc);
        userRest.setFrom(from);

        // create synchronously
        return CreateUserOp.createUser(conn, account, userRest, true);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    static public class Builder {
        JSONObject _data = new JSONObject();

        public Builder setQuickMode(boolean quick) {
            _data.put("mode", quick);
            return this;
        }

        public Builder setAdmins(List<String> admins) {
            _data.put("admins", admins);
            return this;
        }

        public Builder setUsers(List<String> users) {
            _data.put("users", users);
            return this;
        }

        public AtomicTask build() {
            AtomicTask t = new AtomicTask();
            t.setId(0);
            t.setEnqueuedAtEpochMs(System.currentTimeMillis());
            t.setDequeuedAtEpochMs(0);
            t.setStatus(AtomicTask.Status.READY);

            JSONObject j = new JSONObject();
            j.put("ver", 1);
            j.put("type", TYPE);
            j.put("data", _data);
            t.setData(j);

            return t;
        }
    }

}
