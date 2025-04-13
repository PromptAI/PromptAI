package com.zervice.kbase.api.restful.pojo;

import com.alibaba.fastjson.JSONObject;
import com.zervice.kbase.database.pojo.PublishedProject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Peng Chen
 * @date 2022/8/3
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RestPublishedProject {

    public RestPublishedProject(PublishedProject publishedProject) {
        this._id = publishedProject.getId();
        this._status = publishedProject.getStatus();
        this._agentId = publishedProject.getAgentId();
        this._token = publishedProject.getToken();
        this._properties = publishedProject.getProperties();

        // 这的信息前端不需要
        if (this._properties != null) {
            _properties.setAi(new JSONObject());
        }
    }

    @Deprecated
    private List<RestAgentTask> _recentTasks = new ArrayList<>();

    private String _id;

    private String _status;

    private String _agentId;

    private String _token;

    /**
     * 发布时产生的新任务, 查询project时没有该值St
     */
    private RestAgentTask _newTask;

    private RestPublishRecord _newRecord;

    private PublishedProject.Prop _properties;
    private List<RestPublishRecord> _recentRecords;

    public RestPublishedProject(PublishedProject publishedProject, RestPublishRecord publishRecord,
                                List<RestPublishRecord> recentRecords) {
        this(publishedProject);
        this._recentRecords = recentRecords;
        this._newRecord = publishRecord;
    }


    public boolean published(String rootId) {
        if (rootId == null) {
            return false;
        }
        if (CollectionUtils.isEmpty(_properties.getComponentIds())) {
            return false;
        }

        return _properties.getComponentIds().contains(rootId);
    }

}
