package com.zervice.kbase.ai.output.pojo;

import com.alibaba.fastjson.JSONArray;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;

/**
 * @author chenchen
 * @Date 2024/9/10
 */
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class LlmAgent {

    private String _name;

    private String _description;

    private String _prompt;

    /**
     *
     */
    private List<Argument> _args;

    // uses - call python...
    private JSONArray _uses;

    public JSONArray buildArgs() {
        if (CollectionUtils.isEmpty(_args)) {
            return new JSONArray();
        }

        JSONArray result = new JSONArray();
        for (Argument argument : _args) {
            result.add(argument.getName());

        }

        return result;
    }


}
