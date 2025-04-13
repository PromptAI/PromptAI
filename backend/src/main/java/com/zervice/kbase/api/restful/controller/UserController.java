package com.zervice.kbase.api.restful.controller;

import cn.hutool.core.lang.Validator;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.RSA;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.zervice.common.pojo.common.EmptyResponse;
import com.zervice.common.restful.exception.RestException;
import com.zervice.common.restful.exception.StatusCodes;
import com.zervice.common.utils.Constants;
import com.zervice.common.utils.IconUtils;
import com.zervice.common.utils.IdGenerator;
import com.zervice.common.utils.LayeredConf;
import com.zervice.kbase.AccountCatalog;
import com.zervice.kbase.ZBotConfig;
import com.zervice.kbase.api.BaseController;
import com.zervice.kbase.api.restful.AuthFilter;
import com.zervice.kbase.api.restful.pojo.*;
import com.zervice.kbase.api.restful.util.PageStream;
import com.zervice.kbase.critical.CreateUserCO;
import com.zervice.kbase.critical.DeleteUserCO;
import com.zervice.kbase.critical.UpdateUserCO;
import com.zervice.kbase.database.SecurityUtils;
import com.zervice.kbase.database.dao.CommonBlobDao;
import com.zervice.kbase.database.dao.RbacRoleDao;
import com.zervice.kbase.database.dao.UserDao;
import com.zervice.kbase.database.pojo.CommonBlob;
import com.zervice.kbase.database.pojo.RbacRole;
import com.zervice.kbase.database.pojo.User;
import com.zervice.kbase.database.utils.DaoUtils;
import com.zervice.kbase.rbac.AccessControlManager;
import com.zervice.kbase.rbac.RBACConstants;
import com.zervice.kbase.service.BlobService;
import com.zervice.kbase.utils.FileUtils;
import lombok.Cleanup;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpSession;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


@Log4j2
@RestController
@RequestMapping("/api/settings/users")
public class UserController extends BaseController {


    private static BlobService blobService = BlobService.getInstance();
    private String privateKey = LayeredConf.getString(ZBotConfig.KEY_RSA_KEY, ZBotConfig.DEFAULT_RSA_KEY);

    @Value("${file.max.byte_size:512000}")
    private double fileMaxByteSize;
    public static final Pattern passPattern = Pattern.compile("(?=.*[0-9])(?=.*[a-zA-Z])(?=.*[^a-zA-Z0-9])");
    /**
     * 最低密码长度
     */
    public static final int PASS_MIN_LENGTH = 10;

    /***
     * 1、default role
     * 2、login via email or phone
     * 3、
     *
     */
    @PostMapping
    public RestUser add(@Validated @RequestBody RestUser user,
                        @RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName,
                        @RequestHeader(AuthFilter.X_EXTERNAL_USER_ID_HEADER) String uid) throws Exception {
        checkPass(user.getPassword());

        long userId = User.fromExternalId(uid);
        AccountCatalog account = AccountCatalog.ensure(dbName);
        AccessControlManager acm = account.getAcm();
        User currentUser = account.getUser(userId);

        _checkLoginAccount(user);

        acm.checkAccess(userId, RBACConstants.RESOURCE_USERS, RBACConstants.OPERATION_ADD);

        Connection conn = null;
        try {
            conn = DaoUtils.getConnection(true);

            // default Role = SYS_USER
            Set<RestRole> defaultRoles = _getDefaultRoles(conn, dbName);
            user.setRoles(defaultRoles);

            User.UserProp prop = _checkInput(acm, user);
            prop.setCreatorId(userId);

            // add config
            JSONObject config = _buildUserConfigWhenCreate(currentUser);
            prop.setConfig(config);

            String avatar = generateImageIfNotExist(user.getUsername(), user.getAvatar(), conn, dbName);
            prop.setAvatar(avatar);

            CreateUserCO.UserParams userParams = CreateUserCO.create()
                    .setName(user.getUsername())
                    .setPassword(user.getPassword())
                    .setProperties((JSONObject) JSON.toJSON(prop))
                    .setRoles((JSONArray) JSON.toJSON(user.getRoles()))
                    .setMobile(user.getMobile()).setEmail(user.getEmail());
            User newUserAfter = CreateUserCO.process(dbName, userParams);
            List<RestRole> roles = _queryRoleByIds(newUserAfter.getRoles(), acm);
            return toRestUser(newUserAfter, roles, account, conn, dbName);
        } catch (Exception e) {
            if (e.getMessage().contains("Duplicate entry")) {
                throw new RestException(StatusCodes.BadRequest, String.format(
                        "name(%s) is already exists", user.getUsername()
                ));
            } else {
                throw e;
            }
        } finally {
            DaoUtils.closeQuietly(conn);
        }
    }

