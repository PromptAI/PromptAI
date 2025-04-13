package com.zervice.kbase.api.restful.pojo;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.zervice.kbase.AccountCatalog;
import com.zervice.kbase.database.pojo.AgentTask;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author Peng Chen
 * @date 2022/6/28
 */
@Setter@Getter
@AllArgsConstructor
@NoArgsConstructor
public class RestAgentTask {

    public RestAgentTask(AgentTask agentTask, String dbName) {
        this._id = agentTask.getId();
        this._name = agentTask.getName();
        this._agentId = agentTask.getAgentId();
        this._status = agentTask.getStatus();
        this._schedule = agentTask.getSchedule();

        this._properties = agentTask.getProperties();
        this._type = agentTask.getType();
        this._publishedProjectId = agentTask.getPublishedProjectId();
        _setUserName(dbName);

    }

    @JsonIgnore
    @JSONField(serialize = false,deserialize = false)
    private void _setUserName(String dbName) {
        if (_properties == null) {
            return;
        }
        AccountCatalog accountCatalog = AccountCatalog.ensure(dbName);

        _createByName = accountCatalog.getUserNameIfExist(_properties.getCreateBy());
        _updateByName = accountCatalog.getUserNameIfExist(_properties.getUpdateBy());
    }

    private Long _id;

    private String _name;

    private String _agentId;

    private Integer _status;

    private Integer _type;

    private Long _schedule;

    private AgentTask.Prop _properties;

    private String _createByName;

    private String _updateByName;

    private String _publishedProjectId;

}
