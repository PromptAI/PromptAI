package com.zervice.kbase.database.dao;

import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.zervice.kbase.database.helpers.CustomerDbTable;
import com.zervice.kbase.database.pojo.UserRbacRole;
import com.zervice.kbase.database.utils.DaoUtils;
import com.zervice.kbase.database.utils.RecordLoader;
import lombok.NonNull;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

@CustomerDbTable(after = "UserDao,RbacRoleDao")
public class UserRbacRoleDao {
    public static final String TABLE_NAME = "user_rbacroles";

    private static final String _SQL_INSERT = "INSERT INTO %s." + TABLE_NAME + " (user_id, rbacrole_id, params) VALUES (?, ?, ?) ";
    private static final String _SQL_SELECT_ALL = "SELECT id, user_id, rbacrole_id, params FROM %s." + TABLE_NAME;
    private static final String _SQL_SELECT_BY_USER_ID = "SELECT id, user_id, rbacrole_id, params FROM %s." + TABLE_NAME + " WHERE user_id=?";
    private static final String _SQL_SELECT_BY_USER_ID_AND_RBACROLE_ID = "SELECT id, user_id, rbacrole_id, params FROM %s." + TABLE_NAME + " WHERE user_id=? AND rbacrole_id=?";
    private static final String _SQL_DELETE_USER_ID = "DELETE FROM %s." + TABLE_NAME + " WHERE user_id=?";

    private final static RecordLoader<UserRbacRole> _LOADER = rs -> {
        UserRbacRole u = new UserRbacRole();
        u.setId(rs.getLong(1));
        u.setUserId(rs.getLong(2));
        u.setRbacRoleId(rs.getLong(3));
        u.setParams(JSONObject.parseObject(rs.getString(4)));
        return u;
    };


    public static void createTable(@NonNull Connection conn,
                                   String dbName) throws SQLException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(dbName));

        String sql = String.format("  CREATE TABLE %s." + TABLE_NAME + " (\n" +
                "           id BIGINT not null primary key auto_increment,\n" +
                "           user_id BIGINT not null,\n" +
                "           rbacrole_id BIGINT not null,\n" +
                "           params text(65535) not null,\n" +
                "\n" +
                "           FOREIGN KEY (user_id) REFERENCES %s.users(id) ON DELETE CASCADE,\n" +
                "           FOREIGN KEY (rbacrole_id) REFERENCES %s.rbacroles(id) ON DELETE CASCADE\n" +
                "         ) engine=innodb ROW_FORMAT=DYNAMIC\n", dbName, dbName, dbName);
        DaoUtils.executeUpdate(conn, sql);
    }


    public static List<UserRbacRole> getAll(@NonNull Connection conn,
                                            String dbName) throws SQLException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(dbName));

        return DaoUtils.getList(conn, String.format(_SQL_SELECT_ALL + " ORDER BY user_id", dbName), _LOADER);
    }


    public static void add(@NonNull Connection conn,
                           String dbName,
                           long userId,
                           long roleId,
                           JSONObject params) throws SQLException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(dbName) && userId > 0 && roleId > 0);
        DaoUtils.executeUpdate(conn, String.format(_SQL_INSERT, dbName), userId, roleId, params.toString());
    }

    public static List<UserRbacRole> get(@NonNull Connection conn,
                                         String dbName,
                                         long userId) throws SQLException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(dbName) && userId > 0);

        return DaoUtils.getList(conn, String.format(_SQL_SELECT_BY_USER_ID, dbName), _LOADER, userId);
    }

    public static List<UserRbacRole> get(@NonNull Connection conn,
                                         String dbName,
                                         long userId,
                                         long rbacroleId) throws SQLException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(dbName) && userId > 0);

        return DaoUtils.getList(conn, String.format(_SQL_SELECT_BY_USER_ID_AND_RBACROLE_ID, dbName), _LOADER, userId, rbacroleId);
    }

    public static void deleteByUserId(@NonNull Connection conn,
                                      String dbName,
                                      long userId) throws SQLException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(dbName) && userId > 0);
        DaoUtils.executeUpdate(conn, String.format(_SQL_DELETE_USER_ID, dbName), userId);
    }
}
