package com.zervice.kbase.database.helpers;

import java.sql.Connection;

/*
 * AccountDbMigration is for migration which need to upgrade account db.
 * By using this interface, you can avoid write run function name
 */
public interface AccountDbUpgrade {
    void upgradeAccountDb(Connection conn, String dbName) throws Exception;
}
