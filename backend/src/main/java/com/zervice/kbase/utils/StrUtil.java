package com.zervice.kbase.utils;

import org.apache.commons.lang3.StringUtils;

/**
 * @author chenchen
 * @Date 2024/1/15
 */
public class StrUtil {

    /**
     * 取前 N 个字符
     *  例如：12345
     *  - 0 -> 抛出异常
     *  - 2 -> 12
     *  - 10  -> 12345
     *  - null -> null
     *
     * @param count 需要字符的数量 需要大于 0
     * @param text  文本
     *
     * @return 截取后的文本
     */
    public static String first(int count, String text) {
        if (count <= 0) {
            throw new IllegalArgumentException("count must gather than 0!");
        }
        if (text == null) {
            return null;
        }

        if (text.length() < count) {
            return text;
        }

        return text.substring(0, count);
    }

    /**
     * 适用于模糊搜索
     *
     * @param text       文本
     * @param searchKeys 搜索的关键字
     * @return true if matched
     */
    public static boolean containsAnyIgnoreCase(String text, String searchKeys) {
        if (text == null || searchKeys == null) {
            return false;
        }

        return StringUtils.containsAnyIgnoreCase(text, searchKeys);
    }

    /**
     * 字符串分段
     *
     * @param text t
     * @param len  最大长度
     * @return array
     */
    public static String[] split(String text, int len) {
        return cn.hutool.core.util.StrUtil.split(text, len);
    }

}
