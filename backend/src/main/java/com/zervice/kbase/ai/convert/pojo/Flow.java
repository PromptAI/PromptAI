package com.zervice.kbase.ai.convert.pojo;

import cn.hutool.core.lang.Pair;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.zervice.common.restful.exception.RestException;
import com.zervice.common.restful.exception.StatusCodes;
import com.zervice.kbase.ai.convert.PublishContext;
import com.zervice.kbase.ai.convert.VariablesHelper;
import com.zervice.kbase.ai.convert.condition.BotConditionHelper;
import com.zervice.kbase.api.restful.pojo.*;
import com.zervice.kbase.api.restful.pojo.mica.ConditionPojo;
import com.zervice.kbase.api.restful.pojo.mica.SlotPojo;
import com.zervice.kbase.database.pojo.ProjectComponent;
import lombok.*;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * FlowAgent 节点转换
 * <pre>
 * 特殊情况：如果Flow只有一个 GPT 节点
 *  - 当前 FlowAgent 不转换
 *  - 在ensemble_agent.contains中注册GPT节点
 * </pre>
 *
 * @author chenchen
 * @Date 2024/9/4
 */
@Log4j2
public class Flow {

    public JSONArray _steps;
    private final RestProjectComponentConversation _flow;
    @Getter
    private final String _name;
    private final String _description;
    private Set<State> _states;
    private final JSONArray _subflows;

    private Integer _subflowIndex = 0;

    /**
     * flow + GPT 的情况，没有其他 子节点
     */
    private final Boolean _llmAgent;

    /**
     * 变量注册表
     * -  标记变量提取来自哪里，使用的时候“来源.名称”
     */
    private Map<String /* slot name*/, String /*slot full name*/> _argsTable = new HashMap<>();

    public Flow(RestProjectComponentConversation flow, PublishContext context) {
        this._flow = flow;
        this._name = flow.getData().getName();
        this._description = flow.getData().getDescription();
        this._subflows = new JSONArray();

        // 判断是否只有一个 GPT节点
        this._llmAgent = _llmGpt(flow);

        // 当前 flow 的 GPT 需要独立工作，标记需要注册
        if (_llmAgent) {
            _registerGpt(context);
        }

        List<RestBaseProjectComponent> children = PublishContext.flatChildren(flow);

        _buildArgsTable(children, context);

        // build states form child components
        _buildStates(children, context);

        // build goto labels
        _buildLabels4Goto(children);

        // build steps and subflows
        _buildStepsAndSubflows(context);
    }

    public static JSONObject buildCall(String method) {
        return _buildCall(method, null, null);
    }

    public static List<Flow> buildFlows(PublishContext context) {
        return context.getRoots().stream()
                .filter(p -> ProjectComponent.TYPE_CONVERSATION.equals(p.getType()))
                .map(flow -> new Flow((RestProjectComponentConversation) flow, context))
                // 过滤掉不需要转换的 flow
                .filter(Flow::needConvert)
                .collect(Collectors.toList());
    }

    private void _registerGpt(PublishContext context) {
        context.needRegister((RestProjectComponentGpt) _flow.getChildren().get(0));
    }

    /**
     * 判断当前 flow 是否只有子 GPT 节点，用于后续转换判断
     */
    private boolean _llmGpt(RestProjectComponentConversation conversation) {
        if (RestProjectComponentConversation.Data.TYPE_LLM_AGENT.equals(conversation.getData().getType())) {
            return true;
        }
        List<RestBaseProjectComponent> children = _flow.getChildren();
        if (children.size() != 1) {
            return false;
        }

        RestBaseProjectComponent child = children.get(0);
        return child instanceof RestProjectComponentGpt && CollectionUtils.isEmpty(child.getChildren());
    }

    private static JSONObject _buildCall(String name, Collection<String> agrs, Map<String, String> argsTable) {
        JSONObject call = new JSONObject();
        call.put("action", "call");
        call.put("value", name);

        if (CollectionUtils.isNotEmpty(agrs)) {

            // 这只需要一对一映射即可
            Map<String, String> args = agrs.stream()
                    .collect(Collectors.toMap(a -> a, a -> argsTable.getOrDefault(a, a)));
            call.put("args", args);
        }

        return call;
    }

