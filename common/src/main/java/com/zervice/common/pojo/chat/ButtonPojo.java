package com.zervice.common.pojo.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;

/**
 * conversation 转button的结构
 *
 * @author chen
 * @date 2022/10/14
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Log4j2
public class ButtonPojo {

    public static ButtonPojo factory(String id, String name, Boolean hidden) {
        return ButtonPojo.builder()
                .id(id).name(name).hidden(hidden)
                .build();
    }

    /**
     * 节点id
     */
    private String _id;

    /**
     * 节点名称
     */
    private String _name;

    /**
     * 是否折叠
     */
    private Boolean _hidden;
}
