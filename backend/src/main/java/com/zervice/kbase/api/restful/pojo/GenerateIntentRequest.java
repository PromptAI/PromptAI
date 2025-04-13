package com.zervice.kbase.api.restful.pojo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.NotBlank;

import java.util.Set;

/**
 * 根据一句话生成intents
 *
 * @author chenchen
 * @date 2023/6/28
 */
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GenerateIntentRequest {

    @NotBlank
    private String _intent;

    /**
     * 扩展问 防止重复生成
     */
    private Set<String> _exts;

    /**
     * 答案
     */
    private String _answer;

    @Max(30)
    @Min(1)
    @NotNull
    private Integer _count;
}
