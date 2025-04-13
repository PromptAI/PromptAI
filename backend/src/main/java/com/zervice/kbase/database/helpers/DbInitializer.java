package com.zervice.kbase.database.helpers;

import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Strings;
import com.zervice.common.pojo.common.Account;
import com.zervice.common.restful.exception.RestException;
import com.zervice.common.restful.exception.StatusCodes;
import com.zervice.common.utils.Constants;
import com.zervice.common.utils.LayeredConf;
import com.zervice.kbase.Environment;
import com.zervice.kbase.ZBotConfig;
import com.zervice.kbase.ZBotRuntime;
import com.zervice.kbase.database.DBException;
import com.zervice.kbase.database.DatabaseSchemaVersion;
import com.zervice.kbase.database.DependencySorter;
import com.zervice.kbase.database.dao.*;
import com.zervice.kbase.database.pojo.DatabaseUpgrade;
import com.zervice.kbase.database.pojo.RbacRole;
import com.zervice.kbase.database.pojo.User;
import com.zervice.kbase.database.utils.DaoUtils;
import com.zervice.kbase.rbac.RBACConstants;
import com.zervice.kbase.utils.CommandRunner;
import com.zervice.kbase.utils.ReflectionUtils;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.tomcat.util.http.fileupload.FileUtils;

import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.zervice.kbase.database.utils.DaoUtils.getConnection;

/**
 * DbInitializer provides following functionalities
 * 1. create the coredb if it doesn't exist
 * 2. create the account db
 * 3. migrate the schema to the latest
 * 4. bring the db to the testing environment
 */
@Log4j2
public class DbInitializer {
    static final String _DAO_PACKAGE_NAME = "com.zervice.kbase.database.dao";
    static final String _UPGRADE_PACKAGE_NAME = "com.zervice.kbase.database.upgrade";
    private static final Pattern DB_URL_PATTERN = Pattern.compile(".*//(?<host>[^:?]*)(?::(?<port>[1-9][0-9]*))?.*");

    private static ExecutorService _backupExecutor = null;


    /**
     * Creates the core db if it doesn't exist.
     * And it will add upgrade list of current version to upgrade history
     *
     * @throws DBException
     */
    public static void createCoreDbIfNotExist() throws DBException {
        Connection conn = null;
        try {
            conn = getConnection(false);
            if (Environment.isH2()) {
                // always need to create
                _createCoreDb(conn);
            } else {
                if (!DaoUtils.dbExists(conn, Constants.COREDB)) {
                    LOG.info("Creating non-existing core db ...");
                    _createCoreDb(conn);
                    conn.commit();
                    LOG.info("Core DB created!");
                }
                else {
                    LOG.info("Core DB already exists. Not initializing");
                }
            }
        } catch (SQLException e) {
            DaoUtils.rollbackQuietly(conn);
            LOG.fatal("Encounter database error", e);
            throw new DBException(e.getMessage(), e);
        } finally {
            DaoUtils.closeQuietly(conn);
        }
    }


    static void _createCoreDb(final Connection conn) throws DBException {
        try {
            LOG.info("Create core database ...");
            DaoUtils.createDb(conn, Constants.COREDB);


            List<Class> daoClasses = _getOrderedDaoClasses(CoreDbTable.class);
            LOG.info("Try creating #{} tables for core DB ...", daoClasses.size());

            for (Class clz : daoClasses) {
                LOG.info("Create table " + clz.getName());
                _createTable(conn, clz, Constants.COREDB);
            }

            LOG.info("Record DB version - " + DatabaseSchemaVersion.VER);
            ConfigurationDao.add(conn, Constants.COREDB, ConfigurationDao.DB_VER, DatabaseSchemaVersion.VER);
            ConfigurationDao.add(conn, Constants.COREDB, ConfigurationDao.PARSER_VER, 1);

            //add current db upgrade to upgrade history
            List<DbUpgrade> upgradeList = _getDbUpgradeListByVersion(DatabaseSchemaVersion.VER);
            for (DbUpgrade upgrade : upgradeList) {
                LOG.info("Add upgrade history with version={} and ticket={}", upgrade.ver(), upgrade.ticket());

                addDbUpgradeHistory(conn, Constants.COREDB, upgrade.ver(), upgrade.ticket());
            }
        } catch (SQLException ex) {
            LOG.error("Unable to create core db", ex);
            throw new DBException(ex.getMessage(), ex);
        } catch (IOException | ClassNotFoundException | NoSuchMethodException |
                IllegalAccessException | InvocationTargetException ex) {
            LOG.error("Encounter non-expected exceptions when creating core db", ex);
        }
    }


