package com.zervice.kbase.api.restful.pojo.mica;

import lombok.*;

/**
 * Condition
 *
 * @author chen
 * @date 2022/10/7
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConditionPojo {

    public static final String TYPE_IS_EMPTY = "isEmpty";
    public static final String TYPE_IS_NOT_EMPTY = "isNotEmpty";
    public static final String TYPE_NOT_EQUAL = "notEqual";
    public static final String TYPE_EQUAL = "equal";
    public static final String TYPE_GREATER_THAN = "greaterThan";
    public static final String TYPE_GREATER_THAN_OR_EQUAL = "greaterThanOrEqual";
    public static final String TYPE_LESS_THAN = "lessThan";
    public static final String TYPE_LESS_THAN_OR_EQUAL = "lessThanOrEqual";
    public static final String TYPE_REGEX = "regex";
    public static final String TYPE_CONTAINS = "contains";
    public static final String TYPE_NOT_CONTAINS = "notContains";
    public static final String TYPE_STARTS_WITH = "startsWith";
    public static final String TYPE_ENDS_WITH = "endsWith";


    private String _slotName;

    private String _slotDisplay;

    private String _slotId;

    /**
     * not none 、None 、Regex、比较 等
     */
    private String _type;

    private String _value;
}
