package com.zervice.kbase.database;

public class DBException extends RuntimeException {
    public DBException(String msg, Throwable e) {
        super(msg, e);
    }

    public DBException(String msg) {
        super(msg);
    }
}
