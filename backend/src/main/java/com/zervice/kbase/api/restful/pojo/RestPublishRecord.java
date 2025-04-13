package com.zervice.kbase.api.restful.pojo;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.zervice.kbase.AccountCatalog;
import com.zervice.kbase.database.pojo.PublishRecord;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author chenchen
 * @Date 2023/12/11
 */
@Setter@Getter
@AllArgsConstructor
@NoArgsConstructor
public class RestPublishRecord {

    private Long _id;
    private String _projectId;
    private String _publishedProjectId;
    private String _status;
    private PublishRecord.Prop _properties;

    private String _createByName;
    private String _updateByName;

    public RestPublishRecord(PublishRecord publishRecord,String dbName) {
        this._id = publishRecord.getId();
        this._projectId = publishRecord.getProjectId();
        this._publishedProjectId = publishRecord.getPublishedProjectId();
        this._status = publishRecord.getStatus();
        this._properties = publishRecord.getProperties();

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
}
