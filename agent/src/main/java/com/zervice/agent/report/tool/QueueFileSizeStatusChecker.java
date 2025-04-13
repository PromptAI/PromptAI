package com.zervice.agent.report.tool;

import com.zervice.agent.report.PersistentReporter;
import com.zoomphant.common.util.*;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class QueueFileSizeStatusChecker implements QueueStatusChecker {

    @Override
    public boolean isAbnormal(PersistentReporter pq) {
        if (pq.getReporterConf().dataKeepSizeInMBytes > 0) {
            long dirSize = DirUtils.getDirectorySize(pq.getDataPath());
            if (dirSize / 1024 / 1024 > pq.getReporterConf().dataKeepSizeInMBytes) {
                LOG.warn("Queue has a high volume. queue" + pq.getReporterName()
                        + " currentMB:" + (dirSize / 1024 / 1024) + " maxBytes:" + pq.getReporterConf().dataKeepSizeInMBytes);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isRestored(PersistentReporter pq) {
        if (pq.getReporterConf().dataKeepSizeInMBytes > 0) {
            long dirSize = DirUtils.getDirectorySize(pq.getDataPath());
            if (dirSize / 1024 / 1024 < pq.getReporterConf().dataKeepSizeInMBytes * 0.6) {
                LOG.info("Queue high volume restored. queue" + pq.getReporterName()
                        + " currentMB:" + (dirSize / 1024 / 1024) + " maxBytes:" + pq.getReporterConf().dataKeepSizeInMBytes);
                return true;
            } else {
                return false;
            }
        }
        return true;
    }
}
