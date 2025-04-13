package com.zervice.kbase.database.pojo;

import com.alibaba.fastjson.JSONObject;
import com.zervice.common.utils.IdGenerator;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * project release record
 *
 * @author chenchen
 * @Date 2023/12/5
 */
@Builder
@Getter @Setter
@AllArgsConstructor
@NoArgsConstructor
public class PublishRecord {
    public static final String STATUS_RUNNING = "running";
    public static final String STATUS_CANCEL = "cancel";
    public static final String STATUS_FAILED = "failed";
    public static final String STATUS_SUCCESS = "success";

    private Long _id;

    private String _projectId;

    private String _publishedProjectId;

    private String _status;

    private Prop _properties;

    public void cancel(Long userId) {
        this._status = STATUS_CANCEL;
        this._properties.setUpdateTime(System.currentTimeMillis());
        this._properties.setUpdateBy(userId);
    }

    public static Long generateId() {
        return IdGenerator.generateId();
    }

    public static PublishRecord createPublishRecordFromDao(Long id, String  projectId,
                                                           String publishedProjectId, String status,
                                                           String prop) {
        return PublishRecord.builder()
                .id(id).projectId(projectId).publishedProjectId(publishedProjectId)
                .status(status).properties(JSONObject.parseObject(prop, Prop.class))
                .build();
    }

    public static PublishRecord factory(String projectId, String publishedProjectId,
                                        List<String> publishRoots,long userId) {
        Prop prop = Prop.builder()
               .publishRoots(publishRoots)
                .stages(new ArrayList<>())
                .createTime(System.currentTimeMillis()).createBy(userId)
                .build();

        return PublishRecord.builder()
                .id(generateId())
                .status(STATUS_RUNNING).projectId(projectId)
                .publishedProjectId(publishedProjectId).properties(prop)
                .build();

    }

    public void checkSuccess() {
        if (isFinished()) {
            this._status = STATUS_SUCCESS;
        }
    }

    public void success() {
        this._status = STATUS_SUCCESS;
        this._properties.setUpdateTime(System.currentTimeMillis());
    }

    public void failed() {
        this._status = STATUS_FAILED;
        this._properties.setUpdateTime(System.currentTimeMillis());
    }

    public boolean running() {
        return STATUS_RUNNING.equals(_status);
    }

    public boolean isFinished() {
        if (CollectionUtils.isEmpty(_properties._stages)) {
            return true;
        }

        for (Stage stage : _properties._stages) {
            if (!Objects.equals(Stage.STATUS_FINISH, stage._status)) {
                return false;
            }
        }

        return true;
    }

    public Stage getStage(String name) {
        if (CollectionUtils.isEmpty(_properties._stages)) {
            return null;
        }

        for (Stage stage : _properties._stages) {
            if (Objects.equals(name, stage._name)) {
                return stage;
            }
        }

        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PublishRecord that = (PublishRecord) o;
        return Objects.equals(_id, that._id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(_id);
    }

    @Setter
    @Getter
    @SuperBuilder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Prop extends BaseProp {
        @Builder.Default
        private List<Stage> _stages = new ArrayList<>();

        private String _runModel;
        private String _publishModel;

        /**
         * 如果是部署的task，这里保存部署的模型id
         * 如果是训练的task，这里保存训练的模型id (如果有模型训练出来)
         */
        private Long _relatedModelId;

        /**
         * 如果有快照产生，这里对应快照..
         * {@link PublishRecord#_id}
         */
        private Long _relatedSnapshotId;

        private List<String> _publishRoots;
    }

    @Builder
    @Setter
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Stage {
        public static final String NAME_AGENT_TASK = "agent_task";
        public static final String NAME_AGENT_PUBLISH = "agent_publish";
        public static final String NAME_SNAPSHOT = "snapshot";

        public static final String STATUS_NOT_START = "not_start";
        public static final String STATUS_RUNNING = "status_running";
        public static final String STATUS_FILED = "failed";
        public static final String STATUS_FINISH = "finish";

        private String _name;

        private Long _refId;

        private Long _createTime;

        private Long _startTime;

        private Long _endTime;

        private String _status;

        private String _lastMessage;

        public static Stage factory(String name, Long refId) {
            return Stage.builder()
                    .name(name).refId(refId).status(STATUS_NOT_START)
                    .createTime(System.currentTimeMillis())
                    .build();
        }

        public boolean isNotStart() {
            return Objects.equals(STATUS_NOT_START, _status);
        }

        public void start() {
            this._status = STATUS_RUNNING;
            this._startTime = System.currentTimeMillis();
        }
        public void finish() {
            this._status = STATUS_FINISH;
            this._endTime = System.currentTimeMillis();
        }

        public void failed(String msg) {
            this._lastMessage = msg;
            this._status = STATUS_FILED;
            this._endTime = System.currentTimeMillis();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Stage stage = (Stage) o;
            return Objects.equals(_name, stage._name) && Objects.equals(_refId, stage._refId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(_name, _refId);
        }
    }


}
