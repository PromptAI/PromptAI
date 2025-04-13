package com.zervice.kbase.database.pojo;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.zervice.common.utils.IdGenerator;
import com.zervice.kbase.api.rpc.pojo.ReportTaskStep;
import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Agent task
 *
 * @author Peng Chen
 * @date 2022/6/16
 */
@Log4j2
@ToString
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AgentTask {

    /** 训练 */
    public static final int TYPE_TRAIN = 1;


    /** 预约中 */
    public static int STATUS_SCHEDULE = 0;
    /** 已取消，未执行过 */
    public static int STATUS_CANCEL = 1;
    /** 执行中 */
    public static int STATUS_EXECUTING = 2;
    /** 终止，执行过程中取消 */
    public static int STATUS_TERMINATE = 3;
    /** 执行失败 */
    public static int STATUS_ERROR = 4;
    /** 执行成功 */
    public static int STATUS_FINISH = 5;

    private Long _id;

    private String _name;

    private String _agentId;

    private Integer _status;

    private Integer _type;

    private Long _schedule;

    private List<TaskStep> _taskSteps;

    /**
     * run on which publishedProject
     */
    private String _publishedProjectId;

    private Prop _properties;

    public boolean isOk() {
        return STATUS_FINISH == _status;
    }

    public void executing() {
        this._status = STATUS_EXECUTING;
        long now = System.currentTimeMillis();
        _properties.setUpdateTime(now);
        _properties.setStartTime(now);
    }

    public void finish(boolean ok, String msg) {
        int status = ok ? AgentTask.STATUS_FINISH : AgentTask.STATUS_ERROR;

        ExecutionRecord record = ExecutionRecord.build(ok, msg);

        this._status = status;
        this.appendStepRecord(record);

        long now = System.currentTimeMillis();
        _properties.setUpdateTime(now);
        _properties.setEndTime(now);

        _properties._calculateElapsed();
    }

    /**
     * 总耗时
     */
    public long usedInMills() {
        Long startTime = _properties.getStartTime();
        Long endTime = _properties.getEndTime();
        if (startTime == null || endTime == null) {
            return -1;
        }

        try {
            return Math.abs(endTime - startTime);
        } catch (Exception e) {
            LOG.error("[:{}][calculate task usedInMills fail:{}]", _publishedProjectId, e.getMessage(), e);
            return -1;
        }
    }

    public void cancel(Long userId) {
        cancel(userId, null);
    }

    public void cancel(Long userId, String remark) {
        if (this._status == STATUS_EXECUTING) {
            this._status = STATUS_TERMINATE;
        } else if (this._status == STATUS_SCHEDULE) {
            this._status = STATUS_CANCEL;
        }
        long now = System.currentTimeMillis();
        _properties.setUpdateTime(System.currentTimeMillis());
        _properties.setUpdateBy(userId);

        ExecutionRecord record = ExecutionRecord.build(false, remark);
        appendStepRecord(record);

        if (_properties.getStartTime() != null) {
            _properties.setEndTime(now);
        }
        _properties.setRemark(remark);
    }

    public static long generateId() {
        return IdGenerator.generateId();
    }

    public void appendStepRecord(ExecutionRecord record) {
        if (CollectionUtils.isEmpty(this._properties._records)) {
            this._properties._records = new ArrayList<>();
        }

        this._properties._records.add(record);
    }

    public static AgentTask createAgentTaskFromDao(Long id, String name,
                                                   String agentId, Integer status,
                                                   Integer type, Long schedule, String taskSteps,
                                                   String publishedProjectId, String properties) {
        return AgentTask.builder()
                .id(id).name(name).agentId(agentId)
                .status(status).type(type).schedule(schedule)
                .publishedProjectId(publishedProjectId)
                .taskSteps(JSONArray.parseArray(taskSteps, TaskStep.class))
                .properties(JSONObject.parseObject(properties, Prop.class))
                .build();
    }

    /**
     * 渲染参数分两步：
     * 1、训练文件上下文
     * 2、AI 模型上下文 (这一步放到执行去做，生成任务时AI还未启动,读取不到这部分参数)
     * 这里执行第二步
     * @param params
     */
    public void renderSteps(Map<String, String> params) {
        for (TaskStep step : _taskSteps) {
            step.render(params);
        }
    }

    private long _calculateExecutionEscape(Set<String> steps) {
        if (CollectionUtils.isEmpty(_properties._records)) {
            return 0L;
        }
        return _properties._records.stream()
                .filter(r -> StringUtils.isNotBlank(r.getStep()) && steps.contains(r.getStep()))
                .mapToLong(ExecutionRecord::getElapsed)
                .sum() / 1000;
    }


    @Getter
    @Setter
    @SuperBuilder
    @NoArgsConstructor
    @ToString
    public static class Prop extends BaseProp {

        /**
         * 所属的发布id
         */
        private Long _publishRecordId;

        private String _trainDataPath;

        /**
         * train data digest
         */
        private String _trainDataDigest;

        private String _agentName;

        private String _remark;

        private List<ExecutionRecord> _records;

        private List<TaskProgress> _taskProgress;


        private Long _startTime;

        private Long _endTime;

        private Long _elapsed;

        private List<String> _componentIds;

        /**
         * 预估数据准备阶段时长
         * TODO：目前在训练前会让容器重启
         */
        @Builder.Default
        private Long _prepareTimeInSec = 100L;

        /**
         * 预估训练阶段时长
         */
        @Builder.Default
        private Long _trainingTimeInSec = 120L;

        /**
         * 预估替换模型准备阶段时长
         */
        @Builder.Default
        private Long _replaceModelTimeInSec = 90L;

        /**
         * 训练时间是否从上一个任务
         * 这里比决定前端显示
         *  - false： 预估时间
         *  - ture： 上次耗时
         */
        @Builder.Default
        private Boolean _timeFromLastTask = false;

        /**
         * 如果是部署的task，这里保存部署的模型id
         * 如果是训练的task，这里保存训练的模型id (如果有模型训练出来)
         */
        private Long _relatedModelId;

        public void _calculateElapsed() {
            if (CollectionUtils.isEmpty(_records)) {
                return;
            }

            _elapsed = _records.stream()
                    .mapToLong(ExecutionRecord::getElapsed)
                    .sum();
        }
    }

    @Getter
    @Setter
    @SuperBuilder
    @NoArgsConstructor
    @ToString
    public static class ExecutionRecord {
        private Long _createTime;
        /** name of step */
        private String _step;
        /** index of step */
        private Integer _index;
        private String _content;
        private Boolean _ok;
        /** spend time in millis */
        @Builder.Default
        private Long _elapsed = 0L;

        public static ExecutionRecord build(boolean ok, String content) {
            return ExecutionRecord.builder()
                    .ok(ok)
                    .content(content)
                    .createTime(System.currentTimeMillis())
                    .build();
        }

        public static ExecutionRecord from(ReportTaskStep taskRecord) {
            // TODO  fix me 这里有特殊字符导致更新db失败 需要更通用些
            String message = taskRecord.getMessage();
            String clearedMessage = StringUtils.isBlank(message) ? message : message.replace("\uD83D\uDEA8\n", "");

            return ExecutionRecord.builder()
                    .content(clearedMessage)
                    .step(taskRecord.getStep())
                    .createTime(System.currentTimeMillis())
                    .index(taskRecord.getIndex())
                    .ok(taskRecord.getOk() != null && taskRecord.getOk())
                    .elapsed(taskRecord.getElapsed())
                    .build();
        }
    }


    @Data
    @SuperBuilder
    @NoArgsConstructor
    public static class TaskProgress {
        /**
         * 准备数据
         */
        public static final String TASK_STAGE_PREPARE_DATA = "STAGE_PREPARE_DATA";
        /**
         * 训练模型
         */
        public static final String TASK_STAGE_TRAIN_MODEL = "STAGE_TRAIN_MODEL";
        /**
         * 部署模型
         */
        public static final String TASK_STAGE_DEPLOY_MODEL = "DEPLOY_MODEL";
        /**
         * 结束 - 这儿不用给前端展示
         */
        public static final String TASK_STAGE_FINISH = "STAGE_FINISH";

        /** 训练模型 */
        private static List<String> PROGRESS_TRAIN_STAGES = new ArrayList<>();
        /** 替换模型 */
        private static List<String> PROGRESS_REPLACE_STAGES = new ArrayList<>();
        static {
            PROGRESS_TRAIN_STAGES.add(TASK_STAGE_PREPARE_DATA);
            PROGRESS_TRAIN_STAGES.add(TASK_STAGE_TRAIN_MODEL);
            PROGRESS_TRAIN_STAGES.add(TASK_STAGE_DEPLOY_MODEL);
            PROGRESS_TRAIN_STAGES.add(TASK_STAGE_FINISH);

            PROGRESS_REPLACE_STAGES.add(TASK_STAGE_PREPARE_DATA);
            PROGRESS_REPLACE_STAGES.add(TASK_STAGE_DEPLOY_MODEL);
            PROGRESS_REPLACE_STAGES.add(TASK_STAGE_FINISH);

        }

        public static List<String> _stages(int taskType) {
            if (AgentTask.TYPE_TRAIN == taskType) {
                return PROGRESS_TRAIN_STAGES;
            }

            LOG.error("[unknown task type:{}]", taskType);
            throw new RuntimeException("unknown task type");
        }

        public static Integer stageIndex(String stageName, int taskType) {
            List<String> stages = _stages(taskType);
            for (int i = 0; i < stages.size(); i++) {
                if (stageName.equals(stages.get(i))) {
                    return i;
                }
            }

            return null;
        }


        public static final String PERCENT_FINISH = "100%";

        /** 前端展示用的name */
        private String _stageName;
        /** 训练任务进度百分比 */
        private String _stepPercent;
        /** index of step */
        private Integer _index;
        private Integer _usedInSec;
        private Boolean _ok;

        public static TaskProgress finish(String stage, int index) {

            return TaskProgress.builder()
                    .stageName(stage)
                    .stepPercent(PERCENT_FINISH)
                    .usedInSec(0)
                    .index(index).ok(true)
                    .build();
        }

        public static TaskProgress factory(String stageName, String stepPercent,
                                           int usedInSec, int index) {
            return TaskProgress.builder()
                    .stageName(stageName)
                    .stepPercent(stepPercent)
                    .usedInSec(usedInSec)
                    .index(index)
                    .build();
        }
    }

}