    /*
     * get ordered dao classes by annotation.
     * The ordered will be sorted by annotation after
     */
    static List<Class> _getOrderedDaoClasses(Class annotation) throws IOException, ClassNotFoundException {
        Class[] classes = ReflectionUtils.getClasses(_DAO_PACKAGE_NAME);

        LOG.info("Try find classes with given annotation {}", annotation.getName());
        ArrayList<Class> result = new ArrayList<>();
        HashMap<String, Class> classMap = new HashMap<>();

        List<Pair<String, String[]>> dependencies = new ArrayList<>(classes.length);

        for (Class cls : classes) {
            if (!cls.getSimpleName().endsWith("Dao")) {
                LOG.info("Ignoring none DAO class - " + cls.getSimpleName());
                continue;
            }

            Annotation a = cls.getAnnotation(annotation);
            if (a == null) {
                LOG.info("Ignoring none annotated class - " + cls.getSimpleName());
                continue;
            }

            String dependencyTable = _getAfter(a);
            if (!Strings.isNullOrEmpty(dependencyTable)) {
                String[] dependenciesArray = dependencyTable.split(",");
                dependencies.add(new ImmutablePair<>(cls.getSimpleName(), dependenciesArray));
            } else {
                Pair<String, String[]> dependency = new ImmutablePair<>(cls.getSimpleName(), new String[]{});
                dependencies.add(dependency);
            }

            LOG.info("Adding annotated class - " + cls.getSimpleName());
            classMap.put(cls.getSimpleName(), cls);
        }

        //call dependencySorter to sort class by using after annotation
        DependencySorter dependencySorter = new DependencySorter(dependencies);
        List<String> sortedClassNameList = dependencySorter.sort();
        for (String simpleClassName : sortedClassNameList) {
            result.add(classMap.get(simpleClassName));
        }
        return result;
    }


    static String _getAfter(Annotation a) {
        if (a instanceof CoreDbTable) {
            return ((CoreDbTable) a).after();
        } else if (a instanceof CustomerDbTable) {
            return ((CustomerDbTable) a).after();
        }

        throw new IllegalArgumentException("Unknown annotation " + a.getClass().getName());
    }


