package com.zervice.kbase.critical;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.zervice.kbase.AccountCatalog;
import com.zervice.kbase.database.dao.UserDao;
import com.zervice.kbase.database.pojo.User;
import com.zervice.kbase.database.utils.DaoUtils;
import lombok.extern.log4j.Log4j2;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

@Log4j2
public class DeleteUserCO {

    public static void process(String dbName, DeleteParams deleteParams) throws Exception{
        Connection conn = null;

        try {
            conn = DaoUtils.getConnection(true);

            List<Long> userIds = deleteParams._userIds;

            for(long userId : userIds) {
                User u = UserDao.get(conn, dbName, userId);
                if (u == null) {
                    LOG.error("Cannot find user to delete - " + userId);

                    // TODO? siliently ignore?
                    //throw new WebApplicationException(Response.Status.NOT_FOUND);

                    continue;
                }

                UserDao.delete(conn, dbName, userId);

                LOG.info("Deleted user - " + userId + " (name=" + u.getUsername() + ")");

                AccountCatalog.guess(dbName).onDeleteUser(u);
            }
        }
        catch (IllegalArgumentException e) {
            LOG.error("Invalid data to delete user", e);
            DaoUtils.rollbackQuietly(conn);
            throw e;
        }
        catch (Exception e) {
            DaoUtils.rollbackQuietly(conn);
            LOG.error("Failed to delete user", e);
            throw e;
        }
        finally {
            DaoUtils.closeQuietly(conn);
        }
    }


    /**
     * @throws IllegalArgumentException
     */
    private static List<Long> _getUserIds(JSONObject data) {
        try {
            JSONArray arr = data.getJSONArray("userIds");
            if (arr.isEmpty()) {
                throw new IllegalArgumentException("Missing userId");
            }

            return arr.toJavaList(Long.class);
        }
        catch (Exception e) {
            if (e instanceof IllegalArgumentException)
                throw e;
            else
                throw new IllegalArgumentException(e);
        }
    }


    public static DeleteParams create() {
        return new DeleteParams();
    }

    static public class DeleteParams {
        List<Long> _userIds = new ArrayList<>();

        public DeleteParams setUserIds(List<Long> userIds) {
            _userIds = userIds;
            return this;
        }

    }
}
