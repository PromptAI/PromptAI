package com.zervice.kbase.database.dao;

import com.alibaba.fastjson.JSONObject;
import com.zervice.kbase.database.helpers.CustomerDbTable;
import com.zervice.kbase.database.pojo.ProjectComponent;
import com.zervice.kbase.database.utils.DaoUtils;
import com.zervice.kbase.database.utils.RecordLoader;
import org.apache.commons.collections4.CollectionUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Peng Chen
 * @date 2022/6/22
 */
@CustomerDbTable
public class ProjectComponentDao {

    public static final String TABLE_NAME = "project_components";

    private static final String _SQL_INSERT = "INSERT INTO %s." + TABLE_NAME + " (id, projectId, rootComponentId, type, properties) VALUES (?, ?, ?, ?, ?) ";
    private static final String _SQL_SELECT = "SELECT id, projectId, rootComponentId, type, properties FROM %s." + TABLE_NAME;
    private static final String _SQL_SELECT_ID_BY_PROJECT_ID = "SELECT id FROM %s." + TABLE_NAME + " where projectId=?";
    private static final String _SQL_SELECT_BY_ID = "SELECT id, projectId, rootComponentId, type, properties FROM %s." + TABLE_NAME + " where id=?";
    private static final String _SQL_SELECT_BY_IDS = "SELECT * from  %s." + TABLE_NAME + " where id ";
    private static final String _SQL_SELECT_COUNT = "SELECT count(id) FROM %s." + TABLE_NAME;
    private static final String _SQL_SELECT_BY_PROJECT_ID_AND_TYPE = "SELECT id, projectId, rootComponentId, type, properties FROM %s." + TABLE_NAME + " where projectId=? and type=? ";
    private static final String _SQL_SELECT_BY_PROJECT_ID_AND_ROOT_COMPONENT_Id = "SELECT id, projectId, rootComponentId, type, properties FROM %s." + TABLE_NAME + " where projectId=? and rootComponentId=? ";
    private static final String _SQL_SELECT_BY_TYPE = "SELECT id, projectId, rootComponentId, type, properties FROM %s." + TABLE_NAME + " where type=? ";
    private static final String _SQL_SELECT_BY_PROJECT_ID_AND_TYPES = "SELECT id, projectId, rootComponentId, type, properties FROM %s." + TABLE_NAME + " where projectId=? ";
    private static final String _SQL_SELECT_BY_TYPES = "SELECT id, projectId, rootComponentId, type, properties FROM %s." + TABLE_NAME;
    private static final String _SQL_DELETE_BY_ID = "DELETE from  %s." + TABLE_NAME + " where id=? ";
    private static final String _SQL_DELETE_BY_IDS = "DELETE from  %s." + TABLE_NAME + " where id ";
    private static final String _SQL_DELETE_BY_PROJECT_ID = "DELETE from  %s." + TABLE_NAME + " where projectId=? ";
    private static final String _SQL_UPDATE_ALL = "UPDATE %s." + TABLE_NAME + " SET projectId=?, rootComponentId=?, type=?, properties=? WHERE id=?";
    private static final String _SQL_SELECT_BY_PROJECT_ID = "SELECT id, projectId, rootComponentId, type, properties FROM %s." + TABLE_NAME + " where projectId=? ";
    private static final String _SQL_UPDATE_PROP = "UPDATE %s." + TABLE_NAME + " SET  properties=? WHERE id=?";


    private static RecordLoader<ProjectComponent> _LOADER = rs -> ProjectComponent.createProjectComponentFromDao(
            rs.getString("id"),
            rs.getString("projectId"),
            rs.getString("rootComponentId"),
            rs.getString("type"),
            rs.getString("properties")
    );

    public static void createTable(Connection conn, String dbName) throws SQLException {
        String sql = String.format(
                "create table %s." + TABLE_NAME + " (\n" +
                        "          id varchar(64) not null primary key,\n" +
                        "          projectId varchar(60) not null,\n" +
                        "          rootComponentId varchar(60) ,\n" +
                        "          type varchar(36) not null ,\n" +
                        "          properties LONGTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci\n" +
                        "         ) engine=innodb ROW_FORMAT=DYNAMIC ", dbName
        );
        DaoUtils.executeUpdate(conn, sql);
    }

