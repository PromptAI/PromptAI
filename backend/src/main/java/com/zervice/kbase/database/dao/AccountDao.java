package com.zervice.kbase.database.dao;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Preconditions;
import com.zervice.common.pojo.common.Account;
import com.zervice.common.utils.Constants;
import com.zervice.kbase.api.restful.pojo.AccountCriteria;
import com.zervice.kbase.api.restful.pojo.PageRequest;
import com.zervice.kbase.database.helpers.CoreDbTable;
import com.zervice.kbase.database.utils.DaoUtils;
import com.zervice.kbase.database.utils.PageResult;
import com.zervice.kbase.database.utils.RecordLoader;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Access controldb.accounts table
 * <p>
 * CREATE TABLE %s.accounts ( " +
 * "  id BIGINT not null primary key,
 * "  name VARCHAR(128) not null unique,
 * "  dbName VARCHAR(128) not null unique,\n" +
 * "  events JSON not null,
 * "  notes JSON not null,
 * "  properties TEXT(65535) not null - this is a JSON string
 * " ) engine=innodb
 */
@CoreDbTable
public class AccountDao {
    public static String TABLE_NAME = "accounts";

    private final static String SQL_GET_WITHOUT_REST_TOKEN = "SELECT id, name, dbName, events, notes, properties FROM %s.accounts";
    private final static String SQL_INSERT = "INSERT INTO %s.accounts (id, name, dbName, restToken, events, notes, properties) VALUES (?, ?, ?, ?, ?, ?, ?)";
    private final static String SQL_DELETE = "DELETE FROM %s.accounts WHERE id=?";
    private final static String SQL_UPDATE_PROP = "UPDATE %s.accounts set properties=? WHERE id=?";
    private final static String SQL_UPDATE_ALL = "UPDATE %s.accounts set name=?, dbName=?,events=?,notes=?,properties=? WHERE id=?";
    private final static String SQL_GET_BY_NAME = "SELECT id, name, dbName, restToken, events, notes, properties FROM %s.accounts WHERE name=?";
    private final static String SQL_GET_BY_ID = "SELECT id, name, dbName, restToken, events, notes, properties FROM %s.accounts WHERE id=?";
    private final static String SQL_GET_BY_NAME_OR_DBNAME = "SELECT id, name, dbName, restToken, events, notes ,properties FROM %s.accounts WHERE name like ? or dbName like ?";
    private final static String SQL_GET_BY_DBNAME = "SELECT id, name, dbName, restToken, events, notes,properties FROM %s.accounts WHERE dbName like ?";
    private final static String SQL_GET_REST_TOKEN = "SELECT restToken FROM %s.accounts WHERE id = ?";
    private final static String SQL_UPDATE_NOTES = "UPDATE %s.accounts set notes=? WHERE id=?";
    private final static String SQL_UPDATE_EVENTS = "UPDATE %s.accounts set events=? WHERE id=?";

    /*
     * createTable must have dbName parameter, although we didn't use it
     * Because we will use reflect to call createTable when create db
     */
    public static void createTable(@NonNull Connection conn, String dbName) throws SQLException {
        String sql = String.format(
                "CREATE TABLE IF NOT EXISTS %s.accounts (\n" +
                        "         id BIGINT not null primary key,\n" +
                        "         name VARCHAR(128) not null unique,\n" +
                        "         dbName VARCHAR(128) not null unique,\n" +
                        "         restToken BIGINT not null default 0,\n" +
                        "         events JSON not null,\n" +
                        "         notes JSON not null,\n" +
                        "         properties JSON not null \n" +
                        "        ) engine=innodb ROW_FORMAT=DYNAMIC", Constants.COREDB
        );
        DaoUtils.executeUpdate(conn, sql);
    }


    private final static RecordLoader<Account> _LOADER_ALL = rs -> {
        Account account = new Account();
        account.setId(rs.getLong("id"));
        account.setName(rs.getString("name"));
        account.setDbName(rs.getString("dbName"));
        account.setRestToken(rs.getLong("restToken"));
        account.setEvents(JSONArray.parseArray(rs.getString("events")));
        account.setNotes(JSONArray.parseArray(rs.getString("notes")));
        JSONObject jo = JSONObject.parseObject(rs.getString("properties"));
        account.setProperties(jo);
        return account;
    };

    private final static RecordLoader<Account> _LOADER_WITHOUT_REST_TOKEN = rs -> {
        Account account = new Account();
        account.setId(rs.getLong("id"));
        account.setName(rs.getString("name"));
        account.setDbName(rs.getString("dbName"));
        // 不查询这个字段
//        account.setRestToken(rs.getLong("restToken"));
        account.setEvents(JSONArray.parseArray(rs.getString("events")));
        account.setNotes(JSONArray.parseArray(rs.getString("notes")));
        JSONObject jo = JSONObject.parseObject(rs.getString("properties"));
        account.setProperties(jo);
        return account;
    };

    public static void saveNotes(@NonNull Connection conn, long id, String notes) throws SQLException {
        DaoUtils.executeUpdate(conn, String.format(SQL_UPDATE_NOTES, Constants.COREDB), notes, id);
    }

    public static void saveEvents(@NonNull Connection conn, long id, String notes) throws SQLException {
        DaoUtils.executeUpdate(conn, String.format(SQL_UPDATE_EVENTS, Constants.COREDB), notes, id);
    }

    public static List<Account> getAll(@NonNull Connection conn) throws SQLException {
        List<Account> accounts = DaoUtils.getList(conn,
                String.format(SQL_GET_WITHOUT_REST_TOKEN, Constants.COREDB),
                _LOADER_WITHOUT_REST_TOKEN::load);
        return _filterNotDelete(accounts);
    }

    private static List<Account> _filterNotDelete(List<Account> accounts) {
        return accounts.stream()
                .filter(a -> !a.isDeleted())
                .collect(Collectors.toList());
    }
    /**
     * TODO: remove me
     */
    public static List<Account> getAll(@NonNull Connection conn, String coreDb) throws SQLException {
        List<Account> accounts = DaoUtils.getList(conn,
                String.format(SQL_GET_WITHOUT_REST_TOKEN, coreDb),
                _LOADER_WITHOUT_REST_TOKEN::load);
        return _filterNotDelete(accounts);
    }

    public static List<Account> getDeletedAccounts(Connection conn) throws SQLException{
        List<Account> accounts = DaoUtils.getList(conn,
                String.format(SQL_GET_WITHOUT_REST_TOKEN, Constants.COREDB),
                _LOADER_WITHOUT_REST_TOKEN::load);

        return accounts.stream()
                .filter(Account::isDeleted)
                .collect(Collectors.toList());
    }


    /**
     * Remove all accounts from the accounts table
     */
    public static void removeAll(@NonNull Connection conn) throws SQLException {
        DaoUtils.executeUpdate(conn, String.format("DELETE FROM %s.accounts", Constants.COREDB));
    }


    public static void add(@NonNull Connection conn, @NonNull Account account) throws SQLException {
        String events = account.getEvents() == null ? "[]" : JSONObject.toJSONString(account.getEvents());
        String notes = account.getNotes() == null ? "[]" : JSONObject.toJSONString(account.getNotes());
        String props = account.getProperties() == null ? "{}" : JSONObject.toJSONString(account.getProperties());

        long restToken = account.getRestToken() == null ? 0 : account.getRestToken();
        DaoUtils.executeUpdate(conn, String.format(SQL_INSERT, Constants.COREDB), account.getId(), account.getName(),
                account.getDbName(), restToken, events, notes, props);
    }

    public static void updateProp(@NonNull Connection conn,  @NonNull Account account) throws SQLException {
        DaoUtils.executeUpdate(conn, String.format(SQL_UPDATE_PROP, Constants.COREDB), account.getProperties(), account.getId());
    }

    /**
     * 这里不更新 rest Token，这是计费的字段，单独更新
     */
    public static void update(@NonNull Connection conn, @NonNull Account account) throws SQLException {
        Preconditions.checkArgument(account.getId() > 0);
        String events = account.getEvents() == null ? "[]" : JSONObject.toJSONString(account.getEvents());
        String notes = account.getNotes() == null ? "[]" : JSONObject.toJSONString(account.getNotes());
        String props = account.getProperties() == null ? "{}" : JSONObject.toJSONString(account.getProperties());

        DaoUtils.executeUpdate(conn, String.format(SQL_UPDATE_ALL, Constants.COREDB),
                account.getName(),
                account.getDbName(),
                events, notes,
                props, account.getId());
    }

    /**
     * 这个方法在增减rest token余额，这里有一些注意事项
     * 1、需要保证调用该方法时开启事务
     * 2、该事物只执行该操作：mysql发现是写入会开启写锁，所以这并发场景下不会有问题
     */
    public static void recalculateRestToken(@NonNull Connection conn, long id, long count) throws SQLException {
        Preconditions.checkArgument(id > 0);
        String sql = "update %s.%s set restToken = restToken + " + count + " where id = ?";
        DaoUtils.executeUpdate(conn, String.format(sql, Constants.COREDB, TABLE_NAME), id);
    }

    /**
     * 判断token是否足量
     */
    public static boolean enoughToken(@NonNull Connection conn, long id) throws SQLException{
        return getRestToken(conn, id) > 0;
    }

    public static void delete(@NonNull Connection conn, long id) throws SQLException {
        DaoUtils.executeUpdate(conn, String.format(SQL_DELETE, Constants.COREDB), id);
    }


    public static Account getByName(@NonNull Connection conn, @NonNull String name) throws SQLException {
        return DaoUtils.get(conn, String.format(SQL_GET_BY_NAME, Constants.COREDB), _LOADER_WITHOUT_REST_TOKEN::load, name);
    }

    public static Long getRestToken(Connection conn, long id) throws SQLException{
        return DaoUtils.getLong(conn, String.format(SQL_GET_REST_TOKEN, Constants.COREDB), id);
    }

    public static Account getByDbName(@NonNull Connection conn,  @NonNull String dbName) throws SQLException {
        return DaoUtils.get(conn, String.format(SQL_GET_BY_DBNAME, Constants.COREDB), _LOADER_WITHOUT_REST_TOKEN::load, dbName);
    }
    public static Account get(@NonNull Connection conn, long id) throws SQLException {
        Preconditions.checkArgument(id > 0);
        Account account = DaoUtils.get(conn, String.format(SQL_GET_BY_ID, Constants.COREDB), _LOADER_ALL::load, id);
        if (account == null || account.isDeleted()) {
            return null;
        }

        return account;
    }


    public static void updateStatus(@NonNull Connection conn,
                                    long acctId,
                                    String newStatus) throws SQLException {
        Preconditions.checkArgument(acctId > 0 &&
                !StringUtils.isEmpty(newStatus) &&
                Account.isValidStatus(newStatus));

        Account account = get(conn, acctId);
        if (account == null) {
            throw new IllegalArgumentException("No such account - " + acctId);
        }
        JSONObject prop = account.getProperties();
        prop.put("status", newStatus);
        DaoUtils.executeUpdate(conn, String.format(SQL_UPDATE_PROP, Constants.COREDB),
                prop.toString(), acctId);
    }

    public static PageResult<Account> getList(@NonNull Connection conn, AccountCriteria criteria, PageRequest pageRequest)
            throws Exception {

        String querySql = SQL_GET_WITHOUT_REST_TOKEN + "  WHERE 1=1 ";

        String countSql = "select count(id) from %s.%s where 1=1 ";
        //build page sql
        StringBuilder pageSqlBuilder = new StringBuilder(querySql);
        StringBuilder countSqlBuilder = new StringBuilder(countSql);

        if (criteria.getName() != null) {
            pageSqlBuilder.append(" and name like '%%").append(criteria.getName()).append("%%' ");
            countSqlBuilder.append(" and name like '%%").append(criteria.getName()).append("%%' ");
        }

        PageRequest.buildSortAndLimitSql(pageRequest, pageSqlBuilder);

        //query list
        List<Account> accounts = DaoUtils.getList(conn, String.format(pageSqlBuilder.toString(), Constants.COREDB, TABLE_NAME), _LOADER_ALL);

        //query total count
        Long totalCount = DaoUtils.getLong(conn, String.format(countSqlBuilder.toString(), Constants.COREDB, TABLE_NAME));

        return PageResult.of(accounts, totalCount);

    }
}
