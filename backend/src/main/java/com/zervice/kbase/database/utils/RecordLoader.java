package com.zervice.kbase.database.utils;

import java.sql.ResultSet;
import java.sql.SQLException;

public interface RecordLoader<T> {
    T load(ManagedResultSet rs) throws SQLException;
}
