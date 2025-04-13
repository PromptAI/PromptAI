package com.zervice.kbase.database.dao;

import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Preconditions;
import com.zervice.common.utils.Constants;
import com.zervice.kbase.database.helpers.CoreDbTable;
import com.zervice.kbase.database.pojo.AccountRequest;
import com.zervice.kbase.database.utils.DaoUtils;
import com.zervice.kbase.database.utils.RecordLoader;
import lombok.NonNull;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

@CoreDbTable
public class AccountRequestDao {

    public static String TABLE_NAME = "account_requests";
    private final static String SQL_GET_ALL = "SELECT id, status,company,name,domain,mxDomain,creator, properties FROM %s.account_requests";
    private final static String SQL_INSERT = "INSERT INTO %s.account_requests ( status,company,name,domain,mxDomain,creator, properties) VALUES (?, ?, ?, ?, ? ,? ,? )";
    private final static String SQL_DELETE = "DELETE FROM %s.account_requests WHERE id=?";
    private final static String SQL_UPDATE = "UPDATE %s.account_requests set status=?,company=?,name=?,domain=?,mxDomain=?,creator=?, properties=? WHERE id=?";

    public static void createTable(@NonNull Connection conn, String dbName) throws SQLException {


        String sql = String.format(
                "CREATE TABLE IF NOT EXISTS %s.%s (\n" +
                        "         id BIGINT not null primary key auto_increment,\n" +
                        "         status VARCHAR(32) not null default 'requested', \n" +
                        "         company VARCHAR(512) not null, \n" +
                        "         name VARCHAR(32) not null,\n" +
                        "         domain VARCHAR(256) not null,\n" +
                        "         mxDomain VARCHAR(256) not null,\n" +
                        "         creator VARCHAR(256) not null,\n" +
                        "         properties JSON not null \n" +
                        "        ) engine=innodb ROW_FORMAT=DYNAMIC", Constants.COREDB, TABLE_NAME
        );
        DaoUtils.executeUpdate(conn, sql);
    }

    private final static RecordLoader<AccountRequest> _LOADER = rs -> {
        AccountRequest accountRequest = AccountRequest.builder().build();
        accountRequest.setId(rs.getLong(1));
        accountRequest.setStatus(rs.getString(2));
        accountRequest.setCompany(rs.getString(3));
        accountRequest.setName(rs.getString(4));
        accountRequest.setDomain(rs.getString(5));
        accountRequest.setMxDomain(rs.getString(6));
        accountRequest.setCreator(rs.getString(7));
        JSONObject prop = JSONObject.parseObject(rs.getString(8));
        accountRequest.setProperties(prop);
        return accountRequest;
    };

    public static List<AccountRequest> getAll(@NonNull Connection conn) throws Exception {
        return DaoUtils.getList(conn,
                String.format(SQL_GET_ALL, Constants.COREDB),
                _LOADER::load);
    }

    public static void add(@NonNull Connection conn, @NonNull AccountRequest accountRequest) throws Exception {
        String props = accountRequest.getProperties() == null ? "{}" : JSONObject.toJSONString(accountRequest.getProperties());
        DaoUtils.executeUpdate(conn, String.format(SQL_INSERT, Constants.COREDB), accountRequest.getStatus(),
                accountRequest.getCompany(), accountRequest.getName(), accountRequest.getDomain(),
                accountRequest.getMxDomain(), accountRequest.getCreator(), props);
    }

    public static void update(@NonNull Connection conn, @NonNull AccountRequest accountRequest) throws SQLException {
        Preconditions.checkArgument(accountRequest.getId() > 0);
        String props = accountRequest.getProperties() == null ? "{}" : JSONObject.toJSONString(accountRequest.getProperties());
        DaoUtils.executeUpdate(conn, String.format(SQL_UPDATE, Constants.COREDB),
                accountRequest.getStatus(),
                accountRequest.getCompany(), accountRequest.getName(), accountRequest.getDomain(),
                accountRequest.getMxDomain(), accountRequest.getCreator(), props, accountRequest.getId());
    }

    public static void delete(@NonNull Connection conn, long id) throws SQLException {
        DaoUtils.executeUpdate(conn, String.format(SQL_DELETE, Constants.COREDB), id);
    }
}
