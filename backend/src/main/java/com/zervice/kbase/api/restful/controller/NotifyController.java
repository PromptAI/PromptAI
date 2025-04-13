package com.zervice.kbase.api.restful.controller;

import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson.JSONObject;
import com.google.common.util.concurrent.RateLimiter;
import com.zervice.common.pojo.common.EmptyResponse;
import com.zervice.common.restful.exception.RestException;
import com.zervice.common.restful.exception.StatusCodes;
import com.zervice.common.utils.JSONUtils;
import com.zervice.common.utils.LoginUtils;
import com.zervice.kbase.AccountCatalog;
import com.zervice.kbase.api.BaseController;
import com.zervice.kbase.api.restful.AuthFilter;
import com.zervice.kbase.database.dao.UserDao;
import com.zervice.kbase.database.pojo.User;
import com.zervice.kbase.database.utils.DaoUtils;
import com.zervice.kbase.email.Email;
import com.zervice.kbase.email.EmailRender;
import com.zervice.kbase.email.EmailService;
import com.zervice.kbase.service.sms.NotifyType;
import lombok.Cleanup;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.sql.Connection;
import java.util.Map;
import java.util.Objects;

/**
 * Manage notifications that can be public accessible
 *
 * Whenever we want to send SMS notification, we would challenge user with a captcha
 * in a two-step verification fashion:
 * 1) user input SMS
 * 2) user click "Get SMS Code"
 * 3) UI would popup the slide captcha (call /api/notify/captcha/get)
 * 4) User shall solve the puzzle      (call /api/notify/captcha/check)
 * 5) UI then a call one the notify API to initiate sending SMS
 *    {
 *        mobile: xxx,
 *        captcha: {
 *            ... // as required by the captcha
 *        }
 *    }
 *
 * TODO: for the captcha, we now rely on sessions, which would be a problem
 *       to scale our service. Let's fix this when we need to
 */
@Log4j2
@RestController
@RequestMapping("/api/notify")
public class NotifyController extends BaseController {

    /**
     * 目前一分钟允许发10条短信
     */
    private static final double _SMS_ALLOW_PER_SEC = 10 / (60 * 1.0);
    private static final RateLimiter SMS_LIMITER = RateLimiter.create(_SMS_ALLOW_PER_SEC);

    @PostMapping("forget/pwd")
    public JSONObject forgetPwd(HttpServletRequest request,
                                @RequestBody JSONObject body) {
        String type = body.getString("type");
        if (StringUtils.isBlank(type)) {
            LOG.error("[unknown type:{}]", type);
            throw new RestException(StatusCodes.BadRequest);
        }

        String email = body.getString("email");
        if (StringUtils.isBlank(email)) {
            LOG.error("[unknown email:{}]", type);
            throw new RestException(StatusCodes.BadRequest);
        }

        if ("email".equals(type)) {
            AccountCatalog accountCatalog = AccountCatalog.getUserAccountByEmail(email);
            if (accountCatalog == null) {
                throw new RestException(StatusCodes.USER_NOT_EXISTS);
            }

            User user = accountCatalog.findUserByEmail(email);
            if (user == null) {
                throw new RestException(StatusCodes.USER_NOT_EXISTS);
            }

            JSONObject info = new JSONObject(Map.of("email", email));
            return validateAndSendEmail(request, info, NotifyType.ResetPwd, true, false);
        }

        throw new RestException(StatusCodes.BadRequest);
    }
    /**
     * 登录后重置密码
     *
     * @return
     */
    @PostMapping("reset/pwd")
    public JSONObject restPwd(HttpServletRequest request,
                              @RequestBody String body,
                              @RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName,
                              @RequestHeader(AuthFilter.X_EXTERNAL_USER_ID_HEADER) String uid) throws Exception {
        long userId = User.fromExternalId(uid);

        @Cleanup Connection conn = DaoUtils.getConnection(true);
        User user = UserDao.get(conn, dbName, userId);
        if (user == null) {
            throw new RestException(StatusCodes.ACCOUNT_NOT_EXISTS);
        }
        JSONObject req = JSONObject.parseObject(body);
        String type = JSONUtils.getOrDefault(req, "type", "email");

        if ("email".equals(type)) {
            String email = user.getEmail();

            JSONObject info = new JSONObject(Map.of("email", email));
            return validateAndSendEmail(request, info, NotifyType.ResetPwd, true, false);
        }

        // not supported type
        throw new RestException(StatusCodes.BadRequest);
    }

