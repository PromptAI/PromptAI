package com.zervice.common.jmx;

import javax.management.MXBean;

@MXBean
public interface DbConnectionMXbean {
    long getConnCount();
    long getTotalConnCount();
    HistogramMbean getHistogramInMilliSeconds();
}
