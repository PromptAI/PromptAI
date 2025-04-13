package com.zervice.kbase;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.zervice.common.cron.CronService;
import com.zervice.common.i18n.MessageUtils;
import com.zervice.common.pojo.common.Account;
import com.zervice.common.restful.exception.AccessControlException;
import com.zervice.common.utils.Constants;
import com.zervice.kbase.cache.AccountAccessTokens;
import com.zervice.kbase.cache.AccountCache;
import com.zervice.kbase.cache.AccountPublishedProjects;
import com.zervice.kbase.database.dao.AccountDao;
import com.zervice.kbase.database.dao.UserDao;
import com.zervice.kbase.database.pojo.User;
import com.zervice.kbase.database.utils.DaoUtils;
import com.zervice.kbase.rbac.AccessControlManager;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manage accounts in system
 * <p>
 * This would be the entrance point for all accessing account specific information
 */
@Log4j2
public class AccountCatalog {

    /**
     * In China, it's hard for every company to have corporate email, let's build an email
     * to company map so when signin, we can get the company from user email directly
     * <p>
     * If an email is used for more than one company, we would only allow one company, so user
     * can sign in to just one of the company
     */
    private final static ConcurrentHashMap<String /*email*/, AccountCatalog> _emailsToAccounts =
            new ConcurrentHashMap<>();
    private final static ConcurrentHashMap<String /* mobile */, AccountCatalog> _mobilesToAccounts =
            new ConcurrentHashMap<>();

    private final static ConcurrentHashMap<Long/*acctId*/, AccountCatalog> _accounts =
            new ConcurrentHashMap<>();

    private final static ConcurrentHashMap<String/*accountName*/, Long /*acctId*/> _accountNames =
            new ConcurrentHashMap<>();


    /**
     * Get account by account ID
     *
     * @return
     */
    public static AccountCatalog of(long acctId) {
        return _accounts.get(acctId);
    }

    /**
     * If the email is one of the sys admin of the management account
     *
     * @param email
     * @return
     */
    public static boolean isSiteAdmin(String email) {
        AccountCatalog account = of(ZBotRuntime.SYSMANAGE_ACCOUNTID);
        if (account == null) {
            // no management account created!
            return false;
        }

        User u = account.findUserByEmail(email);
        if (u == null) {
            return false;
        }

        return account.isAccountAdmin(u);
    }

    public static boolean isSiteAdmin(Long id) {
        AccountCatalog account = of(ZBotRuntime.SYSMANAGE_ACCOUNTID);
        if (account == null) {
            // no management account created!
            return false;
        }
        User u = account.getUser(id);
        if (u == null) {
            return false;
        }

        return account.isAccountAdmin(u);
    }

    public static boolean isSystemAccount(AccountCatalog catalog) {
        return catalog.getId() == ZBotRuntime.SYSMANAGE_ACCOUNTID;
    }

    public static boolean isGlobalAdminAccount(String email) {
        return "admin@zervice.cn".equals(email) || "admin@zervice.us".equals(email)
                || "admin@talk2bits.com".equals(email) || "admin@promptai.cn".equals(email)
                || "admin@promptai.us".equals(email);
    }

    /**
     * Get account by account name
     *
     * @return
     */
    public static AccountCatalog of(String name) {
        Long id = _accountNames.get(name);
        if (id == null) {
            return null;
        }

        return of(id);
    }

    /**
     * Get by name or external ID
     *
     * @return
     */
    public static AccountCatalog guess(String nameOrExternalId) {
        if (_accountNames.containsKey(nameOrExternalId)) {
            return of(nameOrExternalId);
        } else if (nameOrExternalId.startsWith("a")) {
            return of(Account.fromExternalId(nameOrExternalId));
        } else {
            return null;
        }
    }

    /**
     * Get an account and ensure it exists. If not found, throw an AccessControlException
     */
    public static AccountCatalog ensure(String nameOrExternalId) {
        AccountCatalog account = guess(nameOrExternalId);

        if (account == null) {
            throw new AccessControlException("Invalid account - " + nameOrExternalId);
        }

        return account;
    }

