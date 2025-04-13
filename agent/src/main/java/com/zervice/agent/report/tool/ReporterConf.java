package com.zervice.agent.report.tool;

import java.util.concurrent.*;

public class ReporterConf {
    public long dataKeepSizeInMBytes = -1;
    public long dataKeepTimeInMinutes = TimeUnit.HOURS.toMinutes(12);
    public boolean debug = false;

    /**
     * -1 not controlled
     */
    public int qps = -1; //

    public static class ReporterConfBuilder {
        private ReporterConf reporterConf;

        public ReporterConfBuilder() {
            this.reporterConf = new ReporterConf();
        }

        public ReporterConfBuilder(ReporterConf reporterConf) {
            this.reporterConf = reporterConf;
        }

        public ReporterConf build() {
            return this.reporterConf;
        }

        public ReporterConfBuilder dataKeepTimeInMins(long dataKeepTimeInMins) {
            this.reporterConf.dataKeepTimeInMinutes = dataKeepTimeInMins;
            return this;
        }
        public ReporterConfBuilder dataKeepSizeInMBytes(long mbytes) {
            this.reporterConf.dataKeepSizeInMBytes = mbytes;
            return this;
        }

        public ReporterConfBuilder debug(boolean debug) {
            this.reporterConf.debug = debug;
            return this;
        }

        public ReporterConfBuilder qps(int c) {
            this.reporterConf.qps = c;
            return this;
        }
    }

    @Override
    public String toString() {
        return "ReporterConf{" +
                "dataKeepTimeInMins=" + dataKeepTimeInMinutes +
                ", debug=" + debug +
                ", qps=" + qps +
                ", dataKeepSizeInMBytes=" + dataKeepSizeInMBytes +
                '}';
    }
}
