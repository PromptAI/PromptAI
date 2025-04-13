package com.zervice.kbase.database.dao;

import com.zervice.common.utils.Constants;
import com.zervice.kbase.database.helpers.CoreDbTable;
import com.zervice.kbase.database.helpers.CustomerDbTable;
import com.zervice.kbase.database.pojo.Configuration;
import com.zervice.kbase.database.utils.DaoUtils;
import com.zervice.kbase.database.utils.RecordLoader;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Manage the configurations table in control db
 *
 * CREATE TABLE %s.configurations (
 "   name VARCHAR(1024) not null primary key,
 "   value TEXT(65535) not null
 " ) engine=innodb
 */
@CoreDbTable
@CustomerDbTable
public class ConfigurationDao {
    public final static String TABLE_NAME = "configurations";

    public static String MODE = "mode";
    public static String DB_VER = "db_ver";
    public static String PARSER_VER = "parser.ver";

    private static String _SQL_GET = "SELECT value FROM %s.configurations WHERE name=? ";
    private static String _SQL_SELECT_ALL = "SELECT name, value FROM %s.configurations ";
    private static String _SQL_SELECT_BY_NAME = "SELECT name, value FROM %s.configurations WHERE name=? ";
    private static String _SQL_PUT = "INSERT INTO %s.configurations (name, value) VALUES (?, ?) ";
    private static String _SQL_UPDATE = "UPDATE %s.configurations SET value=? WHERE name=? ";

    public static void createTable(@NonNull Connection conn, @NonNull String dbName) throws SQLException {
        String sql = String.format(
                "CREATE TABLE IF NOT EXISTS %s.configurations (\n" +
                        "  name VARCHAR(1024) not null primary key,\n" +
                        "  value TEXT(65535) not null\n" +
                        ") engine=innodb ROW_FORMAT=DYNAMIC", dbName
        );
        DaoUtils.executeUpdate(conn, sql);
    }

    private static final RecordLoader<Configuration> _LOADER = rs -> {
        Configuration configuration = Configuration.createConfiguration(
                rs.getString("name"),
                rs.getString("value")
        );
        return configuration;
    };

    public static void removeAll(@NonNull Connection conn, @NonNull String dbName) throws SQLException {
        DaoUtils.executeUpdate(conn, String.format("DELETE FROM %s.configurations", dbName));
    }

    public static List<Configuration> get(@NonNull Connection conn, String dbName) throws SQLException {
        return DaoUtils.getList(conn, String.format(_SQL_SELECT_ALL, dbName), _LOADER);
    }


    public static String getMode() throws SQLException {
        Connection conn = null;
        try {
            conn = DaoUtils.getConnection();

            return getString(conn, Constants.COREDB, MODE, Constants.MODE_STANDALONE);
        }
        finally {
            if (conn != null) {
                DaoUtils.closeQuietly(conn);
            }
        }
    }

    public static int getInt(Connection conn,
                             String dbName,
                             String key,
                             int defVal) throws SQLException {
        String sql = String.format(_SQL_GET, dbName);
        Integer i = DaoUtils.getInt(conn, sql, key);
        return i==null ? defVal : i;
    }


    public static long getLong(Connection conn,
                               String dbName,
                               String key,
                               long defVal) throws SQLException {
        String sql = String.format(_SQL_GET, dbName);
        Long l = DaoUtils.getLong(conn, sql, key);
        return l==null ? defVal : l;
    }

    public static String getString(Connection conn,
                                   String dbName,
                                   String key,
                                   String defVal) throws SQLException {
        String sql = String.format(_SQL_GET, dbName);
        String v = DaoUtils.getString(conn, sql, key);
        return StringUtils.isEmpty(v) ? defVal : v;
    }
    public static boolean getBoolean(Connection conn,
                                   String dbName,
                                   String key,
                                   boolean defVal) throws SQLException {
        String sql = String.format(_SQL_GET, dbName);
        String v = DaoUtils.getString(conn, sql, key);
        if (StringUtils.isBlank(v)) {
            return defVal;
        }

        if ("true".equalsIgnoreCase(v)) {
            return true;
        }

        if ("1".equals(v)) {
            return true;
        }
        return false;
    }

    public static double getDouble(Connection conn,
                                   String dbName,
                                   String key,
                                   double defVal)throws SQLException {
        String sql = String.format(_SQL_GET, dbName);
        String v = DaoUtils.getString(conn, sql, key);

        try {
            if (StringUtils.isEmpty(v)) {
                return defVal;
            }

            return Double.parseDouble(v);
        }
        catch (Exception e) {
            return defVal;
        }
    }

    public static void add(Connection conn, String dbName,
                           Configuration configuration) throws SQLException {
        add(conn, dbName, configuration.getName(), configuration.getValue());
    }
    public static void add(Connection conn,
                           String dbName,
                           String key,
                           Object value) throws SQLException {
        String sql = String.format(_SQL_PUT, dbName);
        DaoUtils.executeUpdate(conn, sql, key, value.toString());
    }

    public static void update(Connection conn,
                              String dbName,
                              String key,
                              Object value) throws SQLException {
        String sql = String.format(_SQL_UPDATE, dbName);
        DaoUtils.executeUpdate(conn, sql, value.toString(), key);
    }

    public static Configuration getByName(Connection conn, String dbName, String key) throws SQLException {
        return DaoUtils.get(conn, String.format(_SQL_SELECT_BY_NAME, dbName), _LOADER, key);
    }
}

