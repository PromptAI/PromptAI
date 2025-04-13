package com.zervice.kbase.llm.pojo;

import com.zervice.kbase.llm.pojo.openai.OpenAIMessage;
import lombok.*;

/**
 * FAQ的历史记录
 */
@Builder
@ToString
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class History {
    public static final String ROLE_USER = "user";
    public static final String ROLE_ASSISTANT = "assistant";
    public static final String ROLE_SYSTEM = "system";
    private String _role;
    private String _content;

    public static History user(String content) {
        return factory(ROLE_USER, content);
    }

    public static History assistant(String content) {
        return factory(ROLE_ASSISTANT, content);
    }

    public static History system(String content) {
        return factory(ROLE_SYSTEM, content);
    }

    private static History factory(String role, String content) {
        return History.builder()
                .role(role).content(content)
                .build();
    }

    public OpenAIMessage.Msg toMsg() {
        return OpenAIMessage.Msg.factory(_role, _content);
    }
}