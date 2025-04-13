package com.zervice.kbase.llm.pojo.openai;

import com.alibaba.fastjson.JSONObject;
import lombok.*;

import java.util.List;

/**
 * embedding request
 *
 * @author chenchen
 * @Date 2023/10/16
 */
@Builder
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class OpenAIEmbedding {
    public static final String MODEL_ADA_002 = "text-embedding-ada-002";

    private String _model;

    /**
     * string or json string array
     */
    private Object _input;

    public static OpenAIEmbedding factory(String text) {
        return factory(List.of(text));
    }

    public static OpenAIEmbedding factory(List<String> texts) {
        return OpenAIEmbedding.builder()
                .input(texts)
                .model(MODEL_ADA_002)
                .build();
    }

    public JSONObject toJSON() {
        return JSONObject.parseObject(JSONObject.toJSONString(this));
    }
}
