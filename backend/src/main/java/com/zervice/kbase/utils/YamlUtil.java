package com.zervice.kbase.utils;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

/**
 * @author chenchen
 * @Date 2024/9/6
 */
public class YamlUtil {
    public static String dump(Object o, boolean pretty) {
        String dump = dump(o);
        if (pretty) {
            return prettyYaml(dump);
        }
        return dump;
    }

    public static String dump(Object o) {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK); // 使用块格式
        options.setPrettyFlow(true); // 启用更人性化的格式
        options.setIndent(2); // 设置缩进为 2 个空格
        options.setIndicatorIndent(0); // 列表和块样式的缩进一致
        options.setLineBreak(DumperOptions.LineBreak.UNIX); // 使用 UNIX 换行符

        // 创建 Yaml 实例
        Yaml yaml = new Yaml(options);
        return removeEmptyLines(yaml.dump(o));
    }

    public static String removeEmptyLines(String yaml) {
        return yaml.replace(": ''", "");
    }

    public static String prettyYaml(String yaml) {
        String[] lines = yaml.split("\n");
        StringBuilder output = new StringBuilder(lines[0]); // 保留第一行

        for (int i = 1; i < lines.length; i++) {
            if (!lines[i].isEmpty() && !Character.isWhitespace(lines[i].charAt(0))) {
                output.append("\n"); // 在当前行前加换行符
            }
            output.append("\n").append(lines[i]);
        }

        return output.toString();
    }

    public static <T> T load(String yamlStr, Class<T> clazz) {
        Yaml yaml = new Yaml();
        return yaml.loadAs(yamlStr, clazz);
    }
}
