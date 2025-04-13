package com.zervice.kbase.service;

import com.zervice.kbase.api.restful.pojo.VerificationCode;

public interface VerificationCodeService {

    /**
     * 验证
     * @param code 验证码
     */
    boolean validated(String uuid, VerificationCode code);

    void registerNewCode(String uuid, VerificationCode code);
}
