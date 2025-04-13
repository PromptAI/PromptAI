package com.zervice.kbase.api.rpc;

import com.alibaba.fastjson.JSONObject;
import com.zervice.common.pojo.common.EmptyResponse;
import com.zervice.common.restful.exception.RestException;
import com.zervice.common.restful.exception.StatusCodes;
import com.zervice.common.utils.DownloadUtil;
import com.zervice.kbase.api.BaseController;
import com.zervice.kbase.api.restful.AuthFilter;
import com.zervice.kbase.api.rpc.pojo.ReportTaskResult;
import com.zervice.kbase.api.rpc.pojo.ReportTaskStep;
import com.zervice.kbase.database.dao.AgentTaskDao;
import com.zervice.kbase.database.pojo.AgentTask;
import com.zervice.kbase.database.utils.DaoUtils;
import com.zervice.kbase.service.AgentTaskService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Cleanup;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.sql.Connection;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Peng Chen
 * @date 2022/6/16
 */
@Log4j2
@RestController
@RequestMapping("/rpc/agent/task")
public class RpcAgentTaskController extends BaseController {
    @Autowired
    private AgentTaskService agentTaskService;

    private static final int ES_COUNT = 10;
    private static ExecutorService[] executorService = new ExecutorService[ES_COUNT];
    private static ConcurrentMap<Long, List<ReportTaskStep>> stepRecordsCacheMap = new ConcurrentHashMap<>(200);

    static {
        for (int i = 0; i < executorService.length; i++) {
            executorService[i] = Executors.newSingleThreadExecutor();
        }
    }

    /**
     * agent download train data, if the task is a train task
     */
    @GetMapping("download/{taskId}")
    public void download(@PathVariable Long taskId,
                         HttpServletResponse response,
                         @RequestHeader(RpcFilter.X_EXTERNAL_AGENT_ID_HEADER) String agentId,
                         @RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName) throws Exception {
        @Cleanup Connection conn = DaoUtils.getConnection(true);

        AgentTask agentTask = AgentTaskDao.get(conn, dbName, taskId);
        if (agentTask == null || StringUtils.isBlank(agentTask.getProperties().getTrainDataPath())) {
            LOG.warn("[{}][agent:{} get train data fail, this task:{} not exist or no train data]", dbName, agentId, taskId);
            throw new RestException(StatusCodes.NotFound);
        }

        // not executing or schedule
        if (AgentTask.STATUS_EXECUTING != agentTask.getStatus() && AgentTask.STATUS_SCHEDULE != agentTask.getStatus()) {
            LOG.warn("[{}] can't download not executing agent task.id:{} , status:{}", dbName, agentTask.getId(), agentTask.getStatus());
            throw new RestException(StatusCodes.AiAgentTaskNotSupport);
        }
        String trainDataPath = agentTask.getProperties().getTrainDataPath();

        File trainData = new File(trainDataPath);
        if (!trainData.exists()) {
            LOG.warn("[{}][agent:{} get train data fail, task:{} train data not found in:{}]", dbName, agentId, taskId, trainDataPath);
            throw new RestException(StatusCodes.NotFound);
        }

        DownloadUtil.download(response, trainData);

        LOG.info("[{}:{}][success download task:{} train data:{}]", dbName, agentId, taskId, trainDataPath);
    }

    @PostMapping("report/result")
    public Object reportTaskResult(@RequestBody ReportTaskResult result,
                                   @RequestHeader(RpcFilter.X_EXTERNAL_AGENT_ID_HEADER) String agentId,
                                   @RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName) throws Exception {
        @Cleanup Connection conn = DaoUtils.getConnection(true);

        // 1、save task step
        AgentTask agentTask = AgentTaskDao.get(conn, dbName, result.getTaskId());
        if (agentTask == null) {
            LOG.warn("[{}][ignore record task result log, task:{} not exits]", dbName, result.getTaskId());
            return EmptyResponse.empty();
        }

        // TODO  fix me 这里有特殊字符导致更新db失败 需要更通用些
        String message = result.getMessage();
        message = StringUtils.isBlank(message) ? message : message.replace("\uD83D\uDEA8\n", "");
        agentTask.finish(result.getOk(), message);

        AgentTaskDao.update(conn, dbName, agentTask);

        agentTaskService.afterTaskFinished(agentTask, message, dbName);
        return EmptyResponse.empty();
    }

    @PostMapping("report/step")
    public Object reportTaskStep(@RequestBody ReportTaskStep stepRecord,
                                 @RequestHeader(RpcFilter.X_EXTERNAL_AGENT_ID_HEADER) String agentId,
                                 @RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName) throws Exception {

        int chooseThread = (int) (stepRecord.getTaskId() & Integer.MAX_VALUE) % ES_COUNT;
        Long taskId = stepRecord.getTaskId();
        //增加日志，方便排查step是否存上
        LOG.info("[{}:{}] [rec task record. record:{}]", dbName, taskId, JSONObject.toJSONString(stepRecord));
        _cacheAddStepRecord(taskId, Arrays.asList(stepRecord));
        executorService[chooseThread].submit(() -> {
            try {
                _saveTaskStep(taskId, dbName);
            } catch (RestException restException) {
                throw restException;
            } catch (Exception e) {
                LOG.error("[{}][save task:{} step:{} fail:{}]", dbName, stepRecord.getTaskId(),
                        JSONObject.toJSONString(stepRecord), e.getMessage(), e);
                throw new RestException(StatusCodes.InternalError, e.getMessage());
            }
        }).get();

        return EmptyResponse.empty();
    }

    private void _saveTaskStep(Long taskId, String dbName) throws Exception {

        @Cleanup Connection conn = DaoUtils.getConnection(false);
        AgentTask agentTask = AgentTaskDao.get(conn, dbName, taskId);
        if (agentTask == null) {
            LOG.warn("[{}][ignore record task stop log, task:{} not exits]", dbName, taskId);
            return;
        }
        List<ReportTaskStep> records = _cacheGetStepRecords(taskId);
        Set<ReportTaskStep> stepRecords = new HashSet<>(records);
        try {
            for (ReportTaskStep record : stepRecords) {
                agentTask.appendStepRecord(AgentTask.ExecutionRecord.from(record));
            }
            AgentTaskDao.update(conn, dbName, agentTask);
            conn.commit();
        } catch (Exception ex) {
            conn.rollback();
            conn.commit();
            LOG.error("[{}][save task step records fail. taskId:{}, error:{}]",
                    dbName, taskId, ex.getMessage(), ex);
            //如果更新失败，必须重新加进去，避免记录丢失
            _cacheAddStepRecord(taskId, records);
        }
    }

    private synchronized void _cacheAddStepRecord(Long taskId, List<ReportTaskStep> taskStepRecords) {
        List<ReportTaskStep> records = stepRecordsCacheMap.get(taskId);
        if (records == null) {
            records = new ArrayList<>();
        }
        records.addAll(taskStepRecords);
        stepRecordsCacheMap.put(taskId, records);
    }

    private synchronized List<ReportTaskStep> _cacheGetStepRecords(Long taskId) {
        List<ReportTaskStep> records = stepRecordsCacheMap.get(taskId);
        stepRecordsCacheMap.remove(taskId);
        return records;
    }
}
