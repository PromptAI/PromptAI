package com.zervice.kbase.database.dao;

import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.zervice.kbase.database.helpers.CustomerDbTable;
import com.zervice.kbase.database.pojo.PublishedProject;
import com.zervice.kbase.database.utils.DaoUtils;
import com.zervice.kbase.database.utils.RecordLoader;
import lombok.NonNull;
import org.apache.commons.collections4.CollectionUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Peng Chen
 * @date 2022/8/3
 */
@CustomerDbTable
public class PublishedProjectDao {

    public static final String TABLE_NAME = "published_projects";

    private static final String _SQL_INSERT = "INSERT INTO %s." + TABLE_NAME + " (id, status, agentId, token, properties) VALUES (?, ?, ?, ?, ?)";
    private static final String _SQL_SELECT_ALL = "SELECT id, status, agentId, token, properties FROM %s." + TABLE_NAME;
    private static final String _SQL_SELECT_BY_ID = _SQL_SELECT_ALL + " WHERE id=?";
    private static final String _SQL_SELECT_BY_AGENT_ID = _SQL_SELECT_ALL + " WHERE agentId=?";
    private static final String _SQL_SELECT_BY_ID_IN = _SQL_SELECT_ALL + " WHERE  id in (%s)";
    private static final String _SQL_SELECT_BY_STATUS_AND_ID_IN = _SQL_SELECT_ALL + " WHERE status=? and id in (%s)";
    private static final String _SQL_SELECT_BY_STATUS_IN = "SELECT id, status, agentId, token, properties FROM %s." + TABLE_NAME + " where status in (%s)";
    private static final String _SQL_UPDATE_STATUS = "UPDATE %s." + TABLE_NAME + " SET status=? WHERE id=?";
    private static final String _SQL_UPDATE_STATUS_AND_PROPS = "UPDATE %s." + TABLE_NAME + " SET status=?, properties=? WHERE id=?";
    private static final String _SQL_UPDATE_PROPS = "UPDATE %s." + TABLE_NAME + " SET properties=? WHERE id=?";
    private static final String _SQL_DELETE = "DELETE FROM %s." + TABLE_NAME + " WHERE id=? ";


    private final static RecordLoader<PublishedProject> _LOADER = rs -> PublishedProject.createPublishProjectFromDao(
            rs.getString("id"),
            rs.getString("status"),
            rs.getString("agentId"),
            rs.getString("token"),
            rs.getString("properties")
    );

    public static void createTable(@NonNull Connection conn,
                                   String dbName) throws SQLException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(dbName));

        String sql = String.format(
                "create table %s.%s (\n" +
                        " id varchar(64) not null primary key,\n" +
                        " status varchar(16) not null,\n" +
                        " agentId varchar(64) not null,\n" +
                        " token varchar(256) not null unique,\n" +
                        " properties JSON not null \n" +
                        " ) engine=innodb ROW_FORMAT=DYNAMIC ", dbName, TABLE_NAME
        );
        DaoUtils.executeUpdate(conn, sql);
    }

    public static void add(@NonNull Connection conn, String dbName, @NonNull PublishedProject publishedProject) throws SQLException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(dbName));
        String prop = publishedProject.getProperties() == null ? "{}" : JSONObject.toJSONString(publishedProject.getProperties());

        DaoUtils.executeUpdate(conn, String.format(_SQL_INSERT, dbName), publishedProject.getId(),
                publishedProject.getStatus(), publishedProject.getAgentId(), publishedProject.getToken(), prop);
    }


    public static PublishedProject get(@NonNull Connection conn,
                                       String dbName,
                                       String id) throws SQLException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(dbName));

        return DaoUtils.get(conn, String.format(_SQL_SELECT_BY_ID, dbName), _LOADER, id);
    }

    public static List<PublishedProject> get(@NonNull Connection conn,
                                             String dbName,
                                             List<String> ids) throws SQLException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(dbName));

        if (CollectionUtils.isEmpty(ids)) {
            return new ArrayList<>();
        }

        String idStr = ids.stream()
                .map(id -> "'" + id + "'")
                .collect(Collectors.joining(""));
        return DaoUtils.getList(conn, String.format(_SQL_SELECT_BY_ID_IN, dbName, idStr), _LOADER);
    }

    public static List<PublishedProject> getAll(@NonNull Connection conn,
                                                String dbName) throws SQLException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(dbName));

        return DaoUtils.getList(conn, String.format(_SQL_SELECT_ALL, dbName), _LOADER);
    }

    public static List<PublishedProject> getByAgentId(@NonNull Connection conn,
                                                      String dbName, String agentId) throws SQLException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(dbName));

        return DaoUtils.getList(conn, String.format(_SQL_SELECT_BY_AGENT_ID, dbName), _LOADER, agentId);
    }

    public static void updateStatus(@NonNull Connection conn, String dbName, @NonNull PublishedProject publishedProject) throws SQLException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(dbName));

        DaoUtils.executeUpdate(conn, String.format(_SQL_UPDATE_STATUS, dbName), publishedProject.getStatus(), publishedProject.getId());
    }

    public static void updateStatusAndProp(@NonNull Connection conn, String dbName, @NonNull PublishedProject publishedProject) throws SQLException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(dbName));

        String prop = publishedProject.getProperties() == null ? "{}" : JSONObject.toJSONString(publishedProject.getProperties());

        DaoUtils.executeUpdate(conn, String.format(_SQL_UPDATE_STATUS_AND_PROPS, dbName), publishedProject.getStatus(), prop, publishedProject.getId());
    }

    public static void updateProp(@NonNull Connection conn, String dbName, @NonNull PublishedProject publishedProject) throws SQLException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(dbName));

        String prop = publishedProject.getProperties() == null ? "{}" : JSONObject.toJSONString(publishedProject.getProperties());

        DaoUtils.executeUpdate(conn, String.format(_SQL_UPDATE_PROPS, dbName), prop, publishedProject.getId());
    }


    public static int delete(@NonNull Connection conn,
                             String dbName,
                             String id) throws SQLException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(dbName));
        return DaoUtils.executeUpdate(conn, String.format(_SQL_DELETE, dbName), id);
    }

    public static List<PublishedProject> getByStatusIn(Connection conn, String dbName, List<String> status) throws SQLException {
        if (CollectionUtils.isEmpty(status)) {
            return new ArrayList<>();
        }
        String statusStr = status.stream()
                .map(i -> "'" + i + "'")
                .collect(Collectors.joining(","));
        ;
        return DaoUtils.getList(conn, String.format(_SQL_SELECT_BY_STATUS_IN, dbName, statusStr), _LOADER);
    }

    public static List<PublishedProject> getByStatusAndIdIn(Connection conn, String dbName,
                                                            String status, List<String> ids) throws SQLException {
        if (CollectionUtils.isEmpty(ids)) {
            return new ArrayList<>();
        }
        String idStr = ids.stream()
                .map(id -> "'" + id + "'")
                .collect(Collectors.joining(""));
        return DaoUtils.getList(conn, String.format(_SQL_SELECT_BY_STATUS_AND_ID_IN, dbName, idStr), _LOADER, status);
    }

}
