package com.zervice.kbase.database.dao;

import com.zervice.common.utils.Constants;
import com.zervice.kbase.api.restful.pojo.PageRequest;
import com.zervice.kbase.database.criteria.FeedbackCriteria;
import com.zervice.kbase.database.helpers.CoreDbTable;
import com.zervice.kbase.database.pojo.Feedback;
import com.zervice.kbase.database.utils.DaoUtils;
import com.zervice.kbase.database.utils.PageResult;
import com.zervice.kbase.database.utils.RecordLoader;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

@CoreDbTable
public class FeedbackDao {

    public static final String TABLE_NAME = "feedbacks";

    private static final String _SQL_INSERT = "INSERT INTO %s." + TABLE_NAME + " (accountId, contact, content, time) VALUES (?, ?, ?, ?) ";
    private static final String _SQL_SELECT = "SELECT id, accountId, contact, content, time FROM %s." + TABLE_NAME;
    private static final String _SQL_SELECT_BY_ID = "SELECT id, accountId, contact, content, time FROM %s." + TABLE_NAME + " where id=? ";
    private static final String _SQL_SELECT_COUNT = "SELECT count(id) FROM %s." + TABLE_NAME;

    private static RecordLoader<Feedback> _LOADER = rs -> Feedback.createFeedbackFromDao(
            rs.getLong("id"),
            rs.getLong("accountId"),
            rs.getString("contact"),
            rs.getString("content"),
            rs.getLong("time")
    );

    public static void createTable(Connection conn, String dbName) throws SQLException {
        String sql = String.format(
                "create table %s." + TABLE_NAME + " (\n" +
                        "          id bigint not null primary key auto_increment,\n" +
                        "          accountId bigint not null,\n" +
                        "          contact varchar(50) not null,\n" +
                        "          content text not null,\n" +
                        "          time bigint not null\n" +
                        "         ) engine=innodb ROW_FORMAT=DYNAMIC ", Constants.COREDB
        );
        DaoUtils.executeUpdate(conn, sql);
    }

    public static Long add(Connection conn, Feedback feedback) throws SQLException {
        return DaoUtils.executeUpdateWithLastInsertId(conn, String.format(_SQL_INSERT, Constants.COREDB),
                feedback.getAccountId(), feedback.getContact(), feedback.getContent(), feedback.getTime());
    }
    public static PageResult<Feedback> get(@NonNull Connection conn, FeedbackCriteria criteria,
                                             PageRequest pageRequest) throws Exception {
        StringBuilder pageSql = new StringBuilder(_SQL_SELECT).append(" where 1=1");
        StringBuilder countSql = new StringBuilder(_SQL_SELECT_COUNT).append(" where 1=1");

        if (StringUtils.isNotBlank(criteria.getContact())) {
            pageSql.append(" and contact like '%%").append(criteria.getContact()).append("%%' ");
            countSql.append(" and contact like '%%").append(criteria.getContact()).append("%%' ");
        }

        if (StringUtils.isNotBlank(criteria.getContent())) {
            pageSql.append(" and content like '%%").append(criteria.getContent()).append("%%' ");
            countSql.append(" and content like '%%").append(criteria.getContent()).append("%%' ");
        }

        if (criteria.getAccountId() != null) {
            pageSql.append(" and accountId =").append(criteria.getAccountId()).append(" ");
            countSql.append(" and accountId =").append(criteria.getAccountId()).append(" ");
        }

        PageRequest.buildSortAndLimitSql(new PageRequest(pageRequest.getPage(), pageRequest.getSize(), "id,desc"), pageSql);

        //query list
        List<Feedback> feedbacks = DaoUtils.getList(conn, String.format(pageSql.toString(), Constants.COREDB), _LOADER);

        //query total count
        Long totalCount = DaoUtils.getLong(conn, String.format(countSql.toString(), Constants.COREDB));

        return PageResult.of(feedbacks, totalCount);
    }

}
