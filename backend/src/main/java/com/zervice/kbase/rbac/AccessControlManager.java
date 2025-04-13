package com.zervice.kbase.rbac;

import com.alibaba.fastjson.JSONObject;
import com.zervice.common.restful.exception.AccessControlException;
import com.zervice.kbase.AccountCatalog;
import com.zervice.kbase.database.dao.RbacRoleDao;
import com.zervice.kbase.database.dao.UserRbacRoleDao;
import com.zervice.kbase.database.pojo.RbacRole;
import com.zervice.kbase.database.pojo.User;
import com.zervice.kbase.database.pojo.UserRbacRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manage resource access permissions in the system
 * We define permissions on roles, so each role may have a list of permissions like this
 * permissions: [{
 *    resources: 'depts',
 *    operations: ['get']
 *    filter: { id: [1] },
 *    denyAccess: true
 * }, {
 *     resources: 'kbs',
 *     operations: ['get']
 *     filter: {},
 *    denyAccess: false
 * }]
 *
 * All permissions for a role should be *explicitly* given, otherwise the role would be taken as not be able to
 * perform certain action on that resource
 *
 * The only exception is, for a role, if we given an empty permission array, the user would be take as having all
 * the permissions in the system, but it would be good to represent such permission as this
 * permissions: [{
 *   resource: '*',
 *   operations: '*'
 *   filter: {}
 * }]
 *
 * IMPORTANT: For a given role, for each resource, we allow a single permission entry in the permission array, so
 * it is not possible to define following permissions arrays
 * permissions: [{
 *     resource: 'user',
 *     ...
 * }, {
 *     resource: 'user',
 *     ...
 * }
 */
@Log4j2
public class AccessControlManager {
    private final AccountCatalog _account;

    @Getter
    private long _adminRoleId = 0;
    /**
     * User to role mapping. In our design we also have a params field in user_rbacroles table, but
     * we are not using the params yet, so let's keep it as is
     */
    private final Map<Long/*UserId*/, Pair<Long/*roleId*/, JSONObject/*params*/>[]> _userRoles = new ConcurrentHashMap<>();

    /**
     * Rbac role cache
     */
    private final Map<Long/*RoleId*/, RbacRole> _rbacRoles = new ConcurrentHashMap<>();

    /**
     * Build allow & deny table here
     * @param account
     */
    private final Map<Long /*Role Id*/, Map<String /* resource-operation */, List<AccessFilter>>> _allowMap = new ConcurrentHashMap<>();
    private final Map<Long /*Role Id*/, Map<String /* resource-operation */, List<AccessFilter>>> _denyMap = new ConcurrentHashMap<>();

    public AccessControlManager(@NonNull AccountCatalog account) {
        _account = account;
    }

    public static AccessControlManager ensure(String dbName) {
        AccountCatalog account = AccountCatalog.ensure(dbName);

        return account.getAcm();
    }

    /**
     * Build access permission map here
     */
    public void initialize(@NonNull Connection conn) throws SQLException  {
        try {
            // load all roles
            for (RbacRole rr : RbacRoleDao.getAll(conn, _account.getDBName())) {
                _rbacRoles.put(rr.getId(), rr);
                if (rr.getName().equals(RBACConstants.SYS_ADMIN)) {
                    _adminRoleId = rr.getId();
                }

                _buildAccessMap(rr);
            }

            // load all user-><roleId, params>
            ArrayList<Pair<Long/*roleId*/, JSONObject/*params*/>> item = new ArrayList<>();
            long curUserId = 0;

            for (UserRbacRole urr : UserRbacRoleDao.getAll(conn, _account.getDBName())) {
                if (urr.getUserId() != curUserId) {
                    // dump the old user
                    if (curUserId != 0) {
                        Pair<Long, JSONObject>[] a = new Pair[item.size()];
                        item.toArray(a);
                        _userRoles.put(curUserId, a);

                        item = new ArrayList<>();
                        curUserId = urr.getUserId();
                    } else {  // curUserId is 0, set it.
                        // set the cur user Id
                        curUserId = urr.getUserId();
                    }
                }

                item.add(Pair.of(urr.getRbacRoleId(), urr.getParams()));
            }

            // dump the last user
            Pair<Long, JSONObject>[] b = new Pair[item.size()];
            item.toArray(b);
            _userRoles.put(curUserId, b);

        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateUser(long userId, Pair[] roleParams) {
        _userRoles.put(userId, roleParams);
    }

    public void removeUser(long userId) {
        _userRoles.remove(userId);
    }

    public List<RbacRole> getAllRoles() {
        return new ArrayList<>(_rbacRoles.values());
    }

    public Map<Long, RbacRole> getRoleMap() {
        return _rbacRoles;
    }

    public RbacRole getRole(long id) {
        return _rbacRoles.get(id);
    }

    public RbacRole getRole(String name) {
        for (RbacRole rr : _rbacRoles.values()) {
            if (rr.getName().equalsIgnoreCase(name)) {
                return rr;
            }
        }

        return null;
    }

    /**
     * Get the first role the user has
     */
    public RbacRole getUserRole(String username) {
        User u = _account.getUser(username);
        if (u == null) {
            return null;
        }

        Pair<Long/*roleId*/, JSONObject/*params*/>[] userRoles = _userRoles.get(u.getId());
        for (Pair<Long, JSONObject> item : userRoles) {
            long roleId = item.getKey();
            RbacRole role = _rbacRoles.get(roleId);
            if (role == null) {
                continue;
            }

            // get the first role
            return role;
        }
        return null;
    }

    public boolean isAccountAdmin(User u) {
        return Arrays.stream(_userRoles.getOrDefault(u.getId(), new Pair[]{}))
                .anyMatch(pair ->  pair.getKey().equals(_adminRoleId));
    }


    /**
     * get list of user roles of a user.
     */
    public List<RbacRole> getUserRoleList(String username) {
        User u = _account.getUser(username);
        if (u == null) {
            throw new AccessControlException(String.format("No such user (username=%s)", username));
        }

        return getUserRoleList(u.getId());
    }

    public List<RbacRole> getUserRoleList(long userId) {
        Pair<Long/*roleId*/, JSONObject/*params*/>[] userRoles = _userRoles.get(userId);
        if (userRoles == null) {
            LOG.warn("No any role assigned to this user {}", userId);
            return Collections.EMPTY_LIST;
        }

        List<RbacRole> roles = new ArrayList<>();
        for (Pair<Long, JSONObject> item : userRoles) {
            long roleId = item.getKey();
            RbacRole role = _rbacRoles.get(roleId);
            if (role == null) {
                continue;
            }

            roles.add(role);
        }


        return roles;
    }

    public boolean isSiteAdmin(long userId) {
        List<RbacRole> roles = getUserRoleList(userId);
        for(RbacRole role : roles) {
            if(RBACConstants.SYS_ADMIN.equals(role.getName())) {
                return true;
            }
        }

        return false;
    }

    /**************************************************************************************************************
     * Valid if a user can perform certain action on a resource, filter is not considered
     * Throw AccessControlException if user is not allowed to perform such operation
     **************************************************************************************************************/
    public void checkAccess(long userId, String resource, String operation) {
        Pair<Long/*roleId*/, JSONObject/*params*/>[] userRoles = _userRoles.get(userId);
        if (userRoles == null) {
            throw new AccessControlException(String.format("The user %d wasn't assigned any role", userId));
        }

        for (Pair<Long, JSONObject> item : userRoles) {
            long roleId = item.getKey();
            RbacRole role = _rbacRoles.get(roleId);
            if (role == null) {
                LOG.warn(String.format("Can't find the role %d for the user %d. Ignored.", roleId, userId));
                continue;
            }

            if(_checkAccess(roleId, resource, operation)) {
                LOG.info(String.format("Role %d for the user %d allows performing [%s] on [%s]", roleId, userId, resource, operation));
                return;
            }

            // otherwise, let's continue to see if another role allows
        }

        // reach here, no role can access the resource
        throw new AccessControlException(String.format("The user %d cannot perform operation [%s] on resource [%s]", userId, operation, resource));
    }


    /**************************************************************************************************************
     * Get allowed resource IDs for given operation. This is useful for filtering when access resource DB
     *
     * Notice that, for a given role and a given resource, there would be just one permission entries, so
     * it should be either in _denyMap or _accessMap
     *
     * As a result, to filter resources, we shall first check if given resource in deniedResources, then
     * check if it is in allowed Resources
     *
     * Return values:
     *    return null to indicate no resource allowed
     *    return empty to indicate allow all
     *    otherwise, only resource ID in the set could be allowed
     **************************************************************************************************************/
    @AllArgsConstructor
    public static class ResourceSet {
        private final List<Long> _resources;

        //
        // If no element in the set
        public boolean empty() {
            return _resources == null;
        }

        //
        // If all element in the set?
        public boolean all() {
            return _resources != null && _resources.isEmpty();
        }

        public List<Long> list() {
            return _resources;
        }

        public Set<Long> set() {
            return new HashSet<>(_resources);
        }

        public final static ResourceSet EMPTY = new ResourceSet(null);
        public final static ResourceSet ALL = new ResourceSet(new ArrayList<>());
    }


    /**
     * can access specified object id
     * @param userId  userId
     * @param resource  resource type.
     * @param operation  operation
     * @param resId resource Id
     * @return  true/allowed | false
     */
    public boolean allowedResourceId(long userId, String resource, String operation, long resId) {
        ResourceSet resourceSet = allowedResources(userId, resource, operation);
        if (resourceSet.empty()) {
            return false;
        } else if (resourceSet.all()) {
            return true;
        } else {
            return resourceSet.set().contains(resId);
        }
    }

    /**
     * same with {@link AccessControlManager#allowedResourceId(long, String, String, long)}
     * except this method throws an {@link AccessControlException} if failed
     * @param userId
     * @param resource
     * @param operation
     * @param resId
     * @throws AccessControlException  if no access
     */
    public void checkAllowResourceId(long userId, String resource, String operation, long resId)
            throws AccessControlException {
        if (!allowedResourceId(userId, resource, operation, resId)) {
            throw new AccessControlException(resource, operation, resId);
        }
    }

    /**
     * same with {@link AccessControlManager#checkAllowResourceId(long, String, String, long)}
     * @param userId
     * @param resource
     * @param operation
     * @param resIds
     * @throws AccessControlException  if no access
     */
    public void checkAllowResourceIds(long userId, String resource, String operation, List<Long> resIds)
            throws AccessControlException {
        for (long id : resIds) {
            checkAllowResourceId(userId, resource, operation, id);
        }
    }


    /**
     * be careful with this method
     * have to check whether it's empty or ALL
     * @param userId
     * @param resource
     * @param operation
     * @return
     */
    private ResourceSet allowedResources(long userId, String resource, String operation) {
        List<RbacRole> roles = getUserRoleList(userId);
        if(roles.isEmpty()) {
            // use has no roles!!! allow none!
            return ResourceSet.EMPTY;
        }

        return _filter(_allowMap, roles, resource, operation);
    }

    private ResourceSet _filter(Map<Long, Map<String, List<AccessFilter>>> accessMap, List<RbacRole> userRoles, String resource,
                                String operation) {
        List<Long> resources = new ArrayList<>();
        for (RbacRole role : userRoles) {
            // otherwise, let's continue to see if another role allows
            Map<String, List<AccessFilter>> filterMap = accessMap.get(role.getId());
            if (filterMap == null) {
                continue;
            }

            List<AccessFilter> filters = _getFilter(filterMap, resource, operation);
            if (CollectionUtils.isEmpty(filters)) {
                continue;
            }

            for (AccessFilter filter : filters) {
                if (filter == null) {
                    continue;
                }

                if (filter.getResourceIds().isEmpty()) {
                    // all of this resource!!!!
                    return ResourceSet.ALL;
                }

                resources.addAll(filter.getResourceIds());
            }
        }

        if (resources.isEmpty()) {
            return ResourceSet.EMPTY;
        }

        return new ResourceSet(resources);
    }

    private List<AccessFilter> _getFilter(Map<String, List<AccessFilter>> map, String resource, String operation) {
        //
        // For each role, each resource could have one permission entry, so we check in order and return
        // the first one hit
        // *-*: all access without filter
        // resource-*: all operation on resource
        // *-operation: operation on all resource
        // resource-operation
        if (map.containsKey("*-*")) {
            return map.get("*-*");
        }

        if (map.containsKey(resource.toLowerCase() + "-*")) {
            return map.get(resource.toLowerCase() + "-*");
        }

        if (map.containsKey("*-" + operation.toLowerCase())) {
            return map.get("*-" + operation.toLowerCase());
        }

        return map.get(resource.toLowerCase() + "-" + operation.toLowerCase());
    }

    /**
     * Core logic here!!!!
     * For any access,
     * 1. we first check the deny map, if we found a match, return false (as it is denied)
     * 2. we then check allow map, if found a match, return true
     * 3. the default behavior would always be false (no rule to deny, no rule to allow, the default is to deny)
     * @return
     */
    private boolean _checkAccess(long roleId, String resource, String operation) {
        // check for general case
        if(_checkAccess(roleId, "*-*")) {
            return true;
        }

        if(_checkAccess(roleId, resource + "-*")) {
            return true;
        }

        if(_checkAccess(roleId, "*-" + operation)) {
            return true;
        }

        return _checkAccess(roleId, resource + "-" + operation);
    }

    private boolean _checkAccess(long roleId, String permKey) {
        String lowercasedPermKey = permKey.toLowerCase();

        if (_denyMap.containsKey(roleId)) {
            Map<String, List<AccessFilter>> map = _denyMap.get(roleId);
            if (map.containsKey(lowercasedPermKey)) {
                return false;
            }
        }

        if (!_allowMap.containsKey(roleId)) {
            return false;
        }

        Map<String, List<AccessFilter>> map = _allowMap.get(roleId);
        return map.containsKey(lowercasedPermKey);
    }

    //
    // Build the access control maps (i.e. the deny map and the allow map)
    private void _buildAccessMap(RbacRole role) {
        List<RbacRole.Permission> perms = role.getPermissions();

        Map<String /* resource-operation*/, List<AccessFilter>> allow = new HashMap<>();
        Map<String /* resource-operation*/, List<AccessFilter>> deny = new HashMap<>();

        // allow or deny all resource
        String allResource = "/**";

        if (perms == null || perms.size() == 0) {
            LOG.info(String.format("Role %d (%s) has full access to system", role.getId(), role.getName()));
            // special case, we allow all for this role
            _addAccessFilterIntoResourceOperationMap(allow, "*-*", new AccessFilter());
        }
        else {
            perms.forEach(perm -> {
                String res = allResource.equals(perm.getResource()) ? "*" : perm.getResource().toLowerCase();

                JSONObject filter = perm.getFilter();
                List<String> operations = perm.getOperations();
                if (operations.isEmpty()) {
                    if (perm.isDenyAccess()) {
                        LOG.info(String.format("Role %d (%s) CAN NOT perform any operation on resource [%s] with filter %s", role.getId()
                                , role.getName(), res, filter.toString()));
                        _addAccessFilterIntoResourceOperationMap(deny, res + "-*", new AccessFilter(filter));
                    }
                    else {
                        LOG.info(String.format("Role %d (%s) CAN perform all operation on resource [%s] with filter %s", role.getId(),
                                role.getName(), res, filter.toString()));
                        _addAccessFilterIntoResourceOperationMap(allow, res + "-*", new AccessFilter(filter));
                    }
                }

                perm.getOperations().forEach(op -> {
                    if (perm.isDenyAccess()) {
                        LOG.info(String.format("Role %d (%s) CAN NOT perform %s on resource [%s] with filter %s", role.getId(),
                                role.getName(), op, res, filter.toString()));
                        _addAccessFilterIntoResourceOperationMap(deny, res + "-" + op.toLowerCase(), new AccessFilter(filter));
                    }
                    else {
                        LOG.debug(String.format("Role %d (%s) CAN perform %s on resource [%s] with filter %s", role.getId(),
                                role.getName(), op, res, filter.toString()));

                        /**
                         * 这里存在：
                         *  RbacRole.Permission(_resource=kbGroups, _operations=[get, add, update, delete], _denyAccess=false, _filter={})
                         *  RbacRole.Permission(_resource=kbGroups, _operations=[get, add, update, delete, audit, export],_denyAccess=false, _filter={"id":["1"]})
                         */
                        _addAccessFilterIntoResourceOperationMap(allow, res + "-" + op.toLowerCase(), new AccessFilter(filter));
                    }
                });
            });
        }

        if (allow.size() > 0) {
            _allowMap.put(role.getId(), allow);
        }

        if (deny.size() > 0) {
            _denyMap.put(role.getId(), deny);
        }
    }

    /**
     * we will create a new List, if map not contains the key
     */
    private void _addAccessFilterIntoResourceOperationMap(Map<String , List<AccessFilter>> map, String key, AccessFilter filter) {
        if (map.containsKey(key)) {
            map.get(key).add(filter);
            return;
        }

        List<AccessFilter> filters = new ArrayList<>();
        filters.add(filter);
        map.put(key, filters);
    }

    /**
     * Filter objects in resource ...
     */
    private static class AccessFilter {
        /**
         * If the access is for a specific # of resources, we put the IDs of those resources here
         */
        @Getter
        private final List<Long> _resourceIds = new ArrayList<>();

        // reserved for future use. Currently we only support ID filters, so we elevate ID out
        private final Map<String, List> _filters = new HashMap<>();

        public AccessFilter() {
            this(new JSONObject());
        }

        public AccessFilter(JSONObject filter) {
            if (filter.containsKey("id")) {
                _resourceIds.addAll(filter.getJSONArray("id").stream()
                        .map(obj -> Long.parseLong(obj.toString())).collect(Collectors.toList()));
            }
        }
    }

    /**
     * a new RbacRole created
     */
    public void onCreateRole(RbacRole rbacRole) {
        _rbacRoles.put(rbacRole.getId(), rbacRole);

        _buildAccessMap(rbacRole);
    }

    /**
     * a new RbacRole deleted
     */
    public void onDeleteRole(Long rbacRoleId) {
        _rbacRoles.remove(rbacRoleId);
        _allowMap.remove(rbacRoleId);
        _denyMap.remove(rbacRoleId);
    }

    /**
     * a new RbacRole updated
     */
    public void onUpdateRole(RbacRole rbacRole) {
        _rbacRoles.put(rbacRole.getId(), rbacRole);
        _buildAccessMap(rbacRole);
    }
}
