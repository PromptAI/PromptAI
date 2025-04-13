package com.zervice.common.config;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.Map;

@Configuration
public class JacksonConfig {
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jackson2ObjectMapperBuilderCustomizer() {
        return builder -> {
            // Long 会自定转换成 String
            builder.serializerByType(Long.class, ToStringSerializer.instance);
        };
    }

    @Bean
    public ObjectMapper jsonObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        SimpleModule module = new SimpleModule("org.json");
        module.addSerializer(JSONObject.class, new JsonSerializer<JSONObject>() {
            @Override
            public void serialize(JSONObject value, JsonGenerator jgen, SerializerProvider provider) throws IOException {

                // in case of a map which key is integer eg: map [1, "v1"]
                // if not set this feature, it will generate invalid json
                String json = value.toString(SerializerFeature.WriteNonStringKeyAsString);
                jgen.writeRawValue(json);
            }
        });

        // 解决long返回给前端精度丢失问题
        module.addSerializer(Long.class, ToStringSerializer.instance);
        module.addSerializer(Long.TYPE, ToStringSerializer.instance);

        module.addSerializer(JSONArray.class, new JsonSerializer<JSONArray>() {
            @Override
            public void serialize(JSONArray value, JsonGenerator jgen, SerializerProvider serializers) throws IOException {
                String json = value.toString(SerializerFeature.WriteNonStringKeyAsString);
                jgen.writeRawValue(json);
            }
        });

        module.addDeserializer(JSONObject.class, new JsonDeserializer<JSONObject>() {
            @Override
            public JSONObject deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
                Map<String, Object> bean = jp.readValueAs(new TypeReference<Map<String, Object>>() {
                });
                return new JSONObject(bean);
            }
        });

        module.addDeserializer(JSONArray.class, new JsonDeserializer<JSONArray>() {
            @Override
            public JSONArray deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
                ObjectMapper mapper = (ObjectMapper) jp.getCodec();
                ArrayNode root = mapper.readTree(jp);
                return JSONArray.parseArray(root.toString());
            }
        });

        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        mapper.registerModule(module);

        return mapper;
    }
}
