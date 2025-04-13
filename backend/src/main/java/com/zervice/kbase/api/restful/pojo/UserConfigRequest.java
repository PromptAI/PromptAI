package com.zervice.kbase.api.restful.pojo;

import lombok.Data;

/**
 *
 * @author admin
 * @date 2023/2/20
 */
@Data
public class UserConfigRequest {
    /**
     * 语言
     */
    private String _language;
    /**
     * 时区 默认从account读取
     */
    private String _timeZone;
}
