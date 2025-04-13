package com.zervice.kbase.queues.atomic;

import com.google.common.base.Stopwatch;
import com.zervice.kbase.database.pojo.AtomicTask;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;

/**
 * The task is an infinite loop. It peeks the head mcOp from the db,
 * process it, and remove it from db.
 */
@Log4j2
public class AtomicTaskProcessor implements Callable<Void> {
    private static final long _TASK_PEEKING_TIMEOUT_MS = 1000;

    private final AtomicTaskQueue _queue;

    AtomicTaskProcessor(@NonNull AtomicTaskQueue queue) {
        _queue = queue;
    }

    @Override
    public Void call() throws Exception {
        LOG.info("[call atomic task with acctId:{}]", _queue.getAccountId());

        LOG.info("[Start atomic task processing loop ...]");
        long maxProcessedTxnId = 0;

        while (true) {
            try {
                AtomicTask task = _queue.peek(_TASK_PEEKING_TIMEOUT_MS);
                if (task.getId() <= maxProcessedTxnId) {
                    // discard this task and log error
                    LOG.info("[Found not used message. Discarded! task:{}, currentId:{}]",task.toString(), maxProcessedTxnId);
                    _queue.remove(task);
                }
                else {
                    Stopwatch stopWatch = Stopwatch.createStarted();
                    try {
//                        ZPContext.setIdentity(task.getIdentity());
                        task.process();
                    }
                    catch (Throwable e) {
                        // will never to here, all throwables are caught in
                        // Micro code FutureTask process() within MicroCodeQueues.process(task).
                        LOG.error("Cannot process an atomic task:{} due to exception:{}", task.toString(), e.getMessage(), e);
                    }
                    finally {
                        maxProcessedTxnId = task.getId();
                        _queue.remove(task);
                    }
                    stopWatch.stop();
                    // todo The future implementation
//                    _queue.getMetric(task.getType()).increaseProcessTime(stopWatch.elapsed(TimeUnit.MICROSECONDS));
                }
            } catch (TimeoutException e) {
                // no new tasks, try again
            } catch (Exception e) {
                // triggered by no-recoverable issues. Nothing we can do besides
                // logging and quit
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }

                LOG.error("Quitting atomic task processing loop due to non-recoverable error:{}", e.getMessage(), e);
                break;
            }
        }

        LOG.info("Atomic task processing loop done");
        return null;
    }
}
