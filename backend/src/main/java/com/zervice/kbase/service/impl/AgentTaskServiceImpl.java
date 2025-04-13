package com.zervice.kbase.service.impl;

import com.zervice.common.ding.DingTalkSender;
import com.zervice.common.utils.Constants;
import com.zervice.kbase.ServerInfo;
import com.zervice.kbase.database.dao.AgentTaskDao;
import com.zervice.kbase.database.pojo.AgentTask;
import com.zervice.kbase.database.utils.DaoUtils;
import com.zervice.kbase.service.AgentService;
import com.zervice.kbase.service.AgentTaskService;
import com.zervice.kbase.service.PublishRecordService;
import lombok.Cleanup;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author Peng Chen
 * @date 2022/6/29
 */
@Log4j2
@Service
public class AgentTaskServiceImpl implements AgentTaskService {
    @Autowired
    private AgentService agentService;

    @Autowired
    private PublishRecordService publishRecordService;

    @Async
    @Override
    public void afterTaskFinished(AgentTask task, String message, String dbName) throws Exception {
        publishRecordService.afterTaskEnd(task, message, dbName);

        if (task.isOk()) {
            return;
        }

        long timeConsuming = System.currentTimeMillis() - task.getProperties().getStartTime();
        boolean timeOut = timeConsuming > TimeUnit.SECONDS.toMillis(500);
        // 忽略主动取消且，没有超时的情况的情况
        if (!timeOut && Constants.TASK_TERMINATED_MESSAGE.equals(message)) {
            return;
        }

        String e = String.format("[%s:%s][run task:%d fail:%s spend:%d]", ServerInfo.getName(), dbName, task.getId(), message, timeConsuming);
        DingTalkSender.sendQuietly(e);
    }

    @Override
    public void run(AgentTask task, String dbName) {
        try {
            @Cleanup Connection conn = DaoUtils.getConnection(true);
            agentService.runTask(task, dbName, conn);
        } catch (Exception ex) {
            LOG.error("[{}][run task:{} failed, agentId:{}, error:{}]",
                    dbName, task.getId(), task.getAgentId(), ex.getMessage(), ex);
        }
    }

    @Override
    public boolean hasRunningOrSchedulingTask(String id, Connection conn, String dbName) throws Exception {
        return _existTaskByStatus(id, Arrays.asList(AgentTask.STATUS_EXECUTING, AgentTask.STATUS_SCHEDULE), conn, dbName);
    }

    @Override
    public boolean hasRunningTask(String id, Connection conn, String dbName) throws Exception {
        return _existTaskByStatus(id, Arrays.asList(AgentTask.STATUS_EXECUTING), conn, dbName);
    }

    private boolean _existTaskByStatus(String publishedProjectId, List<Integer> status,
                                       Connection conn, String dbName) throws Exception {
        List<AgentTask> runningTasks = AgentTaskDao.getByStatusIn(conn, dbName, status);
        for (AgentTask agentTask : runningTasks) {
            if (agentTask.getPublishedProjectId().equals(publishedProjectId)) {
                return true;
            }
        }

        return false;
    }
}
