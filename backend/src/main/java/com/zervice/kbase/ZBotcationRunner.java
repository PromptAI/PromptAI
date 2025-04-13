package com.zervice.kbase;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.serializer.ToStringSerializer;
import com.zervice.common.utils.IdGenerator;
import com.zervice.common.utils.LayeredConf;
import com.zervice.kbase.api.rpc.helper.ChatHelper;
import com.zervice.kbase.database.helpers.DbInitializer;
import com.zervice.kbase.database.utils.DaoUtils;
import com.zervice.kbase.email.EmailService;
import com.zervice.kbase.eventbus.EventBusService;
import com.zervice.kbase.service.AgentClientService;
import com.zervice.kbase.service.PublishSnapshotService;
import lombok.extern.log4j.Log4j2;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;

@Component
@Log4j2
public class ZBotcationRunner implements ApplicationRunner {

    @Autowired
    private PublishSnapshotService snapshotService;
    @Override
    public void run(ApplicationArguments args) throws Exception {
        LOG.info("Initializing ...");

        IdGenerator.init(LayeredConf.getInt("nodeId", 0));

        try {
            DaoUtils.waitForReady(10 * 1000);
        }
        catch(Exception cnfe) {
            LOG.error("Got exception when waiting DB ready. Check DB status and driver used are correct!");
            throw new IllegalStateException("Cannot load MySQL driver", cnfe);
        }
        if (Environment.isMaster()) {
            LOG.info("Im master, let's init db");
            _initDb();
        } else {
            LOG.info("Im not master not init db (including upgrade database....)");
        }


        //AccountCatalog should be the first in order that other server may need the company name.
        LOG.info("Initialize account catalog ...");
        AccountCatalog.initialize();
        LOG.info("Done");

        LOG.info("Initializing eventbus ...");
        EventBusService.init(LayeredConf.getConfig(ZBotConfig.CONFIG_OBJECT_EVENTBUS).getInt(ZBotConfig.EVENTBUS_THREAD_NUM, ZBotConfig.EVENTBUS_THREAD_NUM_DEFAULT));
        LOG.info("Done");

        // start NoReplyEmail
        LOG.info("Start EmailService ...");
        EmailService.getInstance().start();
        LOG.info("Done");
        //set fast json SerializeConfig
        SerializeConfig.getGlobalInstance().put(Long.TYPE, ToStringSerializer.instance);
        SerializeConfig.getGlobalInstance().put(Long.class, ToStringSerializer.instance);
        // forbidden $ref
        JSON.DEFAULT_GENERATE_FEATURE |= SerializerFeature.DisableCircularReferenceDetect.getMask();

        snapshotService.init();
        AgentClientService.getInstance().init();
        ChatHelper.init();

        Application.isReady = true;
        LOG.info("System is ready. I'm {} ...", Environment.isMaster() ? "MASTER" : "SLAVE");
    }

    @Bean(name="kbdatasource")
    public DataSource getDataSource() {
        LayeredConf.Config config = LayeredConf.getConfig(ZBotConfig.CONFIG_OBJECT_DATABASE);

        String type = config.getString(ZBotConfig.DATABASE_DATASOURCE_TYPE, ZBotConfig.DATABASE_DATASOURCE_TYPE_DEFAULT);
        Class typeClass = org.apache.tomcat.jdbc.pool.DataSource.class;
        if(!StringUtils.isEmpty(type)) {
            try {
                LOG.info("Try load type class - " + type);
                typeClass = Class.forName(type);
            } catch (ClassNotFoundException e) {
                LOG.warn("Cannot load type class for database, use default");
            }
        }

        PoolProperties dsPoolProps = new PoolProperties();

        dsPoolProps.setUrl(config.getString(ZBotConfig.DATABASE_URL, ZBotConfig.DATABASE_URL_DFEAULT));
        dsPoolProps.setUsername(config.getString(ZBotConfig.DATABASE_USERNAME, ZBotConfig.DATABASE_USERNAME_DEFAULT));
        dsPoolProps.setPassword(config.getString(ZBotConfig.DATABASE_PASSWORD, ZBotConfig.DATABASE_PASSWORD_DEFAULT));
        dsPoolProps.setMaxActive(config.getInt("maxActive", 100));

        dsPoolProps.setDriverClassName(config.getString(ZBotConfig.DATABASE_DRIVER, ZBotConfig.DATABASE_DRIVER_DEFAULT));
        dsPoolProps.setJmxEnabled(false);
        LOG.info("Using jdbc driver class: {}", dsPoolProps.getDriverClassName());
        dsPoolProps.setTestWhileIdle(false);
        dsPoolProps.setTestOnBorrow(true);
        dsPoolProps.setValidationQuery("SELECT 1");
        dsPoolProps.setTestOnReturn(false);
        // in mills
        dsPoolProps.setValidationInterval(config.getInt("validationInterval", 30000));

        // in mills
        dsPoolProps.setTimeBetweenEvictionRunsMillis(config.getInt("timeBetweenEvictionRunsMillis", 30000));
        dsPoolProps.setInitialSize(10);

        // in mills
        dsPoolProps.setMaxWait(config.getInt("maxWait", 10000));

        dsPoolProps.setLogAbandoned(true);
        dsPoolProps.setRemoveAbandoned(true);
        // in seconds
        dsPoolProps.setRemoveAbandonedTimeout(config.getInt("removeAbandonedTimeout", 60));

        dsPoolProps.setJdbcInterceptors(config.getString("jdbcInterceptors", "ResetAbandonedTimer"));

        // in mills
        dsPoolProps.setMinEvictableIdleTimeMillis(config.getInt("minEvictableIdleTimeMillis", 30000));
        dsPoolProps.setMinIdle(10);



        LOG.info("Creating datasource with type " + type);
        return new org.apache.tomcat.jdbc.pool.DataSource(dsPoolProps);
    }

    /**
     * Initialize the database. This is controlled by configurations:
     *   - initDb - indicates if we create the controldb
     *   - initTestDataSet - indicates if we create a sample account with default space, index, and
     *                       collector records. If this flag is enabled, all existing data will be
     *                       deleted.
     *   - migrateDb - migrate database to latest version
     */
    private static void _initDb() throws Exception {
        LayeredConf.Config config = LayeredConf.getInstance().getConfig(ZBotConfig.CONFIG_OBJECT_DATABASE);

        // try to create the controldb if initDb is set.
        if (config.getBoolean(ZBotConfig.DATABASE_INIT_DB, ZBotConfig.DATABAES_INIT_DB_DEFAULT)) {
            LOG.info("Initializing database ...");
            DbInitializer.createCoreDbIfNotExist();
        }
        else {
            LOG.info("Not initialize CoreDB ...");
        }

        // try to initiate test dataset
        if (config.getBoolean(ZBotConfig.DATABASE_INIT_TESTDATA, ZBotConfig.DATABAES_INIT_TESTDATA_DEFAULT)) {
            LOG.info("Initializing testing data set ...");
            DbInitializer.initTestingEnv();
        }
        else {
            LOG.info("Not initialize testing data set ...");
        }

        if (config.getBoolean(ZBotConfig.DATABASE_MIGRATE_DB, ZBotConfig.DATABAES_MIGRATE_DB_DEFAULT)) {
            LOG.info("Migrating database ...");
            DbInitializer.migrateDb();
        }
        else {
            LOG.info("Not migrating database ...");
        }

        DbInitializer.setupAuditing();
    }
}

