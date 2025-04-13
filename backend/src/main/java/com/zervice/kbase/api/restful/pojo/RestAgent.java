package com.zervice.kbase.api.restful.pojo;

import com.zervice.kbase.database.pojo.Agent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author chen
 * @date 2023/5/5 10:27
 */
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class RestAgent {

    public RestAgent(Agent agent, Integer running) {
        this(agent);
        this._running = running;
    }

    public RestAgent(Agent agent) {
        this._id = agent.getId();
        this._dbName = agent.getDbName();
        this._status = agent.getStatus();
        this._name = agent.getProperties().getName();
        this._lastConnectIn = agent.getProperties().getLastConnectIn();
        this._default = agent.getProperties().getDefault();
        this._hasGpu = agent.getProperties().getHasGpu();
        this._canUseGpu = agent.getProperties().getCanUseCpu();
        this._version = agent.getProperties().getVersion();
        this._createTime = agent.getProperties().getCreateTime();
        this._ip = agent.getProperties().getIp();
    }

    /**
     * same with zp agent id
     */
    private String _id;

    /**
     * who own this agent (indicating this agent installed by account)
     * AccountDbName
     */
    private String _dbName;


    private Integer _status;

    private String _name;

    private Long _lastConnectIn;

    /**
     * 是否为默认的
     */
    private Boolean _default;

    /**
     * 运行发布容器的数量
     */
    private Integer _running;

    private String _version;

    private Boolean _hasGpu;

    private Boolean _canUseGpu;

    private Long _createTime;

    private String _ip;

    /**
     * 在界面中初始化一个agent，如果db下有了，则无需创建
     */
    private Boolean _init;
}
