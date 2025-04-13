package com.zervice.kbase.api.restful.pojo;

import lombok.*;

/**
 * @author chen
 * @date 2022/11/10
 */
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestTaskStep {

    private String _type;

    private Integer _retryTimes;

}
