package com.zervice.common.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper functions to convert from JSON to map or list ...
 */
@UtilityClass
public class JSONUtils {
    public boolean nullOrEmpty(JSONObject jo) {
        return jo == null || jo.isEmpty();
    }

    public String toString(JSONObject j) {
        if (j == null) {
            return "{}";
        }

       return j.toString(SerializerFeature.PrettyFormat,
                SerializerFeature.MapSortField, SerializerFeature.WriteMapNullValue,
                SerializerFeature.DisableCircularReferenceDetect);
    }
    public String toString(JSONArray j) {
        if (j == null) {
            return "{}";
        }

        return j.toString(SerializerFeature.PrettyFormat,
                SerializerFeature.MapSortField, SerializerFeature.WriteMapNullValue,
                SerializerFeature.DisableCircularReferenceDetect);
    }

    public String toString(JSONObject j, boolean beautify) {
        if (j == null) {
            return "{}";
        }

        if (beautify) {
            return j.toString(SerializerFeature.PrettyFormat);
        } else {
            return j.toString();
        }
    }

    public String toString(JSONArray j, boolean beautify) {
        if (j == null) {
            return "[]";
        }

        if (beautify) {
            return j.toString(SerializerFeature.PrettyFormat);
        } else {
            return j.toString();
        }
    }

    public JSONObject parseObject(String jsonStr) {
        if (StringUtils.isEmpty(jsonStr)) {
            return new JSONObject();
        }

        return JSON.parseObject(jsonStr);
    }

    public JSONObject toJsonObject(Object o) {
        return JSONObject.parseObject(JSONObject.toJSONString(o));
    }

    public <T> T copy(Object origin, Class<T> t) {
        return toJsonObject(origin).toJavaObject(t);
    }

    public JSONArray parseArray(String jsonStr) {
        if (StringUtils.isEmpty(jsonStr)) {
            return new JSONArray();
        }

        return JSON.parseArray(jsonStr);
    }

    public JSONObject getJSONObjectOrError(JSONObject jo, String key) {
        if (jo.containsKey(key)) {
            return jo.getJSONObject(key);
        }

        throw new MissingFieldError(key);
    }

    public JSONObject getJSONObjectOrEmpty(JSONObject jo, String key) {
        if (jo != null && jo.containsKey(key)) {
            return jo.getJSONObject(key);
        }

        return new JSONObject();
    }

    public JSONArray getJSONArrayOrError(JSONObject jo, String key) {
        if (jo.containsKey(key)) {
            return jo.getJSONArray(key);
        }

        throw new MissingFieldError(key);
    }

    public JSONArray getJSONArrayOrEmpty(JSONObject jo, String key) {
        if (jo.containsKey(key)) {
            return jo.getJSONArray(key);
        }

        return new JSONArray();
    }

    /**
     * IMPORTANT: This function assume the caller know the  type of field. It won't actually
     * be able to do data type conversion as JSONObject.getXXX
     *
     * e.g. if the field save a string "1", you cannot use this function to get an integer or
     * long 1 out of the field, but JSONObject.getLong or JSONObject.getInteger would do the
     * cast!!!
     */
    public <T> T getOrDefault(JSONObject jo, String key, T def) {
        if (jo.containsKey(key)) {
            return (T) jo.get(key);
        }

        return def;
    }

    public <T> T getJavaObject(JSONObject jo, String key, T def) {
        if(jo.containsKey(key)) {
            JSONObject obj = jo.getJSONObject(key);
            return (T)JSON.toJavaObject(obj, def.getClass());
        }

        return def;
    }

    /**
     * Convert a list of object to JSONArray
     *
     * @param items
     * @return
     */
    public <T> JSONArray toJSONArray(List<T> items) {
        JSONArray ja = new JSONArray();
        if (items != null && items.size() > 0) {
            items.forEach(ja::add);
        }
        return ja;
    }

    /**
     * Convert a JSONArray of Long to list
     *
     * @param ja
     * @return
     */
    public List<Long> asLongList(JSONArray ja) {
        List<Long> contained = new ArrayList<>(ja.size());
        for (int i = 0; i < ja.size(); i++) {
            contained.add(ja.getLong(i));
        }
        return contained;
    }

    public List<String> asStringList(JSONArray ja) {
        List<String> contained = new ArrayList<>(ja.size());
        for (int i = 0; i < ja.size(); i++) {
            contained.add(ja.getString(i));
        }
        return contained;
    }

    private final static Pattern REPLACE_TOKEN_PATTERN = Pattern.compile("\\{\\{(\\w+)}}");

