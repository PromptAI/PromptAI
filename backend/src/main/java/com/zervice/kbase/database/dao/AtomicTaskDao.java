package com.zervice.kbase.database.dao;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.zervice.common.utils.IdGenerator;
import com.zervice.kbase.database.helpers.Auditable;
import com.zervice.kbase.database.helpers.CustomerDbTable;
import com.zervice.kbase.database.pojo.AtomicTask;
import com.zervice.kbase.database.utils.DaoUtils;
import com.zervice.kbase.database.utils.RecordLoader;
import lombok.NonNull;

import java.sql.Connection;
import java.sql.SQLException;


@CustomerDbTable
@Auditable(disabled = true)
public class AtomicTaskDao {
    public static final String TABLE_NAME = "atomic_task";

    private static final String _SQL_INSERT =
            "INSERT INTO %s.atomic_task (id, enqueued_at_epoch_ms, dequeued_at_epoch_ms, status, username, " +
            "data) VALUES (?, ?, ?, ?, ?, ?)";

    private static final String _SQL_DELETE = "DELETE FROM %s.atomic_task WHERE id=?";

    private static final String _SQL_GET =
            "SELECT id, enqueued_at_epoch_ms, dequeued_at_epoch_ms, status, username, data FROM %s.atomic_task ";
    private static final String _SQL_GETHEAD = _SQL_GET + " ORDER BY id ASC LIMIT 1";

    private static final String _SQL_UPDATE_STATUS = "UPDATE %s.atomic_task SET dequeued_at_epoch_ms=?, status=? WHERE id=?";

    private static final String _SQL_LENGTH = "SELECT count(*) from %s.atomic_task";



    public static void createTable(@NonNull Connection conn,
                                   String dbName) throws SQLException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(dbName));

        String sql = String.format(
                "CREATE TABLE %s.atomic_task ( " +
                        "id bigint not null primary key, " +
                        "enqueued_at_epoch_ms bigint not null, " +
                        "dequeued_at_epoch_ms bigint not null, " +
                        "status int not null, " +
                        "username varchar(256) not null default '', " +
                        "data text(65535) not null " +
                        ") ENGINE=INNODB", dbName
        );
        DaoUtils.executeUpdate(conn, sql);
    }

    private final static RecordLoader<AtomicTask> _LOADER = rs -> {
        AtomicTask s = new AtomicTask();
        s.setId(rs.getLong(1));
        s.setEnqueuedAtEpochMs(rs.getLong(2));
        s.setDequeuedAtEpochMs(rs.getLong(3));
        s.setStatus(AtomicTask.Status.fromInt(rs.getInt(4)));
        s.setIdentity(rs.getString(5));
        s.setData(JSON.parseObject(rs.getString(6)));
        return s;
    };

    public static long add(@NonNull Connection conn,
                           String dbName,
                           @NonNull AtomicTask task) throws SQLException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(dbName));

        long id = IdGenerator.generateId();
        task.setId(id);
        String sql = String.format(_SQL_INSERT, dbName);

        DaoUtils.executeUpdate(conn, sql, id, task.getEnqueuedAtEpochMs(), task.getDequeuedAtEpochMs(),
                AtomicTask.Status.toInt(task.getStatus()), task.getIdentity() == null ? "" : task.getIdentity(),
                task.getData().toString());

        return id;
    }


    public static void remove(@NonNull Connection conn,
                              String dbName,
                              long taskId) throws SQLException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(dbName));
        Preconditions.checkArgument(taskId > 0);

        String sql = String.format(_SQL_DELETE, dbName);
        DaoUtils.executeUpdate(conn, sql, taskId);
    }


    public static AtomicTask getHead(@NonNull Connection conn,
                                        String dbName) throws SQLException {
        String sql = String.format(_SQL_GETHEAD, dbName);
        AtomicTask task = DaoUtils.get(conn, sql, _LOADER::load);
        if (task != null) {
            sql = String.format(_SQL_UPDATE_STATUS, dbName);
            DaoUtils.executeUpdate(conn, sql, System.currentTimeMillis(),
                    AtomicTask.Status.toInt(AtomicTask.Status.PROCESSING), task.getId());
        }
        return task;
    }


    public static AtomicTask get(@NonNull Connection conn,
                                    String dbName,
                                    long id) throws SQLException {
        String sql = String.format(_SQL_GET + " WHERE id=?", dbName);
        return DaoUtils.get(conn, sql, _LOADER::load, id);
    }


    public static int length(@NonNull Connection conn,
                             String dbName) throws SQLException {
        String sql = String.format(_SQL_LENGTH, dbName);
        return DaoUtils.getInt(conn, sql);
    }
}
