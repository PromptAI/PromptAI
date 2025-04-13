package com.zervice.kbase.queues.atomic;

import com.zervice.common.pojo.common.Account;
import com.zervice.kbase.AccountCatalog;
import com.zervice.kbase.database.pojo.AtomicTask;
import com.zervice.kbase.database.utils.DaoUtils;
import com.zervice.kbase.queues.AccountTaskQueues;
import com.zervice.kbase.queues.atomic.operations.AtomicOperation;
import com.zervice.kbase.utils.ReflectionUtils;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Log4j2
public class AtomicTaskQueues extends AccountTaskQueues<AtomicTaskQueue> {
    @Getter
    private static final AtomicTaskQueues _instance = new AtomicTaskQueues();

    private AtomicTaskQueues() {
        super(AccountTaskQueues.ACCOUNT_QUEUE_ATOMIC);
    }

    /**
     * Supported tasks for this queue. Each specific task shall have a definition class  to define its behavior
     *
     * All task class should be a subclass of Task type
     */
    private final ConcurrentHashMap<String, Class> _taskDefinitions = new ConcurrentHashMap<>();

    @Override
    public void init() throws ClassNotFoundException, IOException, NoSuchMethodException, NoSuchFieldException, IllegalAccessException {
        Set<String> types = new HashSet<>();
        Class[] classes = ReflectionUtils.getClasses(AtomicTaskQueue.class.getPackage().getName());
        for (Class cls : classes) {
            Annotation a = cls.getAnnotation(AtomicOperation.class);
            if (a == null) {
                continue;
            }

            AtomicOperation op = (AtomicOperation) a;

            LOG.info("Load atomic operation:{} definition class:{}", op.value(), cls.getCanonicalName());

            /**
             * Make sure this class has a "process" method
             */
            cls.getMethod("process", AtomicTask.class);

            // check for duplicated TYPE !!! this is important to us
            Field field = cls.getField("TYPE");
            if (!types.add((String) field.get(null))) {
                LOG.info("Detects CO TYPE conflicts!, class:{}, peration:{}, type:{}", cls.getCanonicalName(), op.value(), field.get(null));
                throw new IllegalStateException("Found CO with same type name - " + field.get(null));
            }
            ;
            _taskDefinitions.put(op.value(), cls);
        }

        /**
         * We need to create consumer for known accounts on initiation
         *
         * TODO: Add account CRUD to pub/sub system so MgmtSvc can create / stop tasks for account accordingly
         */
        /*
         * Initialize account critical queue in AccountCatalog
        Connection conn = null;
        try {
            conn = DaoUtils.getConnection(false);
            List<Account> accounts = AccountDao.getAll(conn);
            for(Account account : accounts) {
                long accountId = account.getId();

                LOG.info()
                        .params("account", account.getName(), "accountId", accountId)
                        .message("Create atomic task consumer");

                _queues.put(account.getId(), _createQueue<(accountId));
            }
        }
        catch (Exception e) {
            LOG.error().exception(e).empty();
        }
        finally {
            DaoUtils.closeQuietly(conn);
        }
        */
    }

    public void start(AccountCatalog account) {
        if (!_queues.containsKey(account.getId())) {
            LOG.info("Create atomic task consumer, account:{}, accountId:{}", account.getName(), account.getId());

            _queues.put(account.getId(), new AtomicTaskQueue(account));
        }
    }

    public void putWithCommit(long acctId,
                              @NonNull AtomicTask task) throws SQLException {
        Connection conn = null;
        try {
            conn = DaoUtils.getConnection(false);
            putWithCommit(conn, acctId, task);
        } finally {
            DaoUtils.closeQuietly(conn);
        }
    }

    public void putWithCommit(@NonNull Connection conn,
                              long acctId,
                              @NonNull AtomicTask task) throws SQLException {
        task.setAcctId(acctId);
        AtomicTaskQueue q = get(acctId);
        long txnid = q.beginTransaction();
        q.put(txnid, task);
        q.commitTransaction(txnid, conn);
    }


    public void putWithCommit(@NonNull Connection conn,
                              long acctId,
                              @NonNull List<AtomicTask> tasks) throws SQLException {
        AtomicTaskQueue q = get(acctId);
        long txnid = q.beginTransaction();
        tasks.forEach(task -> {
            task.setAcctId(acctId);
            q.put(txnid, task);
        });
        q.commitTransaction(txnid, conn);
    }


    /**
     * Micro Queue Task loop / peek individual Micro Code Tasks, and execute its process method.
     */
    public Object process(@NonNull AtomicTask task) {
        Class cls = _taskDefinitions.get(task.getType());
        String dbName = Account.getAccountDbName(task.getAcctId());
        if (cls == null) {
            LOG.error("[{}][Unable to find a processor for given atomic task! type:{}, task:{}]", dbName, task.getType(), task.toString());

            return null;
        }

        try {
            Field field = cls.getField("TYPE");
            String type = (String) field.get(null);

            Method method = cls.getMethod("process", AtomicTask.class);
            return method.invoke(null, new Object[]{task});
        } catch (Throwable e) {
            LOG.error("[{}][Exception:{} caught when executing atomic task]", dbName, e.getMessage(), e);
            task.setException(e);
        } finally {
        }

        return null;
    }

    protected AtomicTaskQueue _createQueue(AccountCatalog account) {
        return new AtomicTaskQueue(account);
    }
}
