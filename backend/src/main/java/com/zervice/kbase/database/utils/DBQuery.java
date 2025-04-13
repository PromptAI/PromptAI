package com.zervice.kbase.database.utils;

import org.apache.commons.lang3.tuple.Pair;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DBQuery {
    private final DBOperator _operator;

    public DBQuery(Connection conn, String sql, Object...params) throws SQLException {
        _operator = new DBOperator(conn, sql, params);
    }

    public  <T> T query(RecordLoader<T> rLoader) throws SQLException {
        ManagedResultSet rs = null;
        try {
            rs = new ManagedResultSet(_operator.query());

            if (rs.next()) {
                return rLoader.load(rs);
            }

            return null;
        }
        finally {
            DaoUtils.closeQuietly(rs);
        }
    }

    public  <K, V> Map<K, V> queryMap(RecordLoader<Pair<K, V>> loader) throws SQLException {
        ManagedResultSet rs = null;
        try {
            rs = new ManagedResultSet(_operator.query());

            Map<K, V> r = new HashMap<>();
            while (rs.next()) {
                Pair<K, V> p = loader.load(rs);
                r.put(p.getKey(), p.getValue());
            }

            return r;
        }
        finally {
            DaoUtils.closeQuietly(rs);
        }
    }

    public  <T> List<T> queryList(RecordLoader<T> rLoader) throws SQLException {
        ManagedResultSet rs = null;
        try {
            rs = new ManagedResultSet(_operator.query());

            ArrayList<T> r = new ArrayList<>();
            while (rs.next()) {
                r.add(rLoader.load(rs));
            }

            return r;
        }
        finally {
            DaoUtils.closeQuietly(rs);
        }
    }

    public void close() {
        _operator.close();
    }
}
