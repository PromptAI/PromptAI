package com.zervice.kbase.database.utils;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.util.concurrent.Uninterruptibles;
import com.zervice.common.utils.Constants;
import com.zervice.common.utils.ServletUtils;
import com.zervice.kbase.Environment;
import com.zervice.kbase.ZBotContext;
import com.zervice.kbase.database.dao.AuditLogDao;
import com.zervice.kbase.database.helpers.Auditable;
import com.zervice.kbase.database.pojo.AuditLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import javax.sql.DataSource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An utility class implement DAO functions
 */
@Component
@Log4j2
public class DaoUtils {
    @Builder
    @ToString
    @AllArgsConstructor
    static class AuditOption {
        public final String table;
        public final boolean create;
        public final boolean read;
        public final boolean update;
        public final boolean delete;

        public AuditOption(String table) {
            this.table = table;
            create = read = update = delete = false;
        }
    }

    private static final Map<String /*class name*/, AuditOption> _audits = new ConcurrentHashMap<>();

    public static void audit(Class clazz, Auditable auditable) throws Exception {
        String className = clazz.getName();

        // each Dao class shall define this!!!
        String table;
        try {
            Field field = clazz.getDeclaredField("TABLE_NAME");
            field.setAccessible(true);
            table = (String) field.get(clazz);
        }
        catch(Exception ne) {
            String simpleName = clazz.getSimpleName();

            // get rid of last 'Dao'
            table = "DAO:" + simpleName.substring(0, simpleName.length() - 3).toLowerCase();
        }

        if(auditable.disabled()) {
            LOG.warn(("NOT auditing table [" + table + "] with [" + className + "]"));
            return;
        }

        AuditOption option = AuditOption.builder()
                .table(table)
                .create(auditable.create())
                .read(auditable.read())
                .update(auditable.update())
                .delete(auditable.delete())
                .build();

        LOG.debug("Auditing table [" + table + "] with [" + className + "]: " + option.toString());

        _audits.put(className, option);
    }

    public static String CREATE_DATABASE_SQL = "CREATE DATABASE IF NOT EXISTS %s DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci";

    public final static int ERROR_MYSQL_DUPLICATE_KEY = 1062;
    /**
     * The class we used for DB operations, we use reflection to overcome some problems ...
     * @See DBOperator
     */
    public static final Class CONN_CLASS = Connection.class;
    public static final Class STMT_CLASS = PreparedStatement.class;

    private static DataSource _connPool = null;

    @Qualifier("kbdatasource")
    @Autowired
    public void setConnPool(DataSource connPool) {
        DaoUtils._connPool = connPool;
    }

