package com.zervice.kbase.database.dao;

import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.zervice.kbase.database.helpers.CustomerDbTable;
import com.zervice.kbase.database.pojo.PublishSnapshot;
import com.zervice.kbase.database.utils.DaoUtils;
import com.zervice.kbase.database.utils.RecordLoader;
import lombok.NonNull;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * @author chenchen
 * @Date 2023/12/5
 */
@CustomerDbTable
public class PublishSnapshotDao {

    public static final String TABLE_NAME = "publish_snapshots";

    private static final String _SQL_INSERT = "INSERT INTO %s." + TABLE_NAME + " (projectId, publishedProjectId, status, tag, properties) VALUES (?, ?, ?, ?, ?)";
    private static final String _SQL_SELECT_ALL = "SELECT id, projectId, publishedProjectId, status, tag, properties FROM %s." + TABLE_NAME;
    private static final String _SQL_SELECT_BY_STATUS = _SQL_SELECT_ALL + " WHERE status=?";
    private static final String _SQL_SELECT_BY_ID = _SQL_SELECT_ALL + " WHERE id=?";
    private static final String _SQL_UPDATE = "UPDATE %s." + TABLE_NAME + " SET projectId=?, publishedProjectId=?, status=?, tag=?, properties=? WHERE id=?";
    private static final String _SQL_DELETE = "DELETE FROM %s." + TABLE_NAME + " WHERE id=? ";

    private final static RecordLoader<PublishSnapshot> _LOADER = rs -> PublishSnapshot.createPublishSnapshotFromDao(
            rs.getLong("id"),
            rs.getString("projectId"),
            rs.getString("publishedProjectId"),
            rs.getString("status"),
            rs.getString("tag"),
            rs.getString("properties")
    );

    public static void createTable(@NonNull Connection conn,
                                   String dbName) throws SQLException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(dbName));

        String sql = String.format(
                "create table %s.%s (\n" +
                        " id BIGINT not null primary key auto_increment,\n" +
                        " projectId varchar(64) not null,\n" +
                        " publishedProjectId varchar(64) not null,\n" +
                        " status varchar(20) not null,\n" +
                        " tag varchar(20) not null unique,\n" +
                        //    验证一下 本地插入 1.5M 2M文件
                        // blob 查询是一个引用
                        " properties JSON not null, \n" +
                        " index index_status(status) \n" +
                        " ) engine=innodb ROW_FORMAT=DYNAMIC ", dbName, TABLE_NAME
        );
        DaoUtils.executeUpdate(conn, sql);
    }
    
    public static Long add(Connection conn, String dbName, PublishSnapshot snapshot) throws SQLException {
        String prop = snapshot.getProperties() == null ? "{}" : JSONObject.toJSONString(snapshot.getProperties());

        return DaoUtils.executeUpdateWithLastInsertId(conn, String.format(_SQL_INSERT, dbName), snapshot.getProjectId(), snapshot.getPublishedProjectId(),
                snapshot.getStatus(), snapshot.getTag(), prop);
    }

    public static void update(Connection conn, String dbName, PublishSnapshot snapshot) throws SQLException {
        String prop = snapshot.getProperties() == null ? "{}" : JSONObject.toJSONString(snapshot.getProperties());
        DaoUtils.executeUpdate(conn, String.format(_SQL_UPDATE, dbName), snapshot.getProjectId(), snapshot.getPublishedProjectId(),
                snapshot.getStatus(), snapshot.getTag(), prop, snapshot.getId());
    }

    public static PublishSnapshot get(Connection conn, String dbName, Long id) throws SQLException{
        return DaoUtils.get(conn, String.format(_SQL_SELECT_BY_ID, dbName), _LOADER, id);
    }
    public static List<PublishSnapshot> getByStatus(Connection conn, String dbName, String status) throws SQLException{
        return DaoUtils.getList(conn, String.format(_SQL_SELECT_BY_STATUS, dbName), _LOADER, status);
    }
}
