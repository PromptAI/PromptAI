package com.zervice.kbase.database.dao;


import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.zervice.kbase.database.helpers.CustomerDbTable;
import com.zervice.kbase.database.pojo.AccessToken;
import com.zervice.kbase.database.utils.DaoUtils;
import com.zervice.kbase.database.utils.RecordLoader;
import lombok.NonNull;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * create table access_tokens (
 * id bigint not null primary key,
 * userid bigint not null,
 * token varchar(256) not null,
 * status varchar(16) not null,
 * createdon bigint not null
 * ) engine=innodb
 */
@CustomerDbTable
public class AccessTokenDao {
    public static final String TABLE_NAME = "access_tokens";

    private static final String _SQL_INSERT = "INSERT INTO %s." + TABLE_NAME + " (id, userid, token, status, createdon) VALUES (?, ?, ?, ?, ?)";
    private static final String _SQL_SELECT_ALL = "SELECT id, userid, token, status, createdon FROM %s." + TABLE_NAME;
    private static final String _SQL_SELECT_BY_USERID = _SQL_SELECT_ALL + " WHERE userid=?";
    private static final String _SQL_SELECT_BY_ID = _SQL_SELECT_ALL + " WHERE id=?";
    private static final String _SQL_SELECT_ALL_ACTIVE = _SQL_SELECT_ALL + " WHERE status='" + AccessToken.STATUS_ACTIVE + "'";
    private static final String _SQL_UPDATE_STATUS = "UPDATE %s." + TABLE_NAME + " SET status=? WHERE id=?";
    private static final String _SQL_DELETE = "DELETE FROM %s." + TABLE_NAME + " WHERE id=? ";


    private final static RecordLoader<AccessToken> _LOADER = rs -> {
        AccessToken accessToken = new AccessToken();
        accessToken.setId(rs.getLong("id"));
        accessToken.setUserId(rs.getLong("userid"));
        accessToken.setToken(rs.getString("token"));
        accessToken.setStatus(rs.getString("status"));
        accessToken.setCreatedOn(rs.getLong("createdon"));
        return accessToken;
    };

    public static void createTable(@NonNull Connection conn,
                                   String dbName) throws SQLException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(dbName));

        String sql = String.format(
                "create table %s.%s (\n" +
                        " id bigint not null primary key,\n" +
                        " userid bigint not null,\n" +
                        " token varchar(256) not null,\n" +
                        " status varchar(16) not null,\n" +
                        " createdon bigint not null, \n" +
                        " CONSTRAINT `FK_ACCESS_TOKEN_USER_ID` FOREIGN KEY (`userid`) REFERENCES `" + UserDao.TABLE_NAME + "` (`id`) ON DELETE CASCADE\n" +
                        " ) engine=innodb ROW_FORMAT=DYNAMIC ", dbName, TABLE_NAME
        );
        DaoUtils.executeUpdate(conn, sql);
    }

    public static void add(@NonNull Connection conn, String dbName, @NonNull AccessToken accessToken) throws SQLException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(dbName));

        DaoUtils.executeUpdate(conn, String.format(_SQL_INSERT, dbName), accessToken.getId(),
                accessToken.getUserId(), accessToken.getToken(), accessToken.getStatus(), accessToken.getCreatedOn());
    }


    public static AccessToken get(@NonNull Connection conn,
                                  String dbName,
                                  long id) throws SQLException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(dbName) && id > 0);

        return DaoUtils.get(conn, String.format(_SQL_SELECT_BY_ID, dbName), _LOADER, id);
    }


    public static List<AccessToken> getAll(@NonNull Connection conn,
                                           String dbName) throws SQLException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(dbName));

        return DaoUtils.getList(conn, String.format(_SQL_SELECT_ALL, dbName), _LOADER);
    }

    public static List<AccessToken> getAllActive(@NonNull Connection conn,
                                                 String dbName) throws SQLException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(dbName));

        return DaoUtils.getList(conn, String.format(_SQL_SELECT_ALL_ACTIVE, dbName), _LOADER);
    }

    public static List<AccessToken> getAllByUserId(@NonNull Connection conn,
                                                   String dbName, long userId) throws SQLException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(dbName) && userId > 0);

        return DaoUtils.getList(conn, String.format(_SQL_SELECT_BY_USERID, dbName), _LOADER, userId);
    }

    public static void updateStatus(@NonNull Connection conn, String dbName, @NonNull AccessToken accessToken) throws SQLException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(dbName));

        DaoUtils.executeUpdate(conn, String.format(_SQL_UPDATE_STATUS, dbName), accessToken.getStatus(), accessToken.getId());
    }

    public static int delete(@NonNull Connection conn,
                             String dbName,
                             long id) throws SQLException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(dbName) && id > 0);
        return DaoUtils.executeUpdate(conn, String.format(_SQL_DELETE, dbName), id);
    }
}
