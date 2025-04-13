package com.zervice.kbase.critical;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.zervice.common.restful.exception.RestException;
import com.zervice.common.restful.exception.StatusCodes;
import com.zervice.kbase.AccountCatalog;
import com.zervice.kbase.api.restful.pojo.RestRole;
import com.zervice.kbase.database.dao.UserDao;
import com.zervice.kbase.database.dao.UserRbacRoleDao;
import com.zervice.kbase.database.pojo.User;
import com.zervice.kbase.database.utils.DaoUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Creates a new user.
 *
 * The data json has the format
 *
 * {                                // <-- jdata
 *     ver: 1,
 *     type: "createUserOp",
 *     data: {                      // <-- juserdata
 *       username: "def@cnn.com",
 *       properties: {              // <-- jprops
 *         ver: 1,
 *         password: "dasf09343",    // should be encrypted in the future
 *         timezone: "AST",          // Store abbreviation. use the global one if not set
 *         ...
 *       },
 *
 *       // this section for rbac
 *       accessControl: {
 *         role: "site_admin"
 *         params: {          // empty if for role "site_admin"
 *             managedResources: ["spc343dd", "spc343dd"],
 *             readResources: ["spc565f", "spc98d"]
 *         }
 *       }
 *     }
 * }
 *
 */
@Log4j2
public class CreateUserCO {

    public static User process(String dbName, UserParams newUserParams) throws Exception {
        final AccountCatalog account = AccountCatalog.guess(dbName);
        Connection conn = null;

        try {
            conn = DaoUtils.getConnection(false);

            UserParams params = newUserParams;

            String username = params.getName().toLowerCase();
            if (account.getUser(username) != null) {
                LOG.error("[{}][add user fail， username exits. username:{}]", dbName, username);
                throw new RestException(StatusCodes.BadRequest, "username exits");
            }

            User dbUser = User.factory(
                    params.getName(),
                    params.getPassword(),
                    params.getEmail(), params.getMobile(),
                    params.getProperties().toJavaObject(User.UserProp.class)
            );

            List<Pair<Long, JSONObject>> roleParams = new ArrayList<>();


            Set<Long> roles = new HashSet<>(params.getRoles().size());
            long newUserId = UserDao.addReturnId(conn, dbName, dbUser);
            if (newUserId == 0) {
                throw new IllegalStateException("The user added failed " + dbName + " user:" + dbUser.getUsername());
            } else {
                dbUser.setId(newUserId);

                for (Object roleObj : params.getRoles()) {
                    RestRole restRole = ((JSONObject) roleObj).toJavaObject(RestRole.class);
                    JSONObject roleParam = new JSONObject();
                    long id = StringUtils.isBlank(restRole.getId()) ? -1 : Long.parseLong(restRole.getId());
                    UserRbacRoleDao.add(conn, dbName, newUserId, id, roleParam);
                    roleParams.add(new MutablePair<>(id, roleParam));

                    roles.add(id);
                }
            }

            dbUser.setRoles(roles);

            conn.commit();

            // update AccountControlBlock
            account.onCreateUser(dbUser);

            // update extra props and cache to acb before return
            account.onUpdateUser(dbUser, roleParams);
            return dbUser;
        } catch (IllegalArgumentException e) {
            DaoUtils.rollbackQuietly(conn);
            LOG.error("[{}][Invalid user data to create user]", dbName, e);
            throw e;
        } catch (Exception e) {
            DaoUtils.rollbackQuietly(conn);
            LOG.error("[{}][Failed to create user]", dbName, e);
            throw e;
        } finally {
            DaoUtils.closeQuietly(conn);
        }
    }

    public static UserParams create() {
        return new UserParams();
    }

    @Setter
    @Getter
    @Accessors(chain = true)
    static public class UserParams {
        String _name;
        String _password;
        String _email;
        String _mobile;
        JSONObject _properties;
        JSONArray _roles;
    }

}
