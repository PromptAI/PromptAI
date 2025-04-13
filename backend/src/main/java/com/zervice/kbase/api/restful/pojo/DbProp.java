package com.zervice.kbase.api.restful.pojo;

import com.alibaba.fastjson.JSONObject;
import lombok.*;

/**
 * @author Peng Chen
 * @date 2020/4/21
 */
@ToString
@Builder
@AllArgsConstructor
@Getter
@Setter
public class DbProp {

    public DbProp(){}
    /**
     * 创建人
     */
    Long _createBy;

    /**
     * 修改人
     */
    Long _updateBy;

    Long _createTime;

    Long _updateTime;

    public static DbProp parse(String properties) {
        try {
            return JSONObject.parseObject(properties, DbProp.class);
        } catch (Exception e) {
            return new DbProp();
        }
    }
}