    public static AccountCatalog getUserAccountByEmail(String email) {
        return _emailsToAccounts.get(email.toLowerCase());
    }

    /**
     * Helper to walk all account in system
     *
     * @return
     */
    public static Iterator<AccountCatalog> iterator() {
        return _accounts.values().iterator();
    }

    public static void initialize() {
        Connection conn = null;
        try {
            conn = DaoUtils.getConnection(true);
            for (Account account : AccountDao.getAll(conn)) {
                AccountCatalog acb = new AccountCatalog(account);

                _accountNames.put(account.getName(), account.getId());
                _accounts.put(account.getId(), acb);

                acb._init(conn);
            }
        } catch (SQLException e) {
            LOG.fatal("Cannot initialize account catalog", e);
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            DaoUtils.closeQuietly(conn);
        }
    }

    public static AccountCatalog build(Connection conn, Account account) throws SQLException {
        AccountCatalog acb = new AccountCatalog(account);

        _accountNames.put(account.getName(), account.getId());
        _accounts.put(account.getId(), acb);

        acb._init(conn);
        return acb;
    }

    public static String getUserNameById(String dbName, long userId) {
        AccountCatalog accountCatalog = AccountCatalog.ensure(dbName);
        User u = accountCatalog.getUser(userId);
        return u == null ? MessageUtils.get(Constants.I18N_DELETED) : u.getUsername();
    }


    public static AccountCatalog getUserAccountByMobile(String mobile) {
        return _mobilesToAccounts.get(mobile);
    }

    public static void onDeleteAccount(AccountCatalog accountCatalog) {
        _accountNames.remove(accountCatalog.getName());
        _accounts.remove(accountCatalog.getId());
        _mobilesToAccounts.values().removeIf(account -> account.getId() == accountCatalog.getId());
        _emailsToAccounts.values().removeIf(account -> account.getId() == accountCatalog.getId());
        stop(accountCatalog);
    }

    public static void onCreateAccount(Account a) {
        _accountNames.put(a.getName(), a.getId());
        AccountCatalog newAccount = new AccountCatalog(a);
        _accounts.put(a.getId(), newAccount);
        Connection conn = null;
        try {
            conn = DaoUtils.getConnection(true);
            newAccount._init(conn);
        } catch (SQLException e) {
            LOG.error("Error when creating account", e);
            DaoUtils.rollbackQuietly(conn);
        } finally {
            DaoUtils.closeQuietly(conn);
        }
    }

    public static void stop(AccountCatalog catalog) {
        _accountNames.remove(catalog.getName());
        _accounts.remove(catalog.getId());

        catalog.dispose();
    }

    /**
     * account id
     */
    @Getter
    private final long _id;

    /**
     * account name
     */
    @Getter
    private final String _name;

    private final String _type;

    @Getter
    private final String _status;

    @Getter
    private String _owner;

    @Getter
    private TimeZone _timezone;

    @Getter
    private JSONArray _events;

    /**
     * Running background tasks for this company
     */
    @Getter
    private final CronService _cron;


    @Getter
    private final AccessControlManager _acm = new AccessControlManager(this);

    /**
     * User cache
     */
    private final ConcurrentHashMap<String/*username*/, User> _usersByName = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String/*email*/, User> _usersByEmail = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long/*usernameId*/, User> _usersById = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AccountCache> _caches = new ConcurrentHashMap<>();

    public AccountCatalog(Account account) {
        this._id = account.getId();
        this._name = account.getName();
        this._owner = account.getProperties().getString(Account.PROP_CREATE_BY);
        this._type = account.getProperties().getString(Account.PROP_TYPE);
        this._status = account.getProperties().getString(Account.PROP_STATUS);
        this._timezone = _getTimezone(account.getProperties().getString(Account.PROP_TIMEZONE));
        this._cron = CronService.create(_name);
        this._events = account.getEvents();
    }

