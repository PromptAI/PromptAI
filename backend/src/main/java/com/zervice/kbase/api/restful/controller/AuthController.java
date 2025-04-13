package com.zervice.kbase.api.restful.controller;

import com.alibaba.fastjson.JSONObject;
import com.wf.captcha.ArithmeticCaptcha;
import com.zervice.common.pojo.common.EmptyResponse;
import com.zervice.common.restful.exception.RestException;
import com.zervice.common.restful.exception.StatusCodes;
import com.zervice.kbase.AccountCatalog;
import com.zervice.kbase.api.BaseController;
import com.zervice.kbase.api.restful.AuthFilter;
import com.zervice.kbase.api.restful.pojo.RestUser;
import com.zervice.kbase.api.restful.pojo.VerificationCode;
import com.zervice.kbase.database.dao.UserDao;
import com.zervice.kbase.database.pojo.User;
import com.zervice.kbase.database.utils.DaoUtils;
import com.zervice.kbase.jwt.JwtTokenUtil;
import com.zervice.kbase.service.LoginService;
import com.zervice.kbase.service.VerificationCodeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.Cleanup;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Log4j2
@RestController
@RequestMapping("/api/auth")
public class AuthController extends BaseController {
    private LoginService loginService;

    @Autowired
    public AuthController(LoginService loginService) {
        this.loginService = loginService;
    }

    @Autowired
    private VerificationCodeService verificationCodeService;


    @GetMapping("refresh/token")
    public Object get(HttpServletRequest request,
                      @RequestHeader(AuthFilter.X_EXTERNAL_USER_ID_HEADER) String uid,
                      @RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName) throws Exception{
        long userId = User.fromExternalId(uid);
        return loginService.refreshToken(request, userId, dbName);
    }

    @PostMapping("reset/pass")
    public Object resetPass(@RequestBody JSONObject req,
                             HttpSession session) throws Exception{
        String email = req.getString("email");
        String code = req.getString("code");
        String newPass = req.getString("newPass");
        if (StringUtils.isBlank(email) || StringUtils.isBlank(code) || StringUtils.isBlank(newPass)) {
            throw new RestException(StatusCodes.BadRequest);
        }

        // make sure pass is valid
        UserController.checkPass(newPass);

        AccountCatalog accountCatalog = AccountCatalog.getUserAccountByEmail(email);
        if (accountCatalog == null) {
            throw new RestException(StatusCodes.USER_NOT_EXISTS);
        }

        User user = accountCatalog.findUserByEmail(email);
        if (user == null) {
            throw new RestException(StatusCodes.USER_NOT_EXISTS);
        }

        NotifyController.verifySecInfo(session, email, code);

        String dbName = accountCatalog.getDBName();

        @Cleanup Connection conn = DaoUtils.getConnection(false);
        User dbUser = UserDao.get(conn, dbName, user.getId());

        dbUser.setPassword(newPass);
        dbUser.getProperties().setInitPass("");
        UserDao.updatePassword(conn, dbName, dbUser);

        conn.commit();
        LOG.info("[{}][user:{} success find pass via email]", dbName, dbUser.getId());
        return EmptyResponse.empty();
    }


    @PostMapping("login")
    public Object login(HttpServletRequest request) throws Exception {
        return loginService.login(request);
    }

    @DeleteMapping("logout")
    public EmptyResponse logout(HttpSession session) {
        return loginService.logout(session);
    }

    @GetMapping("info")
    public RestUser info(@RequestHeader(AuthFilter.TOKEN) String token,
                         @RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName) throws Exception {
        Long userId = JwtTokenUtil.getUserIdByToken(token);

        RestUser user = loginService.info(userId, dbName);

        return user;
    }

    @GetMapping(value = "/code")
    public ResponseEntity<Object> getCode() {
        // 算术类型 https://gitee.com/whvse/EasyCaptcha
        String result = null;
        ArithmeticCaptcha captcha = null;
        while (true) {
            captcha = new ArithmeticCaptcha(111, 36);
            // 几位数运算，默认是两位
            captcha.setLen(2);
            // 获取运算的结果
            result = captcha.text();
            if (!result.startsWith("-")) {
                // 不使用负数结果
                break;
            }
        }
        String uuid = UUID.randomUUID().toString();

        // 验证码信息
        Map<String,Object> imgResult = new HashMap<>(2);
        imgResult.put("img", captcha.toBase64());
        imgResult.put("uuid", uuid);

        verificationCodeService.registerNewCode(uuid, new VerificationCode(result));

        return ResponseEntity.ok(imgResult);
    }

    public static User resolveIdentity(AccountCatalog account, String identity) {
        if(identity.startsWith(AuthFilter.ID_EMAIL)) {
            return account.findUserByEmail(identity.substring(AuthFilter.ID_EMAIL.length()));
        }
        else {
            return account.findUserByMobile(identity.substring(AuthFilter.ID_MOBILE.length()));
        }
    }
}
