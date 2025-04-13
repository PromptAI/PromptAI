package com.zervice.kbase;

import com.zervice.common.restful.exception.RestException;
import com.zervice.common.restful.exception.StatusCodes;
import com.zervice.common.utils.LayeredConf;

public class Environment {
    public static long ACCOUNT_ID_TEST = 0x12345678L;

    /**
     * is current database is H2 MEMORY db?
     * @return default is false
     */
    public static boolean isH2() {
        return LayeredConf.getString("database.type", "mysql").equalsIgnoreCase("h2");
    }

    public static boolean isTestMode() {
        return LayeredConf.getBoolean("testmode.enable", false);
    }

    /**
     * Am i the master?
     * @return
     */
    public static boolean isMaster() {
        return LayeredConf.getBoolean("isMaster", false);
    }

    public static void mustIsMaster() {
        if (!isMaster()) {
            throw new RestException(StatusCodes.MUST_ON_MASTER_NODE);
        }
    }
}