    public static JSONObject buildBotText(String text) {
        return _buildBotText(text);
    }

    public static JSONObject buildSet(Map<String, String> setMap) {
        return _buildSet(setMap);
    }

    /**
     * 找出变量所属来源， 比如:agent.name 提取了 name变量，则使用name 的时候替换为 ${agent.name}
     *
     * @param components
     * @param context
     */
    private void _buildArgsTable(List<RestBaseProjectComponent> components, PublishContext context) {
        Map<String, String> nameTable = new HashMap<>();
        for (RestBaseProjectComponent component : components) {
            if (component instanceof RestProjectComponentGpt) {
                RestProjectComponentGpt gpt = (RestProjectComponentGpt) component;
                Agent agent = new Agent(gpt, context);
                if (CollectionUtils.isNotEmpty(agent.getArgs())) {
                    for (Agent.Argument argument : agent.getArgs()) {
                        nameTable.put(argument.getName(), agent.getName() + "." + argument.getName());
                    }
                }
                continue;
            }

            if (component instanceof RestProjectComponentBot) {
                RestProjectComponentBot bot = (RestProjectComponentBot) component;
                // webhook 中新产生的变量
                for (RestProjectComponentBot.BotResponse response : bot.getData().getResponses()) {
                    if (response.isWebhook()) {
                        String webhookId = response.getContent().getString("id");

                        RestProjectComponentWebhook webhook = new RestProjectComponentWebhook(context.get(webhookId));
                        List<Pair> responseHandleParse = webhook.responseHandleParse();

                        Python python = new Python(webhook, context);

                        // handler 中的 key
                        if (CollectionUtils.isNotEmpty(responseHandleParse)) {
                            responseHandleParse.forEach(p -> nameTable.put(p.getKey().toString(), python.getName() + "." + p.getKey()));
                        }
                    }
                }
            }
        }

        this._argsTable = nameTable;
    }

    /**
     * 根据子节点树生成 steps & subflows
     *
     * @param context context
     */
    private void _buildStepsAndSubflows(PublishContext context) {
        JSONArray mainStapes = initMainSteps();

        _buildStep(_flow.getChildren(), mainStapes, context);

        this._steps = mainStapes;
    }

    private void _buildStep(List<RestBaseProjectComponent> children, JSONArray steps, PublishContext context) {
        if (children.size() > 1) {
            // build if node
            JSONArray ifNode = _buildIf(IfHolder.build(children), context);

//            // create subflow
//            String name = _buildSubflowName("if");
//            JSONArray subIFSteps = _buildSubflow(name);
//            subIFSteps.addAll(ifNode);
//
//            // call subflow at main...
//            steps.add(_buildNext(name));

            steps.addAll(ifNode);
        }

        if (children.size() == 1) {
            RestBaseProjectComponent child = children.get(0);

            // build label node
            if (StringUtils.isNotBlank(child.getLabel())) {
                steps.add(_buildLabel(child.getLabel()));
            }

            // build steps
            List<JSONObject> childSteps = _buildChildSteps(child, context);
            steps.addAll(childSteps);
            if (CollectionUtils.isNotEmpty(children)) {
                _buildStep(child.getChildren(), steps, context);
            }
        }

        // no children
    }

    /**
     * build if node
     */
    private JSONArray _buildIf(IfHolder holder, PublishContext context) {
        String type = holder.getType();
        if (ProjectComponent.TYPE_USER.equals(type)) {
            return _buildUserIf(holder, context);
        }
        if (ProjectComponent.TYPE_BOT.equals(type)) {
            return _buildBotIf(holder, context);
        }

        throw new RestException(StatusCodes.BadRequest, "unsupported if node type:" + type);
    }