    public static long add(Connection conn, String dbName, ProjectComponent projectComponent) throws SQLException {
        String prop = projectComponent.getProperties() == null ? "{}" : JSONObject.toJSONString(projectComponent.getProperties());
        if (projectComponent.getId() == null) {
            projectComponent.setId(ProjectComponent.generateId(projectComponent.getType()));
        }
        return DaoUtils.executeUpdate(conn, String.format(_SQL_INSERT, dbName),
                projectComponent.getId(), projectComponent.getProjectId(), projectComponent.getRootComponentId(),
                projectComponent.getType(), prop);
    }


    public static long update(Connection conn, String dbName, ProjectComponent projectComponent) throws SQLException {
        String prop = projectComponent.getProperties() == null ? "{}" : JSONObject.toJSONString(projectComponent.getProperties());

        return DaoUtils.executeUpdate(conn, String.format(_SQL_UPDATE_ALL, dbName),
                projectComponent.getProjectId(), projectComponent.getRootComponentId(),
                projectComponent.getType(), prop, projectComponent.getId());
    }

    public static long updateProp(Connection conn, String dbName, ProjectComponent projectComponent) throws SQLException {
        String prop = projectComponent.getProperties() == null ? "{}" : JSONObject.toJSONString(projectComponent.getProperties());

        return DaoUtils.executeUpdate(conn, String.format(_SQL_UPDATE_PROP, dbName), prop, projectComponent.getId());
    }

    public static List<ProjectComponent> getByProjectIdAndType(Connection conn, String dbName, String projectId, String type) throws SQLException {
        return DaoUtils.getList(conn, String.format(_SQL_SELECT_BY_PROJECT_ID_AND_TYPE, dbName), _LOADER, projectId, type);
    }

    public static List<ProjectComponent> getByProjectIdAndRootComponentId(Connection conn, String dbName,
                                                                          String projectId, String rootComponentId) throws SQLException {
        return DaoUtils.getList(conn, String.format(_SQL_SELECT_BY_PROJECT_ID_AND_ROOT_COMPONENT_Id, dbName), _LOADER, projectId, rootComponentId);
    }

    public static List<ProjectComponent> getByType(Connection conn, String dbName, String type) throws SQLException {
        return DaoUtils.getList(conn, String.format(_SQL_SELECT_BY_TYPE, dbName), _LOADER, type);
    }


    public static List<ProjectComponent> getByProjectIdAndTypes(Connection conn, String dbName, String projectId, List<String> types) throws SQLException {
        if (CollectionUtils.isEmpty(types)) {
            return new ArrayList<>();
        }
        StringBuilder querySql = new StringBuilder(_SQL_SELECT_BY_PROJECT_ID_AND_TYPES);
        if (CollectionUtils.isNotEmpty(types)) {
            // 字符串需要拼接单引号
            String sql = types.stream()
                    .map(i -> "'" + i + "'")
                    .collect(Collectors.joining(","));
            querySql.append("and type in ( ").append(sql).append(" )");
        }
        return DaoUtils.getList(conn, String.format(querySql.toString(), dbName), _LOADER, projectId);
    }

    public static List<ProjectComponent> getByTypes(Connection conn, String dbName, List<String> types) throws SQLException {
        if (CollectionUtils.isEmpty(types)) {
            return new ArrayList<>();
        }
        StringBuilder querySql = new StringBuilder(_SQL_SELECT_BY_TYPES);
        if (CollectionUtils.isNotEmpty(types)) {
            // 字符串需要拼接单引号
            String sql = types.stream()
                    .map(i -> "'" + i + "'")
                    .collect(Collectors.joining(","));
            querySql.append(" where type in ( ").append(sql).append(" )");
        }
        return DaoUtils.getList(conn, String.format(querySql.toString(), dbName), _LOADER);
    }

