package com.zervice.common.jmx;

import com.codahale.metrics.Snapshot;

import java.beans.ConstructorProperties;

public class HistogramMbean {
    private Snapshot snapshot;
    private long histogramCount;
    @ConstructorProperties({"min", "max", "mean", "95th", "count"})
    public HistogramMbean(Snapshot snapshot, long count) {
        this.snapshot = snapshot;
        this.histogramCount = count;
    }

    public double getMin() {
        return this.snapshot.getMin();
    }

    public double getMax() {
        return this.snapshot.getMax();
    }

    public double getMean() {
        return this.snapshot.getMean();
    }

    public double get95th() {
        return this.snapshot.get95thPercentile();
    }

    public long getCount() {
        return this.histogramCount;
    }
}
