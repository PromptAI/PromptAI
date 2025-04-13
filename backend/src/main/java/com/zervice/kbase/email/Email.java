package com.zervice.kbase.email;

import com.alibaba.fastjson.JSONObject;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Data @Builder
public class Email {
    public static final String BODY_TYPE_PLAIN = "plain";
    public static final String BODY_TYPE_HTML = "html";

    // INVITATION, DELETION, SUSPEND, ALERT_NOTIFY, ALERT_CLEAR
    public static final String TYPE_ACCOUNT_REQUEST_NOTIFY = "accountRequestNotify";
    public static final String TYPE_ACCOUNT_REQUEST_REJECT = "accountRequestReject";
    public static final String TYPE_ACTIVATION = "activation";
    public static final String TYPE_INVITATION = "invitation";
    public static final String TYPE_RESET_PASSWORD = "resetPassword";
    public static final String TYPE_DELETION = "deletion";
    public static final String TYPE_SUSPEND = "suspend";
    public static final String TYPE_ALERT_NOTIFY = "alertNotify";
    public static final String TYPE_ALERT_CLEAR = "alertClear";

    public static final String TYPE_REGISTER_CODE = "registerCode";
    public static final String TYPE_LOGIN_CODE = "loginCode";
    public static final String TYPE_MODIFY_CODE = "modifyCode";
    public static final String TYPE_FUNCTION_ANNOUNCE = "functionAnnounce";
    public static final String TYPE_ACCOUNT_CREATED = "accountCreated";
    public static final String TYPE_ACCOUNT_REST_TOKEN_NOT_ENOUGH = "restTokenNotEnough";

    private final String _type;

    @Builder.Default
    private final List<String> _to = new ArrayList<>();
    @Builder.Default
    private final List<String> _cc = new ArrayList<>();
    @Builder.Default
    private final List<String> _bcc = new ArrayList<>();

    private final String _subject;
    private final String _body;
    private final String _htmlBody;

    @Builder.Default
    private final String _bodyType = BODY_TYPE_PLAIN;

    public Email addTo(String to) {
        _to.add(to);
        return this;
    }

    public Email addCc(String cc) {
        _cc.add(cc);
        return this;
    }

    public Email addBcc(String bcc) {
        _bcc.add(bcc);
        return this;
    }

    public boolean hasRecipients() {
        return _to.size() > 0 || _cc.size() > 0 || _bcc.size() > 0;
    }

    public String getRecipients() {
        if(_cc.isEmpty() && _bcc.isEmpty()) {
            return StringUtils.join(_to, ",");
        }

        JSONObject jo = new JSONObject();
        jo.put("to", StringUtils.join(_to, ","));
        jo.put("cc", StringUtils.join(_cc, ","));
        jo.put("bcc", StringUtils.join(_bcc, ","));

        return jo.toString();
    }
}
