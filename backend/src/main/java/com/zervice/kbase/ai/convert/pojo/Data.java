package com.zervice.kbase.ai.convert.pojo;

import cn.hutool.core.lang.Pair;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.zervice.kbase.ai.convert.PublishContext;
import com.zervice.kbase.api.restful.pojo.RestProjectComponentEntity;
import com.zervice.kbase.api.restful.pojo.RestProjectComponentGpt;
import com.zervice.kbase.api.rpc.RpcLlmChatController;
import lombok.*;
import org.apache.commons.collections4.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author chenchen
 * @Date 2024/8/19
 */
@Builder
@Setter @Getter
@AllArgsConstructor
@NoArgsConstructor
public class Data {

    private JSONArray _llmAgents;
    private JSONObject _ensembleAgents;
    private JSONArray _flowAgents;
    private JSONArray _functions;

    /**
     * webhooks
     */
    private JSONArray _pythons;
    private JSONObject _main;

    public Data(PublishContext publishContext) {
        this._llmAgents = _buildLlmAgents(publishContext);
        this._functions = _buildFunctions(publishContext);
        this._pythons = _buildPythons(publishContext);
        this._flowAgents = _buildFlowAgents(publishContext);
        this._main = _buildMain(publishContext);
        this._ensembleAgents = _buildEnsembleAgents(publishContext);
    }

    public static Data factory(PublishContext publishContext) {
        return new Data(publishContext);
    }

    private JSONObject _buildMain(PublishContext context) {
        JSONObject main = new JSONObject();
        main.put("call", "meta");
        main.put("schedule", context.schedule());
        return main;
    }

    private JSONObject _buildEnsembleAgents(PublishContext context) {
        JSONObject meta = new JSONObject();
        meta.put("description", "You can select an agent to response user's question.");
        meta.put("contain", _buildMetaContain(context));
        // welcome message
        meta.put("steps", _buildMeatSteps(context));
        meta.put("fallback", _buildMeatFallback(context));

        JSONObject ensembleAgents = new JSONObject(true);
        ensembleAgents.put("meta", meta);
        ensembleAgents.put("flowAgents", _flowAgents);
        ensembleAgents.put("llmAgents", _llmAgents);
        ensembleAgents.put("functions", _functions);
        ensembleAgents.put("pythons", _pythons);
        ensembleAgents.put("main", _main);
        return ensembleAgents;
    }

    private Set<String> _buildMetaContain(PublishContext context) {
        Set<String> contain = new HashSet<>();
        for (int i = 0; i < _flowAgents.size(); i++) {
            JSONObject flow = _flowAgents.getJSONObject(i);
            contain.add(flow.getString("name"));
        }

        // 需要注册的GPT 节点
        for (RestProjectComponentGpt gpt : context.getNeedRegisterGpts()) {
            contain.add(Agent.name(gpt));
        }

        return contain;
    }

    /**
     * 可以为一个bot语句，也可以为一个定义好的llm_agent。
     * <pre>
     *  fallback:
     *   - bot:default fallback or
     *   - call:fallback agent(defined by user)
     * </pre>
     */
    private JSONObject _buildMeatFallback(PublishContext context) {
        JSONObject fallback = new JSONObject();
        // for now，we only support one text
        fallback.put("policy", context.fallback());
        return fallback;
    }

    /**
     * dynamic slots &  welcome message
     */
    private List<JSONObject> _buildMeatSteps(PublishContext context) {
        // add dynamic slots
        List<JSONObject> steps = new ArrayList<>(_buildDynamicSlots(context));

        // add welcome message
        // TODO: 欢迎语的变量需要 设置meta.variables: variables_context.name
        steps.add(Flow.buildBotText(context.welcome()));

        // 如果 只有一个 Agent,主动 call
        // 1、flow 1个，agent 至少 个，且agent都属于属于这个flow
        // 2、flow 1个，agent 0 个
        // 3、flow 0个，agent 1 个

        // context.roots 为 1

        if (context.getRoots().size() == 1) {
            String agentName = _flowAgents.isEmpty() ?
                    _llmAgents.getJSONObject(0).getString("name") :
                    _flowAgents.getJSONObject(0).getString("name");
            steps.add(Flow.buildCall(agentName));
        }
        return steps;
    }

