package com.zervice.kbase.database.dao;

import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Preconditions;
import com.zervice.kbase.database.helpers.CustomerDbTable;
import com.zervice.kbase.database.pojo.AgentTask;
import com.zervice.kbase.database.utils.DaoUtils;
import com.zervice.kbase.database.utils.RecordLoader;
import org.apache.commons.collections4.CollectionUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Peng Chen
 * @date 2022/6/8
 */
@CustomerDbTable
public class AgentTaskDao {
    public static final String TABLE_NAME = "agent_tasks";

    private static final String _SQL_INSERT = "INSERT INTO %s." + TABLE_NAME + " (id, name, agentId, status, type, schedule, taskSteps, publishedProjectId, properties) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) ";
    private static final String _SQL_SELECT = "SELECT id, name, agentId, status, type, schedule, taskSteps, publishedProjectId, properties FROM %s." + TABLE_NAME;
    private static final String _SQL_SELECT_COUNT = "SELECT count(id) FROM %s." + TABLE_NAME;
    private static final String _SQL_SELECT_BY_ID = "SELECT id, name, agentId, status, type, schedule, taskSteps, publishedProjectId, properties FROM %s." + TABLE_NAME + " where id=? ";
    private static final String _SQL_SELECT_BY_STATUS = "SELECT id, name, agentId, status, type, schedule, taskSteps, publishedProjectId, properties FROM %s." + TABLE_NAME + " where status =? ";
    private static final String _SQL_SELECT_BY_STATUS_IN = "SELECT id, name, agentId, status, type, schedule, taskSteps, publishedProjectId, properties FROM %s." + TABLE_NAME + " where status in (%s)";
    private static final String _SQL_SELECT_BY_PUBLISHED_PROJECT_ID_AND_STATUS_IN = "SELECT id, name, agentId, status, type, schedule, taskSteps, publishedProjectId, properties FROM %s." + TABLE_NAME + " where publishedProjectId=? and status in (%s)";
    private static final String _SQL_DELETE = "DELETE FROM %s." + TABLE_NAME + " WHERE id=? ";
    private static final String _SQL_UPDATE_ALL = "UPDATE %s." + TABLE_NAME + " SET name=?, agentId=?, status=?, type=?, schedule=?, taskSteps=?, publishedProjectId=?, properties=? WHERE id=?";
    private static final String _SQL_UPDATE_PROPERTIES = "UPDATE %s." + TABLE_NAME + " SET properties=? WHERE id=?";
    private static final String _SQL_UPDATE_STATUS_AND_PROPERTIES = "UPDATE %s." + TABLE_NAME + " SET status=?, properties=? WHERE id=?";


    private static RecordLoader<AgentTask> _LOADER = rs -> AgentTask.createAgentTaskFromDao(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getString("agentId"),
            rs.getInt("status"),
            rs.getInt("type"),
            rs.getLong("schedule"),
            rs.getString("taskSteps"),
            rs.getString("publishedProjectId"),
            rs.getString("properties")
    );

    public static void createTable(Connection conn, String dbName) throws SQLException {
        String sql = String.format(
                "create table %s." + TABLE_NAME + " (\n" +
                        "          id bigint not null primary key auto_increment,\n" +
                        "          name varchar(36) not null,\n" +
                        "          agentId varchar(64) not null,\n" +
                        "          status tinyint(2) not null,\n" +
                        "          type tinyint(2) not null,\n" +
                        "          schedule bigint not null default 0,\n" +
                        "          taskSteps text(65535) not null,\n" +
                        "          publishedProjectId varchar(64) not null,\n" +
                        "          properties text(65535) not null\n" +
                        "         ) engine=innodb ROW_FORMAT=DYNAMIC ", dbName
        );
        DaoUtils.executeUpdate(conn, sql);
    }

    public static long add(Connection conn, String dbName, AgentTask task) throws SQLException {
        String prop = task.getProperties() == null ? "{}" : JSONObject.toJSONString(task.getProperties());
        String taskSteps = task.getTaskSteps() == null ? "[]" : JSONObject.toJSONString(task.getTaskSteps());

        if (task.getId() == null) {
            task.setId(AgentTask.generateId());
        }

        return DaoUtils.executeUpdateWithLastInsertId(conn, String.format(_SQL_INSERT, dbName), task.getId(), task.getName(),
                task.getAgentId(), task.getStatus(), task.getType(), task.getSchedule(), taskSteps, task.getPublishedProjectId(), prop);
    }

