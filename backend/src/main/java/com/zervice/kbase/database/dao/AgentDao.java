package com.zervice.kbase.database.dao;

import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Preconditions;
import com.zervice.common.utils.Constants;
import com.zervice.kbase.api.restful.pojo.PageRequest;
import com.zervice.kbase.database.criteria.AgentCriteria;
import com.zervice.kbase.database.helpers.CoreDbTable;
import com.zervice.kbase.database.pojo.Agent;
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
@CoreDbTable
public class AgentDao {
    public static final String TABLE_NAME = "agents";

    private static final String _SQL_INSERT = "INSERT INTO %s." + TABLE_NAME + " (id ,dbName, ak, status, properties) VALUES (?, ?, ?, ?, ?) ";
    private static final String _SQL_SELECT = "SELECT id, dbName, ak, status, properties FROM %s." + TABLE_NAME;
    private static final String _SQL_SELECT_COUNT = "SELECT count(id) FROM %s." + TABLE_NAME;
    private static final String _SQL_SELECT_BY_DB_NAME = "SELECT id, dbName, ak, status, properties FROM %s." + TABLE_NAME + " where dbName=? ";
    private static final String _SQL_SELECT_BY_ID = "SELECT id, dbName, ak, status, properties FROM %s." + TABLE_NAME + " where id=? ";
    private static final String _SQL_SELECT_BY_ID_AND_DB_NAME = "SELECT id, dbName, ak, status, properties FROM %s." + TABLE_NAME + " where id=? and dbName=? ";
    private static final String _SQL_DELETE = "DELETE FROM %s." + TABLE_NAME + " WHERE id=? ";
    private static final String _SQL_UPDATE_ALL = "UPDATE %s." + TABLE_NAME + " SET dbName=?, ak=?, status=?,  properties=? WHERE id=?";
    private static final String _SQL_UPDATE_STATUS = "UPDATE %s." + TABLE_NAME + " SET  status=?  WHERE id=?";
    private static final String _SQL_UPDATE_PROP = "UPDATE %s." + TABLE_NAME + " SET  properties=?  WHERE id=?";


    private static RecordLoader<Agent> _LOADER = rs -> Agent.createAgentFromDao(
            rs.getString("id"),
            rs.getString("dbName"),
            rs.getString("ak"),
            rs.getInt("status"),
            rs.getString("properties")
    );

    public static void createTable(Connection conn, String dbName) throws SQLException {
        String sql = String.format(
                "create table %s." + TABLE_NAME + " (\n" +
                        "          id varchar(64) not null unique,\n" +
                        "          dbName varchar(256) not null,\n" +
                        "          ak varchar(256) not null,\n" +
                        "          status tinyint(2)  not null,\n" +
                        "          properties text(65535) not null\n" +
                        "         ) engine=innodb ROW_FORMAT=DYNAMIC ", Constants.COREDB
        );
        DaoUtils.executeUpdate(conn, sql);
    }


    public static void add(Connection conn, Agent agent) throws SQLException {

        Preconditions.checkArgument(StringUtils.isNotEmpty(agent.getId()));
        String prop = agent.getProperties() == null ? "{}" : JSONObject.toJSONString(agent.getProperties());

        if (StringUtils.isBlank(agent.getId())) {
            agent.setId(Agent.generateId());
        }

        DaoUtils.executeUpdate(conn, String.format(_SQL_INSERT, Constants.COREDB), StringUtils.lowerCase(agent.getId()),
                agent.getDbName(), agent.getAk(), agent.getStatus(), prop);
    }

    public static void update(Connection conn, Agent agent) throws SQLException {
        Preconditions.checkArgument(StringUtils.isNotEmpty(agent.getId()));

        String prop = agent.getProperties() == null ? "{}" : JSONObject.toJSONString(agent.getProperties());

        DaoUtils.executeUpdate(conn, String.format(_SQL_UPDATE_ALL, Constants.COREDB), agent.getDbName(),
                agent.getAk(), agent.getStatus(), prop, agent.getId());
    }

    public static void updateStatus(Connection conn, Agent agent) throws SQLException {
        Preconditions.checkArgument(StringUtils.isNotEmpty(agent.getId()));
        DaoUtils.executeUpdate(conn, String.format(_SQL_UPDATE_STATUS, Constants.COREDB), agent.getStatus(), agent.getId());
    }

    public static void updateProp(Connection conn, Agent agent) throws SQLException {
        Preconditions.checkArgument(StringUtils.isNotEmpty(agent.getId()));
        String prop = agent.getProperties() == null ? "{}" : JSONObject.toJSONString(agent.getProperties());
        DaoUtils.executeUpdate(conn, String.format(_SQL_UPDATE_PROP, Constants.COREDB), prop, agent.getId());
    }

    public static Agent get(Connection conn, String id) throws SQLException {
        Preconditions.checkArgument(StringUtils.isNotEmpty(id));

        return DaoUtils.get(conn, String.format(_SQL_SELECT_BY_ID, Constants.COREDB), _LOADER, id);
    }

    public static Agent get(Connection conn, String dbName, String id) throws SQLException {
        Preconditions.checkArgument(StringUtils.isNotEmpty(id));
        return DaoUtils.get(conn, String.format(_SQL_SELECT_BY_ID_AND_DB_NAME, Constants.COREDB), _LOADER, id, dbName);
    }

    public static List<Agent> get(Connection conn) throws SQLException {
        return DaoUtils.getList(conn, String.format(_SQL_SELECT, Constants.COREDB), _LOADER);
    }

    public static List<Agent> getByDbName(Connection conn, String dbName) throws SQLException {
        return DaoUtils.getList(conn, String.format(_SQL_SELECT_BY_DB_NAME, Constants.COREDB), _LOADER, dbName);
    }

    public static Agent getById(Connection conn, String id) throws SQLException {
        return DaoUtils.get(conn, String.format(_SQL_SELECT_BY_ID, Constants.COREDB), _LOADER, id);
    }

    public static PageResult<Agent> get(Connection conn,
                                        AgentCriteria criteria, PageRequest pageRequest) throws Exception {
        StringBuilder pageSql = new StringBuilder(_SQL_SELECT).append(" where 1=1");
        StringBuilder countSql = new StringBuilder(_SQL_SELECT_COUNT).append(" where 1=1");

        if (StringUtils.isNotBlank(criteria.getDbName())) {
            pageSql.append(" and dbName = '").append(criteria.getDbName()).append("' ");
            countSql.append(" and dbName = '").append(criteria.getDbName()).append("' ");
        }

        if (criteria.getStatus() != null) {
            pageSql.append(" and status =").append(criteria.getStatus()).append(" ");
            countSql.append(" and status =").append(criteria.getStatus()).append(" ");
        }

        PageRequest.buildSortAndLimitSql(pageRequest, pageSql);

        //query list
        List<Agent> hotQuestions = DaoUtils.getList(conn, String.format(pageSql.toString(), Constants.COREDB, Constants.COREDB), _LOADER);

        //query total count
        Long totalCount = DaoUtils.getLong(conn, String.format(countSql.toString(), Constants.COREDB, Constants.COREDB));

        return PageResult.of(hotQuestions, totalCount);
    }

    public static void delete(Connection conn, String id) throws SQLException {
        Preconditions.checkArgument(id != null);

        DaoUtils.executeUpdate(conn, String.format(_SQL_DELETE, Constants.COREDB), id);
    }
}
