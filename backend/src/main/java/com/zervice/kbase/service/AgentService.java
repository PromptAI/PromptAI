package com.zervice.kbase.service;

import com.alibaba.fastjson.JSONObject;
import com.zervice.kbase.database.pojo.Agent;
import com.zervice.kbase.database.pojo.AgentTask;
import com.zervice.kbase.database.pojo.PublishedProject;

import java.io.File;
import java.sql.Connection;

/**
 * @author Peng Chen
 * @date 2022/6/22
 */
public interface AgentService {

    /**
     * when agent success start...
     *
     * @param data ai param
     * @throws Exception e
     */
    void registry(String agentId, JSONObject data) throws Exception;

    /**
     * get first available agent by accountName
     *
     * @return agent
     * @throws Exception e
     */
    Agent selectAnAgent(Connection conn, String dbName) throws Exception;

    /**
     * wither agent available in this db
     *
     * @param conn
     * @param dbName
     * @return
     * @throws Exception
     */
    boolean available(Connection conn, String dbName) throws Exception;

    Agent getByAgentId(String agentId, Connection onn) throws Exception;

    void runTask(AgentTask task, String accountName, Connection conn) throws Exception;

    void cancelQuietly(AgentTask task, String accountName, Connection onn) throws Exception;

    /**
     * cancel all task
     *
     * @param userId by user
     * @param dbName dbName
     */
    void cancelAllTask(long userId, String dbName);

    void uninstall(Agent agent, Connection conn) throws Exception;

    String runPublishedProject(Agent agent, String data, String publishedProjectId);

    void publish(String data, Agent agent, PublishedProject publishedProject, String dbName);

    void publish(File data, Agent agent, PublishedProject publishedProject, String dbName);


    String stopPublishedProject(Agent agent, String publishedProjectId, boolean rmContainer);

    /**
     * 获取Project，返回结果的同时，可以要求上报一次
     * 通过{@link com.zervice.kbase.api.rpc.RpcPublishedProjectController#started(JSONObject, String, String)}告知结果
     */
    void get(Agent agent, String publishedProjectId);

    /**
     * 移除容器
     */
    void releaseQuietly(PublishedProject project) throws Exception;

}
