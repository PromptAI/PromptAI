package com.zervice.kbase.database.pojo;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Story agent task template.
 * This template stored in core db
 * <p>
 * AgentTask TaskTemplateManager#generate(Agent,TaskTemplate)
 *
 * @author Peng Chen
 * @date 2022/6/16
 */
@ToString
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AgentTaskTemplate {



    private Long _id;

    /**
     * unique name
     */
    private String _name;

    private Integer _type;

    private List<TaskStep> _steps;

    private Boolean _blnActive;

    private Prop _properties;

    public static AgentTaskTemplate creatAgentTaskTemplateFromDao(Long id, String name,
                                                                  Integer type, String steps,
                                                                  Boolean blnActive, String prop) {
        return AgentTaskTemplate.builder()
                .id(id).name(name).type(type).blnActive(blnActive)
                .steps(JSONArray.parseArray(steps, TaskStep.class))
                .properties(JSONObject.parseObject(prop, Prop.class))
                .build();
    }





    @Getter
    @Setter
    @SuperBuilder
    @NoArgsConstructor
    @ToString
    public static class Prop extends BaseProp {
        @Builder.Default
        private Set<String> _usedParams = new HashSet<>();
    }

}
