package com.zervice.common.cron;

public interface CronTask extends Runnable {
    public final static long DAY = 24 * 3600 * 1000;
    public final static long HOUR = 3600 * 1000;
    public final static long MINUTE = 60 * 1000;
    public final static long SECOND = 1 * 1000;

    /**
     * Get interval in milliseconds
     * @return
     */
    default long getInterval() {
        return HOUR;
    }

    /**
     * Get delay in millis
     * @return
     */
    default long getDelay() {
        return 0;
    }
}
