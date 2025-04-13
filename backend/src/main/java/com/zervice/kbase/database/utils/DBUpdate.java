package com.zervice.kbase.database.utils;

import java.sql.*;

public class DBUpdate {
    private final DBOperator _operator;

    public DBUpdate(Connection conn, String sql, Object... params) throws SQLException {
        _operator = new DBOperator(conn, sql, params);
    }

    public int update() throws SQLException {
        return _operator.update();
    }

    public void close() {
        _operator.close();
    }
}

