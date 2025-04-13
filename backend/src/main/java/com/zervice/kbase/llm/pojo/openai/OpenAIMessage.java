package com.zervice.kbase.llm.pojo.openai;

import com.alibaba.fastjson.JSONObject;
import com.zervice.kbase.llm.pojo.History;
import lombok.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author chenchen
 * @Date 2023/8/31
 */
@Builder
@Setter@Getter
@AllArgsConstructor
@NoArgsConstructor
public class OpenAIMessage {

    public static OpenAIMessage Gpt3(List<History> histories) {
        List<Msg> msgs = histories.stream()
                .map(History::toMsg)
                .collect(Collectors.toList());

        return factory(_MODEL_3_5_16K, 0.0, 1, msgs);
    }
    public static OpenAIMessage Gpt3(String content) {
        Msg massage = Msg.user(content);
        return factory(_MODEL_3_5_16K, 0.0, 1, List.of(massage));
    }

    public static OpenAIMessage factory(String model, Double temperature,
                                        Integer top_p, List<Msg> msgs) {
        return OpenAIMessage.builder()
                .model(model).temperature(temperature)
                .messages(msgs).top_p(top_p)
                .build();
    }

    private static final String _MODEL_3_5_16K = "gpt-3.5-turbo-16k";

    private String _model;

    private Double _temperature;

    private Integer _top_p;

    private List<Msg> _messages;

    public JSONObject toJson() {
        return JSONObject.parseObject(JSONObject.toJSONString(this));
    }

    @Builder
    @Setter@Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Msg {

        public static final String ROLE_USER = "user";

        public static Msg user(String content) {
            return factory(ROLE_USER, content);
        }

        public static Msg factory(String role, String content) {
            return Msg.builder()
                    .role(role)
                    .content(content)
                    .build();
        }
        private String _role;
        private String _content;
    }
}
