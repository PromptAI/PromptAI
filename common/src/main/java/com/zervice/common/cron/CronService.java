package com.zervice.common.cron;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Log4j2
public class CronService {
    private final static String CRON_GLOBAL = "GLOABL";

    private final String _name;
    private CronService(String name) {
        _name = name;
    }

    /**
     * This is for global system use. Per account will maintain a separate cron service to run
     * account specific tasks
     */
    @Getter
    private final static CronService _instance = new CronService(CRON_GLOBAL);

    private final ScheduledExecutorService _executor = Executors.newSingleThreadScheduledExecutor(
            new ThreadFactoryBuilder().setDaemon(true).setNameFormat("cron-service").build());

    /**
     * AccountCatalog will use to create a cron service for an account
     * @return
     */
    public final static CronService create(String account) {
        return new CronService("ACCOUNT-" + account);
    }

    public void initialize() {
        // TODO
    }

    public void schedule(CronTask task) {
        _executor.scheduleAtFixedRate(new TaskWrapper(task), task.getDelay(), task.getInterval(), TimeUnit.MILLISECONDS);
    }

    public void shutdown() {
        _executor.shutdownNow();
    }

    @AllArgsConstructor
    public class TaskWrapper implements Runnable {
        private final CronTask _task;

        @Override
        public void run() {
            try {
                _task.run();
            }
            catch(Throwable t) {
                LOG.error("Cannot run task - " + _task.toString(), t);
            }
        }
    }
}
