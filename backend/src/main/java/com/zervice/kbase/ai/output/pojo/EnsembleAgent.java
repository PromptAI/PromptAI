package com.zervice.kbase.ai.output.pojo;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * @author chenchen
 * @Date 2024/9/6
 */
@Setter@Getter
@AllArgsConstructor
@NoArgsConstructor
public class EnsembleAgent {

    private Main _main;

    private Meta _meta;

    private List<FlowAgent> _flowAgents;

    private List<Python> _pythons;

    private List<LlmAgent> _llmAgents;

    private List<Function> _functions;

    @Setter
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Meta {

        private JSONArray _contain;

        private String _description;

        private JSONArray _steps;

        /**
         * 可以为一个bot语句，也可以为一个定义好的llm_agent。
         * <pre>
         *  fallback:
         *   policy: default fallback or
         * </pre>
         */
        private JSONObject _fallback;
    }

    @Setter
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Main {

        private String _call;

        private String _schedule;
    }
}
