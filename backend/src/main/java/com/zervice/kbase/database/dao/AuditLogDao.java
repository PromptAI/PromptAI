package com.zervice.kbase.database.dao;

import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Preconditions;
import com.zervice.kbase.api.restful.pojo.PageRequest;
import com.zervice.kbase.database.helpers.Auditable;
import com.zervice.kbase.database.helpers.CoreDbTable;
import com.zervice.kbase.database.helpers.CustomerDbTable;
import com.zervice.kbase.database.pojo.AuditLog;
import com.zervice.kbase.database.utils.DaoUtils;
import com.zervice.kbase.database.utils.RecordLoader;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Access accountXXX.audit_logs table
 *
 * CREATE TABLE %s.audit_logs (
 *   id BIGINT not null primary key auto-increment,
 *   user VARCHAR(128) not null,
 *   access VARCHAR(32) not null,
 *   table VARCHAR(32) NOT NULL,
 *   query VARCHAR(1024) NOT NULL,
 *   happenedOn TIMESTAMP not null default CURRENT_TIMESTAMP,
 *   message VARCHAR(2048) not null,
 *   properties JSON not null
 " ) engine=innodb
 */
@CustomerDbTable
@CoreDbTable
@Auditable(disabled = true)
public class AuditLogDao {
    public final static String TABLE_NAME = "audit_logs";
    private final static String ORDER_BY = " ORDER BY happenedOn DESC";
    private final static String SQL_BASE = "SELECT id, user, access, tableName, query, message, ip, time, properties FROM %s.audit_logs";
    private final static String SQL_GETALL = SQL_BASE + ORDER_BY;
    private final static String SQL_GET_BY_ID = SQL_BASE + " WHERE id=? " + ORDER_BY;
    private final static String SQL_GET_BY_USER = SQL_BASE + " WHERE user=? " + ORDER_BY;
    private final static String SQL_GET_BY_TABLE = SQL_BASE + " WHERE tableName=? " + ORDER_BY;
    private final static String SQL_GET_BY_ACCESS = SQL_BASE + " WHERE access=? " + ORDER_BY;

    private final static String SQL_INSERT = "INSERT INTO %s.audit_logs (user, access, tableName, query, message, ip, time, properties) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    private final static String SQL_DELETE = "DELETE FROM %s.audit_logs WHERE id=?";
    private final static String SQL_UPDATE_PROPERTIES = "UPDATE %s.audit_logs set properties=? WHERE id=?";

    /*
     * createTable must have dbName parameter although we didn't use it
     * Because we will use reflect to call createTable when create db
     */
    public static void createTable(@NonNull Connection conn, String dbName) throws SQLException {
        String sql = String.format(
                "CREATE TABLE IF NOT EXISTS %s.audit_logs (\n" +
                        "         id BIGINT not null primary key auto_increment,\n" +
                        "         user VARCHAR(128) not null,\n" +
                        "         access VARCHAR(32) not null,\n" +
                        "         tableName VARCHAR(32) not null,\n" +
                        "         query VARCHAR(1024) not null,\n" +
                        "         message VARCHAR(2048) not null,\n" +
                        "         ip varchar(50) not null, \n" +
                        "         time bigint not null, \n" +
                        "         properties JSON not null, \n" +
                        "         index ipTimeIndex(ip,time)\n" +
                        "        ) engine=innodb ROW_FORMAT=DYNAMIC", dbName
        );

        DaoUtils.executeUpdate(conn, sql);
    }


    private final static RecordLoader<AuditLog> _LOADER = rs -> {
        AuditLog auditLog = new AuditLog();
        auditLog.setId(rs.getLong("id"));
        auditLog.setUser(rs.getString("user"));
        auditLog.setAccess(rs.getString("access"));
        auditLog.setTable(rs.getString("tableName"));
        auditLog.setSql(rs.getString("query"));
        auditLog.setMessage(rs.getString("message"));
        auditLog.setIp(rs.getString("ip"));
        auditLog.setTime(rs.getLong("time"));

        JSONObject jo = JSONObject.parseObject(rs.getString("properties"));
        auditLog.setProperties(jo);
        return auditLog;
    };

    public static int count(@NonNull Connection conn, String dbName, String table, String user, String access) throws SQLException {
        String countSql = String.format("SELECT COUNT(*) FROM %s.%s", dbName, TABLE_NAME);

        String sql = _buildFilters(countSql, table, user, access);

        return DaoUtils.getInt(conn, sql, table, user, access);
    }

    public static List<AuditLog> page(@NonNull Connection conn, String dbName, String table, String user, String access, PageRequest pageRequest) throws SQLException {
        String base = String.format(SQL_BASE, dbName);

        // WHERE
        String sql = _buildFilters(base, table, user, access);

        // ORDER BY
        sql += ORDER_BY;

        // LIMIT
        if (pageRequest.getSize() > 0) {
            int start = pageRequest.getSize() * pageRequest.getPage();
            sql += String.format(" LIMIT %d,%d", start, start + pageRequest.getSize());
        } else {
            // let's add a hard limit, this might be two huge ...
            sql += " LIMIT 500";
        }

        return DaoUtils.getList(conn, sql, _LOADER::load, table, user, access);
    }

    public static List<AuditLog> getAll(@NonNull Connection conn, String dbName) throws SQLException {
        return DaoUtils.getList(conn,
                String.format(SQL_GETALL, dbName),
                _LOADER::load);
    }

    public static void add(@NonNull Connection conn, String dbName, String user, String access,
                           String table, String sql, String ip,long time, String message) throws SQLException {
        add(conn, dbName, user, access, table, sql, message, ip, time, new JSONObject());
    }

    public static void add(@NonNull Connection conn, String dbName, String user, String access,
                           String table, String sql, String message, String ip, long time, JSONObject props) throws SQLException {
        if (!DaoUtils.tableExists(conn, dbName, TABLE_NAME)) {
            return;
        }
        DaoUtils.executeUpdate(conn, String.format(SQL_INSERT, dbName), user, access, table, sql, message, ip, time, props.toString());
    }

    public static void updateProperties(@NonNull Connection conn, String dbName, long id, @NonNull JSONObject props) throws SQLException {
        DaoUtils.executeUpdate(conn, String.format(SQL_UPDATE_PROPERTIES, dbName), props.toString(), id);
    }

    public static AuditLog get(@NonNull Connection conn, String dbName, long id) throws SQLException {
        Preconditions.checkArgument(id > 0);
        return DaoUtils.get(conn, String.format(SQL_GET_BY_ID, dbName), _LOADER::load, id);
    }

    public static List<AuditLog> getByUser(@NonNull Connection conn, String dbName, @NonNull String name) throws SQLException {
        return DaoUtils.getList(conn, String.format(SQL_GET_BY_USER, dbName), _LOADER::load, name);
    }

    public static List<AuditLog> getByTable(@NonNull Connection conn, String dbName, @NonNull String name) throws SQLException {
        return DaoUtils.getList(conn, String.format(SQL_GET_BY_TABLE, dbName), _LOADER::load, name);
    }

    public static List<AuditLog> getByAccess(@NonNull Connection conn, String dbName, @NonNull String name) throws SQLException {
        return DaoUtils.getList(conn, String.format(SQL_GET_BY_ACCESS, dbName), _LOADER::load, name);
    }

    public static Long countByIdAndTimeGt(Connection conn, String dbName,
                                          String ip, long time, String tableName, String access) throws SQLException {
        String sql = "select count(id) from %s." + TABLE_NAME + " where ip=? and time >= ? and tableName=? and access= ?";
        return DaoUtils.getLong(conn, String.format(sql, dbName), ip, time, tableName, access);
    }

    private static String _buildFilters(String baseSql, String table, String user, String access) {
        if (!StringUtils.isEmpty(table)) {
            baseSql += " WHERE tableName=?";
        } else {
            baseSql += " WHERE ''=?";
        }

        if (!StringUtils.isEmpty(user)) {
            baseSql += " AND user=?";
        } else {
            baseSql += " AND ''=?";
        }

        if (!StringUtils.isEmpty(access)) {
            baseSql += " AND access=?";
        } else {
            baseSql += " AND ''=?";
        }

        return baseSql;
    }
}
