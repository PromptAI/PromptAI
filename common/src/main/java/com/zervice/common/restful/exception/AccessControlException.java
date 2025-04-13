package com.zervice.common.restful.exception;

public class AccessControlException extends RuntimeException {
    public AccessControlException(String msg) {
        super(msg);
    }

    public AccessControlException(long userId) {
        super(String.format("Permission denied for the user (id=%d)", userId));
    }

    public AccessControlException(String resource, String operation, long redId) {
        super(String.format("Permission denied to perform operation %s on %s with id=%d", operation, resource, redId));
    }

}
