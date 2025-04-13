package com.zervice.kbase.api.restful.pojo.mica;

import lombok.*;

/**
 * Mica的 set slot结构
 *
 * @author chen
 * @date 2022/10/8
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlotPojo {

    private String _componentId;
    private String _slotId;

    private String _slotName;

    private String _slotDisplay;

    private String _value;

    private String _action;

    public static SlotPojo factory(String slotId, String vale) {
        return SlotPojo.builder()
                .slotId(slotId)
                .value(vale)
                .build();
    }
}
