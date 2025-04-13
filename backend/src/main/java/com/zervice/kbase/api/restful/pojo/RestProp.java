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
@NoArgsConstructor
public class RestProp {

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
    /**
     * 修改和创建人的姓名
     */
    String _createByName;
    String _updateByName;


    public static RestProp fromAuditProp(DbProp auditProp) {
        RestProp r = RestProp.builder()
                .createBy(auditProp.getCreateBy())
                .updateBy(auditProp.getUpdateBy())
                .createTime(auditProp.getCreateTime())
                .updateTime(auditProp.getUpdateTime())
                .build();
        return r;
    }

    public static RestProp parse(String prop) {
        return JSONObject.parseObject(prop, RestProp.class);
    }


}
