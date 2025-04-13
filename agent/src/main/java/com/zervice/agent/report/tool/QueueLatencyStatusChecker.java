package com.zervice.agent.report.tool;

import com.zervice.agent.report.PersistentReporter;
import lombok.extern.log4j.Log4j2;

import java.util.concurrent.TimeUnit;

@Log4j2
public class QueueLatencyStatusChecker implements QueueStatusChecker {

    @Override
    public boolean isAbnormal(PersistentReporter pq) {
        // check whether it has a high latency.
        long time = pq.getCurrentProcessDataTime();
        if (time > 0 && System.currentTimeMillis() - time > TimeUnit.MINUTES.toMillis(pq.getReporterConf().dataKeepTimeInMinutes) * 0.8) {
            LOG.warn("Queue has a high latency. queue" + pq.getReporterName()
                    + " timegap:" + (System.currentTimeMillis() - time));
            return true;
        }
        return false;
    }

    @Override
    public boolean isRestored(PersistentReporter pq) {
        // check whether it has a high latency.
        long time = pq.getCurrentProcessDataTime();
        if (System.currentTimeMillis() - time < TimeUnit.MINUTES.toMillis(pq.getReporterConf().dataKeepTimeInMinutes) * 0.2) {
            LOG.info("Queue high latency restored. queue" + pq.getReporterName()
                    + " timegap:" + (System.currentTimeMillis() - time));
            return true;
        }
        return false;
    }
}
