package com.zervice.common.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author chen
 * @date 2023/3/8
 */
public class RegexUtils {

    private static final Pattern VariablePattern = Pattern.compile(".*\\{.+\\}.*");

    /**
     * 构建 ${xxx},可以直接使用String#replaceAll
     */
    public static String buildReferenceKey(String origin) {
        return "\\$\\{" + origin + "\\}";
    }

    /**
     * 构建 {xxx},可以直接使用replaceAll
     */
    public static String buildReferenceKeyWithout$(String origin) {
        return "\\{" + origin + "\\}";
    }


}
