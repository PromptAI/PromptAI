package com.zervice.kbase.ai.output.pojo;

import com.alibaba.fastjson.JSONArray;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author chenchen
 * @Date 2024/9/6
 */
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class FlowAgent {
    private String _name;

    private String _description;

    private JSONArray _steps;

    private JSONArray _subflows;

    private JSONArray _states;
}
