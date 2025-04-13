package com.zervice.kbase.database.dao;

import com.alibaba.fastjson.JSONObject;
import com.zervice.common.utils.ServletUtils;
import com.zervice.kbase.api.restful.pojo.PageRequest;
import com.zervice.kbase.database.criteria.ProjectCriteria;
import com.zervice.kbase.database.helpers.CustomerDbTable;
import com.zervice.kbase.database.pojo.Project;
import com.zervice.kbase.database.utils.DaoUtils;
import com.zervice.kbase.database.utils.PageResult;
import com.zervice.kbase.database.utils.RecordLoader;
import org.apache.commons.lang3.StringUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * @author Peng Chen
 * @date 2022/6/8
 */
@CustomerDbTable
public class ProjectDao {
    public static final String TABLE_NAME = "projects";

    private static final String _SQL_INSERT = "INSERT INTO %s." + TABLE_NAME + " (id, name, ip, time, templateViewCount, properties) VALUES (?, ?, ?, ?, ?, ?) ";
    private static final String _SQL_SELECT_WITHOUT_IP = "SELECT id, name, templateViewCount, properties FROM %s." + TABLE_NAME;
    private static final String _SQL_SELECT_COUNT = "SELECT count(id) FROM %s." + TABLE_NAME;
    private static final String _SQL_SELECT_BY_NAME = _SQL_SELECT_WITHOUT_IP + " where name=? ";
    private static final String _SQL_SELECT_BY_ID = _SQL_SELECT_WITHOUT_IP + " where id=? ";
    private static final String _SQL_DELETE = "DELETE FROM %s." + TABLE_NAME + " WHERE id=? ";
    private static final String _SQL_UPDATE_ALL = "UPDATE %s." + TABLE_NAME + " SET name=?, properties=? WHERE id=?";
    private static final String _SQL_ADD_TEMPLATE_VIEW_COUNT = "UPDATE %s." + TABLE_NAME + " SET templateViewCount=IFNULL(templateViewCount, 0) + 1 WHERE id=?";
    private static final String _SQL_UPDATE_IP_AND_TIME = "UPDATE %s." + TABLE_NAME + " SET ip=?, time=? WHERE id=?";

    private static RecordLoader<Project> _LOADER_WITHOUT_IP_AND_TIME = rs -> Project.createProjectFromDao(
            rs.getString("id"),
            rs.getString("name"),
            null,
            null,
            rs.getLong("templateViewCount"),
            rs.getString("properties")
    );

    public static void createTable(Connection conn, String dbName) throws SQLException {
        String sql = String.format(
                "create table %s." + TABLE_NAME + " (\n" +
                        "          id varchar(36) not null primary key,\n" +
                        "          name varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci not null unique,\n" +
                        "          ip varchar(50) not null,\n" +
                        "          time bigint not null,\n" +
                        "          templateViewCount bigint not null default 0,\n" +
                        "          properties LONGTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci not null,\n" +
                        "          index ipTimeIndex(ip,time)\n" +
                        "         ) engine=innodb ROW_FORMAT=DYNAMIC ", dbName
        );
        DaoUtils.executeUpdate(conn, sql);
    }

    public static void addTemplateViewCount(Connection conn, String dbName, String projectId) throws SQLException {
        DaoUtils.executeUpdate(conn, String.format(_SQL_ADD_TEMPLATE_VIEW_COUNT, dbName), projectId);
    }

    public static long add(Connection conn, String dbName, Project project) throws SQLException {
        String prop = project.getProperties() == null ? "{}" : JSONObject.toJSONString(project.getProperties());
        if (project.getIp() == null) {
            project.setIp(ServletUtils.getCurrentIp());
        }
        if (project.getTime() == null) {
            project.setTime(System.currentTimeMillis());
        }
        if (project.getTemplateViewCount() == null || project.getTemplateViewCount() < 0) {
            project.setTemplateViewCount(0L);
        }

        return DaoUtils.executeUpdate(conn, String.format(_SQL_INSERT, dbName), project.getId(),
                project.getName(), project.getIp(), project.getTime(), project.getTemplateViewCount(), prop);
    }

    public static void update(Connection conn, String dbName, Project project) throws SQLException {
        String prop = project.getProperties() == null ? "{}" : JSONObject.toJSONString(project.getProperties());

        DaoUtils.executeUpdate(conn, String.format(_SQL_UPDATE_ALL, dbName),
                project.getName(), prop, project.getId());
    }

    public static void updateIpAndTime(Connection conn, String dbName, String id,
                                       String ip, Long time) throws Exception{
        DaoUtils.executeUpdate(conn, String.format(_SQL_UPDATE_IP_AND_TIME, dbName), ip, time, id);
    }
    public static Project get(Connection conn, String dbName, String id) throws SQLException {

        return DaoUtils.get(conn, String.format(_SQL_SELECT_BY_ID, dbName), _LOADER_WITHOUT_IP_AND_TIME, id);
    }

    public static Project getByName(Connection conn, String dbName, String name) throws SQLException {

        return DaoUtils.get(conn, String.format(_SQL_SELECT_BY_NAME, dbName), _LOADER_WITHOUT_IP_AND_TIME, name);
    }

    public static List<Project> get(Connection conn, String dbName) throws SQLException {

        return DaoUtils.getList(conn, String.format(_SQL_SELECT_WITHOUT_IP, dbName), _LOADER_WITHOUT_IP_AND_TIME);
    }

    public static PageResult<Project> get(Connection conn, String dbName,
                                          ProjectCriteria criteria, PageRequest pageRequest) throws Exception {
        StringBuilder pageSql = new StringBuilder(_SQL_SELECT_WITHOUT_IP).append(" where 1=1");
        StringBuilder countSql = new StringBuilder(_SQL_SELECT_COUNT).append(" where 1=1");

        if (StringUtils.isNotBlank(criteria.getName())) {
            pageSql.append(" and name like '%%").append(criteria.getName()).append("%%' ");
            countSql.append(" and name like '%%").append(criteria.getName()).append("%%' ");
        }

        PageRequest.buildSortAndLimitSql(pageRequest, pageSql);

        //query list
        List<Project> projects = DaoUtils.getList(conn, String.format(pageSql.toString(), dbName), _LOADER_WITHOUT_IP_AND_TIME);
        //query total count
        Long totalCount = DaoUtils.getLong(conn, String.format(countSql.toString(), dbName));
        return PageResult.of(projects, totalCount);
    }

    public static Long count(Connection conn, String dbName) throws SQLException{
        return DaoUtils.getLong(conn, String.format(_SQL_SELECT_COUNT, dbName));
    }

    public static List<Project> getList(Connection conn, String dbName,
                                        ProjectCriteria criteria) throws Exception {
        StringBuilder pageSql = new StringBuilder(_SQL_SELECT_WITHOUT_IP).append(" where 1=1");

        if (StringUtils.isNotBlank(criteria.getName())) {
            pageSql.append(" and name like '%%").append(criteria.getName()).append("%%' ");
        }
        return DaoUtils.getList(conn, String.format(pageSql.toString(), dbName), _LOADER_WITHOUT_IP_AND_TIME);
    }

    public static void delete(Connection conn, String dbName, String id) throws SQLException {
        DaoUtils.executeUpdate(conn, String.format(_SQL_DELETE, dbName), id);
    }

    public static Long countByIdAndTimeGt(Connection conn, String dbName,
                                          String ip, long time) throws SQLException {
        String sql = "select count(id) from %s." + TABLE_NAME + " where ip=? and time >= ?";
        return DaoUtils.getLong(conn, String.format(sql, dbName), ip, time);
    }
}
