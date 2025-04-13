package com.zervice.kbase.database.pojo;

import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Preconditions;
import com.zervice.kbase.queues.atomic.AtomicTaskQueues;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;


@EqualsAndHashCode
@ToString
public class AtomicTask {
    public static final String TASK_RESULT_IN_DATA = "_ret";
    public static enum Status {
        READY,
        PROCESSING;

        public static Status fromInt(int s) {
            switch (s)
            {
                case 1:
                    return READY;
                case 2:
                    return PROCESSING;
                default:
                    throw new IllegalArgumentException("Unknown code " + s);
            }
        }

        public static int toInt(Status s) {
            switch (s)
            {
                case READY:
                    return 1;
                case PROCESSING:
                    return 2;
                default:
                    throw new IllegalArgumentException("Unknown status " + s);
            }
        }
    }

    @Getter @Setter
    long _id;

    @Getter @Setter
    JSONObject _data;

    @Getter @Setter
    long _enqueuedAtEpochMs;

    @Getter @Setter
    long _dequeuedAtEpochMs;

    @Getter @Setter
    Status _status;

    @Getter @Setter
    long _acctId;

    //
    // which user initiate the CO. We can use this to track or audit user ...
    @Getter @Setter
    String _identity = StringUtils.EMPTY;   // init to empty string rather than null.

    @Getter @Setter
    Throwable _exception;    // exception on this task.

    FutureTask<Object> _futureTask;

    public AtomicTask() {
        final AtomicTask self = this;

        _futureTask = new FutureTask<Object>(new Callable<Object>() {
            @Override
            public Object call() {
                return AtomicTaskQueues.getInstance().process(self);
            }
        });
    }

    public Future<Object> future() {
        return _futureTask;
    }

    public String getType() {
        Preconditions.checkState(_data != null);
        Preconditions.checkState(_data.containsKey("type"));

        return _data.getString("type");
    }

    public void process() {
        _futureTask.run();
    }
}
