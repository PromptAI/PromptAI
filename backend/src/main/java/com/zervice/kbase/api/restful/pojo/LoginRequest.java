package com.zervice.kbase.api.restful.pojo;

import lombok.Getter;
import lombok.Setter;

public class LoginRequest {

    public static final String TYPE_SMS = "sms";
    public static final String TYPE_EMAIL = "email";
    public static final String TYPE_PASSCODE = "passcode";

    @Setter
    @Getter
    private String _username;

    @Setter
    @Getter
    private String _password;

    @Setter
    @Getter
    private String _uuid;

    @Setter
    @Getter
    private String _code;

    @Setter@Getter
    public String _type  = TYPE_PASSCODE;
}
