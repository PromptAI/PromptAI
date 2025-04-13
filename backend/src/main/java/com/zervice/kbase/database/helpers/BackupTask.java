package com.zervice.kbase.database.helpers;

import com.google.common.base.Strings;
import com.zervice.common.utils.LayeredConf;
import com.zervice.kbase.utils.CommandRunner;
import lombok.extern.log4j.Log4j2;

import java.io.File;
import java.util.concurrent.Callable;

@Log4j2
public class BackupTask implements Callable<Integer> {
    private final String _cmd;

    private static final String BACKUP_PARAMS = " --single-transaction --add-drop-database --databases ";
    private static final String BACKUP_PARAMS_NODB = " --single-transaction ";

    public BackupTask(String accessParams, String dbName, String folder, boolean keepDbNames) {
        String backupFile = folder + File.separator + dbName;

        String basedir = LayeredConf.getConfig("mysql").getString("basedir", "");
        if (Strings.isNullOrEmpty(basedir)) {
            if(keepDbNames) {
                _cmd = String.format("mysqldump %s %s %s > %s", accessParams, BACKUP_PARAMS, dbName, backupFile);
            }
            else {
                _cmd = String.format("mysqldump %s %s %s > %s", accessParams, BACKUP_PARAMS_NODB, dbName, backupFile);
            }
        }
        else {
            if (!basedir.endsWith("/")) {
                basedir += "/";
            }

            if(keepDbNames) {
                _cmd = String.format("%sbin/mysqldump %s %s %s > %s", basedir, accessParams, BACKUP_PARAMS, dbName, backupFile);
            }
            else {
                _cmd = String.format("%sbin/mysqldump %s %s %s > %s", basedir, accessParams, BACKUP_PARAMS_NODB, dbName, backupFile);
            }
        }
    }

    @Override
    public Integer call() throws Exception {
        LOG.info("Try backing up database ...");

        CommandRunner.exec(_cmd);

        LOG.info("Successfully backed up database!");

        return 0;
    }
}
