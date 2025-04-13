package com.zervice.kbase.api.restful.pojo;

import cn.hutool.core.util.StrUtil;
import com.zervice.kbase.llm.pojo.LlmConfig;
import lombok.*;

/**
 * @author chenchen
 * @Date 2023/10/18
 */
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RestLlmConfig {

    private String _type;
    private OpenAILlmConfig _openai;

    public RestLlmConfig(LlmConfig llmConfig) {
        this._type = llmConfig.getType();
        this._openai = new OpenAILlmConfig(llmConfig.getOpenai().getApiKey());
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class OpenAILlmConfig {

        private String _apiKey;


        public OpenAILlmConfig(String apiKey) {
            if (apiKey == null) {
                return;
            }

            if (apiKey.length() < 3) {
                this._apiKey = "***";
            } else {
                this._apiKey = StrUtil.hide(apiKey, 3, apiKey.length());
            }
        }
    }
}
