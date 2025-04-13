package com.zervice.agent.report.tool;

import com.zervice.agent.report.PersistentReporter;

public interface QueueStatusChecker {
    boolean isAbnormal(PersistentReporter pq);
    boolean isRestored(PersistentReporter pq);
}
