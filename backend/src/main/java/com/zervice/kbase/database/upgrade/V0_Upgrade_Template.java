package com.zervice.kbase.database.upgrade;

import com.zervice.kbase.database.helpers.AccountDbUpgrade;
import com.zervice.kbase.database.helpers.CoreDbUpgrade;
import com.zervice.kbase.database.helpers.DbUpgrade;

import java.sql.Connection;

@DbUpgrade(ver=0, ticket="#0-change-me-with ticket # and description")
public class V0_Upgrade_Template implements CoreDbUpgrade, AccountDbUpgrade {
    @Override
    public void upgradeCoreDb(Connection conn) {
        // TODO: Change CoreDB ..., DB name set to Constants.COREDB
    }

    @Override
    public void upgradeAccountDb(Connection conn, String dbName) throws Exception {
        // TODO: change account DB, account DB name given by dbName
    }
}
