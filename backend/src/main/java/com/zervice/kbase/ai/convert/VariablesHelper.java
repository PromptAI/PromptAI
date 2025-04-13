package com.zervice.kbase.ai.convert;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 变量工具
 *
 * @author chenchen
 * @Date 2024/12/16
 */
public class VariablesHelper {

    /**
     * 从 text 提取用到的变量
     * <pre>
     *      比如:
     *       - "hell ${name}, welcome to ${hospital}." 可提取出 name, hospital
     *
     * </pre>
     */
    private static final Pattern _VARIABLE_PATTERN = Pattern.compile("\\$\\{(\\w+)}");

    public static Set<String> parse(String text) {
        Matcher matcher = _VARIABLE_PATTERN.matcher(text);
        Set<String> variables = new HashSet<>();
        while (matcher.find()) {
            // 捕获组1即为变量名
            variables.add(matcher.group(1));
        }

        return variables;
    }

    /**
     * 将变量包裹成引用的形式
     * - name -> ${name}
     * - agent.name -> ${agent.name}
     *
     * @param key key
     * @return value
     */
    public static String wrap(String key) {
        return "${" + key + "}";
    }
}