package com.zervice.kbase.database.criteria;

import lombok.Getter;
import lombok.Setter;

/**
 * @author chen
 * @date 2023/5/5 10:25
 */
@Setter@Getter
public class AgentCriteria {

    private Integer _status;

    // 内部使用
    /**
     * db 名称
     */
    private String _dbName;
}
