package com.zervice.common;

import com.google.common.base.Splitter;
import com.zervice.common.utils.LayeredConf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShareConfig {
    public static final String KEY_RPC_AUTH_CODE = "rpc.auth.code";
    //默认rpc 认证码
    public static final String DEFAULT_AUTH_CODE = "474c8a9039f4849b1f0d48c80e8e9ca8";


    public static final List<String> getZpCustomTags() {
        String customTags = LayeredConf.getString("zp.customTags", "");
        List<String> tagNameAndValues = new ArrayList<>();
        Splitter.on(";").omitEmptyStrings().splitToList(customTags)
                .forEach(nameAndValue -> {
                    int index = nameAndValue.indexOf("=");
                    if (index > 0) {
                        tagNameAndValues.add(nameAndValue.substring(0, index));
                        tagNameAndValues.add(nameAndValue.substring(index + 1));
                    }
                });
        return tagNameAndValues;
    }

    public static final Map<String, String> getZpCustomTagsMap() {
        String customTags = LayeredConf.getString("zp.customTags", "");
        Map<String, String> tagNameAndValues = new HashMap<>();
        Splitter.on(";").omitEmptyStrings().splitToList(customTags)
                .forEach(nameAndValue -> {
                    int index = nameAndValue.indexOf("=");
                    if (index > 0) {
                        tagNameAndValues.put(nameAndValue.substring(0, index), nameAndValue.substring(index + 1));
                    }
                });
        return tagNameAndValues;
    }
}
