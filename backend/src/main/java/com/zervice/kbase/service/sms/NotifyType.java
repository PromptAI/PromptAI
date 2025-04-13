package com.zervice.kbase.service.sms;

/**
 * If use tencent as sms provider,
 * it requires the templates. and you can only input some args.
 * See {@link SMSSender#_tencentTemplateID(NotifyType)}
 */
public enum NotifyType {

    /**
     * login via
     */
    Login(1),

    /**
     * reset password
     */
    ResetPwd(1),

    Unknown(0);

    private final int _value;

    NotifyType(int val) {
        _value = val;
    }

    public int value() {
        return _value;
    }

    @Override
    public String toString() {
        if (_value == 1) {
            return "Register";
        } else if (_value == 2) {
            return "Login";
        } else if (_value == 3) {
            return "Account Expiring";
        } else if (_value == 4) {
            return "Account Expired";
        } else if (_value == 5) {
            return "Reset Pwd";
        } else if (_value == 6) {
            return "Apply Passed";
        } else if (_value == 7) {
            return "Account Created";
        } else if (_value == 8) {
            return "Release Account Docker Resource Notified";
        } else if (_value == 9) {
            return "Finished Release Account Docker Resource Notified";
        }

        return "Unknown";
    }
}