    private JSONArray _buildUserIf(IfHolder ifHolder, PublishContext context) {
        RestProjectComponentUser user = (RestProjectComponentUser) ifHolder.getIfNode();

        // if
        JSONObject ifNode = new JSONObject(true);
        ifNode.put("action", "if");
        ifNode.put("condition", _buildUserIfCondition(user));
//        ifNode.put("then", _buildUserIfThenInSubflow(user, "user_if", context));
        ifNode.put("then", _buildUserIfThen(user, context));

        // else if
        List<RestProjectComponentUser> elseUsers = ifHolder.getElseIfNodes().stream()
                .map(c -> (RestProjectComponentUser) c)
                .collect(Collectors.toList());

        JSONArray elseIfs = new JSONArray();
        for (RestProjectComponentUser u : elseUsers) {
            JSONObject elseIf = new JSONObject();

            elseIf.put("condition", _buildUserIfCondition(u));
//            elseIf.put("then", _buildUserIfThenInSubflow(u, "user_else_if", context));
            elseIf.put("then", _buildUserIfThen(u, context));

            elseIfs.add(elseIf);
        }

        ifNode.put("else if", elseIfs);

        JSONArray steps = new JSONArray();
        // 先 add 一个 user，便于后续的 if 读取输入
        steps.add(_buildUser());
        steps.add(ifNode);

        // no else
        return steps;
    }

    private JSONArray _buildBotIf(IfHolder ifHolder, PublishContext context) {
        RestProjectComponentBot ifBot = (RestProjectComponentBot) ifHolder.getIfNode();
        // if
        JSONObject ifNode = new JSONObject(true);
        ifNode.put("action", "if");
        // add conditions
        ifNode.put("condition", _buildBotIfCondition(ifBot, context));
        // add then....
//        ifNode.put("then", _buildBotIfThenInSubflow(ifBot, "bot_if", context));
        ifNode.put("then", _buildBotIfThen(ifBot, context));

        // else if
        List<RestProjectComponentBot> elseBots = ifHolder.getElseIfNodes().stream()
                .map(c -> (RestProjectComponentBot) c)
                .collect(Collectors.toList());

        if (CollectionUtils.isNotEmpty(elseBots)) {
            JSONArray elseIfs = new JSONArray();
            for (RestProjectComponentBot elseBot : elseBots) {
                JSONObject elseIf = new JSONObject();

                // add conditions
                elseIf.put("condition", _buildBotIfCondition(elseBot, context));

                // add then....
//                elseIf.put("then", _buildBotIfThenInSubflow(elseBot, "bot_else_if", context));
                elseIf.put("then", _buildBotIfThen(elseBot, context));

                elseIfs.add(elseIf);
            }

            ifNode.put("else if", elseIfs);
        }

        // else
        RestBaseProjectComponent elseBot = ifHolder.getElseNode();
        if (elseBot != null) {
            RestProjectComponentBot eb = (RestProjectComponentBot) elseBot;
//            ifNode.put("else", _buildBotIfThenInSubflow(eb, "bot_else", context));
            ifNode.put("else", _buildBotIfThen(eb, context));
        }

        JSONArray steps = new JSONArray();
        steps.add(ifNode);
        return steps;
    }

    private JSONArray _buildUserIfThenInSubflow(RestProjectComponentUser user, String name, PublishContext context) {
        String userIfSubflowName = _buildSubflowName(name);
        // build if subflow step
        // user 不需要参与在 child
        _buildStep(user.getChildren(), _buildSubflow(userIfSubflowName), context);
        return _arrayWrapper(_buildNext(userIfSubflowName));
    }


    private JSONArray _buildUserIfThen(RestProjectComponentUser user, PublishContext context) {
        JSONArray steps = new JSONArray();
        if (CollectionUtils.isNotEmpty(user.getData().getSetSlots())) {
            steps.add(_buildSet(user.getData().getSetSlots()));
        }

        _buildStep(user.getChildren(), steps, context);
        return steps;
    }

    private JSONArray _buildBotIfThenInSubflow(RestProjectComponentBot bot, String name, PublishContext context) {
        String botIFSubflowName = _buildSubflowName(name);
        // build if subflow step
        _buildStep(List.of(bot), _buildSubflow(botIFSubflowName), context);
        // add then....
        return _arrayWrapper(_buildNext(botIFSubflowName));
    }

