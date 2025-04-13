package com.zervice.kbase.database.pojo;

import com.alibaba.fastjson.JSONObject;
import com.zervice.common.agent.AgentClient;
import com.zervice.common.utils.Base36;
import com.zervice.common.utils.IdGenerator;
import com.zervice.kbase.database.SecurityUtils;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Agent will install on cloud or users private machine.
 * For now, agent communicate with backend via Http protocol using Ngrok.
 * <p>
 * There are two types of Agent:
 * - The System provide: system provide to user run debug or publish
 * - The User provide: user provide to run debug and publish
 * <p>
 * <p>
 * The steps of create new agent:
 * 1、Apply for an agent by user (Init an agent on Backend);
 * 2、User copy install command of the agent and run on their own machine;
 *
 * @author Peng Chen
 * @date 2022/6/16
 */
@ToString
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Agent {

    /**
     * indicating this account provide by system
     */
    public static final String PUBLIC_DBNAME = "-";


    /**
     * not ready: may the agent installing ?
     */
    public static final int STATUS_INSTALLING = 0;

    /**
     * active
     */
    public static final int STATUS_ACTIVE = 1;

    /**
     * loss connection...
     */
    public static final int STATUS_DEAD = 2;

    /**
     * same with zp agent id
     */
    private String _id;

    /**
     * who own this agent (indicating this agent installed by account)
     * AccountDbName
     */
    private String _dbName;

    /**
     * access key
     */
    private String _ak;

    private Integer _status;

    private Prop _properties;

    /**
     * account's agent
     */
    public boolean isPrivate() {
        return !PUBLIC_DBNAME.equals(_dbName);
    }

    public static Agent factory(String dbName, long userId) {
        Prop prop = Prop.builder()
                .createBy(userId)
                .createTime(System.currentTimeMillis())
                .build();

        return Agent.builder()
                .id(generateId())
                .dbName(dbName)
                .ak(generateAk())
                .status(STATUS_INSTALLING)
                .properties(prop)
                .build();
    }

    /**
     * 设置为默认的Agent
     *
     * @param defaultAgent true/ false
     * @return true 设置更新了，需要更新到db，false 无需更新到db
     */
    public boolean setDefault(boolean defaultAgent) {
        Boolean defaultInProp = _properties._default;
        if (defaultInProp == null) {
            _properties.setDefault(defaultAgent);
            return true;
        }

        if (defaultAgent != defaultInProp) {
            _properties.setDefault(defaultAgent);
            return true;
        }

        return false;
    }

    private static String generateAk() {
        return SecurityUtils.generateCollectorAccessKey();
    }

    public static String generateId() {
        return "ag" + Base36.encode(IdGenerator.generateId());
    }

    public String getPublicUrl() {
        return _properties.getPublicUrl();
    }

    public AgentClient toAgentClient() {
        return new AgentClient(_id, getPublicUrl(), AgentClient.STATUS.valueOf(_status));
    }

    public static Agent createAgentFromDao(String id, String dbName, String ak,
                                           Integer status, String properties) {
        return Agent.builder()
                .id(id).dbName(dbName).ak(ak).status(status)
                .properties(JSONObject.parseObject(properties, Prop.class))
                .build();

    }

    @Getter
    @Setter
    @SuperBuilder
    @NoArgsConstructor
    @ToString
    public static class Prop extends BaseProp {
        private String _name;

        private String _ip;

        private String _publicUrl;

        private Long _lastConnectIn;

        /**
         * 是否为默认的
         */
        @Builder.Default
        private Boolean _default = false;

        /**
         * agent version
         */
        private String _version;

        /**
         * 是否有Nvidia驱动
         */
        private Boolean _hasGpu;

        /**
         * 是否装了Nvidia container cli
         */
        private Boolean _canUseCpu;

        /**
         * this command is zp-agent install command
         */
        private String _installCommand;
    }

}
