package com.zervice.kbase;

import com.zervice.common.pojo.common.Account;
import com.zervice.common.utils.Constants;
import com.zervice.common.utils.EnvUtil;

/**
 * @author Peng Chen
 * @date 2022/7/29
 */
public class ZBotRuntime {

    public static final long DEFAULT_INTERNAL_ACCOUNT_ID = 1;

    /**
     * If running in standalone mode, we would assume ID 1 as the account ID
     */
    public static final String DEFAULT_EXTERNAL_ACCOUNT_ID = Account.getExternalId(DEFAULT_INTERNAL_ACCOUNT_ID);

    /**
     *  // single-user mode, only one account per-server
     */
    public static final String MODE_STANDALONE = "standalone";



    public static final String MODE_SUBDOMAIN = "subdomain";    // subdomain mode, user using our service in subdomains

    public static String MODE = MODE_STANDALONE;


    public static final long STANDALONE_ACCOUNT_ID = 1L;

    /**
     * // No matter it's standalone or not, the first account would be the
     *     // system managemnet account and it is hardcoded to 1.
     */
    public static final long SYSMANAGE_ACCOUNTID = DEFAULT_INTERNAL_ACCOUNT_ID;



    public static boolean usingSubdomains() {
        return MODE_SUBDOMAIN.equalsIgnoreCase(MODE);
    }
    public static boolean isStandalone() {
        return Constants.MODE_STANDALONE.equalsIgnoreCase(MODE);
    }
    public static boolean runInDockerModel() {
        return Constants.SYSTEM_RUN_MODEL_DOCKER.equals(EnvUtil.getEnvOrProp(Constants.SYSTEM_RUN_MODEL_KEY, ""));
    }
}
