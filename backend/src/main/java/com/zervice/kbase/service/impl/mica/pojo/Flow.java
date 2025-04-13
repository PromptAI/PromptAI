package com.zervice.kbase.service.impl.mica.pojo;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.zervice.kbase.api.restful.pojo.RestProjectComponentConversation;
import lombok.*;

import java.util.*;

@Builder
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Flow {

    public static final String TYPE_FLOW = RestProjectComponentConversation.Data.TYPE_FLOW_AGENT;
    public static final String TYPE_LLM = RestProjectComponentConversation.Data.TYPE_LLM_AGENT;
    private String _name;
    private String _description;
    private Set<String> _args;
    private JSONArray _steps;
    private String _type;

    /**
     * zip
     */
    public static List<Flow> parse(Map<String, JSONObject> flows) {
        if (flows == null || flows.isEmpty()) {
            return List.of();
        }
        List<Flow> result = new ArrayList<>();
        for (String key : flows.keySet()) {
            result.add(_convertToFlow(key, flows.get(key)));
        }

        return result;
    }
    /**
     * yaml
     * <pre>
     * {
     * 	"name": {
     * 		"args": ["xx", "22"],
     * 		"description": "xxx",
     * 		"steps": [
     *             {
     * 				"begin": "main"
     *             }, {
     * 				"bot": "xxx"
     *           },
     * 			"end"
     * 		]
     *    }
     * }
     * </pre>
     */
    public static List<Flow> parse(JSONObject flows) {
        if (flows == null || flows.isEmpty()) {
            return List.of();
        }

        List<Flow> result = new ArrayList<>();
        for (String key : flows.keySet()) {
            result.add(_convertToFlow(key, flows.getJSONObject(key)));
        }

        return result;
    }

    private static Flow _convertToFlow(String name, JSONObject body) {
        String description = body.getString("description");
        Set<String> args = new HashSet<>(body.getJSONArray("args").toJavaList(String.class));
        JSONArray steps = body.getJSONArray("steps");
        return Flow.factory(name, TYPE_FLOW, description, args, steps);
    }

    /**
     * 构建一个 flow + GPT 的 flow
     */
    public static Flow virtual(String name, String description, Gpt gpt) {
        return factory(name, TYPE_LLM, description, gpt.getArgs(), _callGPTSteps(gpt));
    }

    private static JSONArray _callGPTSteps(Gpt gpt) {
        JSONArray steps = new JSONArray();
        steps.add(_callStep(gpt.getName(), gpt.getArgs()));
        return steps;
    }

    private static JSONObject _callStep(String target, Set<String> args) {
        JSONObject argsObj = new JSONObject();
        for (String arg : args) {
            argsObj.put(arg, arg);
        }

        JSONObject callStep = new JSONObject();
        callStep.put("call", target);
        callStep.put("args", argsObj);
        return callStep;
    }


    public static Flow factory(String name, String type, String description, Set<String> args, JSONArray steps) {
        return Flow.builder()
                .name(name).type(type)
                .description(description)
                .args(args)
                .steps(steps)
                .build();
    }
}