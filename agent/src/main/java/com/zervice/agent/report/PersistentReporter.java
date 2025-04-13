package com.zervice.agent.report;

import com.google.common.util.concurrent.RateLimiter;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.zervice.agent.report.processor.Processor;
import com.zervice.agent.report.tool.*;
import com.zervice.agent.utils.ConfConstant;
import com.zervice.common.utils.LayeredConf;
import com.zoomphant.common.util.Queue;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.Pair;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * From ZP agent
 * 持久化需要上传的文件
 *
 * @author chen
 * @date 2022/9/1
 */
@Log4j2
public class PersistentReporter implements Reporter {

    /**
     * Data encoder, to encode data to correct format (like Json or grpc)
     */
    private final Encoder _encoder;

    /**
     * Process to actually process the encoded data (e.g. to report back to HTTP server ,etc.)
     */
    private final Processor _processor;
    private final String reporterName;

    private final ExecutorService executor;
    private final List<Queue> queues;
    private final ReporterConf reporterConf;

    /**
     * null if not controlled
     */
    private RateLimiter rateLimiter;
    private volatile boolean highLatency = false;
    private volatile long currentProcessDataTime = System.currentTimeMillis();
    private final Path dataPath;

    /**
     * if stopped, report will drop data in queues and no longer receive any data
     */
    private volatile boolean stop = false;

    private static final ScheduledExecutorService PQCRON =
            Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setDaemon(true).setNameFormat("pqcron").build());
    private static final QueueStatusChecker STATUS_CHECKER = new ChainedQueueStatusChecker(new QueueFileSizeStatusChecker(),
            new QueueLatencyStatusChecker());

    public PersistentReporter(String reporterName, Encoder _encoder, Processor _processor, ReporterConf reporterConf) {
        this._encoder = _encoder;
        this._processor = _processor;
        this.reporterName = reporterName;
        this.reporterConf = reporterConf;

        if (reporterConf.qps > 0) {
            rateLimiter = RateLimiter.create(reporterConf.qps);
        }
        // be power of 2.  Now I only have 1 thread. because, if we want to keep the time order
        // we need to understand the internal data.
        int reporterThreads = 1;

        this.executor = Executors.newFixedThreadPool(reporterThreads,
                new ThreadFactoryBuilder().setDaemon(true).setNameFormat("reporter" + reporterName + "-%d").build());

        // generate the persistent queue
        queues = new ArrayList<>();
        String pqdatapath = LayeredConf.getString("pq.path", null);
        if (pqdatapath == null) {
            pqdatapath = Paths.get(ConfConstant.AGENT_BASE_DIR.toString(), "pqdata").toAbsolutePath().toFile().getPath();
        }
        dataPath = Paths.get(pqdatapath, reporterName);
        for (int i = 0; i < reporterThreads; i++) {
            Queue queue = new Queue(dataPath.toString(), reporterName + "-" + i, new JsonReportDataMapper());
            queues.add(queue);
            LOG.info("New pq reporter thread " + dataPath + " " + i);
            this.executor.submit(() -> pollAndProcess(queue));
        }

        PQCRON.scheduleAtFixedRate(() -> checkQueueStatus(), 30, 60, TimeUnit.SECONDS);

        LOG.info("Init pq name " + reporterName + " conf:" + reporterConf);
    }

    private void checkQueueStatus() {
        if (!highLatency && STATUS_CHECKER.isAbnormal(this)) {
            highLatency = true;
        } else if (highLatency && STATUS_CHECKER.isRestored(this)) {
            highLatency = false;
        }
    }

    public Path getDataPath() {
        return dataPath;
    }

    public ReporterConf getReporterConf() {
        return reporterConf;
    }

    public String getReporterName() {
        return reporterName;
    }

    public long getCurrentProcessDataTime() {
        return currentProcessDataTime;
    }

    private void pollAndProcess(Queue<ReportData> q) {
        long maxInQueueTime = TimeUnit.MINUTES.toMillis(reporterConf.dataKeepTimeInMinutes);
        while (true) {
            Pair<Long, ReportData> p = null;
            try {
                if (q.isClosed()) {
                    LOG.warn("Queue is closed " + q.getQueueName() + " break poll process loop");
                    return;
                }

                p = q.poll();
                if (p == null) {
                    Thread.sleep(500);
                    // update this time when queue is empty
                    currentProcessDataTime = System.currentTimeMillis();
                    continue;
                }

                ReportData data = p.getRight();
                currentProcessDataTime = data.generatedTime;

                if (System.currentTimeMillis() - data.generatedTime > maxInQueueTime) {
                    q.delete(p.getLeft());

                    continue;
                }

                // control the speed for reporting....
                if (rateLimiter != null) {
                    rateLimiter.acquire();
                }

                _processor.process(data.publishedProjectId, data.data);
                if (reporterConf.debug) {
                    LOG.info(String.format("[%s]Reported data - %s", reporterName, data.data));
                }
                q.delete(p.getLeft());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
//                // check whether we need to retry:
//                // 1. if it's an io exception
//                // 2. if it's a 500 code exception.
//                if (e instanceof IOException) {
//                    LOG.error("Fail to process, retry " + e.getMessage());
//                } else {
//                    LOG.error("Fail to process and discard it." + e.getMessage());
//                    try {
//                        if (p != null) {
//                            boolean deleted = q.delete(p.getLeft());
//
//                        }
//                    } catch (Exception exception) {
//                    }
//                }

                if (q.isClosed()) {
                    LOG.warn("Queue is closed " + q.getQueueName() + " break poll process loop");
                    return;
                }
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException interruptedException) {
                    break;
                }
            }
        }
    }

    @Override
    public void report(String publishedProjectId, Object data) {
        if (stop) {
            LOG.info("[ignore report:{} with data:{}, this reporter stopped]", publishedProjectId, data);
            return;
        }

        // 进来时尝试一次，失败后进入队列
        try {
            Object encoded = _encoder.encode(data);
            _processor.process(publishedProjectId, encoded);
            return;
        } catch (Exception ignored) {}

        try {
            // 1. common logic to handle the raw data like attaching labels
            // 3. interaction with server
            Object encoded = _encoder.encode(data);
            this.queues.get(0).push(new ReportData(publishedProjectId, encoded, System.currentTimeMillis()));
        } catch (Exception e) {
            LOG.error("Failed to put into queue raw data", e);
        }
    }

    @Override
    public void stop() {
        stop = true;
        this.queues.clear();
        LOG.info("[this reporter:{} stopped, will no longer receive any request and queues data has been cleared]", this);
    }
}