    public static void main(String[] args) {
        JSONObject j = JSONObject.parseObject(" {\"ver\": 1, \"status\": \"settingUp1\", \"timezone\": \"CST\", \"createdByUser\": \"admin\", \"createdByAdmin\": \"admin\", \"createdAtEpochMs\": \"1654675936129\"}");
        String s = j.getString(Account.PROP_CREATE_BY);

    }

    /************************************************************************************************
     *
     ************************************************************************************************/
    public String getDBName() {
        return Account.getAccountDbName(_id);
    }

    public AccountCache getCache(String name) {
        return _caches.get(name);
    }

    public List<User> getAllUsers() {
        return new ArrayList<>(_usersByName.values());
    }

    public User getUser(String username) {
        return _usersByName.get(username);
    }

    public User getUser(long userId) {
        return _usersById.get(userId);
    }

    public User getUserByEmail(String email) {
        return _usersByEmail.get(email);
    }

    public String getUserNameIfExist(Long userId) {
        if (userId == null) {
            return null;
        }

        if (userId == 0) {
            return "";
        }

        User user = _usersById.get(userId);
        return user == null ? MessageUtils.get(Constants.I18N_DELETED) : user.getUsername();
    }


    public static List<AccountCatalog> getAllAccounts() {
        return new ArrayList<>(_accounts.values());
    }

    /********************************************************************************
     * Helpers follows
     *******************************************************************************/
    private void _init(Connection conn) throws SQLException {
        // init user cache
        _initUserCache(conn);

        // initialize access control manager
        _acm.initialize(conn);

        // init caches
        AccountAccessTokens tokens = new AccountAccessTokens(this);
        tokens.initialize(conn);
        _caches.put(AccountAccessTokens.NAME, tokens);

        AccountPublishedProjects projects = new AccountPublishedProjects(this);
        projects.initialize(conn);
        _caches.put(AccountPublishedProjects.NAME, projects);

        if (Environment.isMaster()) {
            LOG.info("No Scheduling for master");
        } else {
            LOG.info("DONT Scheduling  for SLAVE");
        }
    }

    private void _initUserCache(Connection conn) throws SQLException {
        UserDao.getAll(conn, Account.getAccountDbName(_id)).forEach(u -> {
            LOG.debug(String.format("Load user %s for account %s ...", u.getUsername(), _name));

            _usersByName.put(u.getUsername(), u);
            _usersById.put(u.getId(), u);
            if (u.getEmail() != null) {
                _usersByEmail.put(u.getEmail(), u);
            }

            String phone = u.getMobile();
            if (!StringUtils.isEmpty(phone)) {
                // user has configured phone
                AccountCatalog acct = _mobilesToAccounts.put(phone, this);
                if (acct != null) {
                    // When creating user, we shall avoid this!!!
                    LOG.warn("[{}][User:{} has previously associated with another:{} company!]", this.getName(), u.getMobile(), acct.getName());
                }
            }

            String email = u.getEmail();
            if (!StringUtils.isEmpty(email)) {
                // user has configured phone
                AccountCatalog acct = _emailsToAccounts.put(email, this);
                if (acct != null) {
                    // When creating user, we shall avoid this!!!
                    LOG.warn("[{}][User:{} has previously associated with another:{} company!]", this.getName(), u.getEmail(), acct.getName());
                }
            }
        });
    }

    public static void remove(Account account) {
        AccountCatalog catalog = _accounts.remove(account.getId());

        if (catalog != null) {
            stop(catalog);

            _emailsToAccounts.values().removeIf(acct -> acct == catalog);
            _mobilesToAccounts.values().removeIf(acct -> acct == catalog);
        }
    }

    public void onCreateUser(User u) {
        _usersByName.put(u.getUsername(), u);
    }

    public boolean isTrialAccount() {
        return Account.TYPE_TRIAL.equals(_type);
    }

    public boolean isReady() {
        return Account.STATUS_READY.equals(_status);
    }

    public AccountAccessTokens getAccessTokens() {
        return (AccountAccessTokens) _caches.get(AccountAccessTokens.NAME);
    }

    public AccountPublishedProjects getPublishedProjects() {
        return (AccountPublishedProjects) _caches.get(AccountPublishedProjects.NAME);
    }


