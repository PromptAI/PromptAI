package com.zervice.kbase.service.impl.mica.pojo;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.*;

import java.util.*;

/**
 * 对于Mica, function calling & pythons 都是function.
 *
 * 那么，倒入时PromptAI需要知道那些应该是webhook....
 *
 * 如果当前Function未关联到GPT节点，那么将它转换为webhook
 */
@Builder
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Function {
    private String _name;
    private String _body;

    /**
     * webhook需要的args,暂存一下
     */
    private JSONArray _args;

    @Builder.Default
    private Boolean _usedByGPT = false;


    public static Function factory(String name, String body, JSONArray args) {
        return Function.builder()
                .name(name).body(body).args(args)
                .build();
    }
    public static Function factory(String name, String body) {
        return factory(name, body, null);
    }

    public static List<Function> parse(Map<String, JSONObject> functions) {
        if (functions == null || functions.isEmpty()) {
            return List.of();
        }

        List<Function> result = new ArrayList<>();
        for (String key : functions.keySet()) {
            // code
            String body = functions.get(key).getString("body");
            JSONArray args = functions.get(key).getJSONArray("args");

            result.add(Function.factory(key, body, args));
        }

        return result;

    }

    /**
     * <pre>
     * {
     *   "func1":{
     *      "body":"string"
     *   },
     *   "func2":{
     *      "body":"string"
     *   }
     * }
     * </pre>
     *
     * @param functions json functions form yaml
     * @return list of function model
     */
    public static List<Function> parse(JSONObject functions) {
        if (functions == null || functions.isEmpty()) {
            return List.of();
        }

        List<Function> result = new ArrayList<>();
        for (String key : functions.keySet()) {
            String body = functions.getJSONObject(key).getString("body");
            result.add(Function.factory(key, body));
        }

        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Function function = (Function) o;
        return Objects.equals(_name, function._name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(_name);
    }
}
