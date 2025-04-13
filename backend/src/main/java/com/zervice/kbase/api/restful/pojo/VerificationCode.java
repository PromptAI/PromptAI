package com.zervice.kbase.api.restful.pojo;


import java.io.Serializable;

public class VerificationCode  implements Serializable {


    private final String code;

    /** 创建日期 */
    private final long createTimeInMills = System.currentTimeMillis();

    public VerificationCode(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public long getCreateTimeInMills() {
        return createTimeInMills;
    }
}
