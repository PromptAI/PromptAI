package com.zervice.kbase.database.helpers;

import java.sql.Connection;

/*
 * ControlDbMigration is for migration which need to upgrade control db.
 * By using this interface, you can avoid write run function name
 */
public interface CoreDbUpgrade {
    void upgradeCoreDb(Connection conn) throws Exception;
}
