package com.zervice.agent.report.tool;

import com.zervice.agent.report.PersistentReporter;
import com.zervice.agent.report.tool.QueueStatusChecker;

public class ChainedQueueStatusChecker implements QueueStatusChecker {
    private final QueueStatusChecker[] queueStatusCheckers;
    public ChainedQueueStatusChecker(QueueStatusChecker ...statusCheckers) {
        this.queueStatusCheckers = statusCheckers;
    }

    @Override
    public boolean isAbnormal(PersistentReporter pq) {
        for (QueueStatusChecker statusChecker : queueStatusCheckers) {
            if (statusChecker.isAbnormal(pq)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isRestored(PersistentReporter pq) {
        for (QueueStatusChecker statusChecker : queueStatusCheckers) {
            if (!statusChecker.isRestored(pq)) {
                return false;
            }
        }
        return true;
    }
}