    /**
     * Helper to help get account timezone formatted date
     */
    public String formatDate(String dateFormat, long millis) {
        if (millis == 0) {
            millis = System.currentTimeMillis();
        }

        return formatDate(new SimpleDateFormat(dateFormat), new Date(millis));
    }

    public String formatDate(SimpleDateFormat dateFormat, Date date) {
        dateFormat.setTimeZone(_timezone);
        return dateFormat.format(date);
    }

    private TimeZone _getTimezone(String tz) {
        if (StringUtils.isBlank(tz)) {
            return TimeZone.getDefault();
        }

        return TimeZone.getTimeZone(tz);
    }

    public User findUserByMobile(String mobile) {
        return _usersById.values().stream().filter(u -> StringUtils.equals(mobile, u.getMobile())).findFirst().orElse(null);
    }

    public User findUserByEmail(String email) {
        String lowerCaseEmail = email.toLowerCase(Locale.ROOT);
        return _usersById.values().stream().filter(u -> StringUtils.equals(lowerCaseEmail, u.getEmail())).findFirst().orElse(null);
    }

    public void updateUserProperties(User user) {
        User cached = _usersById.get(user.getId());
        if (cached != null) {
            cached.setProperties(user.getProperties());
        }
    }

    public boolean isAccountAdmin(User u) {
        return _acm.isAccountAdmin(u);
    }

    public void onUpdateUser(User user) {
        _usersById.put(user.getId(), user);

        //username updated
        if (!_usersByName.containsKey(user.getUsername())) {
            String oldUserName = null;
            for (Map.Entry<String, User> userEntry : _usersByName.entrySet()) {
                User oldUser = userEntry.getValue();
                if (oldUser.getId() == user.getId()) {
                    oldUserName = oldUser.getUsername();
                    break;
                }
            }

            //remove old
            if (StringUtils.isNotBlank(oldUserName)) {
                _usersByName.remove(oldUserName);
            }
        }

        _usersByName.put(user.getUsername(), user);

        String phone = user.getMobile();
        if (!StringUtils.isEmpty(phone)) {
            // user has configured phone
            AccountCatalog acct = _mobilesToAccounts.put(phone, this);
            if (acct != null) {
                // When creating user, we shall avoid this!!!
                LOG.warn("[{}][User:{} has previously associated with another:{} company!]", this.getName(), user.getMobile(), acct.getName());
            }
        }

        String email = user.getEmail();
        if (!StringUtils.isEmpty(email)) {
            // user has configured phone
            AccountCatalog acct = _emailsToAccounts.put(email, this);
            if (acct != null) {
                // When creating user, we shall avoid this!!!
                LOG.warn("[{}][User:{} has previously associated with another:{} company!]", this.getName(), user.getEmail(), acct.getName());
            }

            _usersByEmail.put(email, user);
        }

    }

    public void onUpdateUser(User user, List<Pair<Long, JSONObject>> roleParams) {
        onUpdateUser(user);
        _acm.updateUser(user.getId(), roleParams.toArray(new Pair[0]));
    }

    protected void dispose() {
        this._cron.shutdown();
    }

    public void onDeleteUser(User u) {
        _usersByName.remove(u.getUsername());

        if (StringUtils.isNotBlank(u.getEmail())) {
            _emailsToAccounts.remove(u.getEmail());
        }

        if (StringUtils.isNotBlank(u.getMobile())) {
            _mobilesToAccounts.remove(u.getMobile());
        }

        _usersById.remove(u.getId());

        _acm.removeUser(u.getId());
    }

    public static boolean checkUserIsReachable(Set<String> admins) {
        for (String admin : admins) {
            if (_mobilesToAccounts.containsKey(admin)) {
                return false;
            }
        }
        return true;
    }

    public static boolean checkUserIsReachable(String mobileOrEmail) {
        if (_mobilesToAccounts.containsKey(mobileOrEmail)) {
            return true;
        }

        return _emailsToAccounts.containsKey(mobileOrEmail);
    }

    public void onUpdateEvents(JSONArray events) {
        _events = events;
    }
}