    public static ProjectComponent get(Connection conn, String dbName, String id) throws SQLException {
        return DaoUtils.get(conn, String.format(_SQL_SELECT_BY_ID, dbName), _LOADER, id);
    }


    public static void deleteById(Connection conn, String dbName, String id) throws SQLException {
        DaoUtils.executeUpdate(conn, String.format(_SQL_DELETE_BY_ID, dbName), id);
    }

    public static void deleteByIds(Connection conn, String dbName, Set<String> ids) throws SQLException {
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }
        StringBuilder delSql = new StringBuilder(_SQL_DELETE_BY_IDS);
        if (CollectionUtils.isNotEmpty(ids)) {
            // 字符串需要拼接单引号
            String sql = ids.stream()
                    .map(i -> "'" + i + "'")
                    .collect(Collectors.joining(","));
            delSql.append("in ( ").append(sql).append(" )");
        }
        DaoUtils.executeUpdate(conn, String.format(delSql.toString(), dbName));
    }

    public static void deleteByProjectId(Connection conn, String dbName, String projectId) throws SQLException {
        DaoUtils.executeUpdate(conn, String.format(_SQL_DELETE_BY_PROJECT_ID, dbName), projectId);
    }

    public static List<ProjectComponent> get(Connection conn, String dbName) throws Exception {
        return DaoUtils.getList(conn, String.format(_SQL_SELECT, dbName), _LOADER);
    }

    public static List<String> getIdByProjectId(Connection conn, String dbName, String projectId) throws Exception {
        RecordLoader<String> idLoader = rs -> rs.getString("id");
        return DaoUtils.getList(conn, String.format(_SQL_SELECT_ID_BY_PROJECT_ID, dbName), idLoader, projectId);
    }

    public static List<ProjectComponent> getByRootComponentIdAndTypeWithLimit(Connection conn, String dbName,
                                                                              String rootComponentId, String type, int limit) throws SQLException {
        if (limit > 0) {
            String sql = _SQL_SELECT + " where rootComponentId=? and type=? limit ?";
            return DaoUtils.getList(conn, String.format(sql, dbName), _LOADER, rootComponentId, type, limit);
        }

        String sql = _SQL_SELECT + " where rootComponentId=? and type=? ";
        return DaoUtils.getList(conn, String.format(sql, dbName), _LOADER, rootComponentId, type);
    }

    public static List<ProjectComponent> getByProjectId(Connection conn, String dbName, String projectId) throws Exception {
        return DaoUtils.getList(conn, String.format(_SQL_SELECT_BY_PROJECT_ID, dbName), _LOADER, projectId);
    }

    public static void add(Connection conn, String dbName, List<ProjectComponent> components) throws SQLException {
        String sqlInsert = _SQL_INSERT;
        PreparedStatement ps = conn.prepareStatement(String.format(sqlInsert, dbName, TABLE_NAME));
        for (ProjectComponent component : components) {
            if (component.getId() == null) {
                component.setId(ProjectComponent.generateId(component.getType()));
            }
            ps.setString(1, component.getId());
            ps.setString(2, component.getProjectId());
            ps.setString(3, component.getRootComponentId());
            ps.setString(4, component.getType());
            String prop = component.getProperties() == null ? "{}" : JSONObject.toJSONString(component.getProperties());
            ps.setString(5, prop);
            ps.addBatch();
        }
        ps.executeBatch();
    }

    public static List<ProjectComponent> getByIds(Connection conn, String dbName, List<String> ids) throws SQLException {
        if (CollectionUtils.isEmpty(ids)) {
            return new ArrayList<>();
        }
        StringBuilder querySql = new StringBuilder(_SQL_SELECT_BY_IDS);
        if (CollectionUtils.isNotEmpty(ids)) {
            // 字符串需要拼接单引号
            String sql = ids.stream()
                    .map(i -> "'" + i + "'")
                    .collect(Collectors.joining(","));
            querySql.append("in ( ").append(sql).append(" )");
        }
        return DaoUtils.getList(conn, String.format(querySql.toString(), dbName), _LOADER);
    }
}