    private JSONArray _buildBotIfThen(RestProjectComponentBot bot, PublishContext context) {
        JSONArray steps = new JSONArray();
        _buildStep(List.of(bot), steps, context);
        return steps;
    }

    private JSONArray _arrayWrapper(JSONObject action) {
        JSONArray array = new JSONArray();
        array.add(action);
        return array;
    }

    /**
     * 满足条件才转换:flow +GPT +其他节点. 如果只有 flow + GPT,则不转换次 FlowAgent
     *
     * @return true 需要转换
     */
    public boolean needConvert() {
        return !_llmAgent;
    }

    private List<JSONObject> _buildGpt(Agent agent) {
        String name = agent.getName();
        List<JSONObject> steps = new ArrayList<>();

        JSONObject call = _buildCall(name, agent.getCallArgs(), _argsTable);
        steps.add(call);

//        // 如果有提取参数,那么设置回当前的 flow
//        // slot: Agent::getName.slot
//        if (CollectionUtils.isNotEmpty(agent.getArgs())) {
//            Map<String, String> setSlots = agent.getArgs().stream()
//                    .map(Agent.Argument::getName)
//                    .collect(Collectors.toMap(a -> a, a -> name + "." + a));
//            steps.add(_buildSet(setSlots));
//        }

        return steps;
    }

    private JSONObject _buildNext(String subflowNameOrLabel) {
        JSONObject next = new JSONObject();
        next.put("action", "next");
        next.put("value", subflowNameOrLabel);
        return next;
    }

    private JSONArray _buildSubflow(String name) {
        JSONArray subflowSteps = new JSONArray();
        subflowSteps.add(_buildBegin(name));

        JSONObject subflow = new JSONObject();
        subflow.put("name", name);
        subflow.put("steps", subflowSteps);

        _subflows.add(subflow);
        return subflowSteps;
    }

    private String _buildSubflowName(String name) {
        return "sub_" + name + "_" + getSubflowIndexAndIncrement();
    }

    private String _buildLabelName() {
        return "label_" + getSubflowIndexAndIncrement();
    }


    private JSONObject _buildUserIfCondition(RestProjectComponentUser user) {
        JSONObject condition = new JSONObject();
        condition.put("action", "the user claims");
        condition.put("values", user.examples());
        return condition;
    }

    private JSONObject _buildBotIfCondition(RestProjectComponentBot bot, PublishContext context) {
        List<ConditionPojo> conditionPojos = bot.getData().getConditions();
        if (CollectionUtils.isNotEmpty(conditionPojos)) {
            JSONObject condition = new JSONObject();
            condition.put("action", "bot_condition");

            JSONArray values = new JSONArray();
            for (ConditionPojo conditionPojo : conditionPojos) {

                ProjectComponent entity = context.get(conditionPojo.getSlotId());
                RestProjectComponentEntity e = new RestProjectComponentEntity(entity);


                // build entity value
                String conditionVal = _buildConditionVal(e.getName(), conditionPojo);

                // entity name
                values.add(conditionVal);

            }
            condition.put("values", values);
            return condition;
        }

        return new JSONObject();
    }

    private String _buildConditionVal(String name, ConditionPojo conditionPojo) {
        return BotConditionHelper.buildConditionValue(name, conditionPojo);
    }

    private List<JSONObject> _buildChildSteps(RestBaseProjectComponent component, PublishContext context) {
        String type = component.getType();
        switch (type) {
            case ProjectComponent.TYPE_BOT:
                return _buildBotSteps((RestProjectComponentBot) component, context);
            case ProjectComponent.TYPE_USER:
                return _buildUserSteps((RestProjectComponentUser) component, context);
            case ProjectComponent.TYPE_GOTO:
                return _buildGotoSteps((RestProjectComponentGoto) component, context);
            case ProjectComponent.TYPE_GPT:
                return _buildGptSteps((RestProjectComponentGpt) component, context);
            default:
                LOG.error("[{}][unsupported type:{}]", component.getId(), type);
                throw new RestException(StatusCodes.BadRequest, "not support");
        }
    }

