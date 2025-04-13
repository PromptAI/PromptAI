package com.zervice.kbase.database.dao;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.zervice.kbase.database.helpers.CustomerDbTable;
import com.zervice.kbase.database.pojo.User;
import com.zervice.kbase.database.utils.DaoUtils;
import com.zervice.kbase.database.utils.RecordLoader;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static com.alibaba.fastjson.serializer.SerializerFeature.WriteMapNullValue;

@Log4j2
@CustomerDbTable()
public class UserDao {
    public static final String TABLE_NAME = "users";


    private static final String _SQL_INSERT = "INSERT INTO %s." + TABLE_NAME +
            " (username, email, mobile, status, lastAccessEpochMs, properties) VALUES (?, ?, ?, ?, ?, ?)";

    private static final String _SQL_SELECT_BY_USERNAME = "SELECT id, username, email, mobile, status, lastAccessEpochMs, properties FROM %s." + TABLE_NAME + " where username=?";
    private static final String _SQL_SELECT_BY_ID = "SELECT id, username, email, mobile, status, lastAccessEpochMs, properties FROM %s." + TABLE_NAME + " where id=?";
    private static final String _SQL_SELECT_ALL = "SELECT id, username, email, mobile, status, lastAccessEpochMs, properties FROM %s." + TABLE_NAME;
    private static final String _SQL_UPDATE_PROPS_AND_NAME = "UPDATE %s." + TABLE_NAME + " SET properties=?, username=? WHERE id=?";
    private static final String _SQL_UPDATE_PROPS = "UPDATE %s." + TABLE_NAME + " SET properties=? WHERE id=?";
    private static final String _SQL_UPDATE_STATUS = "UPDATE %s." + TABLE_NAME + " SET status=? WHERE username=?";
    private static final String _SQL_UPDATE_LAST_ACCESS = "UPDATE %s." + TABLE_NAME + " SET lastAccessEpochMs=? WHERE username=?";
    private static final String _SQL_UPDATE_ALL = "UPDATE %s." + TABLE_NAME + " SET username=?, email=?, mobile=?, status=?, lastAccessEpochMs=?, properties=? WHERE id=?";
    private static final String _SQL_DELETE = "DELETE FROM %s." + TABLE_NAME + " WHERE id=? ";

    private final static RecordLoader<User> _LOADER = rs ->
            User.createUserFromDao(rs.getLong("id"),
                    rs.getString("username"),
                    rs.getString("email"),
                    rs.getString("mobile"),
                    rs.getBoolean("status"),
                    rs.getLong("lastAccessEpochMs"),
                    rs.getString("properties"));

    public static void createTable(@NonNull Connection conn,
                                   String dbName) throws SQLException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(dbName));

        String sql = String.format(
                "create table %s." + TABLE_NAME + " (\n" +
                        " id bigint not null primary key auto_increment,\n" +
                        " username varchar(256) not null,\n" +
                        " email varchar(512) unique default null,\n" +
                        " mobile varchar(64) unique default null,\n" +
                        " status tinyint(1) not null,\n" +
                        " lastAccessEpochMs bigint not null,\n" +
                        " properties text(65535)\n" +
                        " ) engine=innodb ROW_FORMAT=DYNAMIC ", dbName, dbName, dbName
        );
        DaoUtils.executeUpdate(conn, sql);
    }

    public static void add(@NonNull Connection conn, String dbName, @NonNull User user) throws SQLException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(dbName));

        DaoUtils.executeUpdate(conn, String.format(_SQL_INSERT, dbName), user.getUsername(), user.getStatus(), user.getLastAccessEpochMs(),
                JSON.toJSONString(user.getProperties(), WriteMapNullValue));
    }

    public static long addReturnId(@NonNull Connection conn, String dbName, @NonNull User user) throws SQLException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(dbName));
        return DaoUtils.executeUpdateWithLastInsertId(conn, String.format(_SQL_INSERT, dbName), user.getUsername(),
                user.getEmail(), user.getMobile(), user.getStatus(), user.getLastAccessEpochMs(),
                JSON.toJSONString(user.getProperties(), WriteMapNullValue));
    }

    public static User get(@NonNull Connection conn,
                           String dbName,
                           String username) throws SQLException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(dbName));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(username));

        return DaoUtils.get(conn, String.format(_SQL_SELECT_BY_USERNAME, dbName), _LOADER, username);
    }

    public static User get(@NonNull Connection conn,
                           String dbName,
                           long id) throws SQLException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(dbName) && id > 0);

        return DaoUtils.get(conn, String.format(_SQL_SELECT_BY_ID, dbName), _LOADER, id);
    }

    public static List<User> getAll(@NonNull Connection conn,
                                    String dbName) throws SQLException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(dbName));

        return DaoUtils.getList(conn, String.format(_SQL_SELECT_ALL, dbName), _LOADER);
    }

    public static void updatePropertiesAndName(@NonNull Connection conn, String dbName, @NonNull User user) throws SQLException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(dbName));

        DaoUtils.executeUpdate(conn, String.format(_SQL_UPDATE_PROPS_AND_NAME, dbName), JSON.toJSONString(user.getProperties()), user.getUsername(), user.getId());
    }

    public static void updateProperties(@NonNull Connection conn, String dbName, @NonNull User user) throws SQLException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(dbName));

        DaoUtils.executeUpdate(conn, String.format(_SQL_UPDATE_PROPS, dbName), JSON.toJSONString(user.getProperties()), user.getId());
    }

    public static void updateStatus(@NonNull Connection conn, String dbName, @NonNull User user) throws SQLException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(dbName));

        DaoUtils.executeUpdate(conn, String.format(_SQL_UPDATE_STATUS, dbName), user.getStatus(), user.getUsername());
    }

    public static void updateLastAccess(@NonNull Connection conn, String dbName, @NonNull User user) throws SQLException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(dbName));

        DaoUtils.executeUpdate(conn, String.format(_SQL_UPDATE_LAST_ACCESS, dbName), user.getLastAccessEpochMs(), user.getUsername());
    }

    public static void update(@NonNull Connection conn, String dbName, @NonNull User user) throws SQLException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(dbName));
        String prop = user.getProperties() == null ? "{}" : JSONObject.toJSONString(user.getProperties());

        DaoUtils.executeUpdate(conn, String.format(_SQL_UPDATE_ALL, dbName), user.getUsername(), user.getEmail(), user.getMobile(),
                user.getStatus(), user.getLastAccessEpochMs(), prop, user.getId());
    }

    public static void updatePassword(@NonNull Connection conn,
                                      String dbName,
                                      User user) throws SQLException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(dbName));
        String prop = JSONObject.toJSONString(user.getProperties());

        DaoUtils.executeUpdate(conn, String.format(_SQL_UPDATE_PROPS, dbName), prop, user.getId());
    }


    public static void delete(@NonNull Connection conn,
                              String dbName,
                              long userId) throws SQLException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(dbName) && userId > 0);
        DaoUtils.executeUpdate(conn, String.format(_SQL_DELETE, dbName), userId);
    }


}
