package com.zervice.kbase.database.criteria;

import lombok.*;

/**
 * Role查询参数
 * @author Peng Chen
 * @date 2020/3/19
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RoleCriteria {
    @Optional
    String _name;
    @Optional
    Boolean _status;
}