    private JSONObject _buildLabel(String labelName) {
        JSONObject label = new JSONObject();
        label.put("action", "label");
        label.put("value", labelName);
        return label;
    }

    private JSONObject _buildSet(List<SlotPojo> setSlots) {
        JSONObject set = new JSONObject();
        set.put("action", "set");

        JSONObject value = new JSONObject();
        for (SlotPojo slotPojo : setSlots) {
            String name = slotPojo.getSlotName();
            String v = slotPojo.getValue();
            value.put(name, v == null ? "None" : v);
        }

        set.put("value", value);
        return set;
    }

    private static JSONObject _buildSet(Map<String, String> setMap) {
        JSONObject set = new JSONObject();
        set.put("action", "set");

        JSONObject value = new JSONObject();
        for (Map.Entry<String, String> entry : setMap.entrySet()) {
            String name = entry.getKey();
            String v = entry.getValue();
            value.put(name, v == null ? "None" : v);
        }

        set.put("value", value);
        return set;
    }

    private List<JSONObject> _buildUserSteps(RestProjectComponentUser user, PublishContext context) {
        List<JSONObject> steps = new ArrayList<>();
        steps.add(_buildUser());

        // reset slot
        if (CollectionUtils.isNotEmpty(user.getData().getSetSlots())) {
            steps.add(_buildSet(user.getData().getSetSlots()));
        }
        return steps;
    }

    private List<JSONObject> _buildGotoSteps(RestProjectComponentGoto gotoNode, PublishContext context) {
        List<JSONObject> steps = new ArrayList<>();
        steps.add(_buildNext(gotoNode.getNext()));

        return steps;
    }

    private List<JSONObject> _buildGptSteps(RestProjectComponentGpt gpt, PublishContext context) {
        return _buildGpt(new Agent(gpt, context));
    }


    /**
     * 检查当前bot是否需要构建 bot.if
     * -1、当前 bot 节点不处于分叉中
     * -2、 存在conditions
     *
     * @param bot     bot
     * @param context context
     * @return true or false
     */
    private boolean _needBuildBotIf(RestProjectComponentBot bot, PublishContext context) {
        if (CollectionUtils.isEmpty(bot.getData().getConditions())) {
            return false;
        }

        String parentId = bot.getParentId();
        return context.children(parentId).size() < 2;
    }

    private List<JSONObject> _buildBotSteps(RestProjectComponentBot bot, PublishContext context) {
        // 检查当前 bot是否有 conditions, 如果有，构建一个 bot.if
        if (_needBuildBotIf(bot, context)) {
            // if
            JSONObject ifNode = new JSONObject(true);
            ifNode.put("action", "if");
            // add conditions
            ifNode.put("condition", _buildBotIfCondition(bot, context));
            // add then....
            ifNode.put("then", _generateBotSteps(bot, context));
            return List.of(ifNode);
        }

        // 无需 bot
        return _generateBotSteps(bot, context);
    }

    private List<JSONObject> _generateBotSteps(RestProjectComponentBot bot, PublishContext context) {
        List<RestProjectComponentBot.BotResponse> responses = bot.getData().getResponses();
        List<JSONObject> botSteps = new ArrayList<>(responses.size());

        // set reset slots
        List<SlotPojo> setSlots = bot.getData().getSetSlots();
        if (CollectionUtils.isNotEmpty(setSlots)) {
            botSteps.add(_buildSet(setSlots));
        }

        for (RestProjectComponentBot.BotResponse response : responses) {
            String type = response.getType();
            // text
            if (RestProjectComponentBot.BotResponse.TYPE_TEXT.equals(type)) {
                botSteps.add(_buildBotText(response.getContent().getString("text")));
                continue;
            }

            // webhook
            if (RestProjectComponentBot.BotResponse.TYPE_WEBHOOK.equals(type)) {
                String webhookId = response.getContent().getString("id");
                ProjectComponent webhook = context.get(webhookId);

                RestProjectComponentWebhook restWebhook = new RestProjectComponentWebhook(webhook);
                botSteps.add(_buildWebhook(restWebhook, context));

//                // 如果有提取参数,那么设置回当前的 flow
//                // slot: Python::getName.slot
//                List<Pair> handleParse = restWebhook.responseHandleParse();
//                if (CollectionUtils.isNotEmpty(handleParse)) {
//                    String name = Python.buildPythonName(restWebhook.getText());
//
//                    Map<String, String> setSlots = handleParse.stream()
//                            .map(a -> a.getKey().toString())
//                            .collect(Collectors.toMap(a -> a, a -> name + "." + a));
//                    botSteps.add(_buildSet(setSlots));
//                }
            }
        }

        return botSteps;
    }


