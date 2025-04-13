package com.zervice.common.model;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.*;

import java.io.Writer;

/**
 * sse kbqa res
 * @author chen
 * @date 2023/3/30 15:10
 */
@Builder
@Setter
@Getter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class SseKbQaMessageRes {
    public static final String PREFIX = "data:";
    /** 结束标志符号 */
    public static final String ENDING = "[DONE]";

    /** 类型 - 消息 */
    public static String TYPE_MESSAGE = "message";
    /** 类型 - embedding */
    public static String TYPE_EMBEDDING = "embedding";

    private String _type;

    private Object _data;

    public static void writeSseEnd(Writer writer) throws Exception {
        writer.write("data:" + SseKbQaMessageRes.ENDING + "\n\n");
        writer.flush();
    }

    public static void writeSseMessage(Writer writer, String message) throws Exception {
        SseKbQaMessageRes res = SseKbQaMessageRes.message(message);
        writer.write("data:" + JSONObject.toJSONString(res) + "\n\n");
        writer.flush();
    }

    public static void writeSseMessageWithEnd(Writer writer, String message) throws Exception {
        writeSseMessage(writer, message);
        writeSseEnd(writer);
    }

    public static void writeSseEmbedding(Writer writer, JSONArray embedding) throws Exception {
        SseKbQaMessageRes res = SseKbQaMessageRes.embedding(embedding);
        writer.write("data:" + JSONObject.toJSONString(res) + "\n\n");
        writer.flush();
    }

    public static SseKbQaMessageRes factory(String type, Object data) {
        return SseKbQaMessageRes.builder()
                .type(type).data(data)
                .build();
    }

    public static SseKbQaMessageRes message(Object data) {
        return SseKbQaMessageRes.builder()
                .type(TYPE_MESSAGE).data(data)
                .build();
    }

    public static SseKbQaMessageRes embedding(JSONArray data) {
        return SseKbQaMessageRes.builder()
                .type(TYPE_EMBEDDING).data(data)
                .build();
    }
}
