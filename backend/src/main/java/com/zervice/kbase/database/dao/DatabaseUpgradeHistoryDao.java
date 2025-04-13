package com.zervice.kbase.database.dao;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.zervice.kbase.database.helpers.Auditable;
import com.zervice.kbase.database.helpers.CoreDbTable;
import com.zervice.kbase.database.pojo.DatabaseUpgrade;
import com.zervice.kbase.database.utils.DaoUtils;
import com.zervice.kbase.database.utils.RecordLoader;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * create table $controld.db_upgrade_history (
 *   id bigint not null primary key auto_increment,
 *   ver int not null,
 *   ticket varchar(128) not null,
 *   executed_epoch_ms bigint not null
 * ) engine=innodb
 */
@CoreDbTable
@Auditable(disabled = true)
public class DatabaseUpgradeHistoryDao {
    public static final String TABLE_NAME = "db_upgrade_history";

    private static final String _SQL_INSERT = "INSERT INTO %s.db_upgrade_history (ver, ticket, " +
            "executed_epoch_ms) VALUES (?, ?, ?) ";
    private static final String _SQL_SELECT_BY_TICKET = "SELECT id, ver, ticket, executed_epoch_ms FROM %s.db_upgrade_history WHERE ver=%s AND ticket LIKE '%s-%%'";

    private final static RecordLoader<DatabaseUpgrade> _LOADER = rs -> {
        DatabaseUpgrade dbMigration = new DatabaseUpgrade();
        dbMigration.setId(rs.getLong(1));
        dbMigration.setVer(rs.getInt(2));
        dbMigration.setTicket(rs.getString(3));
        dbMigration.setExecutedEpochMs(rs.getLong(4));
        return dbMigration;
    };

    public static void createTable(Connection conn,
                                   String dbName) throws SQLException {
        String sql = String.format(
                "  create table %s.db_upgrade_history (\n" +
                        "            id bigint not null primary key auto_increment,\n" +
                        "            ver int not null,\n" +
                        "            ticket varchar(128) not null,\n" +
                        "            executed_epoch_ms bigint not null\n" +
                        "          ) engine=innodb", dbName
        );
        DaoUtils.executeUpdate(conn, sql);
    }


    public static void add(Connection conn, String coreDb,
                           DatabaseUpgrade dbMigration) throws SQLException {
        String sql = String.format(_SQL_INSERT, coreDb);
        dbMigration.setId(DaoUtils.executeUpdateWithLastInsertId(conn, sql,
                dbMigration.getVer(), dbMigration.getTicket(), System.currentTimeMillis()));
    }

    public static DatabaseUpgrade get(Connection conn, String coreDb, int ver, String ticket) throws SQLException {
        Preconditions.checkNotNull(conn);
        Preconditions.checkArgument(!Strings.isNullOrEmpty(ticket), "Ticket info is missing");

        return DaoUtils.get(conn, String.format(_SQL_SELECT_BY_TICKET, coreDb, ver, ticket), _LOADER::load);
    }
}
