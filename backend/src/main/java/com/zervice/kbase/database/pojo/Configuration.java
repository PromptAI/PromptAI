package com.zervice.kbase.database.pojo;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 系统配置
 *
 * @author Peng Chen
 * @date 2020/6/17
 */
@ToString
@Builder
@Getter
@Setter
public class Configuration {

    public static String ACCOUNT_OWNER = "account_owner";
    /**
     * 微信二维码地址
     */
    public static String GROUP_QRCODE_WECHAT = "group_qrcode_wechat";


    /**
     * 试用申请是否需要审核
     */
    public static String TRIAL_APPLY_AUDIT = "trial.apply.need.audit";

    /**
     * llm设置
     */
    public static String LLM_CONFIG = "llm.config";

    // core db
    public static String AIO_LICENSE= "aio.config";

    String _name;

    String _value;

    /**
     * core db
     */
    public static Configuration createConfiguration(String name, String value) {
        Configuration configuration = Configuration.builder()
                .name(name).value(value)
                .build();
        return configuration;
    }
}
