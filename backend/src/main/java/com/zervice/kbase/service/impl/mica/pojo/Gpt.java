package com.zervice.kbase.service.impl.mica.pojo;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.*;

import java.util.*;

@Builder
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Gpt {

    private String _name;
    private Set<String> _args;
    private String _description;
    /**
     * 使用到的 functions
     */
    private Set<String> _uses;
    private String _prompt;

    @Builder.Default
    private Boolean _usedByFlow = false;


    /**
     * zip
     */
    public static List<Gpt> parse(Map<String /*name*/, JSONObject /* body */> gpts) {
        if (gpts == null || gpts.isEmpty()) {
            return List.of();
        }

        List<Gpt> result = new ArrayList<>(gpts.size());
        for (String key : gpts.keySet()) {
            result.add(_convertToGPT(key, gpts.get(key)));
        }

        return result;
    }

    /**
     * yaml
     * <pre>
     * {
     * 	"name1": {
     * 		"args": ["string", "xx2"],
     * 		"description": "string",
     * 		"prompt": "string",
     * 		"uses": ["string", "xx2"]
     *     }
     * }
     * </pre>
     */
    public static List<Gpt> parse(JSONObject gpts) {
        if (gpts == null || gpts.isEmpty()) {
            return List.of();
        }

        List<Gpt> result = new ArrayList<>(gpts.size());
        for (String key : gpts.keySet()) {
            result.add(_convertToGPT(key, gpts.getJSONObject(key)));
        }

        return result;
    }

    private static Gpt _convertToGPT(String name, JSONObject body) {
        String description = body.getString("description");
        String prompt = body.getString("prompt");
        JSONArray argsArray = body.getJSONArray("args");
        JSONArray usesArray = body.getJSONArray("uses");
        Set<String> args = argsArray == null ? Set.of() : new HashSet<>(argsArray.toJavaList(String.class));
        Set<String> uses = usesArray == null ? Set.of() : new HashSet<>(usesArray.toJavaList(String.class));

        return Gpt.factory(name, description, prompt, args, uses);
    }

    public static Gpt factory(String name, String description, String prompt, Set<String> args, Set<String> uses) {
        return Gpt.builder()
                .name(name)
                .description(description)
                .prompt(prompt)
                .args(args)
                .uses(uses)
                .build();

    }
}