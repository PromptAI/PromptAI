package com.zervice.kbase.service;

import com.zervice.kbase.database.pojo.AgentTask;

import java.sql.Connection;

/**
 * @author Peng Chen
 * @date 2022/6/29
 */
public interface AgentTaskService {

    /**
     * task success executed hook
     *
     * @param task   t
     * @param dbName dbName
     * @throws Exception e
     */
    void afterTaskFinished(AgentTask task, String message, String dbName) throws Exception;

    void run(AgentTask agentTask, String dbName);

    boolean hasRunningOrSchedulingTask(String id, Connection conn, String dbName) throws Exception;

    boolean hasRunningTask(String id, Connection conn, String dbName) throws Exception;


}