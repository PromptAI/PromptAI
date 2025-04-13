package com.zervice.agent.report;

import com.zervice.agent.report.processor.PublishedProjectProcessor;
import com.zervice.agent.report.processor.TaskResultProcessor;
import com.zervice.agent.report.processor.TaskStepProcessor;
import com.zervice.agent.report.tool.JsonDataEncoder;
import com.zervice.agent.report.tool.Reporter;
import com.zervice.agent.report.tool.ReporterConf;
import com.zervice.agent.service.BackendClient;
import com.zervice.common.utils.LayeredConf;
import lombok.Getter;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;

/**
 * @author chen
 * @date 2022/9/1
 */

@UtilityClass
public class Reporters {
    private final BackendClient _backendClient = BackendClient.getInstance();

    @Getter
    private Reporter _publishedProjectReporter = new PersistentReporter("reporter.published.project", JsonDataEncoder.instance,
            new PublishedProjectProcessor(_backendClient), buildConf());

    @Getter
    private Reporter _taskStepReporter = new PersistentReporter("reporter.task.step", JsonDataEncoder.instance,
            new TaskStepProcessor(_backendClient), buildConf());

    @Getter
    private Reporter _taskResultReporter = new PersistentReporter("reporter.task.result", JsonDataEncoder.instance,
            new TaskResultProcessor(_backendClient), buildConf());


    private static List<Reporter> reporters = new ArrayList<>();
    static {
        reporters.add(getTaskStepReporter());
        reporters.add(getTaskResultReporter());
        reporters.add(getPublishedProjectReporter());
    }

    public void stop() {
        for (Reporter reporter : reporters) {
            reporter.stop();
        }
    }
    private ReporterConf buildConf() {
        return new ReporterConf.ReporterConfBuilder()
                .debug(LayeredConf.getBoolean("reporter.logs.debug", false))
                .qps(LayeredConf.getInt("reporter.logs.qps", -1))
                .dataKeepSizeInMBytes(LayeredConf.getInt("reporter.logs.fileRetentionMB", -1))
                .dataKeepTimeInMins(LayeredConf.getInt("reporter.logs.inmins", 60)).build();
    }


}
