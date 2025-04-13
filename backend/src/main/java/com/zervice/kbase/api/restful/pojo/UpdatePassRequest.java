package com.zervice.kbase.api.restful.pojo;

import lombok.Getter;
import lombok.Setter;

/**
 * user update password request
 *
 * @author Peng Chen
 * @date 2020/2/18
 */
@Setter
@Getter
public class UpdatePassRequest {

    public static final String TYPE_SMS = "sms";
    public static final String TYPE_EMAIL = "email";
    public static final String TYPE_PWD = "pwd";

    private String _oldPass;

    private String _newPass;

    /**
     * sms code
     */
    private String _code;

    private String _type = TYPE_PWD;
}
