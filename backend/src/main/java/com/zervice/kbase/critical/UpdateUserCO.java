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

@Log4j2
public class UpdateUserCO {

    public static User process(String dbName, UserParams updateUserParams) throws Exception {
        final AccountCatalog account = AccountCatalog.guess(dbName);

        Connection conn = null;

        try {
            conn = DaoUtils.getConnection(false);

            UserParams params = updateUserParams;

            // shall we get user from acb or from userDao
            User user = UserDao.get(conn, dbName, params.getId());
            if (user == null) {
                throw new RestException(StatusCodes.NotFound, "user not found");
            }

            if (account.getUser(user.getId()) == null) {
                LOG.warn("User not in cache! Possible cache in-consistence for userId - " + params.getId());
            }

            //check name :name updated
            if (!user.getUsername().equalsIgnoreCase(params.getName())) {
                if (UserDao.get(conn, dbName, params.getName()) != null) {
                    LOG.error("update fail， username exits -" + params.getName());
                    throw new RestException(StatusCodes.BadRequest, "username exits");
                }
            }

            user.setUsername(params.getName());
            user.setStatus(params.isStatus());
            user.setEmail(params.getEmail());
            user.setMobile(params.getMobile());

            if(StringUtils.isNotBlank(params.getPassword())) {
                user.setPassword(params.getPassword());
            }

            // update properties
            User.UserProp newProps = params.getProperties().toJavaObject(User.UserProp.class);
            User.UserProp props = user.getProperties();

            if(StringUtils.isNotBlank(newProps.getAvatar())) {
                props.setAvatar(newProps.getAvatar());
            }

            UserDao.update(conn, dbName, user);

            // remove old rbacrole settings
            UserRbacRoleDao.deleteByUserId(conn, dbName, user.getId());

            Set<Long> roles = new HashSet<>(params.getRoles().size());
            List<Pair<Long, JSONObject>> roleParams = new ArrayList<>();
            for (Object roleObj : params.getRoles()) {
                RestRole restRole = ((JSONObject)roleObj).toJavaObject(RestRole.class);
                JSONObject roleParam = new JSONObject();
                long id = StringUtils.isBlank(restRole.getId()) ? -1 : Long.parseLong(restRole.getId());
                UserRbacRoleDao.add(conn, dbName, user.getId(), id, roleParam);
                roleParams.add(new MutablePair<>(id, roleParam));
                roles.add(id);
            }

            conn.commit();

            user.setRoles(roles);

            account.onUpdateUser(user, roleParams);

            return user;
        }
        catch (IllegalArgumentException e) {
            DaoUtils.rollbackQuietly(conn);
            LOG.error("Invalid data to update user", e);
            throw e;
        }
        catch (Exception e) {
            DaoUtils.rollbackQuietly(conn);
            LOG.error("Failed to update user", e);
            throw e;
        }
        finally {
            DaoUtils.closeQuietly(conn);
        }
    }

    public static UserParams create() {
        return new UserParams();
    }

    @Setter
    @Getter
    @Accessors(chain=true)
    static public class UserParams {
        Long _id;
        String _name;
        String _password;
        String _email;
        String _mobile;
        boolean _status;
        JSONObject _properties;
        JSONArray _roles;
    }
}
