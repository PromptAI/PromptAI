package com.zervice.kbase.queues.atomic;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.zervice.common.utils.IdGenerator;
import com.zervice.kbase.AccountCatalog;
import com.zervice.kbase.ZBotContext;
import com.zervice.kbase.database.dao.AtomicTaskDao;
import com.zervice.kbase.database.pojo.AtomicTask;
import com.zervice.kbase.database.utils.DaoUtils;
import com.zervice.kbase.queues.TaskQueueException;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.apache.tomcat.jdbc.pool.PoolExhaustedException;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.*;

@Log4j2
public class AtomicTaskQueue {
    @Getter
    private final long _accountId;
    private final AccountCatalog _account;

    /**
     * for all un-committed transactions, we buffer their mcOp in memory
     */
    private final ConcurrentHashMap<Long/*txnId*/, AtomicTransactions> _pendingTasks = new ConcurrentHashMap<>();

    /**
     * For all committed tasks, we put them into both the db and _mcTaskCache.
     */
    private final ConcurrentHashMap<Long/*TaskId*/, AtomicTask> _taskCache = new ConcurrentHashMap<>();

    private final ExecutorService _executor;
    private final String _company;

    /**
     * Track the MCTask
     */
    private final AtomicTaskProcessor _processor;
    private final Future<Void> _processorFuture;

//    public AtomicTaskQueueMbean getMetric(String taskName) {
//        return JmxMetric.getMbean("type=AtomicTaskQueue,company=" + _company + ",name=" + taskName,
//                AtomicTaskQueueMbean.class);
//    }

    public AtomicTaskQueue(AccountCatalog account) {
        _accountId = account.getId();
        _account = account;
        _company = _account.getName();
        _executor = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat("AtomicTaskThread-" + _company).build());
        _processor = new AtomicTaskProcessor(this);

        /**
         * TODO: it is not good to start the task directly as the system is still initializing
         *
         * We shall have a global flag to indicate the system has been fully initialized so biz logic like
         * this can start to run (e.g. via semaphore or endless repeat checking the flag in MCTask.call?)
         */
        _processorFuture = _executor.submit(_processor);
    }

    public long beginTransaction() {
        long txnId = IdGenerator.generateId();
        AtomicTransactions txn = new AtomicTransactions(txnId);
        _pendingTasks.put(txnId, txn);
        return txnId;
    }

    public void commitTransaction(long txnId, @NonNull Connection conn) throws SQLException {
        LOG.info("AtomicTask commit transaction, txnId:{}", txnId);
        AtomicTransactions txn = _pendingTasks.get(txnId);
        if (txn == null) {
            throw new TaskQueueException("No such transaction " + txnId);
        }

        ArrayList<Long> taskids = new ArrayList<>();
        try {
            String dbName = _account.getDBName();
            Iterator<AtomicTask> tasks = txn.tasks();
            while (tasks.hasNext()) {
                AtomicTask task = tasks.next();
                task.setIdentity(ZBotContext.getIdentity());
                taskids.add(AtomicTaskDao.add(conn, dbName, task));
                _taskCache.put(task.getId(), task);
            }
            conn.commit();
            _pendingTasks.remove(txnId);
            synchronized (_processorFuture) {
                _processorFuture.notifyAll();
            }
        }
        catch (Exception e) {
            _pendingTasks.remove(txnId);
            DaoUtils.rollbackQuietly(conn);
            taskids.forEach(id -> _taskCache.remove(id));
            throw e;
        }
    }

    public void rollbackTransaction(long txnId) {
        _pendingTasks.remove(txnId);
    }

    public void put(long txnId, @NonNull AtomicTask task) {
        LOG.info("[AtomicTask put task, txnId:{}, taskType:{}, taskId:{} ]", txnId,task.getClass().getCanonicalName(), task.getId());
        AtomicTransactions txn = _pendingTasks.get(txnId);
        if (txn == null) {
            throw new TaskQueueException("No such transaction " + txnId);
        }
//        getMetric(task.getType()).increaseQueueSize();
        txn.put(task);
    }

    /**
     * Peek should handle all recoverable errors by itself and only throw out exception
     * when encountering unrecoverable errors, peeking timeout expiring, or be interrupted.
     *
     * There are recoverable errors:
     *   1. MySQL temporarily goes down for a while
     *   2. Unable to get connection from the pool
     *   3. The obtained connection was closed
     *
     * TODO:
     *   gracefully handle all recoverable errors
     */
    AtomicTask peek(long timeoutMs) throws TimeoutException, InterruptedException {
        Preconditions.checkArgument(timeoutMs > 0);

        boolean toSleep = true;
        while (true) {
            Connection conn = null;
            try {
                conn = DaoUtils.getConnection(true);
                AtomicTask task = AtomicTaskDao.getHead(conn, _account.getDBName());
                if (task != null) {
                    task.setAcctId(_accountId);

                    // if the task cache has the task, we will return
                    // the cached copy so that the REST thread sync
                    // on the right future object.
                    // Otherwise, we return the copy from db.
                    //
                    if (_taskCache.containsKey(task.getId())) {
                        LOG.info("[Peak AtomicTask:{}]", task.getId());
                        return _taskCache.remove(task.getId());
                    }

                    LOG.warn("Unable to find the AtomicTask:{} in cache", task.getId());

                    return task;
                }

                // no tasks
                if (toSleep) {
                    synchronized (_processorFuture) {
                        // TODO: we shall release db connection before sleep.
                        _processorFuture.wait(timeoutMs);
                        toSleep = false;
                    }
                }
                else {
                    throw new TimeoutException();
                }
            }
            catch (SQLException e) {
                if (e instanceof PoolExhaustedException) {
                    LOG.error("Connection pool exhausted, try again later");
                    Thread.sleep(Math.min(timeoutMs, 500));
                }
            }
            finally {
                DaoUtils.closeQuietly(conn);
            }
        }
    }

    /**
     * TODO: to handle the following failures
     *  1. MySQL isn't available for a while (e.g. we are upgrading MySQL or network to MySQL temporarily goes down)
     *  2. The connection is closed by the pool
     */
    void remove(AtomicTask task) {
        Connection conn = null;
        try {
            conn = DaoUtils.getConnection(true);
            AtomicTaskDao.remove(conn, _account.getDBName(), task.getId());
            LOG.info("Remove AtomicTask:{}", task.getId());
        }
        catch (Throwable e) {
            LOG.error("Encounter an exception:{} to remove a task:{}", e.getMessage(), task.getId(), e);
        }
        finally {
            DaoUtils.closeQuietly(conn);
        }
//        getMetric(task.getType()).decreaseQueueSize();
    }

    /**
     * Helper class to manage the on-going atomic tasks ...
     */
    static class AtomicTransactions {
        private final long _txnId;
        private final ConcurrentLinkedQueue<AtomicTask> _tasks = new ConcurrentLinkedQueue<>();

        AtomicTransactions(long txnId) {
            _txnId = txnId;
        }

        void put(@NonNull AtomicTask task) {
            _tasks.add(task);
        }

        Iterator<AtomicTask> tasks() {
            return _tasks.iterator();
        }
    }

}