    /**
     * 目前界面中暂不支持角色选择，先给一个默认的admin role
     */
    private Set<RestRole> _getDefaultRoles(Connection conn, String dbName) throws Exception {
        RbacRole rbacRole = RbacRoleDao.get(conn, dbName, RBACConstants.SYS_USER);
        if (rbacRole == null) {
            //TODO fixme use rest exception
            throw new RestException(StatusCodes.BadRequest);
        }

        // 这只需要id
        RestRole restRole = new RestRole();
        restRole.setId(rbacRole.getId() + "");

        return Set.of(restRole);
    }

    /**
     * 创建新用户时初始化config
     * - 如果当前用户配置了，用当前用户的
     * - 当前用户没有，给默认值
     */
    private JSONObject _buildUserConfigWhenCreate(User user) {
        JSONObject config = user.getProperties().getConfig();
        if (config != null) {
            return config;
        }

        return User.UserProp.defaultConfig(user);
    }

    public static String generateImageIfNotExist(String name, String image, Connection conn, String dbName) throws Exception {
        if (StringUtils.isBlank(image)) {
            File imageFile = IconUtils.generateUserImage(name, "/tmp", UUID.randomUUID() + ".png");
            if (imageFile != null) {
                RestBlob blob = blobService.save(imageFile, conn, dbName);
                return blob.getId();
            }
        }

        return image;
    }

    @PutMapping
    public RestUser update(@Validated @RequestBody RestUser user,
                           @RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName,
                           @RequestHeader(AuthFilter.X_EXTERNAL_USER_ID_HEADER) String uid,
                           @RequestHeader(AuthFilter.TOKEN) String token) throws Exception {

        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            throw new RestException(StatusCodes.BadRequest, "param:roles is required");
        }
        checkPass(user.getPassword());

        long userId = User.fromExternalId(uid);
        AccountCatalog account = AccountCatalog.ensure(dbName);
        AccessControlManager acm = account.getAcm();
        if (!acm.allowedResourceId(userId, RBACConstants.RESOURCE_USERS, RBACConstants.OPERATION_UPDATE, userId)) {
            throw new RestException(StatusCodes.Forbidden, "current use has no rights to update user");
        }

