package com.zervice.kbase.queues;

import lombok.NonNull;

/**
 * mcQ framework always throws out this un-checked exception
 */
public class TaskQueueException extends RuntimeException {
    public TaskQueueException(Throwable cause) {
        super(cause);
    }

    public TaskQueueException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public TaskQueueException(@NonNull String message) {
        super(message);
    }
}


