package com.zervice.common.jmx;

import javax.management.MXBean;

@MXBean
public interface QueueMXbean {
    int getQueueSize();
    HistogramMbean getHistogramInMicroSeconds();
    long getTaskCount();
}
