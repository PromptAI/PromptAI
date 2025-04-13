package com.zervice.kbase.service;

import com.zervice.kbase.api.restful.pojo.*;
import com.zervice.kbase.database.criteria.ProjectCriteria;
import com.zervice.kbase.database.pojo.Project;
import com.zervice.kbase.database.pojo.ProjectComponent;

import java.sql.Connection;
import java.util.List;

public interface ProjectService {

    Object getPublicList(String dbName, ProjectCriteria criteria, PageRequest page) throws Exception;

    /**
     * 从模板project克隆出
     *  为保证用户第一次克隆项目能够快速运行。我们在第一次克隆项目时，将id保留，保证能命中缓存的模型。
     *  后面的克隆同一个项目生成不一样的id，
     */
    Project cloneFromPublic(String dbName, long userId, RestCloneProject cloneProject) throws Exception;

    /**
     * publish a project
     */
    RestPublishedProject publish(String projectId, String publishedProjectId, String publishModel,
                                 List<String> componentIds, long userId, String dbName) throws Exception;

    Project save(RestProject project, Long userId, Connection conn, String dbName) throws Exception;

    void save(Project dbProject, Connection conn, String dbName) throws Exception;

    Project update(RestProject restProject, Long userId, String dbName) throws Exception;
    /**
     * 获取是否作为分支button呈现map
     *
     * @param dbName             dbName
     * @param conn               conn
     * @param publishedProjectId publishedProjectId
     * @return
     * @throws Exception
     */
    List<RestProjectComponentConversation> getDeployedConversations(String publishedProjectId, Connection conn, String dbName) throws Exception;

    /**
     * 查询发布的节点
     */
    List<ProjectComponent> getDeployedComponents(String publishedProjectId, Connection conn, String dbName) throws Exception;

}