    private List<JSONObject> _buildDynamicSlots(PublishContext context) {
        List<RestProjectComponentEntity> dynamicSlots = context.dynamicSlots();
        if (CollectionUtils.isEmpty(dynamicSlots)) {
            return List.of();
        }

        // 配置了动态变量的 entity，但是 flow 中不一定用到了
        Set<String> names = dynamicSlots.stream()
                .map(RestProjectComponentEntity::getName)
                .collect(Collectors.toSet());

        // 筛选本次发布用到的变量
        List<Pair<String, String>> usedNames = _filterUsedStates();

        // 只设置用到的变量
        names = names.stream()
                .filter(name -> usedNames.stream().anyMatch(pair -> pair.getValue().equals(name)))
                .collect(Collectors.toSet());

        if (names.isEmpty()) {
            return List.of();
        }

        List<JSONObject> steps = new ArrayList<>();

        String pythonName = Python.CONTEXT_VARIABLES_NAME;

        // call webhook 设置动态变量
        // 1、build webhook and return name
        _prepareDynamicWebhook(names, pythonName, context.dbName());

        // 2、add call to steps
        steps.add(Flow.buildCall(pythonName));

        // 构建call map: name: context_variable.name
        Map<String /* key*/, String /*value*/> dynamicSlotMap = new HashMap<>();
        for (Pair<String, String> p : usedNames) {
            if (names.contains(p.getValue())) {
                String key = p.getKey();
                String name = p.getValue();

                // flow/agent/python.name = context_variable.name
                dynamicSlotMap.put(key + "." + name, pythonName + "." + name);
            }
        }

        // set
        steps.add(Flow.buildSet(dynamicSlotMap));

        return steps;
    }

    /**
     * 将用用到的变量 设置到 context_variable 中
     *
     * @param usedVariables used variables
     * @param pythonName    python name
     * @param dbName        database name
     */
    private void _prepareDynamicWebhook(Set<String> usedVariables,String pythonName, String dbName) {
        JSONObject body = new JSONObject(true);
        body.put("variables", usedVariables);

        List<Pair> variables = usedVariables.stream()
                .map(name -> Pair.of(name, "$." + name))
                .collect(Collectors.toList());

        String url = RpcLlmChatController.llmChatDynamicVariablesWebhookUrl();
        Map<String, String> headers = RpcLlmChatController.AUTH_HEADERS;

        Python python = Python.contextVariables(pythonName, body, variables, url, headers, dbName);

        _pythons.add(python);
    }

    /**
     * 从 flow、agent和python中找到使用的 entity
     *
     * @return sst
     */
    private List<Pair<String, String>> _filterUsedStates() {
        List<Pair<String /* flow / agent / python  name */, String>> names = new ArrayList<>();
        // form flows
        if (CollectionUtils.isNotEmpty(_flowAgents)) {
            for (int i = 0; i < _flowAgents.size(); i++) {
                JSONObject flow = _flowAgents.getJSONObject(i);
                JSONArray states = flow.getJSONArray("states");

                String flowName = flow.getString("name");
                if (CollectionUtils.isNotEmpty(states)) {
                    for (String name : states.toJavaList(String.class)) {
                        names.add(Pair.of(flowName, name));
                    }
                }
            }
        }

        // from agents
        if (CollectionUtils.isNotEmpty(_llmAgents)) {
            for (int i = 0; i < _llmAgents.size(); i++) {
                JSONObject agent = _llmAgents.getJSONObject(i);
                JSONArray args = agent.getJSONArray("args");
                String agentName = agent.getString("name");
                if (CollectionUtils.isNotEmpty(args)) {
                    for (int j = 0; j < args.size(); j++) {
                        JSONObject arg = args.getJSONObject(j);
                        String name = arg.getString("name");
                        names.add(Pair.of(agentName, name));
                    }
                }
            }
        }

        // from pythons
        if (CollectionUtils.isNotEmpty(_pythons)) {
            for (int i = 0; i < _pythons.size(); i++) {
                JSONObject python = _pythons.getJSONObject(i);
                JSONArray args = python.getJSONArray("args");
                String pythonName = python.getString("name");
                if (CollectionUtils.isNotEmpty(args)) {
                    for (String name : args.toJavaList(String.class)) {
                        names.add(Pair.of(pythonName, name));
                    }
                }
            }
        }

        return names;
    }


    private JSONArray _buildPythons(PublishContext context) {
        List<Python> pythons = Python.buildPythons(context);
        JSONArray res = new JSONArray();
        for (Python python : pythons) {
            res.add(python.toPublishPython());
        }

        return res;
    }

    private JSONArray _buildFunctions(PublishContext context) {
        List<Function> functions = Function.buildFunctions(context);
        JSONArray res = new JSONArray();

        for (Function function : functions) {
            res.add(function.toPublishFunction());
        }

        return res;
    }

    private JSONArray _buildFlowAgents(PublishContext context) {
        List<Flow> flows = Flow.buildFlows(context);
        JSONArray flowAgents = new JSONArray();
        for (Flow flow : flows) {
            flowAgents.add(flow.toPublishFlow());
        }

        return flowAgents;
    }

    private JSONArray _buildLlmAgents(PublishContext context) {
        List<Agent> agents = Agent.buildAgents(context);
        JSONArray llmAgents = new JSONArray();
        for (Agent agent : agents) {
            JSONObject a = agent.toPublishAgent();
            llmAgents.add(a);
        }
        return llmAgents;
    }

    public JSONObject toPublishData() {
        JSONObject data = new JSONObject(true);
        data.put("ensembleAgents", _ensembleAgents);
        return data;
    }

}
