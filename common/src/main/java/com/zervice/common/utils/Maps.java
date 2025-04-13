package com.zervice.common.utils;

import java.util.HashMap;
import java.util.Map;

public class Maps {
    public static Map<String, String> of(String k, String v) {
        Map<String, String> map = new HashMap<>(1);
        map.put(k, v);
        return map;
    }

    public static Map<String, String> of(String k, String v, String k2, String v2) {
        Map<String, String> map = new HashMap<>(2);
        map.put(k, v);
        map.put(k2, v2);
        return map;
    }

    public static Map<String, String> of(String k, String v, String k2, String v2, String k3, String v3) {
        Map<String, String> map = new HashMap<>(4);
        map.put(k, v);
        map.put(k2, v2);
        map.put(k3, v3);
        return map;
    }

    public static Map<String, String> of(String k, String v, String k2, String v2, String k3, String v3, String k4, String v4) {
        Map<String, String> map = new HashMap<>(4);
        map.put(k, v);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        return map;
    }

}
