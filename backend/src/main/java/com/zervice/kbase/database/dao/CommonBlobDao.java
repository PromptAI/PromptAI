package com.zervice.kbase.database.dao;

import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.zervice.kbase.database.helpers.CustomerDbTable;
import com.zervice.kbase.database.pojo.CommonBlob;
import com.zervice.kbase.database.utils.DaoUtils;
import com.zervice.kbase.database.utils.RecordLoader;
import lombok.NonNull;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * 获取二进制数据
 */
@CustomerDbTable
public class CommonBlobDao {

    public static final String TABLE_NAME = "blobs";

    private static final String _SQL_INSERT = "insert into %s.blobs (id, type, fileName, content, properties, md5) values(?, ?, ?, ?, ?, ?)";
    private static final String _SQL_SELECT_BY_ID = "select id, type, fileName, content, properties, md5 from %s.blobs where id=?";
    private static final String _SQL_SELECT_BY_MD5 = "select id, type, fileName, content, properties, md5 from %s.blobs where md5=?";
    private static final String _SQL_DELETE_BY_ID = "delete from %s.blobs where id = ?";

    public static void createTable(@NonNull Connection conn,
                                   String dbName) throws SQLException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(dbName));
        String sql = String.format(
                "create table %s.blobs (\n" +
                        " id bigint primary key, \n" +
                        " type int not null,\n" +
                        " fileName varchar(1024) not null, \n" +
                        " content mediumblob not null,\n" +
                        " properties text(1024) not null,\n" +
                        " md5 varchar(32) not null \n" +
                        " ) engine=innodb ROW_FORMAT=DYNAMIC ", dbName
        );
        DaoUtils.executeUpdate(conn, sql);
    }

    private final static RecordLoader<CommonBlob> _LOADER = rs -> CommonBlob.createCommonBlobFromDao(
            rs.getLong("id"),
            rs.getBytes("content"),
            rs.getInt("type"),
            rs.getString("fileName"),
            rs.getString("md5"),
            rs.getString("properties")
    );

    public static void delete(@NonNull Connection conn,
                              String dbName, long id) throws SQLException {
        DaoUtils.executeUpdate(conn, String.format(_SQL_DELETE_BY_ID, dbName), id);
    }

    public static void add(@NonNull Connection conn,
                    String dbName, CommonBlob blob) throws SQLException {
        String prop = blob.getProperties() == null ? "{}" : JSONObject.toJSONString(blob.getProperties());
        DaoUtils.executeUpdate(conn, String.format(_SQL_INSERT, dbName), blob.getId(), blob.getType(),
                blob.getFileName(), blob.getContent(), prop, blob.getMd5());
    }

    public static CommonBlob getById(@NonNull Connection conn,
                                 String dbName, long id) throws SQLException {
        return DaoUtils.get(conn, String.format(_SQL_SELECT_BY_ID, dbName), _LOADER, id);
    }
    public static CommonBlob getByMd5(@NonNull Connection conn,
                                     String dbName, String md5) throws SQLException {
        return DaoUtils.get(conn, String.format(_SQL_SELECT_BY_MD5, dbName), _LOADER, md5);
    }



}
