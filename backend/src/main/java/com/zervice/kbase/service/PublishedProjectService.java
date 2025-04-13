package com.zervice.kbase.service;

import com.zervice.kbase.api.restful.pojo.RestAgentTask;
import com.zervice.kbase.api.restful.pojo.RestPublishRecord;
import com.zervice.kbase.api.restful.pojo.RestPublishedProject;
import com.zervice.kbase.database.pojo.PublishedProject;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * @author Peng Chen
 * @date 2022/8/3
 */
public interface PublishedProjectService {


    RestPublishedProject get(String id, Connection conn, String dbName) throws Exception;

    /**
     * stop a project
     *
     * @param publishProjectId published project id
     * @param userId           uid
     * @param dbName           dbName
     * @return published project
     * @throws Exception e
     */
    RestPublishedProject stop(String publishProjectId, long userId, String dbName) throws Exception;

    /**
     * 确保debug published project 是就绪的
     * 如果没有，则请求Agent 创建一个
     *
     * @param dbName dbName
     * @return
     * @throws Exception e
     */
    PublishedProject preparePublishedProject(long userId, String projectId, String publishProjectId, String publishModel,
                                              String dbName) throws Exception;

    boolean hasRunningOrSchedulingTask(String id, Connection conn, String dbName) throws Exception;

    boolean hasRunningTask(String id, Connection conn, String dbName) throws Exception;

    /**
     * release account's all published project
     *
     * @param userId by user
     * @param dbName dbName
     */
    void release(long userId, String dbName) throws Exception;

    /**
     * release published project
     *
     * @param userId by user
     * @param dbName dbName
     */
    void release(String projectId, long userId, Connection conn, String dbName) throws Exception;

    /**
     * release published project
     *
     * @param userId by user
     * @param dbName dbName
     */
    void release(PublishedProject publishedProject, long userId, Connection conn, String dbName) throws Exception;

    /**
     * 在删除删除Account时，快速回收资源
     *
     *  获取publish project，异步删除
     */
    void quickRelease(Connection conn, String dbName) throws Exception;

    List<RestPublishRecord> loadRecentRecords(String publishedProjectId, Connection conn, String dbName) throws SQLException, Exception;
}
