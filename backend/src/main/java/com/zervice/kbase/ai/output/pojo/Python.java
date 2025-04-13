package com.zervice.kbase.ai.output.pojo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * @author chenchen
 * @Date 2024/9/13
 */
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Python {
    private String _name;

    private String _body;

    private List<String> _required;

    private List<String> _args;
}