    /**
     * post a login SMS code
     * input:
     *    mobile: 13800138000
     *    captcha: {
     *        ... // as required by captcha
     *    }
     */
    @PostMapping("login")
    public JSONObject login(HttpServletRequest request, @RequestBody JSONObject info) {
        String type = JSONUtils.getOrDefault(info, "type", "email");

        if ("email".equals(type)) {
            return validateAndSendEmail(request, info, NotifyType.Login, true, false);
        }

        // not supported type
        throw new RestException(StatusCodes.BadRequest);
    }

    /**
     * 在多阶段需要校验时，提供这个预校验api完成
     */
    @PostMapping("pre/check")
    public Object preCheck(HttpSession session,@RequestBody JSONObject info) {
        String token = info.getString("token");
        String code = info.getString("code");
        preVerifySecInfo(session, token, code);
        return EmptyResponse.empty();
    }

    /**
     * When user try to register via Email
     * @param request
     * @param info
     * @param type
     * @return
     */
    public static JSONObject validateAndSendEmail(HttpServletRequest request, JSONObject info, NotifyType type, boolean existing, boolean businessEmailOnly) {
        String email = JSONUtils.getOrDefault(info, "email", "");

        // for now, Not a valid email #!!!!
        if (StringUtils.isEmpty(email) || !LoginUtils.isValidEmail(email)) {
            LOG.error("Invalid email - " + email);
            throw new RestException(StatusCodes.INVALID_USER_EMAIL);
        }

        if (businessEmailOnly && !LoginUtils.isBusinessEmail(email)) {
            LOG.error("Non-business email - " + email);
            throw new RestException(StatusCodes.Forbidden);
        }

        if (existing && (AccountCatalog.getUserAccountByEmail(email) == null)) {
            LOG.warn("Try sending email to non-existing user - " + email);
            throw new RestException(StatusCodes.ACCOUNT_NOT_EXISTS);
        } else if (!existing && (AccountCatalog.getUserAccountByEmail(email) != null)) {
            LOG.error("Try sending register Email to already existing user - " + email);
            throw new RestException(StatusCodes.ACCOUNT_ALREADY_EXISTS, email);
        }

        HttpSession session = request.getSession();

        Pair<String, String> pair = _generateCode(session, false);

        Email e;
        switch (type) {
            case Login:
                e = EmailRender.renderLoginEmail(email, pair.getRight(), pair.getLeft());
                break;
            case ResetPwd:
                e = EmailRender.renderRestPwdEmail(email, pair.getRight(), pair.getLeft());
                break;
            default:
                throw new RestException(StatusCodes.Forbidden);
        }

        e.addTo(email);

        EmailService.getInstance().sendEmail(e);

        // send success, let's set session and return order
        session.setAttribute("SEC_TOKEN", email);
        session.setAttribute("SEC_CODE", pair.getRight());
        session.setAttribute("SEC_TIME", (System.currentTimeMillis() / 1000) + 120);  // expiring time in seconds

        return new JSONObject(Map.of("order", pair.getLeft(), "type", "email", "email", email));
    }

    public static void verifySecInfo(HttpSession session, String token, String code) {
        preVerifySecInfo(session, token, code);
        session.removeAttribute("SEC_TOKEN");
        session.removeAttribute("SEC_CODE");
    }

    /**
     * 预先校验一次，成功后可在此校验
     */
    public static void preVerifySecInfo(HttpSession session, String token, String code) {
        // now let's check code ...
        Object smsTime = session.getAttribute("SEC_TIME");
        long nowInSecs = System.currentTimeMillis() / 1000;
        if (smsTime == null || ((long) smsTime < nowInSecs) ||
                !Objects.equals(token, session.getAttribute("SEC_TOKEN")) ||
                !Objects.equals(code, session.getAttribute("SEC_CODE"))) {
            LOG.error("User:{}, registration SMS code:{} invalid", token, code);
            throw new RestException(StatusCodes.InvalidCode);
        }
    }

    /**
     * // we generate two random codes, one for displaying purpose, one for sending to mobile. Let's fix the
     *     // the code to mobile as 0000 for testing purpose for now
     * @param session
     * @return
     */
    private static Pair<String /*order*/, String /*code*/> _generateCode(HttpSession session, boolean onlyNumber) {
        int order = 1;
        Object tmp = session.getAttribute("SEC_ORDER");
        if (tmp instanceof Integer) {
            order = (int) tmp + 1;
            if (order > 99) {
                order = 1;
            }
        }

        session.setAttribute("SEC_ORDER", order);

        String code = generateNotifyCode(onlyNumber);

        return new ImmutablePair<>(String.format("%02d", order), code);
    }

    /* generate between 1000 and 9999 */
    public static String generateNotifyCode(boolean onlyNumber) {
        if (onlyNumber) {
            // 短信验证码要求纯数字，且不超过6位
            return  RandomUtil.randomString("1234567890", 6);
        }
        return  RandomUtil.randomString("ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890", 8);
    }
}