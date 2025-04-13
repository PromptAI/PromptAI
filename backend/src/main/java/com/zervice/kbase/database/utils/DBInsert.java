package com.zervice.kbase.database.utils;

import java.sql.*;

public class DBInsert {
    private final DBOperator _operator;

    public DBInsert(Connection conn, String sql, Object... params) throws SQLException {
        _operator = new DBOperator(conn, sql, Statement.RETURN_GENERATED_KEYS, params);
    }

    public long insert() throws SQLException {
        return _operator.insert();
    }

    public void close() {
        _operator.close();
    }
}

