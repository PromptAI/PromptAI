package com.zervice.kbase.cache;

import com.zervice.common.cron.CronTask;
import com.zervice.kbase.AccountCatalog;
import com.zervice.kbase.database.utils.DaoUtils;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.sql.Connection;

/**
 * Base class for account specific cache
 */
@Log4j2
public abstract class AccountCache {
    protected static final long _MIN = 60 * 1000;

    @Getter
    protected final AccountCatalog _account;

    @Getter
    private final String _name;

    protected AccountCache(AccountCatalog catalog, String name) {
        _account = catalog;
        _name = name;
    }

    /**
     * Initialize cache
     */
    public final void initialize(Connection conn) {
        _initialize(conn);
        _account.getCron().schedule(new ReloadTask());
    }

    /**
     * An immediate reload ...
     */
    public final void reload() {
        new ReloadTask().run();
    }

    /**
     * Cleanup cache
     */
    public final void cleanup() {
        _cleanup();
    }

    /**
     * Be default every cache will be reloaded every 30 minutes, but a subclass can override to
     * @return
     */
    protected long _getReloadInterval() {
        return 30 * _MIN;
    }

    /**
     * Initialize cache
     */
    abstract protected void _initialize(Connection conn);

    /**
     * Reload cache
     */
    abstract protected void _reload(Connection conn);

    /**
     * cleanup cache
     */
    protected void _cleanup() {

    }

    protected class ReloadTask implements CronTask {
        @Override
        public long getDelay() {
            return _getReloadInterval();
        }

        @Override
        public long getInterval() {
            return _getReloadInterval();
        }

        @Override
        public void run() {
            Connection conn = null;
            try {
                conn = DaoUtils.getConnection(true);

                _reload(conn);
            } catch (Exception e) {
                LOG.error("[{}][Cannot reload cache for account:{}]", _account.getDBName(), _account.getName(), e);
            } finally {
                DaoUtils.closeQuietly(conn);
            }
        }
    }
}