    public static void update(Connection conn, String dbName, AgentTask task) throws SQLException {
        Preconditions.checkArgument(task.getId() != null && task.getId() > 0);

        String taskSteps = task.getTaskSteps() == null ? "[]" : JSONObject.toJSONString(task.getTaskSteps());
        String prop = task.getProperties() == null ? "{}" : JSONObject.toJSONString(task.getProperties());

        DaoUtils.executeUpdate(conn, String.format(_SQL_UPDATE_ALL, dbName), task.getName(),
                task.getAgentId(), task.getStatus(), task.getType(), task.getSchedule(), taskSteps, task.getPublishedProjectId(),
                prop, task.getId());
    }

    public static Integer updateStatusAndProp(Connection conn, String dbName, AgentTask task,
                                           Integer oldStatus, String oldProp) throws SQLException{
        String sql = _SQL_UPDATE_STATUS_AND_PROPERTIES + " and status=? and properties = ?";
        String prop = task.getProperties() == null ? "{}" : JSONObject.toJSONString(task.getProperties());

        return DaoUtils.executeUpdate(conn, String.format(sql, dbName), task.getStatus(), prop, task.getId(), oldStatus, oldProp);
    }

    public static void updateProp(Connection conn, String dbName, AgentTask task) throws SQLException {
        Preconditions.checkArgument(task.getId() != null && task.getId() > 0);

        String prop = task.getProperties() == null ? "{}" : JSONObject.toJSONString(task.getProperties());

        DaoUtils.executeUpdate(conn, String.format(_SQL_UPDATE_PROPERTIES, dbName), prop, task.getId());
    }

    public static void updateStatusAndProp(Connection conn, String dbName, AgentTask task) throws SQLException {
        Preconditions.checkArgument(task.getId() != null && task.getId() > 0);

        String prop = task.getProperties() == null ? "{}" : JSONObject.toJSONString(task.getProperties());

        DaoUtils.executeUpdate(conn, String.format(_SQL_UPDATE_STATUS_AND_PROPERTIES, dbName), task.getStatus(), prop, task.getId());
    }


    public static List<AgentTask> getByPublishedProjectIdOrderByIdDescWithLimit(Connection conn, String dbName,
                                                                                String publishedProjectId, int limit) throws SQLException {
        if (limit <= 0) {
            limit = 1;
        }
        String sql = _SQL_SELECT + " where publishedProjectId=? order by id desc limit ?";


        sql = String.format(sql, dbName, TABLE_NAME);
        return DaoUtils.getList(conn, sql, _LOADER, publishedProjectId, limit);
    }

    public static AgentTask get(Connection conn, String dbName, Long id) throws SQLException {
        Preconditions.checkArgument(id != null && id > 0);

        return DaoUtils.get(conn, String.format(_SQL_SELECT_BY_ID, dbName), _LOADER, id);
    }

    public static List<AgentTask> getByStatus(Connection conn, String dbName, int status) throws SQLException {
        return DaoUtils.getList(conn, String.format(_SQL_SELECT_BY_STATUS, dbName), _LOADER, status);
    }

    public static List<AgentTask> getByStatusIn(Connection conn, String dbName, List<Integer> status) throws SQLException {
        if (CollectionUtils.isEmpty(status)) {
            return new ArrayList<>();
        }
        String statusStr = status.stream()
                .map(Object::toString)
                .collect(Collectors.joining(","));
        return DaoUtils.getList(conn, String.format(_SQL_SELECT_BY_STATUS_IN, dbName, statusStr), _LOADER);
    }

    public static List<AgentTask> getByPublishedProjectIdAndStatusIn(Connection conn, String dbName,
                                                                     String publishedProjectId, List<Integer> status) throws SQLException {
        if (CollectionUtils.isEmpty(status)) {
            return new ArrayList<>();
        }
        String statusStr = status.stream()
                .map(Object::toString)
                .collect(Collectors.joining(","));
        return DaoUtils.getList(conn, String.format(_SQL_SELECT_BY_PUBLISHED_PROJECT_ID_AND_STATUS_IN, dbName, statusStr), _LOADER, publishedProjectId);
    }

    /**
     * 这里模糊搜索prop的like， 按id降序
     */
    public static List<AgentTask> getByProp(Connection conn, String dbName, String content, int limit) throws SQLException {
        String sql = _SQL_SELECT + " where properties like '%%" + content + "%%'  order by id DESC limit " + limit;
        return DaoUtils.getList(conn, String.format(sql, dbName), _LOADER);
    }

    public static void delete(Connection conn, String dbName, Long id) throws SQLException {
        Preconditions.checkArgument(id != null && id > 0);

        DaoUtils.executeUpdate(conn, String.format(_SQL_DELETE, dbName), id);
    }
}
