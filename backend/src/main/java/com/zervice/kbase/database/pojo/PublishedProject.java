package com.zervice.kbase.database.pojo;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.zervice.common.utils.Constants;
import com.zervice.kbase.AccountCatalog;
import com.zervice.kbase.database.SecurityUtils;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

/**
 * Record published projects.
 * There exists a debug only record, this record id end with debug
 *
 * {
 *   id: string, not null unique, // account_name(dbName_projectId/ dbName_debug)
 *   status: string, not null, //deploying, running, not_running
 *   agentId: string not null, // flow agent id
 *   token: string,  // random
 *   properties: {
 *     projectId: string, //  belong to which project
 *     createTime: long, // extends from base prop
 *     createBy: long,
 *     updateBy: long,
 *     updateTime: long
 *     modelId: long, // which model is being used
 *     ai: {
 *         "ai.trainPath": "/root/.ai/train",
 *         "ai.agentId": "mcXMM8MMRPLPSU",
 *         "ai.logPath": "/root/.ai/log",
 *         "ai.usingGpu": false,
 *         "ai.containerName": string,
 *         "ai.modelPath": "/root/.ai/model"
 *     }
 *   }
 * }
 * @author Peng Chen
 * @date 2022/8/3
 */
public class PublishedProject {

    /**
     * The test only publish project's id end with 'debug'
     */
    public static final String TEST_PROJECT_ID_SUFFIX = "debug";

    /** deploying */
    public static final String STATUS_DEPLOYING ="deploying";
    /** running */
    public static final String STATUS_RUNNING ="running";
    /** not running */
    public static final String STATUS_NOT_RUNNING ="not_running";

    /**
     * read data from db at runtime
     */
    public static final String PUBLISH_MODEL_DEFAULT = "default";
    /**
     * read data from snapshot at runtime
     */
    public static final String PUBLISH_MODEL_SNAPSHOT = "snapshot";

    private static final String _EXTERNAL_ID_SEPARATOR = "_";

    public static final Set<String> VALID_STATUS = new HashSet<>();

    static {
        VALID_STATUS.add(STATUS_RUNNING);
        VALID_STATUS.add(STATUS_DEPLOYING);
    }


    @Setter
    @Getter
    private String _id;

    @Setter
    @Getter
    private String _status;

    @Setter
    @Getter
    private String _agentId;

    @Setter
    @Getter
    private String _token;

    @Setter
    @Getter
    private Prop _properties;

    public boolean isDebug() {
        return _id.endsWith(TEST_PROJECT_ID_SUFFIX);
    }

    public static PublishedProject createPublishProjectFromDao(String id, String status, String agentId,
                                                               String token, String properties) {
        PublishedProject publishProject = new PublishedProject();
        publishProject.setId(id);
        publishProject.setStatus(status);
        publishProject.setAgentId(agentId);
        publishProject.setToken(token);
        publishProject.setProperties(JSONObject.parseObject(properties, Prop.class));
        return publishProject;
    }

    public static PublishedProject factory(String projectId, String publishedProjectId, String agentId, String status,
                                           String publishModel, Long modelId,
                                           long userId) {
        PublishedProject publishProject = new PublishedProject();
        publishProject.setId(publishedProjectId);
        publishProject.setAgentId(agentId);
        publishProject.setStatus(status);
        publishProject.setToken(SecurityUtils.generateCollectorAccessKey());

        // build prop
        Prop prop = Prop.factory(projectId, publishModel,  modelId, userId);
        publishProject.setProperties(prop);

        return publishProject;
    }

    /**
     * build id with externalAccountID + "_" +projectId
     */
    public static String generateId(String externalAccountId, String projectId) {
        AccountCatalog.ensure(externalAccountId);
        return externalAccountId + _EXTERNAL_ID_SEPARATOR + projectId;
    }

    public static String getDbNameFromId(String id) {
        String[] spiltId = id.split(_EXTERNAL_ID_SEPARATOR);
        if (spiltId.length >= 2 ) {
            return id.split(_EXTERNAL_ID_SEPARATOR)[0];
        }

        throw new IllegalArgumentException("invalid published project id:" + id);
    }

    public static String getProjectIdFromId(String id) {
        String[] spiltId = id.split(_EXTERNAL_ID_SEPARATOR);
        if (spiltId.length < 2) {
            throw new IllegalArgumentException("invalid published project id:" + id);
        }

        // a1_p_bwahlwrdiqkg
        // a1_debug
        return id.substring(id.indexOf(_EXTERNAL_ID_SEPARATOR) + 1);
    }

    public static boolean isTestProject(String publishedProjectId) {
        return publishedProjectId.endsWith(TEST_PROJECT_ID_SUFFIX);
    }

    public static String generateDebugPublishId(String dbName) {
        return dbName + _EXTERNAL_ID_SEPARATOR + TEST_PROJECT_ID_SUFFIX;
    }

    public String getDbName() {
        return getDbNameFromId(_id);
    }

    public boolean modelLoaded() {
        return StringUtils.isNotBlank(_properties.getAi().getString(Constants.AGENT_CURRENT_MODEL));
    }

    public boolean hasModel() {
        return _properties.getModelId() != null;
    }

    public void clearModel() {
        _properties.setModelId(null);
        _properties._componentIds = Collections.emptyList();
    }

    @JsonIgnore
    @JSONField(serialize = false, deserialize = false)
    public String getImageId() {
        if (_properties.getAi() == null) {
            return null;
        }
        return _properties.getAi().getString(Constants.AGENT_IMAGE_ID);
    }

    @Getter
    @Setter
    @SuperBuilder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Prop extends BaseProp {

        /**
         * 发布模式
         */
        @Builder.Default
        private String _publishModel = PUBLISH_MODEL_DEFAULT;

        private String _projectId;
        private Long _modelId;
        private JSONObject _ai;


        /**
         * optional snapshot id
         */
        private Long _publishSnapshotId;

        public static Prop factory(String projectId, String publishModel, Long modelId, long userId) {
            long now = System.currentTimeMillis();
            return Prop.builder()
                    .projectId(projectId).modelId(modelId)
                    .publishModel(publishModel)
                    .createBy(userId).updateBy(userId)
                    .createTime(now).updateTime(now)
                    .build();
        }

        /**
         * 记录发布的flow/faq ids
         */
        private List<String> _componentIds;

        /**
         * 正在发布的flow/faqIds
         */
        private List<String> _publishingIds;
    }

    public void fillAiParam(Map<String, String> param) {
        JSONObject ai = _properties.getAi();
        for (String key : ai.keySet()) {
            Object val = ai.get(key);
            if (val instanceof String) {
                param.put("{" + key + "}", (String) val);
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PublishedProject that = (PublishedProject) o;
        return Objects.equals(_id, that._id) && Objects.equals(_agentId, that._agentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(_id, _agentId);
    }

    public void stop(Long userId) {
        _status = STATUS_NOT_RUNNING;
        _properties.setUpdateTime(System.currentTimeMillis());
        _properties.setUpdateBy(userId);
        _properties.setModelId(null);
    }
}
