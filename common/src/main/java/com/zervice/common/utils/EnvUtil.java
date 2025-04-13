package com.zervice.common.utils;

import org.apache.commons.lang3.StringUtils;

public class EnvUtil {
    public static String getEnv(String key, String defaultValue) {
        String value = System.getenv(key);
        return StringUtils.isNotBlank(value) ? value : defaultValue;
    }

    /**
     * try with priority :
     * - from System.getenv
     * - from System.getProperty
     *
     */
    public static String getEnvOrProp(String key, String defaultValue) {
        String value = System.getenv(key);
        if (StringUtils.isNotBlank(value)) {
            return value;
        }

        return System.getProperty(key, defaultValue);
    }
}