    private Integer getSubflowIndexAndIncrement() {
        return _subflowIndex++;
    }

    /**
     * 从子节点中收集使用到的 entity
     * - user
     *
     * @param children 子节点
     * @param context  构建的上下文
     */
    private void _buildStates(List<RestBaseProjectComponent> children, PublishContext context) {
        this._states = new HashSet<>();
        Set<State> states = new HashSet<>();
        for (RestBaseProjectComponent component : children) {
            String type = component.getType();

            if (ProjectComponent.TYPE_USER.equals(type)) {
                RestProjectComponentUser user = (RestProjectComponentUser) component;
                List<String> entityIds = user.findUsedEntityIds();

                List<ProjectComponent> entity = context.get(entityIds);
                for (ProjectComponent e : entity) {
                    states.add(State.from(e));
                }

                List<SlotPojo> slotPojos = user.getData().getSetSlots();
                if (CollectionUtils.isNotEmpty(slotPojos)) {
                    states.addAll(_convertSlotPojoToSates(slotPojos, context));
                }
                continue;
            }

            // TODO: 解析 bot text回复中的变量 ${xxx}
            if (ProjectComponent.TYPE_BOT.equals(type)) {
                RestProjectComponentBot bot = (RestProjectComponentBot) component;
                List<SlotPojo> slotPojos = bot.getData().getSetSlots();
                if (CollectionUtils.isNotEmpty(slotPojos)) {
                    states.addAll(_convertSlotPojoToSates(slotPojos, context));
                }

                List<ConditionPojo> conditionPojos = bot.getData().getConditions();
                if (CollectionUtils.isNotEmpty(conditionPojos)) {
                    states.addAll(_convertConditionToSates(conditionPojos, context));
                }

                // webhook 中新产生的变量
                for (RestProjectComponentBot.BotResponse response : bot.getData().getResponses()) {
                    if (response.isWebhook()) {
                        String webhookId = response.getContent().getString("id");

                        RestProjectComponentWebhook webhook = new RestProjectComponentWebhook(context.get(webhookId));
                        List<Pair> responseHandleParse = webhook.responseHandleParse();

                        // handler 中的 key
                        if (CollectionUtils.isNotEmpty(responseHandleParse)) {
                            responseHandleParse.forEach(p ->
                                    states.add(State.from(p.getKey().toString()))
                            );
                        }
                    }
                }

                continue;
            }

            if (ProjectComponent.TYPE_GPT.equals(type)) {
                RestProjectComponentGpt gpt = (RestProjectComponentGpt) component;
                List<RestProjectComponentGpt.Slot> slots = gpt.getData().getSlots();
                if (CollectionUtils.isNotEmpty(slots)) {
                    List<State> st = slots.stream()
                            .filter(s -> context.get(s.getSlotId()) != null)
                            .map(s -> State.from(context.get(s.getSlotId())))
                            .collect(Collectors.toList());
                    states.addAll(st);
                }
            }
        }

        this._states = states;
    }

