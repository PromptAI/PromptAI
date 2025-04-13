package com.zervice.kbase.database.pojo;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.zervice.common.utils.TimeUtils;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.apache.commons.collections4.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * if a project release with {@link PublishedProject#PUBLISH_MODEL_SNAPSHOT}, will create a snapshot with all related data.
 * All the data will be stored in {@link Prop#_snapshot}, usually, this data is readonly.
 * </p>
 *
 * @author chenchen
 * @Date 2023/12/4
 */
@Builder
@Setter@Getter
@AllArgsConstructor
@NoArgsConstructor
public class PublishSnapshot {

    public static final String STATUS_INIT = "init";
    public static final String STATUS_RUNNING = "running";
    public static final String STATUS_READY = "ready";
    public static final String STATUS_ERROR = "error";

    private Long _id;

    private String _projectId;
    private String _publishedProjectId;

    private String _status;

    /**
     * like: V202312041234 - it's unique..
     */
    private String _tag;

    private Prop _properties;

    public static PublishSnapshot createPublishSnapshotFromDao(Long id, String projectId, String publishedProjectId,
                                                               String status, String tag, String prop) {
        return PublishSnapshot.builder()
                .id(id).projectId(projectId).status(status)
                .publishedProjectId(publishedProjectId)
                .tag(tag).properties(JSONObject.parseObject(prop, Prop.class))
                .build();
    }

    public static String generateTag() {
        String prefix = "S";
        String timestamp = TimeUtils.format(TimeUtils.PATTERN_TIMESTAMP);

        return prefix + timestamp;
    }

    public void error(String msg) {
        this._status = STATUS_ERROR;
        this._properties._lastMessage = msg;
        this._properties.setUpdateTime(System.currentTimeMillis());
    }

    public void ready() {
        this._status = STATUS_READY;
        this._properties.setUpdateTime(System.currentTimeMillis());
    }

    public  String generateQdrantProjectId() {
        return _tag + "_" + _projectId;
    }

    public boolean isOk() {
        return Objects.equals(STATUS_READY, _status);
    }

    @SuperBuilder
    @Setter @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Prop extends BaseProp {
        private Snapshot _snapshot;

        /**
         * 所属的发布id
         */
        private Long _publishRecordId;

        private String _lastMessage;
    }

    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Snapshot {

        @JsonIgnore
        @JSONField(serialize = false,deserialize = false)
        private Map<String, ProjectComponent> componentMap;
        @JsonIgnore
        @JSONField(serialize = false,deserialize = false)
        /**
         *  区别于正常的qdrant数据，这里通过project做隔离
         *  snapshotTag_projectId
         */
        @Setter@Getter
        private String _qdrantProjectId;
        @Setter@Getter
        private Project _project;
        @Setter@Getter
        private List<ProjectComponent> _flows;
        @Setter@Getter
        private List<ProjectComponent> _components;

        @JsonIgnore
        @JSONField(serialize = false,deserialize = false)
        public Map<String, ProjectComponent> getComMap() {
            if (componentMap == null) {
                synchronized (Snapshot.class) {
                    if (componentMap == null) {
                        componentMap = _components.stream()
                                .collect(Collectors.toMap(ProjectComponent::getId, c -> c));
                    }
                }
            }

            return componentMap;
        }

        public ProjectComponent getById(String id) {
            return getComMap().get(id);
        }

        public List<ProjectComponent> getByIds(Collection<String> ids) {
            if (CollectionUtils.isEmpty(ids)) {
                return Collections.emptyList();
            }

            Map<String, ProjectComponent> map = getComMap();

            List<ProjectComponent> components = new ArrayList<>(ids.size());
            for (String id : ids) {
                ProjectComponent component = map.get(id);
                if (component != null) {
                    components.add(component);
                }
            }

            return components;
        }

    }

}
