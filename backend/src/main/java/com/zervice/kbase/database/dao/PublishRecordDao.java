package com.zervice.kbase.database.dao;

import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.zervice.kbase.database.helpers.CustomerDbTable;
import com.zervice.kbase.database.pojo.PublishRecord;
import com.zervice.kbase.database.utils.DaoUtils;
import com.zervice.kbase.database.utils.RecordLoader;
import lombok.NonNull;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * @author chenchen
 * @Date 2023/12/6
 */
@CustomerDbTable
public class PublishRecordDao {

    public static final String TABLE_NAME = "publish_records";

    private static final String _SQL_INSERT = "INSERT INTO %s." + TABLE_NAME + " (id, projectId, publishedProjectId, status, properties) VALUES (?, ?, ?, ?, ?)";
    private static final String _SQL_SELECT_ALL = "SELECT id, projectId, publishedProjectId, status, properties FROM %s." + TABLE_NAME;
    private static final String _SQL_SELECT_BY_ID = _SQL_SELECT_ALL + " WHERE id=?";
    private static final String _SQL_SELECT_BY_PUBLISHED_PROJECT_ID_AND_STATUS = _SQL_SELECT_ALL + " WHERE publishedProjectId=? and  status=?";
    private static final String _SQL_UPDATE = "UPDATE %s." + TABLE_NAME + " SET projectId=?, publishedProjectId=?, status=?, properties=? WHERE id=?";
    private static final String _SQL_UPDATE_PROP = "UPDATE %s." + TABLE_NAME + " SET  properties=? WHERE id=?";
    private static final String _SQL_DELETE = "DELETE FROM %s." + TABLE_NAME + " WHERE id=? ";

    private final static RecordLoader<PublishRecord> _LOADER = rs -> PublishRecord.createPublishRecordFromDao(
            rs.getLong("id"),
            rs.getString("projectId"),
            rs.getString("publishedProjectId"),
            rs.getString("status"),
            rs.getString("properties")
    );

    public static void createTable(@NonNull Connection conn,
                                   String dbName) throws SQLException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(dbName));

        String sql = String.format(
                "create table %s.%s (\n" +
                        " id BIGINT not null primary key,\n" +
                        " projectId varchar(64) not null,\n" +
                        " publishedProjectId varchar(64) not null,\n" +
                        " status varchar(20) not null,\n" +
                        " properties JSON not null, \n" +
                        " index index_ppid(publishedProjectId) \n" +
                        " ) engine=innodb ROW_FORMAT=DYNAMIC ", dbName, TABLE_NAME
        );
        DaoUtils.executeUpdate(conn, sql);
    }
    
    public static void add(Connection conn, String dbName, PublishRecord record) throws SQLException{
        String prop = record.getProperties() == null ? "{}" : JSONObject.toJSONString(record.getProperties());
        if (record.getId() == null) {
            record.setId(PublishRecord.generateId());
        }

        DaoUtils.executeUpdate(conn, String.format(_SQL_INSERT, dbName), record.getId(),
                record.getProjectId(), record.getPublishedProjectId(), record.getStatus(), prop);
    }

    public static PublishRecord get(Connection conn, String dbName, Long id) throws SQLException {
        return DaoUtils.get(conn, String.format(_SQL_SELECT_BY_ID, dbName), _LOADER, id);
    }

    public static void update(Connection conn, String dbName, PublishRecord record) throws SQLException {
        String prop = record.getProperties() == null ? "{}" : JSONObject.toJSONString(record.getProperties());
        DaoUtils.executeUpdate(conn, String.format(_SQL_UPDATE, dbName), record.getProjectId(),
                record.getPublishedProjectId(), record.getStatus(), prop, record.getId());
    }
    public static void updateProp(Connection conn, String dbName, PublishRecord record) throws SQLException {
        String prop = record.getProperties() == null ? "{}" : JSONObject.toJSONString(record.getProperties());
        DaoUtils.executeUpdate(conn, String.format(_SQL_UPDATE_PROP, dbName),prop, record.getId());
    }

    public static List<PublishRecord> getByPublishedProjectIdOrderByIdDescWithLimit(Connection conn, String dbName,
                                                                                String publishedProjectId, int limit) throws SQLException {
        if (limit <= 0) {
            limit = 1;
        }
        String sql = _SQL_SELECT_ALL + " where publishedProjectId=? order by id desc limit ?";


        sql = String.format(sql, dbName, TABLE_NAME);
        return DaoUtils.getList(conn, sql, _LOADER, publishedProjectId, limit);
    }

    public static List<PublishRecord> getByPublishedProjectIdAndStatus(Connection conn, String dbName,
                                                                       String publishedProjectId,
                                                                       String status) throws SQLException{
        return DaoUtils.getList(conn, String.format(_SQL_SELECT_BY_PUBLISHED_PROJECT_ID_AND_STATUS, dbName), _LOADER, publishedProjectId, status);
    }
}
