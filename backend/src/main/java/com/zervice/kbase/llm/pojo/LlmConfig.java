package com.zervice.kbase.llm.pojo;

import lombok.*;

import jakarta.validation.constraints.NotBlank;

/**
 * 不同的LLM服务有不同的配置内容，这决定使用什么服务实现
 *
 * @author chenchen
 * @Date 2023/10/13
 */
@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LlmConfig {

    /**
     * 用户自带的key
     */
    public static final String OPEN_AI = "open_ai";

    @NotBlank
    @Builder.Default
    private String _type = OPEN_AI;

    private OpenAILlmConfig _openai;

    /**
     * 默认使用系统配置
     */
    public static LlmConfig defaultConfig() {
        return LlmConfig.builder()
                .type(OPEN_AI).openai(OpenAILlmConfig.getNewInstance())
                .build();
    }
}
