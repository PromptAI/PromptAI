package com.zervice.kbase.ai.output.pojo;

import com.alibaba.fastjson.JSONArray;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author chenchen
 * @Date 2024/9/10
 */
@Setter@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Argument {
    private String _name;
    private String _description;
    private JSONArray _enum;
}
