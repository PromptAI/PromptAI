package com.zervice.kbase.utils;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;

/**
 * convert yml to json object or string
 */
public class YamlToJsonConverter {

    /**
     * convert yaml text to JSONObject
     *
     * @param yamlContent yml text
     * @return JSONObject
     * @throws IOException e
     */
    public static JSONObject convertYamlToJson(String yamlContent) throws IOException {
        String jsonStr = convertYamlToJsonStr(yamlContent);
        return JSONObject.parseObject(jsonStr);
    }

    /**
     * convert yaml text to JSONObject
     *
     * @param yamlContent yml text
     * @return Json string
     * @throws IOException e
     */
    public static String convertYamlToJsonStr(String yamlContent) throws IOException {
        // 创建YAML解析器
        ObjectMapper yamlReader = new ObjectMapper(new YAMLFactory());
        // 创建JSON写入器
        ObjectMapper jsonWriter = new ObjectMapper();
        jsonWriter.enable(SerializationFeature.INDENT_OUTPUT); // 美化输出

        JsonNode node = yamlReader.readTree(yamlContent);

        // 转换为JSON字符串并返回
        return jsonWriter.writeValueAsString(node);
    }
}
