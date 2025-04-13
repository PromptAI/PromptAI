package com.zervice.kbase.api.restful.controller;

import com.zervice.common.pojo.common.EmptyResponse;
import com.zervice.common.restful.exception.RestException;
import com.zervice.common.restful.exception.StatusCodes;
import com.zervice.kbase.AccountCatalog;
import com.zervice.kbase.api.BaseController;
import com.zervice.kbase.database.criteria.RoleCriteria;
import com.zervice.kbase.database.dao.RbacRoleDao;
import com.zervice.kbase.database.dao.UserDao;
import com.zervice.kbase.database.pojo.RbacRole;
import com.zervice.kbase.database.pojo.User;
import com.zervice.kbase.database.utils.DaoUtils;
import com.zervice.kbase.rbac.AccessControlManager;
import com.zervice.kbase.rbac.RBACConstants;
import com.zervice.kbase.api.restful.AuthFilter;
import com.zervice.kbase.api.restful.group.Insert;
import com.zervice.kbase.api.restful.group.Update;
import com.zervice.kbase.api.restful.pojo.PageResponse;
import com.zervice.kbase.api.restful.pojo.PageRequest;
import com.zervice.kbase.api.restful.pojo.RestRole;
import com.zervice.kbase.api.restful.util.PageStream;
import lombok.Cleanup;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Log4j2
@RestController
@RequestMapping("/api/settings/roles")
public class RbacRoleController extends BaseController {

    @GetMapping
    public PageResponse<RestRole> getAll(@RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName,
                                         @RequestHeader(AuthFilter.X_EXTERNAL_USER_ID_HEADER) String uid,
                                         RoleCriteria roleCriteria,
                                         PageRequest pageRequest) throws Exception {
        AccountCatalog account = AccountCatalog.ensure(dbName);
        long userId = User.fromExternalId(uid);
        AccessControlManager acm = account.getAcm();
        User user = account.getUser(userId);

        @Cleanup Connection conn = DaoUtils.getConnection();
        //filter
        List<RestRole> totalData = acm.getAllRoles().stream().map(RestRole::new)
                .sorted(Comparator.comparingLong((restRole) -> StringUtils.isBlank(restRole.getId()) ? -1 :
                        Long.parseLong(restRole.getId())))
                .filter(restRole -> {
                    long id = StringUtils.isBlank(restRole.getId()) ? -1 : Long.parseLong(restRole.getId());
                    return acm.allowedResourceId(userId, RBACConstants.RESOURCE_RBAC_ROLES, RBACConstants.OPERATION_GET, id);
                })
                .filter(restRole -> {
                    if (StringUtils.isBlank(roleCriteria.getName())) {
                        return true;
                    }

                    return restRole.getName().contains(roleCriteria.getName()) ||
                            restRole.getName().equalsIgnoreCase(roleCriteria.getName());
                })
                .filter(restRole -> {
                    if (roleCriteria.getStatus() == null) {
                        return true;
                    }
                    return restRole.getStatus().equals(roleCriteria.getStatus());
                })
                .collect(Collectors.toList());

        //page
        List<RestRole> pageDat = PageStream.of(RestRole.class, pageRequest, totalData.stream());
        return PageResponse.of(totalData.size(), pageDat);
    }

    @GetMapping("{id}")
    public RestRole getById(@PathVariable long id,
                            @RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName,
                            @RequestHeader(AuthFilter.X_EXTERNAL_USER_ID_HEADER) String uid,
                            @RequestHeader(AuthFilter.TOKEN) String token) throws Exception {

        AccountCatalog account = AccountCatalog.ensure(dbName);
        long userId = User.fromExternalId(uid);
        AccessControlManager acm = account.getAcm();

        Connection conn = null;
        try {
            conn = DaoUtils.getConnection(true);

            RbacRole rbacRole = acm.getRole(id);

            acm.checkAllowResourceId(userId, RBACConstants.RESOURCE_RBAC_ROLES, RBACConstants.OPERATION_UPDATE, id);

            return new RestRole(rbacRole);
        }
        finally {
            DaoUtils.closeQuietly(conn);
        }
    }