    static void _createTable(Connection conn, Class daoClz, String dbName)
            throws SQLException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        LOG.info("Try creating a new table - " + daoClz.getName());
        Method method = daoClz.getMethod("createTable", Connection.class, String.class);
        method.invoke(null, new Object[]{conn, dbName});
    }


    /**
     * Sometimes, we like to remove all existing data and bring the system
     * to a well-defined testing status so we can run test cases deterministically
     *
     * @throws DBException
     */
    public static void initTestingEnv() throws DBException {
        initTestingEnv("test", new JSONObject());
    }

    public static Account initTestingEnv(String accountName, JSONObject properties) throws DBException {
        Connection conn = null;
        try {
            LOG.info("Creating testing data set");
            conn = getConnection(false);

            // drop all account db and clean accounts table
            LOG.info("Drop all account databases");
            _dropAllAccountDBs(conn);

            LOG.info("Clean up all account records");
            AccountDao.removeAll(conn);

            // clean coredb.configurations and accounts table
            LOG.info("Clean up configuration table");
            ConfigurationDao.removeAll(conn, Constants.COREDB);

            LOG.info("Set DB version to - " + DatabaseSchemaVersion.VER);
            ConfigurationDao.add(conn, Constants.COREDB, ConfigurationDao.DB_VER, DatabaseSchemaVersion.VER);
            ConfigurationDao.add(conn, Constants.COREDB, ConfigurationDao.PARSER_VER, 1);

            // generate an account
            LOG.info("Creating testing account record ...");

            Account account = _createTestingAccount(conn, ZBotRuntime.DEFAULT_INTERNAL_ACCOUNT_ID, accountName, properties);

            conn.commit();

            LOG.info("Done creating testing data");
            return account;
        } catch (Exception e) {
            LOG.fatal("Encounters exception to create testing account!", e);
            DaoUtils.rollbackQuietly(conn);
            throw new IllegalStateException(e);
        } finally {
            DaoUtils.closeQuietly(conn);
        }
    }

    private static void _dropAllAccountDBs(Connection conn) throws SQLException {
        List<Account> accounts = AccountDao.getAll(conn);
        for (Account account : accounts) {
            DaoUtils.dropDb(conn, account.getAccountDbName());
        }
    }


    public static Account _createTestingAccount(@NonNull Connection conn,
                                                long accountId,
                                                String accountName,
                                                JSONObject properties) throws SQLException {
        LayeredConf.Config config = new LayeredConf.Config(properties);

        JSONObject props = new JSONObject();
        props.put("user.name", config.getString("username", "admin"));
        props.put("user.password", config.getString("password", "admin"));
        props.put("timezone", config.getString("timezone", "UTC"));
        props.put("admin", config.getString("admin", "admin"));
        props.put("user.email", config.getString("email", "admin@promptai.cn"));
        props.put("user.mobile", config.getString("mobile"));

        String testCreator = props.getString("user.email");

        String dbName = Account.getAccountDbName(accountId);

        Account account = AccountCore.createAccountPhase1(conn, accountId, accountName, dbName, testCreator , props);

        AccountCore.createAccountPhase2(conn, account.getId());
        String username = "System Admin";
        String password = "admin";
        String email = testCreator;
        String mobile = "";
        User.UserProp prop = User.UserProp.empty();

        // for testing account, we simply create the user ...
        User adminUser = User.factory(username, password, email, mobile, prop);

        long userId = UserDao.addReturnId(conn, dbName, adminUser);

        // also the roles as admin
        RbacRole rr = RbacRoleDao.get(conn, account.getAccountDbName(), RBACConstants.SYS_ADMIN);
        UserRbacRoleDao.add(conn, dbName, userId, rr.getId(), new JSONObject());

        return account;
    }


    /**
     * Create the db for a new account. This method just creates the schema by calling
     * various Dao's createTable method.
     * <p>
     * The caller is responsible for inserting more records to get the system ready
     *
     * @throws DBException
     */
    public static void createAccountDb(final Connection conn, long acctId, String dbName) throws DBException {

        LOG.info("Create database for new account with id={} and name={}", acctId, dbName);

        try {
            DaoUtils.createDb(conn, dbName);

            List<Class> daoClasses = _getOrderedDaoClasses(CustomerDbTable.class);
            for (Class clz : daoClasses) {
               LOG.info( clz.getName());
                _createTable(conn, clz, dbName);
            }
        } catch (SQLException ex) {
            LOG.error("Unable to create new account", ex);
            throw new RestException(StatusCodes.InternalError);
        } catch (IOException | ClassNotFoundException | NoSuchMethodException |
                IllegalAccessException | InvocationTargetException ex) {
            LOG.error("Unexpected exception caught to create new account", ex);
            throw new RestException(StatusCodes.InternalError);
        }
    }

    public static void addDbUpgradeHistory(Connection conn, String coreDb, int ver, String ticket) throws SQLException {
        DatabaseUpgrade dbUpgrade = new DatabaseUpgrade();
        dbUpgrade.setVer(ver);
        if (ticket.indexOf('-') > 0) {
            dbUpgrade.setTicket(ticket);
        } else {
            // append a '-' so we can use 'LIKE' syntax in DAO to find out a ticket
            dbUpgrade.setTicket(ticket + '-');
        }
        dbUpgrade.setExecutedEpochMs(System.currentTimeMillis());
        DatabaseUpgradeHistoryDao.add(conn, coreDb, dbUpgrade);
    }

    public static String getDbAccessParams(Connection conn) throws SQLException {
        LayeredConf.Config config = LayeredConf.getConfig(ZBotConfig.CONFIG_OBJECT_DATABASE);
        String dbHost = "localhost";
        String dbPort = "3306";
        String dbUser = config.getString("username", "root");
        ;
        String dbPass;

        StringBuilder sb = new StringBuilder();
        // get mysql server's hostname and port
        String svrUrl = conn.getMetaData().getURL();
        if (!Strings.isNullOrEmpty(svrUrl)) {
            Matcher m = DB_URL_PATTERN.matcher(svrUrl);
            if (m.matches() && m.groupCount() == 2) {
                dbHost = m.group("host");
                if (m.group("port") != null) {
                    dbPort = m.group("port");
                }
            }
        }
        sb.append(" --host=").append(dbHost).append(" ");
        sb.append(" --port=").append(dbPort).append(" ");
        sb.append(" --user=");

        //username format is 'user@host'
        String username = conn.getMetaData().getUserName();
        if (!Strings.isNullOrEmpty(username)) {
            dbUser = username.split("@")[0];
        }
        sb.append(dbUser).append(" ");

        dbPass = config.getString("password", "changeit");
        sb.append(" --password=").append(dbPass).append(" ");

        String params = sb.toString();
        LOG.info("Database access parameters - " + params.replaceAll("password=[^ ]*", "password=******"));

        return params;
    }

    // merge all src files into target file
    private static void _mergeFiles(String targetFile, Collection<String> srcFiles) throws DBException {
        // open target file for write
        BufferedWriter out;
        try {
            FileWriter fileWriter = new FileWriter(targetFile, false); // overwite mode
            out = new BufferedWriter(fileWriter);
        } catch (IOException e) {
            throw new DBException("Can not open backup file: " + targetFile, e);
        }

        // append src files into target file
        for (String srcFile : srcFiles) {
            try {
                LOG.info("Try merging sourcefile={} to target file", srcFile);

                BufferedReader in = new BufferedReader(new FileReader(srcFile));

                String line = "";
                do {
                    out.write(line + "\n");
                    line = in.readLine();
                } while (line != null);

                out.flush();
                in.close();
            } catch (IOException e) {
                throw new DBException("Can not merge backup file: " + String.format("targetFile=%s, srcFile=%s", targetFile, srcFile), e);
            }
        }

        // close file
        try {
            out.close();
        } catch (IOException e) {
            throw new DBException("Can close backup file: " + targetFile, e);
        }
    }

    // merge all data to the first file
    private static void _mergeAllBackupFiles(final String tmpFolder, final String mergeFilename, final Collection<String> dbs) throws DBException {
        String targetFile = tmpFolder + File.separator + "dbbackup.sql";
        LOG.info("Try merge backup files {} ...", targetFile);

        ArrayList<String> srcFiles = new ArrayList<String>();
        for (String db : dbs) {
            srcFiles.add(tmpFolder + File.separator + db);
        }
        _mergeFiles(targetFile, srcFiles);

        String cmd = "gzip " + targetFile + "; mv " + targetFile + ".gz " + mergeFilename;
        CommandRunner.exec(cmd);

        LOG.info("All backup files merged");
    }

    public static void mkdirIfNotExist(String dirPath) {
        File backFolderFile = new File(dirPath);
        if (!backFolderFile.exists()) {
            if (!backFolderFile.mkdir()) {
                LOG.error("Cannot create required directory - ", dirPath);
                throw new DBException(String.format("Cannot create directory %s", dirPath));
            }
        }
    }

    /**
     * Helper, to easy rename database in DB upgrade
     * @param conn
     * @param oldName
     *
     * @return the file containing the backup data
     */
    public static void renameDatabase(Connection conn, String oldName, String newName) throws Exception {
        long epoch = System.currentTimeMillis();
        LocalDateTime now = LocalDateTime.now();

        LayeredConf.Config config = LayeredConf.getConfig(ZBotConfig.CONFIG_OBJECT_DATABASE);
        String tmpFolder = System.getProperty("java.io.tmpdir") + File.separator + "zpdb_backup_tmp_" + epoch;
        String backupFile = tmpFolder + "/dbbackup." + oldName + "." + now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")) + ".sql";

        LOG.info("Creating temporary backup folder - " + tmpFolder);
        mkdirIfNotExist(tmpFolder);

        String basedir = LayeredConf.getConfig("mysql").getString("basedir", "");

        if(!StringUtils.isEmpty(basedir)) {
            if(basedir.endsWith("/")) {
                basedir += "bin/";
            }
            else {
                basedir += "/bin/";
            }
        }

        String accessParams = getDbAccessParams(conn);
        try {
            CommandRunner.exec(String.format("%smysqldump %s --single-transaction %s > %s", basedir, accessParams, oldName, backupFile));
            CommandRunner.exec(String.format("%smysqladmin %s create %s", basedir, accessParams, newName));
            CommandRunner.exec(String.format("%smysql %s %s < %s", basedir, accessParams, newName, backupFile));

            // Fixme? some how drop will hang ... This DB is still in use? So it would be hang ...
            // CommandRunner.exec(String.format("%smysqladmin -f %s drop %s", basedir, accessParams, oldName));
        } finally {
            try {
                FileUtils.deleteDirectory(new File(tmpFolder));
            } catch (IOException e) {
                LOG.warn("Cannnot remove created temporary folder - " + tmpFolder, e);
            }
        }
    }

    public static String backupDbs(Connection conn, List<String> dbs, int ver)
            throws DBException, SQLException, ExecutionException, InterruptedException {
        long epoch = System.currentTimeMillis();
        LocalDateTime now = LocalDateTime.now();

        LayeredConf.Config config = LayeredConf.getConfig(ZBotConfig.CONFIG_OBJECT_DATABASE);
        String tmpFolder = System.getProperty("java.io.tmpdir") + File.separator + "zbotdb_backup_" + ver + "_" + epoch;
        String backupFolder = config.getString(ZBotConfig.DATABASE_BACKUP_DIR, ZBotConfig.DATABASE_BACKUP_DIR_DEFAULT);
        String backupFile = backupFolder + "/dbbackup." + ver + "_" + now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")) + ".sql.gz";

        mkdirIfNotExist(tmpFolder);
        LOG.info("Creating temporary backup folder - " + tmpFolder);

        String accessParams = getDbAccessParams(conn);
        try {

            _backupExecutor = Executors.newFixedThreadPool(20);

            ArrayList<Future> futures = new ArrayList<>();

            for (String db : dbs) {
                BackupTask task = new BackupTask(accessParams, db, tmpFolder, true);
                futures.add(_backupExecutor.submit(task));
            }

            for (Future f : futures) {
                f.get();
            }

            mkdirIfNotExist(backupFolder);

            _mergeAllBackupFiles(tmpFolder, backupFile, dbs);
        } finally {
            if (_backupExecutor != null) {
                _backupExecutor.shutdown();
            }
            try {
                FileUtils.deleteDirectory(new File(tmpFolder));
            } catch (IOException e) {
                LOG.warn("Cannnot remove created temporary folder - " + tmpFolder, e);
            }
        }

        return backupFile;
    }

    public static void restoreDbs(Connection conn, String backupFile) throws Exception {
        LOG.info("Starting restoring database ...");
        String accessParams = getDbAccessParams(conn);

        final String tmpName = System.getProperty("java.io.tmp") + File.separator + String.format("restoreDB_%d.sql", System.currentTimeMillis());

        CommandRunner.exec(String.format("gzip -dc %s > %s", backupFile, tmpName));
        CommandRunner.exec(String.format("mysql %s < %s", accessParams, tmpName));
        CommandRunner.exec(String.format("rm %s", tmpName));

        LOG.info("Database restored");
    }

    private static Auditable _DEFAULT_AUDIT = new Auditable() {
        @Override
        public Class<? extends Annotation> annotationType() {
            return Auditable.class;
        }

        @Override
        public boolean disabled() {
            return false;
        }
        @Override
        public String key() {
            return "id";
        }
        @Override
        public boolean read() {
            return false;
        }
        @Override
        public boolean create() {
            return true;
        }
        @Override
        public boolean update() {
            return true;
        }
        @Override
        public boolean delete() {
            return true;
        }
    };

    public static void setupAuditing() throws Exception {
        Class[] classes = ReflectionUtils.getClasses(_DAO_PACKAGE_NAME);

        for (Class cls : classes) {
            if (!cls.getSimpleName().endsWith("Dao")) {
                LOG.info("Ignoring none DAO class - " + cls.getSimpleName());
                continue;
            }

            Auditable a = (Auditable) cls.getAnnotation(Auditable.class);
            if(a == null) {
                DaoUtils.audit(cls, _DEFAULT_AUDIT);
            }
            else {
                DaoUtils.audit(cls, a);
            }
        }
    }

    /**
     * Migrate the database to the latest version.
     * <p>
     * The indicator of the successful upgrade is (and only is) all databases (
     * include the core db and all accounts db) are successfully migrated.
     * <p>
     * The caller of this method should be responsible for backup and restore the
     * entire DBs before and after the upgrade.
     * <p>
     * backup needs following software to complete backup process:
     * mysqldump
     * gzip
     * <p>
     * you can disable backup by config upgrade.backup.enable, default is true
     * you can set backup folder by config upgrade.backup.folder, default is /usr/local/zervice/kb/data
     * you can set mysqldump base dir by config mysql.basedir,
     * if this is setting, it will use basedir/bin/mysqldump, if not, it will use mysqldump directly in command
     * And we will use /tmp directoy to store tmp files when backup
     *
     * @throws DBException
     */
    public static void migrateDb() throws DBException {
        Connection conn = null;
        try {
            LOG.info("Try starting DB migration. Latest version - " + DatabaseSchemaVersion.VER);
            conn = getConnection(false);


            // TODO: fixme in next version!!!!
            // As we are trying to rename coredb to kbcoredb, when starts, it might be able to find kbcoredb
            // we need then to try fallback to coredb as it has not been upgraded yet!!!!
            String coreDb = Constants.COREDB;
            if (!DaoUtils.tableExists(conn,  coreDb , DatabaseUpgradeHistoryDao.TABLE_NAME)) {
                LOG.info("Creating non-existing upgrade history table ...");
                DatabaseUpgradeHistoryDao.createTable(conn, coreDb);
                conn.commit();
            }

            // get curVer of current data
            // get targetVer of latest schema
            // get all db upgrade classes
            // foreach ver = curVer...targetVer do
            //    get a list of db upgrade classes with ver
            //    put non-executed upgradess into a nonExecutedSet
            //    if nonExecutedSet isn't empty
            //        build the ordered list
            //        execute upgrade one by one
            //
            int curVer = ConfigurationDao.getInt(conn, /* TODO: fixme */ coreDb /*Constants.COREDB*/, ConfigurationDao.DB_VER, 0);
            int targetVer = DatabaseSchemaVersion.VER;
            LOG.info("Current db version {} and target db version {}", curVer, targetVer);
            List<Class> upgrades = _getAllPotentialDbUpgrades(conn, coreDb, curVer);
            if (upgrades.isEmpty()) {
                LOG.info("No DB upgrade tasks to execute for version " + curVer);

                if (targetVer != curVer) {
                    ConfigurationDao.update(conn, Constants.COREDB, ConfigurationDao.DB_VER, targetVer);
                    conn.commit();
                }

                return;
            }

            LOG.info("Execute DB upgrading tasks (curVer={}, targetVer={})", curVer, targetVer);

            List<Account> accountList = AccountDao.getAll(conn, coreDb);

            LayeredConf.Config config = LayeredConf.getConfig(ZBotConfig.CONFIG_OBJECT_DATABASE);
            if (config.getBoolean(ZBotConfig.DATABASE_BACKUP_ENABLED, ZBotConfig.DATABASE_BACKUP_ENABLED_DEFAULT)) {
                List<String> dbsNeedToBackup = new ArrayList<>(accountList.size() + 1);
                dbsNeedToBackup.add(/* TODO: fixme */ coreDb /*Constants.COREDB*/);

                // Also those customer databases
                // FIXME: un-comment below
                 accountList.forEach(account -> dbsNeedToBackup.add(Account.getAccountDbName(account.getId())));

                LOG.info("Try backing up databases for migrating ...");
                String backupFile = backupDbs(conn, dbsNeedToBackup, curVer);
                LOG.info("Successfully backed up database - " + backupFile);
            } else {
                LOG.warn("Not backing up database before migrating ...");
            }

            //
            // We start to upgrade from next version
            for (int ver = curVer; ver <= targetVer; ver++) {
                LOG.info("Try upgrading to version " + ver);
                List<String> ticketList = _upgradeToVer(conn, coreDb, accountList, ver, upgrades);
                ;

                for (String ticket : ticketList) {
                    LOG.info("Upgrading to version {} done for ticket {}!", ver, ticket);
                    addDbUpgradeHistory(conn, coreDb, ver, ticket);
                }
            }

            if (targetVer != curVer) {
                LOG.info("Update DB version after migration ...");
                ConfigurationDao.update(conn, /* TODO: fixme */ coreDb /*Constants.COREDB*/, ConfigurationDao.DB_VER, targetVer);
            }

            conn.commit();
            LOG.info("Db upgrading successfully done");
        } catch (Exception e) {
            LOG.error("Error upgrading", e);

            DaoUtils.rollbackQuietly(conn);
            throw new DBException(e.getMessage(), e);
        } finally {
            DaoUtils.closeQuietly(conn);
        }
    }

    static List<Class> _getAllPotentialDbUpgrades(Connection conn, String coreDb, int minVer) throws IOException,
            ClassNotFoundException, SQLException {
        Class[] classes = ReflectionUtils.getClasses(_UPGRADE_PACKAGE_NAME);
        List<Class> classList = new ArrayList<>();
        for (Class cls : classes) {
            Annotation a = cls.getAnnotation(DbUpgrade.class);
            if (a == null) {
                continue;
            }

            DbUpgrade upgrade = (DbUpgrade) a;
            if (upgrade.ver() < minVer) {
                continue;
            }

            //check if upgrade has been executed
            DatabaseUpgrade dbUpgrade = DatabaseUpgradeHistoryDao.get(conn, coreDb, upgrade.ver(), _ticketNum(upgrade.ticket()));
            if (dbUpgrade != null) {
                //has been executed
                continue;
            }

            classList.add(cls);
        }
        return classList;
    }

    /*
     * get upgrade class list by version
     */
    static List<DbUpgrade> _getDbUpgradeListByVersion(int version) throws IOException, ClassNotFoundException {
        Class[] classes = ReflectionUtils.getClasses(_UPGRADE_PACKAGE_NAME);
        List<DbUpgrade> classList = new ArrayList<>();
        for (Class cls : classes) {
            Annotation a = cls.getAnnotation(DbUpgrade.class);
            if (a == null) {
                continue;
            }

            DbUpgrade upgrade = (DbUpgrade) a;
            if (upgrade.ver() != version) {
                continue;
            }

            classList.add(upgrade);
        }
        return classList;
    }

    private static void _executeUpdateOperationForCoreDb(Class cls, Connection conn)
            throws InvocationTargetException, IllegalAccessException, InstantiationException {
        try {
            Method updateCoreDbMethod = cls.getDeclaredMethod("upgradeCoreDb", Connection.class);
            updateCoreDbMethod.invoke(cls.newInstance(), new Object[]{conn});
        } catch (NoSuchMethodException e) {
            LOG.warn("Ignored from upgrading core db - " + cls.getName());
        }
    }

    private static void _executeUpdateOperationForAccountDb(Class cls, Connection conn, String dbName)
            throws InvocationTargetException, IllegalAccessException, InstantiationException {
        try {
            Method updateAccountDbMethod = cls.getDeclaredMethod("upgradeAccountDb", Connection.class, String.class);
            updateAccountDbMethod.invoke(cls.newInstance(), new Object[]{conn, dbName});
        } catch (NoSuchMethodException e) {
            LOG.debug("Don't need to upgrade AccountDb - " + cls.getName());
        }
    }

    /*
     * upgrade database to specify version
     * potential db upgrades will contain all upgrades which version >= currentVersion
     * And we will filter the upgrades which have been executed
     * And execute update for core db and account db
     */
    private static List<String> _upgradeToVer(Connection conn,
                                              String coreDb,
                                              List<Account> accountList,
                                              int ver,
                                              List<Class> potentialUpgrades)
            throws SQLException, InvocationTargetException, IllegalAccessException, InstantiationException {
        //get upgrade list by ver
        Map<String, Class> ticketToUpgradeMap = new HashMap<>();
        Map<String, String> ticketFullNameMap = new HashMap<>();
        List<Pair<String, String[]>> upgradeDependencies = new ArrayList<>();
        for (Class cls : potentialUpgrades) {
            Annotation a = cls.getAnnotation(DbUpgrade.class);
            if (a == null) {
                continue;
            }

            DbUpgrade upgrade = (DbUpgrade) a;
            if (upgrade.ver() != ver) {
                continue;
            }

            String ticketNum = _ticketNum(upgrade.ticket());
            ticketFullNameMap.put(ticketNum, upgrade.ticket());
            ticketToUpgradeMap.put(ticketNum, cls);
            String after = upgrade.after();
            if (Strings.isNullOrEmpty(after)) {
                upgradeDependencies.add(new ImmutablePair<>(ticketNum, new String[]{}));
            } else {
                upgradeDependencies.add(new ImmutablePair<>(ticketNum, after.split("\\s*,\\s*")));
            }
        }

        if (upgradeDependencies.size() == 0) {
            LOG.info("No upgrading tickets found for version " + ver);
            return new ArrayList<>();
        }

        //sort upgrade class by dependency
        DependencySorter dependencySorter = new DependencySorter(upgradeDependencies);
        List<String> ticketNameSortedByDependency = dependencySorter.sort();

        LOG.info("Found #{} tickets for version {}", ticketNameSortedByDependency.size(), ver);

        //upgrade db
        for (String ticketName : ticketNameSortedByDependency) {
            LOG.info("Start upgrading task for ticket - " + ticketName);
            Class cls = ticketToUpgradeMap.get(ticketName);

            _executeUpdateOperationForCoreDb(cls, conn);

            for (Account account : accountList) {
                LOG.info("Executing upgrading task for account - " + account.getName());
                _executeUpdateOperationForAccountDb(cls, conn, account.getAccountDbName());
            }
        }

        return ticketNameSortedByDependency.stream().map(ticketFullNameMap::get).collect(Collectors.toList());
    }

    /**
     * If found '-', return part before '-' as ticket num
     *
     * @param ticket - a ticket with ticket # and description, it is like #XXX-xxx yy zzz
     * @return
     */
    private static String _ticketNum(String ticket) {
        int index = ticket.indexOf('-');
        if (index > 0) {
            return StringUtils.trim(ticket.substring(0, index));
        }

        return StringUtils.trim(ticket);
    }
}