    public void mergeJSONObjects(JSONObject targetJson, JSONObject sourceJson) {
        Iterator it = sourceJson.keySet().iterator();
        while (it.hasNext()) {
            String key = (String) it.next();
            String value = sourceJson.getString(key);
            Matcher matcher = REPLACE_TOKEN_PATTERN.matcher(value);
            if (matcher.find()) {
                String token = matcher.group(1);
                String tokenValue = targetJson.getString(token);
                if (tokenValue == null) {
                    tokenValue = "";
                }

                value = value.replace("{{" + token + "}}", tokenValue);
            }
            targetJson.put(key, value);
        }
    }

    /**
     * It would replace the {{token}} in targetMap using the entry in properties first, then put the uncontained entry
     * of properties into targetMap.
     *
     * @param targetMap
     * @param properties
     */
    public void replaceTokenAndFillMap(Map<String, String> targetMap, JSONObject properties) {
        for (Map.Entry<String, String> entry : targetMap.entrySet()) {
            String value = entry.getValue();
            Matcher matcher = REPLACE_TOKEN_PATTERN.matcher(value);
            if (matcher.find()) {
                String token = matcher.group(1);
                String tokenValue = properties.getString(token);
                if (tokenValue == null) {
                    tokenValue = "";
                }
                value = value.replace("{{" + token + "}}", tokenValue);
                entry.setValue(value);
            }
        }

        for (String key : properties.keySet()) {
            if (!targetMap.containsKey(key)) {
                targetMap.put(key, properties.getString(key));
            }
        }
    }

    /**
     * If two JSON objects equals?
     */
    public boolean equals(JSONObject jo1, JSONObject jo2) {
        if (jo1 == null && jo2 == null) {
            return true;
        }

        if (jo1 != null && jo2 != null) {
            return jo1.toString(SerializerFeature.MapSortField).equals(jo2.toString(SerializerFeature.MapSortField));
        }

        return false;
    }

    public boolean equalJSONField(@NonNull JSONObject jo1, @NonNull JSONObject jo2, @NonNull String key) {
        Object v1 = jo1.getOrDefault(key, null);
        Object v2 = jo2.getOrDefault(key, null);

        if (v1 == null && v2 == null) {
            return true;
        } else if (v1 != null && v2 != null) {
            // TODO: for now, compare as string ...
            return v1.toString().equals(v2.toString());
        } else {
            return false;
        }
    }

    public static class MissingFieldError extends RuntimeException {
        MissingFieldError(String field) {
            super("JSONObject missing required field '" + field + "'");
        }
    }

    public static class ObjectBuilder {
        ObjectBuilder() {
        }

        JSONObject _object = new JSONObject();

        public ObjectBuilder put(String key, Object val) {
            _object.put(key, val);
            return this;
        }

        public JSONObject build() {
            return _object;
        }
    }

    public static ObjectBuilder objectBuilder() {
        return new ObjectBuilder();
    }


    public static class ArrayBuilder {
        ArrayBuilder() {
        }

        JSONArray _array = new JSONArray();

        public ArrayBuilder add(Object val) {
            _array.add(val);
            return this;
        }

        public ArrayBuilder set(int idx, Object val) {
            _array.set(idx, val);
            return this;
        }

        public JSONArray build() {
            return _array;
        }
    }

    public final ArrayBuilder arrayBuilderBuilder() {
        return new ArrayBuilder();
    }

    private final ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(SerializationFeature.CLOSE_CLOSEABLE, false);
        objectMapper.disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
        SimpleModule simpleModule = new SimpleModule();
        objectMapper.registerModule(simpleModule);
    }

    public <T> String toJSONStr(T t) {
        try {
            return objectMapper.writer().writeValueAsString(t);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public <T> void write(Writer writer, T t) {
        try {
            objectMapper.writer().writeValue(writer, t);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public <T> byte[] write(T t) {
        try {
            return objectMapper.writer().writeValueAsBytes(t);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public <T> T readFromJSON(Class<T> clz, String str) {
        try {
            return objectMapper.reader().forType(clz).readValue(str);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public <T> T readFromJSON(Class<T> clz, byte[] json) {
        try {
            return objectMapper.reader().forType(clz).readValue(json);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public <T> T readFromJSON(TypeReference<T> t, String str) {
        try {
            return objectMapper.reader().forType(t).readValue(str);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public <T> T readFromJSON(TypeReference<T> t, byte[] data) {
        try {
            return objectMapper.reader().forType(t).readValue(data);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