    /**
     * using  label  to implements Goto
     *
     * @param children children nodes
     */
    private void _buildLabels4Goto(List<RestBaseProjectComponent> children) {
        // filter goto
        List<RestProjectComponentGoto> gotos = children.stream()
                .filter(c -> ProjectComponent.TYPE_GOTO.equals(c.getType()))
                .map(c -> (RestProjectComponentGoto) c)
                .collect(Collectors.toList());

        // no labels need to be add...
        if (gotos.isEmpty()) {
            return;
        }

        Map<String, RestBaseProjectComponent> componentMap = children.stream()
                .collect(Collectors.toMap(RestBaseProjectComponent::getId, c -> c));

        for (RestProjectComponentGoto gotoComponent : gotos) {
            String linkTo = gotoComponent.getData().getLinkId();
            if (StringUtils.isBlank(linkTo) || componentMap.get(linkTo) == null) {
                LOG.error("[failed to find goto link node:{}]", linkTo);
                continue;
            }

            RestBaseProjectComponent linkToNode = componentMap.get(linkTo);

            // label
            String label = linkToNode.getLabel();

            // build a new label
            if (StringUtils.isBlank(label)) {
                label = _buildLabelName();
                linkToNode.setLabel(label);

                LOG.info("[generate new label:{} for:{}]", label, linkTo);
            }

            // add label to goto
            gotoComponent.setNext(label);
        }


    }

    private JSONArray initMainSteps() {
        JSONArray steps = new JSONArray();
        steps.add(_buildBegin("main"));
        return steps;
    }

    private JSONObject _buildUser() {
        JSONObject user = new JSONObject();
        user.put("action", "user");
        return user;
    }

    private JSONObject _buildWebhook(RestProjectComponentWebhook webhook, PublishContext context) {
        Python python = new Python(webhook, context);
        return _buildCall(python.getName(), python.getRequired(), _argsTable);
    }

    private static JSONObject _buildBotText(String text) {
        JSONObject bot = new JSONObject();
        bot.put("action", "bot");
        bot.put("text", text);
        return bot;
    }

    public JSONObject toPublishFlow() {
        JSONObject flow = new JSONObject(true);
        flow.put("name", _name);
        flow.put("description", StringUtils.isBlank(_description) ? "-" : _description);
        flow.put("states", _states.stream().map(State::getName).collect(Collectors.toSet()));
        flow.put("steps", _steps);
        flow.put("subflows", _subflows);

        flow = _replaceArgsTable(flow);

        return flow;
    }

    /**
     * 将引用变量的地方替换成全称 (bot & webhook)
     *
     * @param flow 转换后的 flow
     */
    private JSONObject _replaceArgsTable(JSONObject flow) {
        String json = flow.toJSONString();
        for (String key : _argsTable.keySet()) {
            json = json.replace(VariablesHelper.wrap(key), VariablesHelper.wrap(_argsTable.get(key)));
        }

        return JSONObject.parseObject(json);
    }

    private JSONObject _buildBegin(String name) {
        JSONObject begin = new JSONObject();
        begin.put("action", "begin");
        begin.put("value", name);
        return begin;
    }

    private List<State> _convertSlotPojoToSates(List<SlotPojo> slotPojos, PublishContext context) {
        List<String> ids = slotPojos.stream()
                .map(SlotPojo::getSlotId)
                .collect(Collectors.toList());

        return context.get(ids).stream()
                .map(State::from)
                .collect(Collectors.toList());
    }

    private List<State> _convertConditionToSates(List<ConditionPojo> conditionPojos, PublishContext context) {
        List<String> ids = conditionPojos.stream()
                .map(ConditionPojo::getSlotId)
                .collect(Collectors.toList());

        return context.get(ids).stream()
                .map(State::from)
                .collect(Collectors.toList());
    }