        Connection conn = null;
        try {
            conn = DaoUtils.getConnection(true);

            long id = StringUtils.isBlank(user.getId()) ? -1 : Long.parseLong(user.getId());
            User oldUser = UserDao.get(conn, dbName, id);
            if (oldUser == null) {
                LOG.warn("[{}][Cannot update users, user not fund ] - User.id={}", dbName, user.getId());
                throw new RestException(StatusCodes.NotFound, "user not fund with User.id:" + user.getId());
            }


            User.UserProp prop = _checkInput(acm, user);
            if (!StringUtils.isEmpty(user.getFile())) {
                Pair<byte[], String> contentAndSuffix = FileUtils.base64ToBytes(user.getFile());
                String newFileName = newRandomFileName(contentAndSuffix.getRight());
                // give me a id
                long blobId = IdGenerator.generateId();
                CommonBlob blob = CommonBlob.builder().id(blobId)
                        .fileName(newFileName)
                        .type(CommonBlob.TYPE_USER_AVATAR)
                        .md5(DigestUtils.md5DigestAsHex(contentAndSuffix.getLeft()))
                        .content(contentAndSuffix.getLeft())
                        .build();
                CommonBlobDao.add(conn, dbName, blob);
                prop.setAvatar("" + blobId);
//                // delete old blob
//                deleteOldAvatarQuietly(conn, dbName, oldUser.getProperties().getAvatar());
            } else {
                prop.setAvatar(oldUser.getProperties().getAvatar());
            }

            UpdateUserCO.UserParams userParams = UpdateUserCO.create()
                    .setId(id)
                    .setName(user.getUsername())
                    .setPassword(user.getPassword())
                    .setStatus(user.getStatus())
                    .setProperties((JSONObject) JSON.toJSON(prop))
                    .setRoles((JSONArray) JSON.toJSON(user.getRoles()));
            User dbUser = UpdateUserCO.process(dbName, userParams);

            List<RestRole> roles = _queryRoleByIds(dbUser.getRoles(), acm);
            return toRestUser(dbUser, roles, account, conn, dbName);
        } finally {
            DaoUtils.closeQuietly(conn);
        }
    }

    @DeleteMapping
    public EmptyResponse delete(@RequestParam List<Long> ids,
                                @RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName,
                                @RequestHeader(AuthFilter.X_EXTERNAL_USER_ID_HEADER) String uid,
                                @RequestHeader(AuthFilter.TOKEN) String token) throws Exception {
        if (ids.isEmpty()) {
            throw new RestException(StatusCodes.BadRequest, "No user to remove");
        }

        long userId = User.fromExternalId(uid);
        AccountCatalog account = AccountCatalog.ensure(dbName);
        AccessControlManager acm = account.getAcm();

        acm.checkAllowResourceIds(userId, RBACConstants.RESOURCE_USERS, RBACConstants.OPERATION_DELETE, ids);

        // owner user id: the first user can't be deleted
        Long ownerUserId = 1L;

        Connection conn = null;
        try {
            conn = DaoUtils.getConnection(false);

            for (Long id : ids) {
                // 不能删除owner
                if (ownerUserId.equals(id)) {
                    throw new RestException(StatusCodes.NotAllowDeleteDefaultUser);
                }

                // 不能删除自己
                if (userId == id) {
                    throw new RestException(StatusCodes.NotAllowDeleteSelf);
                }

                User user = UserDao.get(conn, dbName, id);
                if (user == null) {
                    LOG.warn("[{}][Cannot delete users, user not fund ] - User.id={}", dbName, id);
                    throw new RestException(StatusCodes.NotFound, "user not fund with User.id:" + id);
                }
            }


            DeleteUserCO.DeleteParams deleteParams = DeleteUserCO.create()
                    .setUserIds(ids);
            DeleteUserCO.process(dbName, deleteParams);
            return new EmptyResponse();

        } finally {
            DaoUtils.closeQuietly(conn);
        }
    }

    @GetMapping("{id}")
    public RestUser getById(@PathVariable long id,
                            @RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName,
                            @RequestHeader(AuthFilter.X_EXTERNAL_USER_ID_HEADER) String uid,
                            @RequestHeader(AuthFilter.TOKEN) String token) throws Exception {
        long userId = User.fromExternalId(uid);
        AccountCatalog account = AccountCatalog.ensure(dbName);
        AccessControlManager acm = account.getAcm();

        acm.checkAllowResourceId(userId, RBACConstants.RESOURCE_USERS, RBACConstants.OPERATION_GET, id);

        User user = account.getUser(id);
        if (user == null) {
            throw new RestException(StatusCodes.NotFound);
        }

        Connection conn = null;
        try {
            conn = DaoUtils.getConnection(true);
            List<RestRole> roles = acm.getUserRoleList(id).stream().map(RestRole::new).collect(Collectors.toList());
            return toRestUser(user, roles, account, conn, dbName);
        } finally {
            DaoUtils.closeQuietly(conn);
        }
    }

    @GetMapping
    public PageResponse<RestUser> getList(@RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName,
                                          @RequestHeader(AuthFilter.X_EXTERNAL_USER_ID_HEADER) String uid,
                                          RestUser user, PageRequest pageRequest) throws Exception {
        long userId = User.fromExternalId(uid);
        AccountCatalog account = AccountCatalog.ensure(dbName);
        AccessControlManager acm = account.getAcm();

        Connection conn = null;
        try {
            conn = DaoUtils.getConnection(true);

            List<User> totalData = account.getAllUsers().stream()
                    .filter(dbUser -> {
                        boolean allowed = acm.allowedResourceId(userId, RBACConstants.RESOURCE_USERS, RBACConstants.OPERATION_GET, dbUser.getId());
                        boolean queryUserName = StringUtils.isBlank(user.getUsername()) || dbUser.getUsername().contains(user.getUsername()) ||
                                dbUser.getUsername().equalsIgnoreCase(user.getUsername());
                        boolean queryStatus = user.getStatus() == null || dbUser.getStatus().equals(user.getStatus());

                        boolean queryEmail = StringUtils.isBlank(user.getEmail()) ||
                                (StringUtils.isNotBlank(dbUser.getEmail()) && dbUser.getEmail().contains(user.getEmail()));
                        boolean queryMobile = StringUtils.isBlank(user.getMobile()) ||
                                (StringUtils.isNotBlank(dbUser.getMobile()) && dbUser.getMobile().contains(user.getMobile()));
                        return allowed && queryUserName && queryStatus && queryEmail && queryMobile;
                    })
                    .collect(Collectors.toList());

            List<User> pageData = PageStream.of(User.class, pageRequest, totalData.stream());

            List<RestUser> restUsers = new ArrayList<>();
            for (int i = 0; i < pageData.size(); i++) {
                User dbUser = pageData.get(i);
                List<RestRole> roles = acm.getUserRoleList(dbUser.getId()).stream().map(RestRole::new).collect(Collectors.toList());
                restUsers.add(toRestUser(dbUser, roles, account, conn, dbName));
            }
            return PageResponse.of(totalData.size(), restUsers);
        } finally {
            DaoUtils.closeQuietly(conn);
        }

    }

    /**
     * Update personal information, no permission check would be done as user can only change his or her information
     *
     * @return
     * @throws Exception
     */
    @PutMapping("center")
    public EmptyResponse center(@RequestBody @Validated UserUpdateRequest updateRequest,
                                @RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName,
                                @RequestHeader(AuthFilter.X_EXTERNAL_USER_ID_HEADER) String uid,
                                @RequestHeader(AuthFilter.TOKEN) String token) throws Exception {
        long userId = User.fromExternalId(uid);
        AccountCatalog.ensure(dbName);

        Connection conn = null;
        try {
            conn = DaoUtils.getConnection(false);

            //query user
            User dbUser = UserDao.get(conn, dbName, userId);
            if (dbUser == null) {
                throw new RestException(StatusCodes.NotFound);
            }

            //set name
            dbUser.setUsername(updateRequest.getName());

            //set and phone
            dbUser.setMobile(updateRequest.getMobile());
            dbUser.setEmail(updateRequest.getEmail());

            UserDao.updatePropertiesAndName(conn, dbName, dbUser);

            conn.commit();
        } finally {
            DaoUtils.closeQuietly(conn);
        }

        return EmptyResponse.empty();
    }

    @PostMapping("updatePass")
    public EmptyResponse updatePass(@RequestBody @Validated UpdatePassRequest updatePassRequest,
                                    HttpSession session,
                                    @RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName,
                                    @RequestHeader(AuthFilter.X_EXTERNAL_USER_ID_HEADER) String uid) throws Exception {
        long userId = User.fromExternalId(uid);


        if (UpdatePassRequest.TYPE_PWD.equals(updatePassRequest.getType())) {
            _updatePassByPasscode(updatePassRequest, userId, dbName);
            return EmptyResponse.empty();
        }

        if (UpdatePassRequest.TYPE_SMS.equals(updatePassRequest.getType())) {
            _updatePassBySms(updatePassRequest, userId, session, dbName);
            return EmptyResponse.empty();
        }

        if (UpdatePassRequest.TYPE_EMAIL.equals(updatePassRequest.getType())) {
            _updatePassByEmail(updatePassRequest, userId, session, dbName);
            return EmptyResponse.empty();
        }

        LOG.error("[{}][unsupported type:{} of update pass form user:{}]", dbName,
                updatePassRequest.getType(), userId);
        throw new RestException(StatusCodes.BadRequest);
    }

    private void _updatePassBySms(UpdatePassRequest updatePassRequest,
                                  long userId, HttpSession session, String dbName) throws Exception {
        if (StringUtils.isBlank(updatePassRequest.getCode())) {
            throw new RestException(StatusCodes.InvalidCode);
        }

        String newPass = updatePassRequest.getNewPass();
        if (StringUtils.isBlank(newPass)) {
            throw new RestException(StatusCodes.INVALID_PASS);
        }

        checkPass(newPass);

        @Cleanup Connection conn = DaoUtils.getConnection(false);
        User user = UserDao.get(conn, dbName, userId);
        if (user == null || StringUtils.isBlank(user.getMobile())) {
            LOG.error("[{}][update pwd by sms fail, user:{} or mobile invalid]", dbName, userId);
            throw new RestException(StatusCodes.InvalidCode);
        }

        String mobile = user.getMobile();
        String code = updatePassRequest.getCode();
        NotifyController.verifySecInfo(session, mobile, code);

        _updateUserPass(updatePassRequest, user, conn, dbName);

        conn.commit();
    }


    private void _updatePassByEmail(UpdatePassRequest updatePassRequest,
                                    long userId, HttpSession session, String dbName) throws Exception {
        if (StringUtils.isBlank(updatePassRequest.getCode())) {
            throw new RestException(StatusCodes.InvalidCode);
        }

        String newPass = updatePassRequest.getNewPass();
        if (StringUtils.isBlank(newPass)) {
            throw new RestException(StatusCodes.INVALID_PASS);
        }

        checkPass(newPass);

        @Cleanup Connection conn = DaoUtils.getConnection(false);
        User user = UserDao.get(conn, dbName, userId);
        if (user == null || StringUtils.isBlank(user.getEmail())) {
            LOG.error("[{}][update pwd by sms fail, user:{} or email invalid]", dbName, userId);
            throw new RestException(StatusCodes.InvalidCode);
        }

        String email = user.getEmail();
        String code = updatePassRequest.getCode();
        NotifyController.verifySecInfo(session, email, code);

        _updateUserPass(updatePassRequest, user, conn, dbName);

        conn.commit();
    }

    private void _updatePassByPasscode(UpdatePassRequest updatePassRequest,
                                       long userId, String dbName) throws Exception {
        checkPass(updatePassRequest.getNewPass());
        @Cleanup Connection conn = DaoUtils.getConnection(false);

        //query
        User dbUser = UserDao.get(conn, dbName, userId);
        if (dbUser == null) {
            throw new RestException(StatusCodes.NotFound);
        }

        RSA rsa = new RSA(privateKey, null);
        //check odl password
        String oldPlainPass = new String(rsa.decrypt(updatePassRequest.getOldPass(), KeyType.PrivateKey));
        if (!SecurityUtils.verifyPassword(oldPlainPass, dbUser.getProperties().getPassword())) {
            throw new RestException(StatusCodes.CurrentPassError);
        }

        _updateUserPass(updatePassRequest, dbUser, conn, dbName);

        conn.commit();
    }

    private void _updateUserPass(UpdatePassRequest updatePassRequest, User dbUser,
                                 Connection conn, String dbName) throws Exception {
        RSA rsa = new RSA(privateKey, null);

        //decrypt new password
        String newPlainPass = new String(rsa.decrypt(updatePassRequest.getNewPass(), KeyType.PrivateKey));

        //encrypt
        dbUser.setPassword(newPlainPass);
        dbUser.getProperties().setInitPass("");

        //update
        UserDao.updatePassword(conn, dbName, dbUser);

        AccountCatalog account = AccountCatalog.ensure(dbName);
        account.onUpdateUser(dbUser);
    }

    @PostMapping("updateAvatar")
    public Object updateAvatar(@RequestParam("file") MultipartFile multipartFile,
                               @RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName,
                               @RequestHeader(AuthFilter.X_EXTERNAL_USER_ID_HEADER) String uid,
                               @RequestHeader(AuthFilter.TOKEN) String token) throws Exception {
        long userId = User.fromExternalId(uid);
        AccountCatalog.ensure(dbName);

        Connection conn = null;

        try {
            conn = DaoUtils.getConnection(false);

            //query
            User dbUser = UserDao.get(conn, dbName, userId);
            if (dbUser == null) {
                throw new RestException(StatusCodes.NotFound);
            }

            Pair<String, byte[]> fileNameAndContent = _generateAvatarFileName(multipartFile);
            // give me a id
            long blobId = IdGenerator.generateId();
            CommonBlob blob = CommonBlob.builder().id(blobId)
                    .fileName(fileNameAndContent.getLeft())
                    .type(CommonBlob.TYPE_USER_AVATAR)
                    .content(fileNameAndContent.getRight())
                    .md5(DigestUtils.md5DigestAsHex(multipartFile.getInputStream()))
                    .build();

            CommonBlobDao.add(conn, dbName, blob);

//            deleteOldAvatarQuietly(conn, dbName, dbUser.getProperties().getAvatar());
            //set avatar
            String avatarId = CommonBlob.toExternalId(dbName, blobId);
            dbUser.getProperties().setAvatar(avatarId);

            UserDao.updateProperties(conn, dbName, dbUser);

            conn.commit();
            return avatarId;
        } finally {
            DaoUtils.closeQuietly(conn);
        }
    }

    @PutMapping("/config")
    public Object updateConfig(@RequestBody UserConfigRequest userConfig,
                               @RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName,
                               @RequestHeader(AuthFilter.X_EXTERNAL_USER_ID_HEADER) String uid,
                               @RequestHeader(AuthFilter.TOKEN) String token) throws Exception {
        long userId = User.fromExternalId(uid);
        AccountCatalog.ensure(dbName);
        Connection conn = null;
        try {
            conn = DaoUtils.getConnection(false);
            //query
            User dbUser = UserDao.get(conn, dbName, userId);
            if (dbUser == null) {
                throw new RestException(StatusCodes.NotFound);
            }
            String timeZone = userConfig.getTimeZone();
            String language = userConfig.getLanguage();
            if (StringUtils.isNotBlank(timeZone) && !_validTimezone(timeZone)) {
                throw new RestException(StatusCodes.INVALID_USER_CONFIG_TIME_ZONE);
            }
            if (StringUtils.isNotBlank(language) && !_validLanguage(language)) {
                throw new RestException(StatusCodes.INVALID_USER_CONFIG_LANGUAGE);
            }
            dbUser.getProperties().updateConfig(language, timeZone);
            UserDao.updateProperties(conn, dbName, dbUser);
            conn.commit();
            return EmptyResponse.empty();
        } finally {
            DaoUtils.closeQuietly(conn);
        }
    }

    private boolean _validTimezone(String timeZone) {

        return Constants.TIME_ZONE.contains(timeZone.toUpperCase());
    }

    private boolean _validLanguage(String language) {
        return Constants.LANGUAGE.contains(language.toUpperCase());
    }

    @PostMapping("resetpass")
    public String resetOtherUserPass(@Validated @RequestBody UpdateOtherUserPassRequest req,
                                     @RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName,
                                     @RequestHeader(AuthFilter.X_EXTERNAL_USER_ID_HEADER) String uid,
                                     @RequestHeader(AuthFilter.TOKEN) String token) throws Exception {
        checkPass(req.getNewPass());

        long userId = User.fromExternalId(uid);
        AccountCatalog account = AccountCatalog.ensure(dbName);
        AccessControlManager acm = account.getAcm();
        acm.checkAllowResourceId(userId, RBACConstants.RESOURCE_USERS, RBACConstants.OPERATION_UPDATE, req.getId());
        Connection conn = null;
        try {
            conn = DaoUtils.getConnection(false);
            User otherUser = UserDao.get(conn, dbName, req.getId());
            otherUser.setPassword(req.getNewPass());
            otherUser.getProperties().setInitPass("");
            UserDao.updatePassword(conn, dbName, otherUser);

            LOG.info("Current user {} try to reset user {} password", userId, otherUser.getUsername());
            conn.commit();

            account.onUpdateUser(otherUser);
            return "ok";
        } finally {
            DaoUtils.closeQuietly(conn);
        }
    }

    @GetMapping("avatar/{id}")
    public ResponseEntity<byte[]> avatar(@PathVariable Long id,
                                         @RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName) throws Exception {

        Connection conn = null;
        try {

            conn = DaoUtils.getConnection(true);

            //query user
            User dbUser = UserDao.get(conn, dbName, id);
            if (dbUser == null) {
                throw new RestException(StatusCodes.NotFound);
            }

            //not set avatar
            if (StringUtils.isBlank(dbUser.getProperties().getAvatar())) {
                throw new RestException(StatusCodes.NotFound);
            }

            //read File
            long blobId = Long.valueOf(dbUser.getProperties().getAvatar());

            //set header
            return BlobController.getBlob(blobId, dbName);

        } finally {
            DaoUtils.closeQuietly(conn);
        }
    }

    private User.UserProp _checkInput(AccessControlManager acm, RestUser user) {

        for (RestRole restRole : user.getRoles()) {
            RbacRole rbacRole = acm.getRole(Long.parseLong(restRole.getId()));
            if (rbacRole == null) {
                throw new RestException(StatusCodes.NotFound, String.format(
                        "No such rbacrole (id=%s)", restRole.getId()
                ));
            }
        }

        if (FileUtils.base64FileSize(user.getFile()) > fileMaxByteSize) {
            throw new RestException(StatusCodes.BadRequest, "avatar max size is " + fileMaxByteSize / 1024 + "kb`");
        }

        return User.UserProp.empty();
    }

    private List<RestRole> _queryRoleByIds(Set<Long> roleIds, AccessControlManager acm) {
        if (CollectionUtils.isEmpty(roleIds)) {
            return new ArrayList<>();
        }
        List<RestRole> rests = new ArrayList<>();
        for (Long id : roleIds) {
            RbacRole r = acm.getRole(id);
            if (r != null) {
                rests.add(new RestRole(r));
            }
        }

        return rests;
    }

    private static String newRandomFileName(String suffix) {
        return UUID.randomUUID().toString().replace("-", "") + suffix;
    }

    // fileName, content
    private Pair<String, byte[]> _generateAvatarFileName(MultipartFile multipartFile) {
        if (multipartFile == null) {
            return null;
        }
        String originalFileName = multipartFile.getOriginalFilename();
        String ext = originalFileName.substring(originalFileName.lastIndexOf("."));

        //random name ext
        String newFileName = newRandomFileName(ext);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        //write file
        try {
            IOUtils.copy(multipartFile.getInputStream(), byteArrayOutputStream);
        } catch (IOException e) {
            throw new RestException(StatusCodes.InternalError, "write file error:" + e);
        }
        return ImmutablePair.of(newFileName, byteArrayOutputStream.toByteArray());
    }

    private RestUser toRestUser(User user, List<RestRole> roleList, AccountCatalog accountCatalog, Connection conn, String dbName) throws Exception {
        String creatorName = "";
        User creator = accountCatalog.getUser(user.getProperties().getCreatorId());
        if (creator != null) {
            creatorName = creator.getUsername();
        }

        return new RestUser(user, roleList, creatorName);
    }

    public static void checkPass(String pass) {

        //update 没有密码
        if (StringUtils.isBlank(pass)) {
            return;
        }

        if (pass.length() < PASS_MIN_LENGTH) {
            throw new RestException(StatusCodes.INVALID_PASS_LENGTH, PASS_MIN_LENGTH);
        }
        if (!passPattern.matcher(pass).find()) {
            throw new RestException(StatusCodes.INVALID_PASS);
        }
    }

    private void _checkLoginAccount(RestUser user) {
        String mobile = user.getMobile();
        String email = user.getEmail();
        if (StringUtils.isBlank(mobile) && StringUtils.isBlank(email)) {
            // TODO FIX ME
            throw new RestException(StatusCodes.BadRequest);
        }

        // 手机号
        if (StringUtils.isNotBlank(mobile)) {

            // invalid format
            if (!Validator.isMobile(mobile)) {
                throw new RestException(StatusCodes.INVALID_USER_MOBILE);
            }

            if (AccountCatalog.getUserAccountByMobile(mobile) != null) {
                throw new RestException(StatusCodes.MOBILE_ALREADY_EXISTS);
            }

            return;
        }

        // 邮箱
        if (StringUtils.isNotBlank(email)) {
            // 无效的邮箱
            if (!Validator.isEmail(email)) {
                throw new RestException(StatusCodes.INVALID_USER_EMAIL);
            }

            if (AccountCatalog.getUserAccountByEmail(email) != null) {
                throw new RestException(StatusCodes.EMAIL_ALREADY_EXISTS);
            }
            return;
        }

        // 都没填写
        throw new RestException(StatusCodes.INVALID_USER_MOBILE_OR_EMAIL);
    }


    private void deleteOldAvatarQuietly(Connection conn, String dbName, String oldBlobId) {
        if (oldBlobId != null) {
            long blobId = 0;
            try {
                blobId = Long.parseLong(oldBlobId);
                CommonBlobDao.delete(conn, dbName, blobId);
            } catch (Exception e) {
            }
        }
    }
}
