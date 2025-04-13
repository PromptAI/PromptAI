package com.zervice.kbase.queues;

import com.google.common.base.Preconditions;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Base class for all account based queues
 *
 * We are multi-tenant application so for different kinds of tasks, we'll have account based task queues
 */
@Log4j2
public abstract class AccountTaskQueues<Queue> {
    /**
     * We current have three types of account based queue
     *   atomic - supporting atomic operation (backend-ed by Mysql)
     *   async - an async task queue
     *   timer - a periodic tasks or one time tasks
     */
    public static final String ACCOUNT_QUEUE_ATOMIC = "atomic";
    public static final String ACCOUNT_QUEUE_ASYNC = "async";
    public static final String ACCOUNT_QUEUE_TIMER = "timer";

    protected final ConcurrentHashMap<Long/*acctId*/, Queue> _queues =
            new ConcurrentHashMap<Long, Queue>();

    /**
     * Supported tasks for this queue. Each specific task shall have a definition class  to define its behavior
     *
     * All task class should be a subclass of Task type
     */
    private final ConcurrentHashMap<String, Class> _taskDefinitions = new ConcurrentHashMap<>();

    /**
     * One of above ACCOUNT_QUEUE_XXXX
     */
    private final String _name;

    public AccountTaskQueues(String name) {
        _name = name;
    }

    /**
     * Get the account specific queue
     */
    public Queue get(final long acctId) {
        Preconditions.checkArgument(acctId > 0);
        Queue q = _queues.get(acctId);
        if (q == null) {
            synchronized (_queues) {
                q = _queues.get(acctId);
                if (q == null) {
                    LOG.warn("[Delayed creation task queue accountId:{}, name:{}]", acctId, _name);

                    q = _createQueue(acctId);
                    if(q != null) {
                        _queues.put(acctId, q);
                    }
                }
            }
        }

        return q;
    }

    public abstract void init() throws ClassNotFoundException, IOException, NoSuchMethodException, NoSuchFieldException, IllegalAccessException;
    protected Queue _createQueue(long accountId) {
        return null;
    }
}