    public static void waitForReady(long timeoutInMs) throws SQLException{
        long startEpochInMs = System.currentTimeMillis();
        while (true) {
            if (System.currentTimeMillis() - startEpochInMs >= timeoutInMs) {
                throw new SQLException("Timed out to wait for MySQL be ready - " + timeoutInMs);
            }

            Connection conn = null;
            try {
                conn = getConnection();
                if (Environment.isH2()) {
                    // for h2 not check because sql not same
                    break;
                } else {
                    Long upTimeInSeconds = get(conn, "show global status like 'uptime'", rs -> rs.getLong(2));
                    if (null != upTimeInSeconds) {
                        LOG.info("MySQL uptime - {} seconds", upTimeInSeconds);
                        break;
                    }
                }
            }
            catch (Exception e) {
                LOG.warn("Init db connection failed, sleep and retry ...", e);
                Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);
            }
            finally {
                closeQuietly(conn);
            }
        }
    }

    public static void closeQuietly(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            }
            catch (SQLException ex) {}
        }
    }
    public static void closeQuietly(ManagedResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            }
            catch (SQLException ex) {}
        }
    }


    public static void rollbackQuietly(final Connection conn) {
        if (conn != null) {
            try {
                if (!conn.getAutoCommit() && !conn.isReadOnly()) {
                    conn.rollback();
                }
            }
            catch (SQLException e) {
                // silently swallow it
            }
        }
    }


    public static void closeQuietly(final ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            }
            catch (SQLException e) {
                // silently swallow it
            }
        }
    }


    public static void closeQuietly(final Connection conn, final ResultSet rs) {
        closeQuietly(rs);
        closeQuietly(conn);
    }


    public static void closeQuietly(final ResultSet rs, final Statement stmt) {
        closeQuietly(rs);
        closeQuietly(stmt);
    }


    public static void closeQuietly(final Statement stmt) {
        if (stmt != null) {
            try {
                stmt.close();
            }
            catch (SQLException e) {
                // silently swallow it
            }
        }
    }


    public static Connection getConnection() throws SQLException {
        // return _connPool.getConnection();

        // Use our managed version to get more insights
        return new ManagedConnection(_connPool.getConnection());
    }

    public static Connection getConnection(boolean autoCommit) throws SQLException {
        Connection conn = getConnection();
        if (conn.getAutoCommit() != autoCommit) {
            conn.setAutoCommit(autoCommit);
        }
        return conn;
    }


    public static Connection getConnection(String driver,
                                           String url,
                                           String username,
                                           String password) throws SQLException, ClassNotFoundException {
        Class.forName(driver);
        return DriverManager.getConnection(url, username, password);
    }

    /**
     * Check if the given database exists
     */
    public static boolean dbExists(final Connection conn, final String dbName) throws SQLException {
        Preconditions.checkNotNull(conn);
        Preconditions.checkNotNull(dbName);

        ResultSet rs = conn.getMetaData().getCatalogs();
        try {
            while (rs.next()) {
                String name = rs.getString(1);
                if (name.equals(dbName)) {
                    return true;
                }
            }

            return false;
        }
        finally {
            closeQuietly(rs);
        }
    }


    public static boolean tableExists(Connection conn, String dbName, String tableName) throws SQLException {
        Preconditions.checkNotNull(conn);
        Preconditions.checkNotNull(dbName);

        String[] types = {"TABLE"};
        ResultSet rs = null;
        try {
            rs = conn.getMetaData().getTables(dbName, null, tableName, types);
            if(rs.next()) {
                return true;
            }
            else {
                return false;
            }
        }
        finally {
            closeQuietly(rs);
        }
    }

    public static boolean columnExists(Connection conn, String dbName, String tableName, String column) throws SQLException{
        Preconditions.checkNotNull(conn);
        Preconditions.checkNotNull(dbName);

        ResultSet rs = null;
        try {
            rs = conn.getMetaData().getColumns(dbName, null, tableName, column);
            return rs.next();
        }
        finally {
            closeQuietly(rs);
        }
    }

    public static boolean indexExists(Connection conn, String dbName, String tableName, String indexName) throws SQLException {
        Preconditions.checkNotNull(conn);
        Preconditions.checkNotNull(dbName);
        Preconditions.checkNotNull(tableName);

        ResultSet rs = null;
        boolean exitIndex = false;
        try {
            rs = conn.getMetaData().getIndexInfo(null, dbName, tableName, false, true);
            while (rs.next()) {
                if (indexName.equals(rs.getString("INDEX_NAME"))) {
                    exitIndex = true;
                }
            }
        } finally {
            closeQuietly(rs);
        }
        return exitIndex;
    }

    /**
     * Create a database using innodb as the engine, and UTF-8 as charset and collation
     */
    public static void createDb(final Connection conn,
                                final String dbName) throws SQLException {
        Preconditions.checkNotNull(conn);
        Preconditions.checkNotNull(dbName);

        // create the control db
        LOG.info("Will create a database {}", dbName);
        String sql = String.format(CREATE_DATABASE_SQL, dbName);
        executeUpdate(conn, sql);

        _audit(conn, dbName, AuditLog.ACCESS_CREATE, "", "Database created");

        LOG.info("Create a database {} success", dbName);
    }


    public static void dropDb(@NonNull Connection conn,
                              String dbName) throws SQLException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(dbName));

        LOG.info("Drop a database {}", dbName);
        String sql = String.format(
                "DROP DATABASE IF EXISTS %s", dbName
        );

        _audit(conn, dbName, AuditLog.ACCESS_DELETE, "", "Database deleted");

        executeUpdate(conn, sql);
    }


    public static void dropDbQuietly(@NonNull Connection conn,
                                     String dbName) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(dbName));
        try {
            dropDb(conn, dbName);
        }
        catch (SQLException ignore) {
            // dumb
        }
    }


    public static void dropTable(@NonNull Connection conn,
                                 String dbName,
                                 String tableName) throws SQLException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(dbName) &&
                !Strings.isNullOrEmpty(tableName));

        LOG.info("Drop a table {} from database {}", tableName, dbName);
        String sql = String.format("DROP TABLE %s.%s", dbName, tableName);
        executeUpdate(conn, sql);

        _audit(conn, dbName, AuditLog.ACCESS_DELETE, tableName, "Table deleted");
    }


    public static void dropTableQuietly(@NonNull Connection conn,
                                        String dbName,
                                        String tableName) throws SQLException {
        try {
            dropTable(conn, dbName, tableName);
        }
        catch (Exception e) {
            // ignore
        }
    }


    public static <T> List<T> getList(Connection conn,
                                      String sql,
                                      RecordLoader<T> rLoader,
                                      Object... params) throws SQLException {
        DBQuery query = new DBQuery(conn, sql, params);

        try {
            List<T>  list = query.queryList(rLoader);

            return list;
        }
        finally {
            query.close();
        }
    }

    public static <T> T get(Connection conn,
                            String sql,
                            RecordLoader<T> rLoader,
                            Object... params) throws SQLException {
        DBQuery query = new DBQuery(conn, sql, params);

        try {
            T t = query.query(rLoader);
            return t;
        }
        finally {
            query.close();
        }

    }


    private final static RecordLoader<Integer> _INT_LOADER = rs -> rs.getInt(1);


    public static Integer getInt(Connection conn,
                                 String sql,
                                 Object... params) throws SQLException {
        return get(conn, sql, _INT_LOADER::load, params);
    }


    public static List<Integer> getIntList(@NonNull Connection conn,
                                           String sql,
                                           Object... params) throws SQLException {
        return getList(conn, sql, _INT_LOADER::load, params);
    }


    private final static RecordLoader<Long> _LONG_LOADER = rs -> rs.getLong(1);


    public static Long getLong(Connection conn,
                               String sql,
                               Object... params) throws SQLException {
        return get(conn, sql, _LONG_LOADER::load, params);
    }


    public static List<Long> getLongList(Connection conn,
                                         String sql,
                                         Object... params) throws SQLException {
        return getList(conn, sql, _LONG_LOADER::load, params);
    }


    private final static RecordLoader<String> _STRING_LOADER = rs -> rs.getString(1);

    public static String getString(Connection conn, String sql, Object... params) throws SQLException {
        return get(conn, sql, _STRING_LOADER::load, params);
    }


    public static <K, V> Map<K, V> getMap(Connection conn,
                                          String sql,
                                          RecordLoader<Pair<K, V>> loader,
                                          Object... params)
            throws SQLException {
        DBQuery query = new DBQuery(conn, sql, params);

        try {
            return query.queryMap(loader);
        }
        finally {
            query.close();
        }

    }



    private final static RecordLoader<Pair<Long, Long>> _LONG_LONG_LOADER = rs -> Pair.of(rs.getLong(1), rs.getLong(2));

    public static Map<Long, Long> getMapOfLongToLong(Connection conn,
                                                     String sql,
                                                     Object... params) throws SQLException {
        return getMap(conn, sql, _LONG_LONG_LOADER::load, params);
    }


    public static int executeUpdate(Connection conn,
                                    String sql,
                                    Object... params) throws SQLException {
        DBUpdate update = new DBUpdate(conn, sql, params);

        try {
            int ret = update.update();
            _audit(conn, sql, true, "", params);
            return ret;
        }
        finally {
            update.close();
        }


    }


    public static long executeUpdateWithLastInsertId(Connection conn,
                                                     String sql,
                                                     Object... params) throws SQLException {
        DBInsert insert = new DBInsert(conn, sql, params);

        try {
            long id = insert.insert();
            _audit(conn, sql, true, "", params);
            return id;
        }
        finally {
            insert.close();
        }

    }


    public static byte[] readBlobAsByteArray(@NonNull ResultSet rs,
                                             int columnIdx) throws SQLException {
        try {
            InputStream s = rs.getBinaryStream(columnIdx);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int nread = 0;
            while ((nread = s.read(buffer)) > 0) {
                bos.write(buffer, 0, nread);
            }

            return bos.toByteArray();
        }
        catch (IOException e) {
            throw new SQLException(e);
        }
    }


    public static boolean getBoolean(ResultSet rs,
                                     int columnIdx) throws SQLException {
        return rs.getString(columnIdx).equalsIgnoreCase("y");
    }


    public static List<String> getTableList(@NonNull Connection conn,
                                            String dbName,
                                            String tableNamePattern) throws SQLException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(dbName) && !Strings.isNullOrEmpty(tableNamePattern));
        String[] types = {"TABLE"};
        List<String> tables = new ArrayList<>();
        ResultSet rs = null;
        try {
            rs = conn.getMetaData().getTables(dbName, null, tableNamePattern, types);
            while (rs.next()) {
                tables.add(rs.getString(3));
            }
            return tables;
        }
        finally {
            closeQuietly(rs);
        }
    }

    public static String getCallerClassName(Class ignoreClazz) {
        StackTraceElement[] stElements = Thread.currentThread().getStackTrace();
        for (int i=1; i<stElements.length; i++) {
            StackTraceElement ste = stElements[i];
            if (!ste.getClassName().equals(ignoreClazz.getName()) && ste.getClassName().indexOf("java.lang.Thread")!=0) {
                return ste.getClassName();
            }
        }
        return null;
    }

    public static void recordAuditLog(Connection conn, String dbName, String user,
                                      String access, String table, String sql, String message) {
        String ip = ServletUtils.getCurrentNoProxyIpWithoutError();
        try {
            AuditLogDao.add(conn, dbName, user, access, table, sql, ip, System.currentTimeMillis(), message);
        }
        catch (Exception e) {
            // for now, let's silently fail ...
            LOG.error("Failed to save audit record for user [ " + user + "] perform [" + access + "] operation on [" + dbName + "." + table + "] (sql = \"" + sql + "\", message=" + message + ")", e);
        }
    }

    /**
     * Tricks here!!!!
     *
     * Currently, we are kind of "hard-coding" dbname in sql (we use String.format), that's too bad.
     * As audit require dbName, let's try to recover this by regex "\s+(.*)\." (we are always using sql like
     *     "update %s.<table> ..."
     *     "select * from %s.<table> ..."
     *
     * TODO: to get real SQL: (but only works to mysql):
     *     https://stackoverflow.com/questions/2382532/how-can-i-get-the-sql-of-a-preparedstatement/26228133
     *        System.out.println(((JDBC4PreparedStatement)stmt).asSql());
     */
    private static Pattern _DBNAME_PATTERN = Pattern.compile("\\s+([a-zA-Z]+[a-zA-Z0-9_]*)\\.");
    private static void _audit(Connection conn, String sql, boolean update, String message, @Nullable Object[] params) {
        String clazz = getCallerClassName(DaoUtils.class);
        if(clazz == null || !_audits.containsKey(clazz)) {
            return;
        }

        AuditOption option = _audits.get(clazz);
        if(option == null) {
            return;
        }

        String access = AuditLog.ACCESS_READ;
        boolean audit = false;
        if(update) {
            if(sql.toLowerCase().startsWith("create")) {
                access = AuditLog.ACCESS_CREATE;
                audit = option.create;
                // override message if it is not empty
                if(StringUtils.isEmpty(message)) {
                    message = "Table created";
                }
            }
            else if(sql.toLowerCase().startsWith("insert")) {
                access = AuditLog.ACCESS_CREATE;
                audit = option.create;
            }
            else if(sql.toLowerCase().startsWith("delete")) {
                access = AuditLog.ACCESS_DELETE;
                audit = option.delete;
            }
            else {
                access = AuditLog.ACCESS_UPDATE;
                audit = option.update;
            }
        }
        else {
            audit = option.read;
        }

        if(!audit) {
            return;
        }

        /**
         * construct params
         */
        StringBuilder builder = new StringBuilder(" params:");
        int maxLenPerParam = 30;
        if (params != null) {
            for (Object o : params) {
                if (o != null) {
                    String s = o.toString();
                    builder.append(s, 0, Math.min(s.length(), maxLenPerParam)).append(";");
                }
                else {
                    builder.append("null;");
                }
            }
        }

        try {
            Matcher m = _DBNAME_PATTERN.matcher(sql);
            String paramStr = builder.substring(0, Math.min(512, builder.length()));
            sql = sql.length() < 512 ? sql : sql.substring(0, 512); // make sure it's not exceeding the column length
            if (m.find() && m.groupCount() > 0) {
                String dbName = m.group(1);
                recordAuditLog(conn, dbName, ZBotContext.getUserName(), access, option.table, sql + paramStr, message);
            }
            else {
                // damn, let's take it as COREDB???
                recordAuditLog(conn, Constants.COREDB, ZBotContext.getUserName(), access, option.table, sql + paramStr, message);
            }
        }
        catch (Exception e) {
            // for now, let's silently fail ...
            LOG.error("Failed to save audit record for SQL [" + sql + "] and message [" + message + "]", e);
        }
    }

    private static void _audit(Connection conn, String dbName, String access, String table, String message) {
        recordAuditLog(conn, dbName, ZBotContext.getUserName(), access, table, "", message);
    }
}