    /**
     * 给一组需要 if 结构的节点，保证 if 的正确性，将它们归类
     * - 如果都没有 if ？ // 全部未设置条件
     * - 如果有多个 else // 多个未设置条件
     * <p>
     * 子节点类型应该是一致的
     */
    @Setter
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class IfHolder {

        /**
         * 有且仅有一个
         */
        private RestBaseProjectComponent _ifNode;

        /**
         * 0个或多个
         */
        private List<RestBaseProjectComponent> _elseIfNodes;

        /**
         * 最多只有一个
         */
        private RestBaseProjectComponent _elseNode;

        private String _type;

        public IfHolder(List<RestBaseProjectComponent> components) {
            if (components.size() <= 1) {
                // invalid if
                throw new RestException(StatusCodes.BadRequest);
            }

            _setType(components);

            // user 节点只有 if 和 else if
            if (ProjectComponent.TYPE_USER.equals(_type)) {
                this._ifNode = components.get(0);
                this._elseIfNodes = components.subList(1, components.size());
                return;
            }

            // as a pool
            Map<String, RestBaseProjectComponent> componentMap = components.stream()
                    .collect(Collectors.toMap(RestBaseProjectComponent::getId, c -> c));

            // filter if
            _ifNode = _filterIf(components);
            if (_ifNode == null) {
                throw new RestException(StatusCodes.BadRequest, "invalid if node");
            }
            componentMap.remove(_ifNode.getId());

            // filter else if
            _elseIfNodes = _filterElseIf(componentMap.values());
            for (RestBaseProjectComponent e : _elseIfNodes) {
                componentMap.remove(e.getId());
            }

            // filter else
            _elseNode = _filterElse(componentMap.values());
        }

        public static IfHolder build(List<RestBaseProjectComponent> components) {
            return new IfHolder(components);
        }

        private RestBaseProjectComponent _filterElse(Collection<RestBaseProjectComponent> components) {
            List<RestBaseProjectComponent> elseNodes = new ArrayList<>();
            for (RestBaseProjectComponent component : components) {
                RestProjectComponentBot bot = (RestProjectComponentBot) component;
                if (CollectionUtils.isEmpty(bot.getData().getConditions())) {
                    elseNodes.add(component);
                }
            }

            if (elseNodes.size() > 1) {
                throw new RestException(StatusCodes.BadRequest, "multi else bot found");
            }

            if (CollectionUtils.isNotEmpty(elseNodes)) {
                return elseNodes.get(0);
            }

            return null;
        }

        private void _setType(List<RestBaseProjectComponent> components) {
            String type = components.get(0).getType();
            for (RestBaseProjectComponent component : components) {
                if (type == null) {
                    type = component.getType();
                    continue;
                }

                if (!type.equals(component.getType())) {
                    throw new RestException(StatusCodes.BadRequest, "invalid  if components");
                }
            }

            // 只能是 bot 或 user
            if (!ProjectComponent.TYPE_USER.equals(type) && !ProjectComponent.TYPE_BOT.equals(type)) {
                throw new RestException(StatusCodes.BadRequest, "invalid  if components");
            }

            this._type = type;
        }

        private RestBaseProjectComponent _filterIf(List<RestBaseProjectComponent> components) {
            for (RestBaseProjectComponent component : components) {
                RestProjectComponentBot bot = (RestProjectComponentBot) component;
                if (CollectionUtils.isNotEmpty(bot.getData().getConditions())) {
                    return bot;
                }

            }

            return null;
        }

        private List<RestBaseProjectComponent> _filterElseIf(Collection<RestBaseProjectComponent> components) {
            List<RestBaseProjectComponent> elseIfNodes = new ArrayList<>();
            for (RestBaseProjectComponent component : components) {
                RestProjectComponentBot bot = (RestProjectComponentBot) component;

                if (CollectionUtils.isNotEmpty(bot.getData().getConditions())) {
                    elseIfNodes.add(component);
                }
            }
            return elseIfNodes;
        }
    }

    /**
     * 对应的 entity
     */
    @Builder
    @Setter
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    private static class State {
        private String _name;

        private String _type;

        private String _defaultValue;

        /**
         * 该方法目前用于 webhook 中新产生的变量，只有 name，并且没有默认值
         */
        public static State from(String name) {
            return State.builder()
                    .name(name)
                    .type(RestProjectComponentEntity.SLOT_TYPE_STRING)
                    .build();
        }

        public static State from(ProjectComponent entity) {
            if (entity == null) {
                return null;
            }

            String type = entity.getType();
            if (ProjectComponent.TYPE_ENTITY.equals(type)) {
                RestProjectComponentEntity e = new RestProjectComponentEntity(entity);
                return State.builder()
                        .name(e.getName())
                        .type(e.getSlotType())
                        .defaultValue(e.getDefaultValue())
                        .build();
            }

            throw new RestException(StatusCodes.BadRequest);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            State state = (State) o;
            return Objects.equals(_name, state._name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(_name);
        }
    }

}
