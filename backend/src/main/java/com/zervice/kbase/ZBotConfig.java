package com.zervice.kbase;

import lombok.experimental.UtilityClass;

/**
 * Manage all configuration items and default values used in system
 */
@UtilityClass
public class ZBotConfig {
    public final String DEFAULT_CONFIG_FILE = "/usr/local/zervice/kb/config/application.properties";

    /**
     * Sub config objects
     */
    public final String CONFIG_OBJECT_LOGGER = "logger";
    public final String CONFIG_OBJECT_DATABASE = "database";
    public final String CONFIG_OBJECT_EVENTBUS = "eventbus";
    public final String CONFIG_OBJECT_EMAIL = "email";

    /**
     * Logger configurations
     */

    /**
     * Database configurations
     */
    public final String DATABASE_DATASOURCE_TYPE = "type";
    public final String DATABASE_DATASOURCE_TYPE_DEFAULT = "org.apache.tomcat.jdbc.pool.DataSource";
    public final String DATABASE_DRIVER = "driver";
    // com.mysql.cj.jdbc.Driver
    // org.mariadb.jdbc.Driver
    public final String DATABASE_DRIVER_DEFAULT = "com.mysql.cj.jdbc.Driver";
    public final String DATABASE_URL = "url";
    public final String DATABASE_URL_DFEAULT = "jdbc:mysql://mysql:3306";
    public final String DATABASE_USERNAME = "username";
    public final String DATABASE_USERNAME_DEFAULT = "root";
    public final String DATABASE_PASSWORD = "password";
    public final String DATABASE_PASSWORD_DEFAULT = "changeit";

    public final String DATABASE_INIT_DB = "initDb";
    public final boolean DATABAES_INIT_DB_DEFAULT = false;
    public final String DATABASE_MIGRATE_DB = "migrateDb";
    public final boolean DATABAES_MIGRATE_DB_DEFAULT = true;
    public final String DATABASE_INIT_TESTDATA = "initTestDataSet";
    public final boolean DATABAES_INIT_TESTDATA_DEFAULT = false;
    public final String DATABASE_BACKUP_ENABLED = "backup";
    public final boolean DATABASE_BACKUP_ENABLED_DEFAULT = true;
    public final String DATABASE_BACKUP_DIR = "backupDir";
    public final String DATABASE_BACKUP_DIR_DEFAULT = "/usr/local/kb/app/backup";


    /**
     * EventBus
     */
    public final String EVENTBUS_THREAD_NUM = "threadNum";
    public final int EVENTBUS_THREAD_NUM_DEFAULT = 1;

    /**
     * Email
     */
    public final String EMAIL_SENDER_ADDRESS = "senderAddress";
    public final String EMAIL_SENDER_ADDRESS_DEFAULT = "notify@promptai.cn";

    public final String EMAIL_SENDER_DISPLAY = "senderDisplay";
    public final String EMAIL_SENDER_DISPLAY_DEFAULT = "Promptai Sys";

    public final String EMAIL_SENDER_TYPE = "protocol";
    public final String EMAIL_SENDER_TYPE_DEFAULT = "smtp";

    public final String EMAIL_SMTP_AUTH_ENABLED = "smtpAuth";
    public final boolean EMAIL_SMTP_AUTH_ENABLED_DEFAULT = false;

    public final String EMAIL_SMTP_AUTH_USER = "smtpUser";
    public final String EMAIL_SMTP_AUTH_USER_DEFAULT = "";

    public final String EMAIL_SMTP_AUTH_PASS = "smtpPass";
    public final String EMAIL_SMTP_AUTH_PASS_DEFAULT = "";

    public final String EMAIL_SMTP_HOST = "smtpHost";
    public final String EMAIL_SMTP_HOST_DEFAULT = "smtp.zoho.com.cn";

    public final String EMAIL_SMTP_PORT = "smtpPort";
    public final int EMAIL_SMTP_PORT_DEFAULT = 25;

    public final String EMAIL_SMTP_TLS_ENABLED = "smtpUseTLS";
    public final boolean EMAIL_SMTP_TLS_ENABLED_DEFAULT = false;

    public final String EMAIL_DEBUG_ENABLED = "debugEnabled";
    public final boolean EMAIL_DEBUG_ENABLED_DEFAULT = false;

    public final String EMAIL_CHECK_IDLE_INTERVAL = "emailPollingInterval";
    public final String EMAIL_CHECK_IDLE_INTERVAL_DEFAULT = "5m";
    /****
     * Testing DATA
     */
    /**
     * Id and name of the testing account
     */
    public static final long TEST_ACCOUNT_ID = 1;
    public static final String TEST_ACCOUNT_NAME = "testacct";

    public static final long TEST_COLLECTOR_ID = 1;
    public static final String TEST_COLLECTOR_ACCESS_KEY = "123456";

    public static final String TEST_USER_NAME = "test@promptai.us";
    public static final String TEST_USER_PASSWORD = "123456";
    public static final String TEST_USER_APIKEY = "123456";
    public static final String TEST_TIMEZONE = "America/Los_Angeles";
    public static final String TEST_ADMIN = "admin";

    /**
     * jwt
     * secret
     * 签发者
     */
    public static final String JWT_SECRET = "EA0vfvy_talk2bits_jws_QEFAASCAT0wggE5Ag";
    public static final String JWT_PAYLOAD_ISS = "talk2bits.com";

    public static final String KEY_FILE_STORE_PATH = "file.store.path";
    public static final String DEFAULT_FILE_STORE_PATH = "/usr/local/kb/app/data";

    public static final String KEY_RSA_KEY = "ras.private_key";
    public static final String DEFAULT_RSA_KEY =
            "MIIBUwIBADANBgkqhkiG9w0BAQEFAASCAT0wggE5AgEAAkEA0vfvyTdGJkdbHkB8mp0f3FE0GYP3AYPaJF7jUd1M0XxFSE2ceK3k2kw20YvQ09NJKk+OMjWQl9WitG9" +
                    "pB6tSCQIDAQABAkA2SimBrWC2/wvauBuYqjCFwLvYiRYqZKThUS3MZlebXJiLB+Ue/gUifAAKIg1avttUZsHBHrop4qfJCwAI0+YRAiEA+W3NK/RaXtnRqm" +
                    "oUUkb59zsZUBLpvZgQPfj1MhyHDz0CIQDYhsAhPJ3mgS64NbUZmGWuuNKp5coY2GIj/zYDMJp6vQIgUueLFXv/eZ1ekgz2Oi67MNCk5jeTF2BurZqNLR3MS" +
                    "mUCIFT3Q6uHMtsB9Eha4u7hS31tj1UWE+D+ADzp59MGnoftAiBeHT7gDMuqeJHPL4b+kC+gzV4FGTfhR9q3tTbklZkD2A==";
}

