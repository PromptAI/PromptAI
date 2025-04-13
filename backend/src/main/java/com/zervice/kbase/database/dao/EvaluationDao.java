package com.zervice.kbase.database.dao;

import com.alibaba.fastjson.JSONObject;
import com.zervice.kbase.database.helpers.CustomerDbTable;
import com.zervice.kbase.database.pojo.Evaluation;
import com.zervice.kbase.database.utils.DaoUtils;
import com.zervice.kbase.database.utils.RecordLoader;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

/**
 * message的评价
 * 目前只有到叶子节点才能评价
 *
 * @author chen
 * @date 2022/10/28
 */
@CustomerDbTable
public class EvaluationDao {

    public static final String TABLE_NAME = "evaluations";

    private static final String _SQL_INSERT = "INSERT INTO %s." + TABLE_NAME + " (componentId, chatId, messageId, helpful, properties) VALUES (?, ?, ?, ?, ?) ";
    private static final String _SQL_SELECT = "SELECT id, componentId, chatId, messageId, helpful, properties FROM %s." + TABLE_NAME;
    private static final String _SQL_SELECT_BY_ID = "SELECT id, componentId, chatId, messageId, helpful, properties FROM %s." + TABLE_NAME + " where id=? ";
    private static final String _SQL_SELECT_BY_CHAT_ID = "SELECT id, componentId, chatId, messageId, helpful, properties FROM %s." + TABLE_NAME + " where chatId=? ";
    private static final String _SQL_SELECT_BY_MESSAGE_ID = "SELECT id, componentId, chatId, messageId, helpful, properties FROM %s." + TABLE_NAME + " where messageId=? ";
    private static final String _SQL_SELECT_COUNT_BY_CHAT_ID = "SELECT count(id) FROM %s." + TABLE_NAME + " where chatId=?";
    private static final String _SQL_DELETE = "DELETE FROM %s." + TABLE_NAME + " WHERE id=? ";

    private static RecordLoader<Evaluation> _LOADER = rs -> Evaluation.createEvaluationFromDao(
            rs.getLong("id"),
            rs.getString("componentId"),
            rs.getString("chatId"),
            rs.getString("messageId"),
            rs.getInt("helpful"),
            rs.getString("properties")
    );

    public static void createTable(Connection conn, String dbName) throws SQLException {
        String sql = String.format(
                "create table %s." + TABLE_NAME + " (\n" +
                        "          id bigint not null primary key auto_increment,\n" +
                        "          componentId varchar(64) not null,\n" +
                        "          chatId varchar(64) not null,\n" +
                        "          messageId varchar(64) unique not null,\n" +
                        "          helpful tinyint(2) not null,\n" +
                        "          properties text(65535) not null,\n" +
                        "          index index_chat_id(chatId)\n" +
                        "         ) engine=innodb ROW_FORMAT=DYNAMIC ", dbName
        );
        DaoUtils.executeUpdate(conn, sql);
    }

    public static Long add(Connection conn, String dbName, Evaluation evaluation) throws SQLException {
        String prop = evaluation.getProperties() == null ? "{}" : JSONObject.toJSONString(evaluation.getProperties());

        return DaoUtils.executeUpdateWithLastInsertId(conn, String.format(_SQL_INSERT, dbName),
                evaluation.getComponentId(), evaluation.getChatId(), evaluation.getMessageId(), evaluation.getHelpful(), prop);
    }

    public static List<Evaluation> getByChatId(Connection conn, String dbName, String chatId) throws SQLException {
        return DaoUtils.getList(conn, String.format(_SQL_SELECT_BY_CHAT_ID, dbName), _LOADER, chatId);
    }

    public static List<Evaluation> getByMessageId(Connection conn, String dbName, String chatId) throws SQLException {
        return DaoUtils.getList(conn, String.format(_SQL_SELECT_BY_CHAT_ID, dbName), _LOADER, chatId);
    }

    public static List<ChatCount> getChatCount(Connection conn, String dbName, List<String> chatIds) throws SQLException{
        if (CollectionUtils.isEmpty(chatIds)) {
            return Collections.emptyList();
        }

        String sql = "select count(id) as count, chatId from %s.%s group by chatId";
        RecordLoader<ChatCount> loader = rs -> ChatCount.factory(
                rs.getString("chatId"),
                rs.getLong("count"));

        return DaoUtils.getList(conn, String.format(sql, dbName, TABLE_NAME), loader);
    }

    public static Long countByChatId(Connection conn, String dbName, String chatId) throws SQLException {
        return DaoUtils.getLong(conn, String.format(_SQL_SELECT_COUNT_BY_CHAT_ID, dbName), chatId);
    }

    @Setter@Getter
    public static class ChatCount {
        private String _chatId;
        private Long _count;

        public static ChatCount empty(String chatId) {
            return factory(chatId, 0L);
        }

        public static ChatCount factory(String chatId, Long count) {
            ChatCount chatCount = new ChatCount();
            chatCount.setCount(count);
            chatCount.setChatId(chatId);
            return chatCount;
        }
    }

}