    @PostMapping
    public RestRole add(@Validated(Insert.class) @RequestBody RestRole role,
                        @RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName,
                        @RequestHeader(AuthFilter.X_EXTERNAL_USER_ID_HEADER) String uid,
                        @RequestHeader(AuthFilter.TOKEN) String token) throws Exception {
        if (role.getPermissions() == null || role.getPermissions().isEmpty()) {
            throw new RestException(StatusCodes.BadRequest, "permission array is required");
        }

        AccountCatalog account = AccountCatalog.ensure(dbName);
        long userId = User.fromExternalId(uid);
        AccessControlManager acm = account.getAcm();
        acm.checkAccess(userId, RBACConstants.RESOURCE_RBAC_ROLES, RBACConstants.OPERATION_ADD);

        Connection conn = null;
        try {
            conn = DaoUtils.getConnection(true);
            String requestUsername = UserDao.get(conn, dbName, userId).getUsername();
            RbacRole dbRole = RbacRole.factory(
                    role.getName(),
                    RbacRole.RbacRoleProp.factory(
                            requestUsername, role.getRemark(), role.getPermissions()
                    )
            );

            long newRbacRoleId = RbacRoleDao.addReturnId(conn, dbName, dbRole);
            if (newRbacRoleId == 0) {
                throw new RestException(StatusCodes.InternalError, "add role failed");
            }
            else {
                dbRole.setId(newRbacRoleId);

                //update acm role
                acm.onCreateRole(dbRole);
                return new RestRole(dbRole);
            }
        }
        catch (SQLException e) {
            if (e.getMessage().contains("Duplicate entry")) {
                throw new RestException(StatusCodes.BadRequest, String.format(
                        "name(%s) is already exists", role.getName()
                ));
            }
            else {
                throw e;
            }
        }
        finally {
            DaoUtils.closeQuietly(conn);
        }
    }

    @DeleteMapping
    public EmptyResponse delete(@RequestBody List<Long> roleIdList,
                                @RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName,
                                @RequestHeader(AuthFilter.X_EXTERNAL_USER_ID_HEADER) String uid,
                                @RequestHeader(AuthFilter.TOKEN) String token) throws Exception {
        if (roleIdList.isEmpty()) {
            throw new RestException(StatusCodes.BadRequest, "Missing role to delete");
        }

        AccountCatalog account = AccountCatalog.ensure(dbName);
        long userId = User.fromExternalId(uid);
        AccessControlManager acm = account.getAcm();

        Connection conn = null;
        try {

            conn = DaoUtils.getConnection(true);

            acm.checkAllowResourceIds(userId, RBACConstants.RESOURCE_RBAC_ROLES, RBACConstants.OPERATION_DELETE, roleIdList);

            for (long roleId : roleIdList) {
                RbacRoleDao.delete(conn, dbName, roleId);
            }

            //update acm
            for (long roleId : roleIdList) {
                acm.onDeleteRole(roleId);
            }

            return EmptyResponse.empty();
        }
        finally {
            DaoUtils.closeQuietly(conn);
        }
    }

    @PutMapping
    public void update(@Validated(Update.class) @RequestBody RestRole role,
                       @RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName,
                       @RequestHeader(AuthFilter.X_EXTERNAL_USER_ID_HEADER) String uid,
                       @RequestHeader(AuthFilter.TOKEN) String token) throws Exception {
        if (role.getPermissions() == null || role.getPermissions().isEmpty()) {
            throw new RestException(StatusCodes.BadRequest, "permission array is required");
        }

        AccountCatalog account = AccountCatalog.ensure(dbName);
        long userId = User.fromExternalId(uid);
        AccessControlManager acm = account.getAcm();

        Connection conn = null;
        try {
            conn = DaoUtils.getConnection(true);

            long id = StringUtils.isBlank(role.getId()) ? -1 : Long.parseLong(role.getId());

            RbacRole dbRole = RbacRoleDao.get(conn, dbName, id);
            if (dbRole == null) {
                throw new RestException(StatusCodes.NotFound);
            }

            acm.checkAllowResourceId(userId, RBACConstants.RESOURCE_RBAC_ROLES, RBACConstants.OPERATION_UPDATE, id);

            dbRole.setName(role.getName());
            dbRole.getProperties().setRemark(role.getRemark());
            dbRole.getProperties().setPermissions(role.getPermissions());
            RbacRoleDao.updateNameProperties(conn, dbName, dbRole);

            //update acm
            acm.onUpdateRole(dbRole);
        }
        catch (SQLException e) {
            if (e.getMessage().contains("Duplicate entry")) {
                throw new RestException(StatusCodes.BadRequest, String.format(
                        "name(%s) is already exists", role.getName()
                ));
            }
            else {
                throw e;
            }
        }
        finally {
            DaoUtils.closeQuietly(conn);
        }
    }
}
