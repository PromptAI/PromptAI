package com.zervice.common.jmx;

import javax.management.MXBean;

@MXBean
public interface ServletMXbean {
    long getRequestCount();
    long getSuccessCount();
    long getFailCount();
    int getInProcessingCount();
    HistogramMbean getHistogramInMilliSeconds();
}
