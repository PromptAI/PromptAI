package com.zervice.kbase.database.criteria;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;
import java.util.Set;

/**
 * @author chenchen
 * @Date 2023/12/12
 */
@Setter @Getter
@NoArgsConstructor
@AllArgsConstructor
public class RpcChatCriteria {

    /**
     * in mills
     */
    private Long _startTime;

    /**
     * in mills
     */
    private Long _endTime;

    /**
     * name like query
     */
    private String _flow;

    private String _projectId;

    /**
     * json
     * {"tenant":"xxx", "slots.xxx":"xx"}
     *
     * 如果没有'.',默认查询properties.variables
     */
    private String _filter;

    private Set<String>  _roots;

    private Map<String /* full path in properties*/, String /*expected value*/> _properties;
}
