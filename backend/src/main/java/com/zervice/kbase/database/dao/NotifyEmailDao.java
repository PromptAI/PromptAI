package com.zervice.kbase.database.dao;

import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.zervice.common.utils.Constants;
import com.zervice.kbase.database.helpers.Auditable;
import com.zervice.kbase.database.helpers.CoreDbTable;
import com.zervice.kbase.database.pojo.NotifyEmail;
import com.zervice.kbase.database.utils.DaoUtils;
import com.zervice.kbase.database.utils.RecordLoader;
import lombok.NonNull;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;


/**
 * this table store all invitation or notification emails sent from system to user.
 * Note we are not implementing an email receiver, so we do not have email folder, tags etc.
 */

/**
 * create table notify_emails (
 *   id bigint not null primary key auto_increment,
 *   recipient varchar(128) not null,
 *   subject varchar(1024) not null,
 *   body text(65535),
 *   category varchar(128) not null,  // INVITATION, DELETION, SUSPEND, see enum in NotifyEmail
 *   status varchar(32) not null,     // created, sent, see constants in NotifyEmail
 *   createdEpochMs bigint not null,
 *   sentEpochMs bigint not null,
 *   properties text(2048) not null
 * ) engine=innodb ROW_FORMAT=DYNAMIC
 */
@CoreDbTable
@Auditable(disabled = true)
public class NotifyEmailDao {

    public static final String TABLE_NAME = "notify_emails";

    private static final String _SQL_DELETE = "DELETE FROM %s.notify_emails WHERE id=? ";

    private static final String _SQL_INSERT = "INSERT INTO %s.notify_emails (" +
            "recipient, subject, body, category, status, createdEpochMs, sentEpochMs, properties) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String _SQL_SELECT_ALL = "SELECT " +
            "id, recipient, subject, body, category, status, createdEpochMs, sentEpochMs, properties FROM %s.notify_emails ";

    private static final String _SQL_SELECT_NO_SENT = "SELECT " +
            "id, recipient, subject, body, category, status, createdEpochMs, sentEpochMs, properties FROM %s.notify_emails where sentEpochMs=0 " +
            "order by createdEpochMs ASC";

    private static final String _SQL_SELECT_NO_SENT_HEAD = "SELECT " +
            "id, recipient, subject, body, category, status, createdEpochMs, sentEpochMs, properties FROM %s.notify_emails where sentEpochMs=0 " +
            "ORDER BY  createdEpochMs ASC LIMIT 1";
    private static final String _SQL_UPDATE_SENT = "UPDATE %s.notify_emails SET status=?, sentEpochMs=? WHERE id=?";

    private static final String _SQL_SELECT_BY_ID = "SELECT " +
            "id, recipient, subject, body, category, status, createdEpochMs, sentEpochMs, properties FROM %s.notify_emails where id=?";

    private static final String _SQL_SELECT_BY_RECIPIENT = "SELECT " +
            "id, recipient, subject, body, category, status, createdEpochMs, sentEpochMs, properties FROM %s.notify_emails where category=? and recipient=? " +
            "order by createdEpochMs";

    private static final String _SQL_UPDATE_PROPS = "UPDATE %s.notify_emails SET properties=? WHERE id=?";
    private static final String _SQL_UPDATE_STATUS = "UPDATE %s.notify_emails SET status=? WHERE id=?";


    private final static RecordLoader<NotifyEmail> _LOADER = rs -> {
        NotifyEmail email = NotifyEmail.builder()
                .id(rs.getLong("id"))
                .recipient(rs.getString("recipient"))
                .subject(rs.getString("subject"))
                .body(rs.getString("body"))
                .category(rs.getString("category"))
                .status(rs.getString("status"))
                .createdEpochMs(rs.getLong("createdEpochMs"))
                .sentEpochMs(rs.getLong("sentEpochMs"))
                .build();

        email.setProperties(NotifyEmail.EmailProp.parse(rs.getString("properties")));
        return email;
    };

    public static void createTable(@NonNull Connection conn, String dbName) throws SQLException {
        String sql = String.format(
                "CREATE TABLE IF NOT EXISTS %s.notify_emails (\n" +
                        " id bigint not null primary key auto_increment,\n" +
                        " recipient varchar(256) not null,\n" +
                        " subject varchar(1024) not null,\n" +
                        " body text(65535),\n " +
                        " category varchar(128) not null,\n" +
                        " status varchar(32) not null,\n" +
                        " createdEpochMs bigint not null,\n" +
                        " sentEpochMs bigint not null,\n" +
                        " properties text(65535) \n" +
                        " ) engine=innodb ROW_FORMAT=DYNAMIC ", dbName
        );

        DaoUtils.executeUpdate(conn, sql);
    }

    public static long addReturnId(@NonNull Connection conn, @NonNull NotifyEmail email) throws SQLException {
        String prop = email.getProperties() == null ? "{}" : JSONObject.toJSONString(email.getProperties());
        return DaoUtils.executeUpdate(conn, String.format(_SQL_INSERT, Constants.COREDB),
                email.getRecipient(), email.getSubject(), email.getBody(), email.getCategory(),
                email.getStatus(), email.getCreatedEpochMs(), email.getSentEpochMs(), prop);
    }

    public static NotifyEmail getUnsentHead(@NonNull Connection conn) throws SQLException {
        return DaoUtils.get(conn, String.format(_SQL_SELECT_NO_SENT_HEAD, Constants.COREDB), _LOADER::load);
    }

    public static List<NotifyEmail> getUnsentAll(@NonNull Connection conn) throws SQLException {
        return DaoUtils.getList(conn, String.format(_SQL_SELECT_NO_SENT, Constants.COREDB), _LOADER::load);
    }

    public static List<NotifyEmail> get(@NonNull Connection conn, String recipient) throws SQLException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(recipient));

        return DaoUtils.getList(conn, String.format(_SQL_SELECT_BY_RECIPIENT, Constants.COREDB), _LOADER::load, recipient);
    }

    public static NotifyEmail get(@NonNull Connection conn, long id) throws SQLException {
        Preconditions.checkArgument(id > 0);

        return DaoUtils.get(conn, String.format(_SQL_SELECT_BY_ID, Constants.COREDB), _LOADER::load, id);
    }


    public static List<NotifyEmail> getAll(@NonNull Connection conn) throws SQLException {

        return DaoUtils.getList(conn, String.format(_SQL_SELECT_ALL, Constants.COREDB), _LOADER::load);
    }

    public static void updateSent(@NonNull Connection conn, long id, String status, long sentEpochMs) throws SQLException {

        DaoUtils.executeUpdate(conn, String.format(_SQL_UPDATE_SENT, Constants.COREDB), status, sentEpochMs, id);
    }

    public static void delete(@NonNull Connection conn, long id) throws SQLException {
        Preconditions.checkArgument(id > 0);
        DaoUtils.executeUpdate(conn, String.format(_SQL_DELETE, Constants.COREDB), id);
    }
}
