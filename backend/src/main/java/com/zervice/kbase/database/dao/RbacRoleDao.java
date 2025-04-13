package com.zervice.kbase.database.dao;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.zervice.kbase.database.helpers.CustomerDbTable;
import com.zervice.kbase.database.pojo.RbacRole;
import com.zervice.kbase.database.utils.DaoUtils;
import com.zervice.kbase.database.utils.RecordLoader;
import lombok.NonNull;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@CustomerDbTable
public class RbacRoleDao {
    public static final String TABLE_NAME = "rbacroles";

    private static final String _SQL_INSERT = "INSERT INTO %s." + TABLE_NAME + " (name, display, status, properties) VALUES (?, ?, ?,?) ";
    private static final String _SQL_INSERT_WITH_ID = "INSERT INTO %s." + TABLE_NAME + " (id, name, display, status, properties) VALUES (?, ?, ?, ?,?) ";
    private static final String _SQL_SELECT = "SELECT id, name, display, status, properties FROM %s." + TABLE_NAME;
    private static final String _SQL_SELECT_BY_NAME = "SELECT id, name, display, status, properties FROM %s." + TABLE_NAME + " where name=? ";
    private static final String _SQL_SELECT_BY_ID = "SELECT id, name, display, status, properties FROM %s." + TABLE_NAME + " where id=? ";
    private static final String _SQL_DELETE = "DELETE FROM %s." + TABLE_NAME + " WHERE id=? ";
    private static final String _SQL_UPDATE_NAME_PROPERTIES = "UPDATE %s." + TABLE_NAME +
            " SET name=?, properties=? WHERE id=?";
    private static final String _SQL_UPDATE = "UPDATE %s." + TABLE_NAME +
            " SET name=?, display=?, status=?, properties=? WHERE id=?";

    private final static RecordLoader<RbacRole> _LOADER = rs ->
            RbacRole.createRbacRoleFromDao(rs.getLong(1),
                    rs.getString(2),
                    rs.getString(3),
                    rs.getBoolean(4),
                    rs.getString(5));

    public static void createTable(@NonNull Connection conn,
                                   String dbName) throws SQLException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(dbName));

        String sql = String.format(
                "create table %s." + TABLE_NAME + " (\n" +
                        "          id bigint not null primary key auto_increment,\n" +
                        "          name varchar(256) not null unique,\n" +
                        "          display varchar(256) not null,\n" +
                        "          status tinyint(1) not null,\n" +
                        "          properties text(65535) not null\n" +
                        "         ) engine=innodb ROW_FORMAT=DYNAMIC ", dbName
        );
        DaoUtils.executeUpdate(conn, sql);
    }

    public static List<RbacRole> getAll(@NonNull Connection conn,
                                        String dbName) throws SQLException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(dbName));

        return DaoUtils.getList(conn, String.format(_SQL_SELECT, dbName), _LOADER);
    }

    public static RbacRole get(@NonNull Connection conn,
                               String dbName,
                               String roleName) throws SQLException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(dbName) && !Strings.isNullOrEmpty(roleName));
        return DaoUtils.get(conn, String.format(_SQL_SELECT_BY_NAME, dbName), _LOADER, roleName);
    }

    public static RbacRole get(@NonNull Connection conn,
                               String dbName,
                               long id) throws SQLException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(dbName) && id > 0);

        return DaoUtils.get(conn, String.format(_SQL_SELECT_BY_ID, dbName), _LOADER, id);
    }

    public static List<RbacRole> getNameLikeIdIn(@NonNull Connection conn,
                                                 String dbName,
                                                 @NonNull Set<String> names,
                                                 @NonNull Set<Long> ids) throws SQLException {
        StringBuilder sql = new StringBuilder(_SQL_SELECT).append(" where 1=1");
        if (!names.isEmpty()) {
            sql.append(" and (");
            for (String name : names) {
                sql.append(" name like '%%").append(name).append("%%' or ");
            }
            sql.replace(sql.lastIndexOf("or"), sql.length(), ")");
        }

        if (!ids.isEmpty()) {
            sql.append(" or id in ").append(ids.stream().map(Objects::toString).collect(Collectors.joining(",", "(", ")")));
        }
        return DaoUtils.getList(conn, String.format(sql.toString(), dbName), _LOADER);
    }

    public static List<RbacRole> getNameLikeById(@NonNull Connection conn,
                                                 String dbName,
                                                 @NonNull Set<String> names,
                                                long id) throws SQLException {
        StringBuilder sql = new StringBuilder(_SQL_SELECT).append(" where 1=1");
        if (!names.isEmpty()) {
            sql.append(" and (");
            for (String name : names) {
                sql.append(" name like '%%").append(name).append("%%' or ");
            }
            sql.replace(sql.lastIndexOf("or"), sql.length(), ")");
        }
        sql.append(" and id=").append(id);
        return DaoUtils.getList(conn, String.format(sql.toString(), dbName), _LOADER);
    }

    public static void add(@NonNull Connection conn,
                           String dbName,
                           RbacRole role) throws SQLException {
        DaoUtils.executeUpdate(conn, String.format(_SQL_INSERT, dbName), role.getName(),role.getDisplay(), role.getStatus(),
                JSON.toJSONString(role.getProperties()));
    }
    public static void addWithId(@NonNull Connection conn,
                           String dbName,
                           RbacRole role) throws SQLException {
        DaoUtils.executeUpdate(conn, String.format(_SQL_INSERT_WITH_ID, dbName),role.getId(), role.getName(), role.getDisplay(), role.getStatus(),
                JSON.toJSONString(role.getProperties()));
    }

    public static long addReturnId(@NonNull Connection conn,
                                   String dbName,
                                   RbacRole role) throws SQLException {
        return DaoUtils.executeUpdateWithLastInsertId(conn, String.format(_SQL_INSERT, dbName), role.getName(),role.getDisplay(), role.getStatus(),
                JSON.toJSONString(role.getProperties()));
    }

    public static void update(@NonNull Connection conn,
                                            String dbName,
                                            @NonNull RbacRole rbacRole) throws SQLException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(dbName));

        String prop = rbacRole.getProperties() == null ? "{}" : JSON.toJSONString(rbacRole.getProperties());

        DaoUtils.executeUpdate(conn, String.format(_SQL_UPDATE, dbName),
                rbacRole.getName(), rbacRole.getDisplay(), rbacRole.getStatus(), prop, rbacRole.getId());

    }

    public static void delete(@NonNull Connection conn,
                              String dbName,
                              long rbacRoleId) throws SQLException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(dbName) && rbacRoleId > 0);
        DaoUtils.executeUpdate(conn, String.format(_SQL_DELETE, dbName), rbacRoleId);
    }

    public static void updateNameProperties(@NonNull Connection conn,
                                            String dbName,
                                            @NonNull RbacRole rbacRole) throws SQLException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(dbName));

        DaoUtils.executeUpdate(conn, String.format(_SQL_UPDATE_NAME_PROPERTIES, dbName),
                rbacRole.getName(), JSON.toJSONString(rbacRole.getProperties()), rbacRole.getId());
    }
}


