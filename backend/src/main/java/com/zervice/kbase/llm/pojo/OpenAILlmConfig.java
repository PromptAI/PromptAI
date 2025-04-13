package com.zervice.kbase.llm.pojo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * User's ChatGPT api Key
 *
 * @author chenchen
 * @Date 2023/10/13
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OpenAILlmConfig {
    private String _apiKey;

    public static OpenAILlmConfig getNewInstance() {
        return new OpenAILlmConfig();
    }

}
