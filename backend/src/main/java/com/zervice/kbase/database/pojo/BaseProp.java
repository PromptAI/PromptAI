package com.zervice.kbase.database.pojo;

import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@ToString
@SuperBuilder
@AllArgsConstructor
@Getter
@Setter
public class BaseProp {
    public BaseProp() {
    }

    /**
     * 创建人
     */
    @Builder.Default
    private Long _createBy = 0L;

    /**
     * 修改人
     */
    @Builder.Default
    private Long _updateBy = 0L;
    @Builder.Default
    private Long _createTime = 0L;
    @Builder.Default
    private Long _updateTime = 0L;


    public static BaseProp parse(String properties) {
        try {
            return JSONObject.parseObject(properties, BaseProp.class);
        } catch (Exception e) {
            return new BaseProp();
        }
    }
}
