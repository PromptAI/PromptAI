package com.zervice.common.agent.pojo;

import lombok.*;

import java.util.Objects;

/**
 * 提供给Broker 使用
 * @author Peng Chen
 * @date 2022/8/10
 */
@ToString
@Setter@Getter
@NoArgsConstructor
public class SimplePublishedProject {

    /** deploying */
    public static final String STATUS_DEPLOYING ="deploying";
    /** running */
    public static final String STATUS_RUNNING ="running";
    /** not running */
    public static final String STATUS_NOT_RUNNING ="not_running";

    public SimplePublishedProject(String id, String projectId, String agentId,
                                  String status, String token,
                                 String accountDbName) {
        this._id = id;
        this._projectId = projectId;
        this._agentId = agentId;
        this._status = status;
        this._token = token;
        this._accountDbName = accountDbName;
    }

    @Setter
    @Getter
    private String _id;

    @Setter @Getter
    private String _status;

    @Setter @Getter
    private String _accountDbName;

    @Setter @Getter
    private String _token;

    @Setter @Getter
    private String _runModel;

    @Setter @Getter
    private String _agentId;

    @Setter @Getter
    private String _projectId;

    /**
     * 是否来自发布
     */
    public boolean publish() {
        return !_id.endsWith("debug");
    }
    public Boolean running() {
        return STATUS_RUNNING.equals(_status);
    }

    public static String getProjectId(String publishedProjectId) {
        try {
            return publishedProjectId.split("_")[1];

        }catch (Exception e){
            return null;
        }
    }

    public boolean validToken(String token) {
        return _token.equals(token);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SimplePublishedProject that = (SimplePublishedProject) o;
        return Objects.equals(_id, that._id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(_id);
    }
}
