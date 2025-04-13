package com.zervice.kbase.database.utils;

import lombok.extern.log4j.Log4j2;

import java.io.ByteArrayInputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

@Log4j2
public class DBOperator {
    private static final String _METHOD_PREPARE_STMT = "prepareStatement";
    private static final String _METHOD_DB_QUERY = "executeQuery";
    private static final String _METHOD_DB_UPDATE = "executeUpdate";
    private static final Method _query;
    private static final Method _update;
    private static final Method _prepare;
    private static final Method _prepareWithFlag;

    static {
        // Create a PreparedStatement for query operations
        try {
            _prepare = DaoUtils.CONN_CLASS.getMethod(_METHOD_PREPARE_STMT, String.class);
        }
        catch (Exception e) {
            //
            LOG.error("Fail to fetch method", e);
            throw new IllegalStateException(e);
        }

        // Create a PreparedStatement for insert, etc.
        try {
            _prepareWithFlag = DaoUtils.CONN_CLASS.getMethod(_METHOD_PREPARE_STMT, String.class, int.class);
        }
        catch (Exception e) {
            //
            LOG.error("Fail to fetch method", e);
            throw new IllegalStateException(e);
        }

        // Execute a query
        try {
            _query = DaoUtils.STMT_CLASS.getMethod(_METHOD_DB_QUERY);
        }
        catch (Exception e) {
            //
            LOG.error("Fail to fetch method", e);
            throw new IllegalStateException(e);
        }

        // Execute an update
        try {
            _update = DaoUtils.STMT_CLASS.getMethod(_METHOD_DB_UPDATE);
        }
        catch (Exception e) {
            LOG.error("Fail to fetch method", e);
            throw new IllegalStateException(e);
        }
    }

    @FunctionalInterface
    public interface SqlProcessor {
        String rewriteSql(String sql);
    }

    public static SqlProcessor globalSqlProcessor = (sql) -> sql;


    private final Object _wrapped;

    public DBOperator(Connection conn, String sql, Object... params) throws SQLException {
        try {
            sql = globalSqlProcessor.rewriteSql(sql);
            _wrapped = _prepare.invoke(conn, sql);
            _bindParameters(params);
        }
        catch (IllegalAccessException | InvocationTargetException e) {
            throw new SQLException("Invalid connection for query!");
        }
    }

    public DBOperator(Connection conn, String sql, int flag, Object... params) throws SQLException {
        try {
            sql = globalSqlProcessor.rewriteSql(sql);
            _wrapped = _prepareWithFlag.invoke(conn, sql, flag);
            _bindParameters(params);
        }
        catch (IllegalAccessException | InvocationTargetException e) {
            throw new SQLException("Invalid connection for update!");
        }
    }

    public ResultSet query() throws SQLException {
        try {
            Object result = _query.invoke(_wrapped);
            return (ResultSet) result;
        }
        catch (IllegalAccessException | InvocationTargetException e) {
            throw new SQLException("Query failed!", e);
        }
    }

    public int update() throws SQLException {
        try {
            return (int) _update.invoke(_wrapped);
        }
        catch (IllegalAccessException | InvocationTargetException e) {
            throw new SQLException("Update failed!", e);
        }
    }

    public long insert() throws SQLException {
        ResultSet keys = null;
        try {
            _update.invoke(_wrapped);

            keys = ((PreparedStatement) _wrapped).getGeneratedKeys();
            keys.next();

            return keys.getLong(1);
        }
        catch (IllegalAccessException | InvocationTargetException e) {
            throw new SQLException("Insert failed!", e);
        }
        finally {
            DaoUtils.closeQuietly(keys);
        }
    }

    public void close() {
        if (_wrapped != null) {
            DaoUtils.closeQuietly((PreparedStatement) _wrapped);
        }
    }

    private void _bindParameters(Object... params) throws SQLException {
        if (_wrapped == null || !(_wrapped instanceof PreparedStatement)) {
            throw new SQLException("Invalid statement to execute!");
        }

        PreparedStatement stmt = (PreparedStatement) _wrapped;

        int idx = 1;
        for (Object param : params) {
            if (param instanceof String) {
                stmt.setString(idx, (String) param);
            }
            else if (param instanceof Long) {
                stmt.setLong(idx, (Long) param);
            }
            else if (param instanceof Integer) {
                stmt.setInt(idx, (Integer) param);
            }
            else if (param instanceof Double) {
                stmt.setDouble(idx, (Double) param);
            }
            else if (param instanceof Float) {
                stmt.setFloat(idx, (Float) param);
            }
            else if (param instanceof Boolean) {
                boolean b = (Boolean) param;
                stmt.setBoolean(idx, b);
            }
            else if (param instanceof byte[]) {
                stmt.setBlob(idx, new ByteArrayInputStream((byte[]) param));
            }
            else if (param instanceof List) {
                List list = (List) param;
                for (Object o : list) {
                    if (o instanceof String) { // param is List<String>
                        stmt.setString(idx, (String) o);
                    }
                    else if (o instanceof Long) { // param is List<Long>
                        stmt.setLong(idx, (Long) o);
                    }
                    idx++;
                }
                continue;
            }
            else {
                if (param == null) {
                    stmt.setNull(idx, Types.NULL);
                }
                else {
                    stmt.setString(idx, param.toString());
                }
            }

            idx++;
        }
    }
}
