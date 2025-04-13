package com.zervice.kbase.ai.convert.pojo;

import com.alibaba.fastjson.JSONObject;
import com.zervice.kbase.ai.convert.PublishContext;
import com.zervice.kbase.ai.convert.VariablesHelper;
import com.zervice.kbase.api.restful.pojo.RestBaseProjectComponent;
import com.zervice.kbase.api.restful.pojo.RestProjectComponentEntity;
import com.zervice.kbase.api.restful.pojo.RestProjectComponentGpt;
import com.zervice.kbase.database.pojo.ProjectComponent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author chenchen
 * @Date 2024/8/19
 */
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Agent {

    private RestProjectComponentGpt _gpt;
    private String _name;
    private String _prompt;
    private String _description;

    /**
     * gpt 声明的 args, 按照目前的定义是需要提取的
     */
    private List<Argument> _args;

    /**
     * Prompt中引用的 Args 名称,可能与 {@link #_args} 重复
     */
    private Set<String> _usedArgs;

    /**
     * 调用此 Agent需要传入的参数： {@link #_usedArgs}使用的，排除{@link #_args}需要提取的
     */
    private Set<String> _callArgs;

    private Set<String> _uses;

    private List<RestProjectComponentGpt.FunctionCalling> _functionCallings;

    public Agent(RestProjectComponentGpt gpt, PublishContext context) {
        this._gpt = gpt;
        RestProjectComponentGpt.Data data = gpt.getData();
        this._name = name(gpt);
        this._description = data.getDescription();
        this._prompt = data.getPrompt();

        //  must before build uses
        this._functionCallings = gpt.getData().getFunctionCalling();


        // generated args build args
        this._args = _buildArgs(context);

        // used args in prompt
        this._usedArgs = VariablesHelper.parse(this._prompt);

        // build call args
        this._callArgs = _buildCallArgs(_args, _usedArgs);

        this._uses = _buildUses();
    }

    /**
     * userArgs 排除掉 args
     */
    private Set<String> _buildCallArgs(List<Argument> args, Set<String> usedArgs) {
        Set<String> generateArgs = args.stream()
                .map(Argument::getName)
                .collect(Collectors.toSet());
        return usedArgs.stream()
                .filter(arg -> !generateArgs.contains(arg))
                .collect(Collectors.toSet());
    }

    public static String name(RestProjectComponentGpt gpt) {
        return gpt.getData().getName();
    }


    public static List<Agent> buildAgents(PublishContext context) {
        List<Agent> result = new ArrayList<>();
        for (RestBaseProjectComponent root : context.getRoots()) {

            List<Agent> children = PublishContext.flatChildren(root).stream()
                    .filter(component -> component.getType().equals(RestProjectComponentGpt.TYPE_NAME))
                    .map(a -> new Agent((RestProjectComponentGpt) a, context))
                    .collect(Collectors.toList());

            result.addAll(children);
        }

        return result;
    }

    private Set<String> _buildUses() {
        if (CollectionUtils.isNotEmpty(_functionCallings)) {
            return _functionCallings.stream()
                    .map(RestProjectComponentGpt.FunctionCalling::getName)
                    .collect(Collectors.toSet());
        }
        return Set.of();
    }

    private List<Argument> _buildArgs(PublishContext context) {
        List<RestProjectComponentGpt.Slot> slots = _gpt.getData().getSlots();
        if (CollectionUtils.isEmpty(slots)) {
            return List.of();
        }

        List<Argument> result = new ArrayList<>();
        for (RestProjectComponentGpt.Slot slot : slots) {
            String slotId = slot.getSlotId();
            ProjectComponent entity = context.get(slotId);

            RestProjectComponentEntity e = new RestProjectComponentEntity(entity);
            result.add(new Argument(e));
        }

        return result;
    }

    public JSONObject toPublishAgent() {
        JSONObject agent = new JSONObject(true);
        agent.put("name", _name);
        agent.put("description", _description);
        agent.put("prompt", _prompt);
        agent.put("args", _args);
        agent.put("uses", _uses);

        return agent;
    }

    @Setter
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Argument {
        private String _name;
        private String _type;
        private String _description;
        private String _defaultValue;
        private List<Object> _enum;

        public Argument(RestProjectComponentEntity entity) {
            this._name = entity.getName();
            this._description = entity.getDescription();
            this._defaultValue = entity.getDefaultValue();
            this._enum = entity.getEnum();
        }

    }
}
