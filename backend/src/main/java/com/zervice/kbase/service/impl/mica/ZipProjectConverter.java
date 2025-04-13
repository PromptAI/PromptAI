package com.zervice.kbase.service.impl.mica;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.zervice.common.i18n.MessageUtils;
import com.zervice.common.restful.exception.RestException;
import com.zervice.common.restful.exception.StatusCodes;
import com.zervice.common.utils.Constants;
import com.zervice.kbase.ai.convert.condition.BotConditionHelper;
import com.zervice.kbase.ai.convert.pojo.Python;
import com.zervice.kbase.ai.output.zip.pojo.ZipContainer;
import com.zervice.kbase.api.restful.pojo.*;
import com.zervice.kbase.api.restful.pojo.mica.ConditionPojo;
import com.zervice.kbase.api.restful.pojo.mica.SlotPojo;
import com.zervice.kbase.database.pojo.Project;
import com.zervice.kbase.database.pojo.ProjectComponent;
import com.zervice.kbase.service.impl.mica.pojo.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Import from mica zip
 */
@Setter
@Getter
@Log4j2
@AllArgsConstructor
@NoArgsConstructor
public class ZipProjectConverter {
    private final AtomicInteger _userCounter = new AtomicInteger(1);
    private final AtomicInteger _flowCounter = new AtomicInteger(1);
    /**
     * name & gpt
     */
    private Map<String, Gpt> _gpts;
    /**
     * name & flow
     */
    private Map<String, Flow> _flows;
    /**
     * name & webhook
     */
    private Map<String, Webhook> _webhooks;
    /**
     * name & functions
     */
    private Map<String, Function> _functions;

    /**
     * name & label
     */
    private Map<String, Label> _labels = new HashMap<>();

    /**
     * 由于存在 build goto时目标节点还没有被编辑好。此时将这一组 label存起来，待 steps绘制完成后goto填充一下即可。
     */
    private Map<Label, ProjectComponent> _laterHandleLabels = new HashMap<>();

    private String _locale;

    public ZipProjectConverter(JSONObject data, JSONObject functions, String locale) {
        this._functions = _parseFunctions(data, functions);
        this._gpts = _parseGpts(data);
        this._webhooks = _parseWebhooks(functions);
        this._flows = _parseFlows(data);
        this._locale = locale;
    }

    public List<ProjectComponent> generate(Project project) {
        String projectId = project.getId();
        // generate entity
        Map<String /* name */, RestProjectComponentEntity> entities = _generateEntities(projectId);

        // generate webhook
        Map<String /* name */, RestProjectComponentWebhook> webhooks = _generateWebhooks(projectId);

        // generate flow
        return _generateFlows(project.getId(), entities, webhooks);
    }

    /**
     * 当需要用户输入时，默认使用模板，并自动生成用户编号
     */
    public String _applyUserSay() {
        return MessageUtils.get(Constants.I18N_YAML_USER_TEMPLATE, _locale) + " - " + _userCounter.getAndIncrement();
    }

    public void _resetApplyUserSay() {
        _userCounter.set(1);
    }

    private List<ProjectComponent> _generateFlows(String projectId,
                                                  Map<String, RestProjectComponentEntity> entities,
                                                  Map<String, RestProjectComponentWebhook> webhooks) {
        List<ProjectComponent> projectComponents = new ArrayList<>();
        for (String flowName : _flows.keySet()) {
            Flow f = _flows.get(flowName);
            projectComponents.addAll(_generateFlow(projectId, f, entities, webhooks));
        }

        // 为独立的 GPT 生成一个 Flow去存储
        for (Gpt gpt : _gpts.values()) {
            if (!gpt.getUsedByFlow()) {
                projectComponents.addAll(_generateForSingleGPT(projectId, gpt, entities, webhooks));
            }
        }

        projectComponents.addAll(entities.values().stream()
                .peek(p -> p.setProjectId(projectId))
                .map(RestProjectComponentEntity::toProjectComponent).collect(Collectors.toList()));
        projectComponents.addAll(webhooks.values().stream()
                .peek(p -> p.setProjectId(projectId))
                .map(RestProjectComponentWebhook::toProjectComponent).collect(Collectors.toList()));

        return projectComponents;
    }

    /**
     * 生成一个虚拟 flow 来容纳独立的 Agent
     */
    private List<ProjectComponent> _generateForSingleGPT(String projectId, Gpt gpt, Map<String, RestProjectComponentEntity> entities,
                                                         Map<String, RestProjectComponentWebhook> webhooks) {
        // Same name & desc with gpt
        String name = gpt.getName();
        String desc = gpt.getDescription();

        Flow virtual = Flow.virtual(name, desc, gpt);
        return _generateFlow(projectId, virtual, entities, webhooks);
    }

    private List<ProjectComponent> _generateFlow(String projectId, Flow flow,
                                                 Map<String, RestProjectComponentEntity> entities,
                                                 Map<String, RestProjectComponentWebhook> webhooks) {

        RestProjectComponentConversation root = RestProjectComponentConversation.newInstance(projectId, flow.getName(), flow.getType(), flow.getDescription());
        JSONArray steps = _registryLabel(flow.getSteps());

        // 重置user say 计数器
        _resetApplyUserSay();

        List<ProjectComponent> projectComponents = new ArrayList<>();
        projectComponents.add(root.toProjectComponent());

        String rootId = root.getId();
        RestBaseProjectComponent parent = root;

        _convertSteps(projectComponents, projectId, rootId, parent.getId(), steps, entities, webhooks);

        // 所有 step绘制完成，处理延迟表
        for (Label label : _laterHandleLabels.keySet()) {
            ProjectComponent gotoComponent = _laterHandleLabels.get(label);
            ProjectComponent linkComponent = label.findLabel(projectComponents);
            if (linkComponent != null) {
                gotoComponent.updateLinkId(linkComponent.getId());
            } else {
                LOG.warn("[failed to find link object:{} for goto:{}]", label.getValue(), gotoComponent.getId());
            }
        }

        return projectComponents;
    }

    /**
     * 在转换 step 之前，需要构建 labels引用表，这样无需担心在 label/next 的先后顺序.
     * 这里做两件事：
     * 1、注册 label
     * 2、移除 label标签
     *
     * @param steps 处理后的 label
     */
    private JSONArray _registryLabel(JSONArray steps) {
        List<Object> newSteps = new ArrayList<>(steps.size());

        for (int i = 0; i < steps.size(); i++) {
            Object item = steps.get(i);
            if (item instanceof JSONObject) {
                JSONObject step = (JSONObject) item;

                // 找到 label
                if (step.containsKey("label")) {
                    String value = step.getString("label");

                    if (steps.size() > i + 1) {
                        Object nextStep = _findUsableNextStep(steps, i);
                        if (nextStep instanceof JSONObject) {
                            _labels.put(value, Label.factory(value, (JSONObject) nextStep));
                            continue;
                        }

                        String nextStepStr = nextStep.toString();
                        // 这里 user的 user 在文件中没有 the user claims "Small",需要申请一句占位符
                        // 这里的占位符与序号有关，那么只要保持_registryLabel 与 convertSteps遍历顺序一直，那么这里的_applyUserSay结果应该是一样的
                        if (Label.REFERENCE_USER.equals(nextStepStr)) {
                            _labels.put(value, Label.factory(value, Label.REFERENCE_USER, _applyUserSay()));
                        }
                    }
                    continue;
                }

                if (step.containsKey("then")) {
                    JSONArray then = step.getJSONArray("then");
                    step.put("then", _registryLabel(then));
                }

                if (step.containsKey("else")) {
                    JSONArray elseSteps = step.getJSONArray("else");
                    step.put("else", _registryLabel(elseSteps));
                }

                newSteps.add(item);
            } else {
                newSteps.add(item);
            }
        }

        return new JSONArray(newSteps);
    }

    /**
     * 当寻找label挂载点时，下列的set不能作为挂载（因为set 需要挂载到前或后的节点），需要找到下一个step
     * <pre>
     *      - if: size != None
     *     then:
     *     - label: label_0
     *     - set:
     *         size: '100'
     *     - bot: Here is the ${size}-inch pizza you ordered.
     * </pre>
     * @param steps
     * @param currentIndex
     * @return
     */
    private JSONObject _findUsableNextStep(JSONArray steps, int currentIndex) {
        for (int i = currentIndex + 1; i < steps.size(); i++) {
            JSONObject step = (JSONObject) steps.get(i);
            // 可以挂载的类型
            for (String validReference : Label.REFERENCE_TYPES) {
                if (step.containsKey(validReference)) {
                    return step;
                }
            }
        }

        return null;
    }

    private String _convertSteps(List<ProjectComponent> components, String projectId, String rootId,
                                 String parentId, JSONArray steps,
                                 Map<String, RestProjectComponentEntity> entities,
                                 Map<String, RestProjectComponentWebhook> webhooks) {
        for (int i = 0; i < steps.size(); i++) {
            Object item = steps.get(i);
            // Why we need this "_nextStep"？
            // 当只有 if 的时候，后续的节点需要跟在 if.then的最后一个节点之后，否则导前后结构不一致
            // 所以这里添加了 _nextStep 用于在 if 节点判断，是否需要回传最后一个节点 id
            Object _nextStep = null;
            if (steps.size() > i + 1) {
                _nextStep = steps.get(i + 1);
            }

            if (item instanceof JSONObject) {
                JSONObject step = (JSONObject) item;
                String newParentId = handleJsonStep(step, _nextStep, components, projectId, rootId, parentId, entities, webhooks);
                if (newParentId != null) {
                    parentId = newParentId;
                }
                continue;
            }

            // handle string item
            String itemStr = item.toString();
            if (itemStr.equals("user") && !_nextIsUserIF(steps, i)) {
                RestProjectComponentUser user = RestProjectComponentUser.newInstance(projectId, parentId, rootId, List.of(_applyUserSay()));
                components.add(user.toProjectComponent());
                parentId = user.getId();
                continue;
            }

            LOG.warn("[ignore step:{}]", item);
        }

        return parentId;
    }

    private String handleJsonStep(JSONObject step, Object nextStep, List<ProjectComponent> components, String projectId, String rootId,
                                  String parentId, Map<String, RestProjectComponentEntity> entities,
                                  Map<String, RestProjectComponentWebhook> webhooks) {
        // Handle bot message
        if (step.containsKey("bot")) {
            return handleBotMessage(step, components, projectId, parentId, rootId);
        }

        // Handle webhook or gpt call
        if (step.containsKey("call")) {
            return handleCall(step, components, projectId, parentId, rootId, entities, webhooks);
        }

        // Handle if condition
        if (step.containsKey("if")) {
            return handleCondition(step, nextStep, "if", components, projectId, parentId, rootId, entities, webhooks);
        }

        // Handle else if condition
        if (step.containsKey("else if")) {
            handleCondition(step, nextStep, "else if", components, projectId, parentId, rootId, entities, webhooks);
            return null;
        }

        // only bot node need this step
        if (step.containsKey("else")) {
            handleCondition(step, nextStep, "else", components, projectId, parentId, rootId, entities, webhooks);
        }

        // label for goto
        if (step.containsKey("label")) {
            handleLabel(step, nextStep);
        }

        // goto
        if (step.containsKey("next")) {
            return handleNext(step, components, projectId, parentId, rootId);
        }

        // set
        if (step.containsKey("set")) {
            handleSet(step, components, parentId, entities);
        }

        return null;
    }

    private void handleSet(JSONObject step, List<ProjectComponent> components,
                           String parentId, Map<String, RestProjectComponentEntity> entities) {

        List<SlotPojo> slotPojos = _buildSetSlotFromSet(step, entities);
        if (slotPojos.isEmpty()) {
            return;
        }
        ProjectComponent component = _findById(parentId, components);
        if (component == null) {
            LOG.error("[failed to find set slot's parent:{}]", parentId);
            return;
        }

        component.updateSetSlots(slotPojos);
    }

    private List<SlotPojo> _buildSetSlotFromSet(JSONObject set, Map<String, RestProjectComponentEntity> entities) {
        JSONObject sets = set.getJSONObject("set");
        List<SlotPojo> slotPojos = new ArrayList<>(sets.size());
        for (String key : sets.keySet()) {
            RestProjectComponentEntity e = entities.get(key);
            if (e == null) {
                LOG.error("[failed to find set slot entity:{}]", key);
                continue;
            }

            String value = sets.getString(key);
            slotPojos.add(SlotPojo.factory(e.getId(), value));
        }
        return slotPojos;
    }

    private ProjectComponent _findById(String id, List<ProjectComponent> components) {
        for (ProjectComponent component : components) {
            if (id.equals(component.getId())) {
                return component;
            }
        }

        return null;
    }

    private String handleNext(JSONObject step, List<ProjectComponent> components,
                              String projectId, String parentId, String rootId) {
        String label = step.getString("next");
        Label labelObj = _labels.get(label);
        if (labelObj != null) {

            ProjectComponent linkedComponent = labelObj.findLabel(components);
            String componentId = linkedComponent == null ? null : linkedComponent.getId();

            RestProjectComponentGoto restGoto = RestProjectComponentGoto.newInstance(projectId, parentId, rootId, componentId);
            ProjectComponent gotoProjectComponent = restGoto.toProjectComponent();

            // 目标节点不存在？ 加入延迟处理表，待 steps绘制完成后渲染这个节点
            if (componentId == null) {
                _laterHandleLabels.put(labelObj, gotoProjectComponent);
            }

            components.add(gotoProjectComponent);
            return gotoProjectComponent.getId();
        }

        LOG.error("[label{} not found]", label);
        throw new RestException(StatusCodes.BadRequest, "label:{} " + label + " not found in labels map");

    }

    private void handleLabel(JSONObject step, Object nextStep) {
        String label = step.getString("label");
        if (nextStep instanceof JSONObject) {
            JSONObject nextStepObj = (JSONObject) nextStep;
            if (nextStepObj.containsKey(Label.REFERENCE_BOT)) {
                String botValue = nextStepObj.getString(Label.REFERENCE_BOT);

                Label labelObj = Label.factory(label, Label.REFERENCE_BOT, botValue);
                _labels.put(label, labelObj);
            }
        }
    }

    private String handleBotMessage(JSONObject step, List<ProjectComponent> components, String projectId, String parentId, String rootId) {
        String text = step.getString("bot");
        RestProjectComponentBot bot = RestProjectComponentBot.newInstance(projectId, parentId, rootId, text);
        components.add(bot.toProjectComponent());
        return bot.getId();
    }

    private String handleCall(JSONObject step, List<ProjectComponent> components, String projectId, String parentId,
                              String rootId, Map<String, RestProjectComponentEntity> entities,
                              Map<String, RestProjectComponentWebhook> webhooks) {
        String gptOrWebhook = step.getString("call");
        // Try GPT First
        Gpt gpt = _gpts.get(gptOrWebhook);
        if (gpt != null) {
            // 标记被 FlowAgent 使用了
            gpt.setUsedByFlow(true);

            return handleGptCall(gpt, components, projectId, parentId, rootId, entities);
        }

        // Try webhook first
        RestProjectComponentWebhook webhook = webhooks.get(gptOrWebhook);
        if (webhook != null) {
            RestProjectComponentBot bot = RestProjectComponentBot.newInstance(projectId, parentId, rootId, webhook);
            components.add(bot.toProjectComponent());
            return bot.getId();
        }


        LOG.error("[unknown call:{} ]", gptOrWebhook);
        throw new RestException(StatusCodes.BadRequest, "unknown call object");
    }

    private String handleGptCall(Gpt gpt, List<ProjectComponent> components, String projectId, String parentId,
                                 String rootId, Map<String, RestProjectComponentEntity> entities) {
        List<RestProjectComponentGpt.Slot> slots = gpt.getArgs().stream()
                .filter(entities::containsKey)
                .map(s -> {
                    RestProjectComponentEntity e = entities.get(s);
                    return RestProjectComponentGpt.Slot.factory(e.getId(), RestProjectComponentUser.Mapping.TYPE_FROM_ENTITY);
                })
                .collect(Collectors.toList());

        List<RestProjectComponentGpt.FunctionCalling> functionCallings = gpt.getUses().stream()
                .filter(_functions::containsKey)
                .map(fc -> {
                    Function function = _functions.get(fc);
                    // 标记被GPT使用了
                    function.setUsedByGPT(true);
                    return RestProjectComponentGpt.FunctionCalling.factory(function.getName(), function.getBody());
                }).collect(Collectors.toList());

        RestProjectComponentGpt gptC = RestProjectComponentGpt.newInstance(projectId, parentId, rootId,
                gpt.getName(), gpt.getPrompt(), gpt.getDescription(), slots, functionCallings);
        components.add(gptC.toProjectComponent());

        return gptC.getId();
    }

    private String handleCondition(JSONObject step, Object nextStep, String conditionType, List<ProjectComponent> components,
                                   String projectId, String parentId, String rootId,
                                   Map<String, RestProjectComponentEntity> entities,
                                   Map<String, RestProjectComponentWebhook> webhooks) {

        boolean needReturnLastNodeId = false;
        // 如果 if 没有 else if && else配对，那么需要返回当前 if 的最后一个 id 当做 parent，否则后续节点连不上
        if ("if".equals(conditionType) && nextStep != null) {
            if (nextStep instanceof JSONObject) {
                JSONObject n = (JSONObject) nextStep;
                boolean nextStepHasElse = n.containsKey("else") || n.containsKey("else if");
                needReturnLastNodeId = !nextStepHasElse;
            } else {
                // 非 json,不存在 else && else if
                needReturnLastNodeId = true;
            }
        }
        String condition = step.getString(conditionType);

        if (condition.startsWith("the user claims")) {
            return handleUserClaim(step, needReturnLastNodeId, condition, components, projectId, rootId, parentId, entities, webhooks);
        } else {
            return handleBotCondition(step, needReturnLastNodeId, condition, components, projectId, rootId, parentId, entities, webhooks);
        }
    }

    private String handleUserClaim(JSONObject step, boolean needReturnLastNodeId, String condition, List<ProjectComponent> components,
                                   String projectId, String rootId, String parentId,
                                   Map<String, RestProjectComponentEntity> entities,
                                   Map<String, RestProjectComponentWebhook> webhooks) {

        Set<String> userExample = _parseUserIf(condition);
        RestProjectComponentUser user = RestProjectComponentUser.newInstance(projectId, parentId, rootId, new ArrayList<>(userExample));
        components.add(user.toProjectComponent());

        String lastNodeId = user.getId();

        JSONArray then = step.getJSONArray("then");
        if (CollectionUtils.isNotEmpty(then)) {
            lastNodeId = _convertSteps(components, projectId, rootId, user.getId(), then, entities, webhooks);
        }

        return needReturnLastNodeId ? lastNodeId : null;
    }

    private String handleBotCondition(JSONObject step, boolean needReturnLastNodeId, String condition, List<ProjectComponent> components,
                                      String projectId, String rootId, String parentId,
                                      Map<String, RestProjectComponentEntity> entities,
                                      Map<String, RestProjectComponentWebhook> webhooks) {
        // else
        if (step.containsKey("else")) {
            JSONArray steps = step.getJSONArray("else");
            _convertSteps(components, projectId, rootId, parentId, steps, entities, webhooks);
            return null;
        }

        // if or else if
        List<ConditionPojo> conditionPojos = BotConditionHelper.parseConditions(condition);
        conditionPojos.forEach(c -> {
            RestProjectComponentEntity e = entities.get(c.getSlotName());
            c.setSlotId(e.getId());
        });

        JSONArray then = step.getJSONArray("then");
        // 这里可能是 webhook //  call or bot
        JSONObject firstChild = then.getJSONObject(0);

        // 需要继续处理的step index，接下来会单独处理第一个可用bot节点？便于挂载判断条件
        int thenStepIndex = 1;

        RestProjectComponentBot bot = null;
        if (firstChild.containsKey("bot")) {
            String text = firstChild.getString("bot");
            bot = RestProjectComponentBot.newInstance(projectId, parentId, rootId, text, conditionPojos);
        } else if (firstChild.containsKey("set")){
            // - if: size != None
            //    then:
            //    - label: label_0
            //    - set:
            //        size: '100'
            //    - bot: Here is the ${size}-inch pizza you ordered.
            List<SlotPojo> setSlots = _buildSetSlotFromSet(firstChild, entities);
            // 再下一个节点才是bot?
            if (then.size() > 1) {
                JSONObject nextChild = then.getJSONObject(1);
                if (nextChild.containsKey("bot")) {
                    String text  = nextChild.getString("bot");
                    bot = RestProjectComponentBot.newInstance(projectId, parentId, rootId, text, conditionPojos);
                    bot.getData().setSetSlots(setSlots);

                    // 这已经往下读取了一个节点，从这开始继续处理后面的
                    thenStepIndex = 2;
                }
            }
        } else {
            // 这里的 bot的第一个 child 是 webhook
            String webhookName = firstChild.getString("call");
            RestProjectComponentWebhook webhook = webhooks.get(webhookName);
            bot = RestProjectComponentBot.newInstance(projectId, parentId, rootId, webhook, conditionPojos);
        }

        components.add(bot.toProjectComponent());

        String lastNodeId = bot.getId();

        if (then.size() > 1) {
            then = new JSONArray(then.subList(thenStepIndex, then.size()));
            lastNodeId = _convertSteps(components, projectId, rootId, bot.getId(), then, entities, webhooks);
        }

        return needReturnLastNodeId ? lastNodeId : null;
    }

    /**
     * 判断下一个节点是否为 user if
     *
     * @param steps current steps
     * @param i     current step index
     * @return Return ture if next node is an if
     */
    private boolean _nextIsUserIF(JSONArray steps, int i) {
        if (steps.size() > i + 1) {
            Object next = steps.get(i + 1);
            if (next instanceof JSONObject) {
                JSONObject nextObj = (JSONObject) next;
                return nextObj.containsKey("if") && nextObj.getString("if").startsWith("the user claims");
            }
        }

        return false;
    }

    private Set<String> _parseUserIf(String userIfCondition) {
        // 使用正则表达式提取引号内的内容
        Pattern pattern = Pattern.compile("\"([^\"]*)\"");
        Matcher matcher = pattern.matcher(userIfCondition);

        // 如果找到匹配项
        if (matcher.find()) {
            String quotedText = matcher.group(1); // 提取引号内的内容
            // 将提取的内容按逗号分隔并转换成数组
            String[] result = quotedText.split(",");
            return Arrays.stream(result).collect(Collectors.toSet());
        }

        return Set.of();
    }

    private Map<String, RestProjectComponentWebhook> _generateWebhooks(String projectId) {
        return _webhooks.values().stream()
                .filter(w -> !Python.CONTEXT_VARIABLES_NAME.equals(w.getName()))
                .map(w -> w.toComponent(projectId))
                .collect(Collectors.toMap(RestProjectComponentWebhook::getText, w -> w));
    }

    /**
     * generate entity
     *
     * @param projectId project id
     * @return map of entity name and entity
     */
    private Map<String, RestProjectComponentEntity> _generateEntities(String projectId) {
        Set<String> entities = new HashSet<>();

        // flows
        for (Map.Entry<String, Flow> entry : _flows.entrySet()) {
            String name = entry.getKey();
            Flow flow = _flows.get(name);
            entities.addAll(flow.getArgs());
        }

        // gpts
        for (Map.Entry<String, Gpt> entry : _gpts.entrySet()) {
            String name = entry.getKey();
            Gpt gpt = _gpts.get(name);
            entities.addAll(gpt.getArgs());
        }

        // webhook
        for (Map.Entry<String, Webhook> entry : _webhooks.entrySet()) {
            String name = entry.getKey();
            Webhook webhook = _webhooks.get(name);

            // webhook 需要的 entity，产生的 entity 暂时不纳入
            entities.addAll(webhook.getRequired());
        }

        return entities.stream().map(e -> new RestProjectComponentEntity(e, projectId))
                .collect(Collectors.toMap(RestProjectComponentEntity::getName, e -> e));
    }

    private Map<String, Flow> _parseFlows(JSONObject data) {
        Map<String, JSONObject> flows = new HashMap<>(16);
        for (String key : data.keySet()) {
            Object obj = data.get(key);
            if (obj instanceof JSONObject) {
                JSONObject item = data.getJSONObject(key);
                String type = item.getString("type");
                if (ZipContainer.TYPE_FLOW_AGENT.equals(type)) {
                    flows.put(key, item);
                }
            }
        }
        return Flow.parse(flows).stream()
                .collect(Collectors.toMap(Flow::getName, f -> f));
    }

    private Map<String, Gpt> _parseGpts(JSONObject data) {
        Map<String, JSONObject> gpts = new HashMap<>(16);
        for (String key : data.keySet()) {
            Object obj = data.get(key);
            if (obj instanceof JSONObject) {
                JSONObject item = (JSONObject) obj;
                String type = item.getString("type");
                if (ZipContainer.TYPE_LLM_AGENT.equals(type)) {
                    gpts.put(key, item);
                }
            }
        }

        return Gpt.parse(gpts).stream()
                .collect(Collectors.toMap(Gpt::getName, g -> g));
    }

    private Map<String, Webhook> _parseWebhooks(JSONObject pythonFunctions) {
        // gpt 节点用到的 function,从pythonFunctions排除这些，剩下的就是webhook
        Set<String> gptUsedFunctions = _gpts.values().stream()
                .filter(g -> CollectionUtils.isNotEmpty(g.getUses()))
                .flatMap(g -> g.getUses().stream())
                .collect(Collectors.toSet());


        Map<String, Function> webhooks = new HashMap<>(16);
        for (String key : pythonFunctions.keySet()) {

            // 是function && 未被GPT使用，这时判定未webhook
            if (!gptUsedFunctions.contains(key)) {
                webhooks.put(key, _functions.get(key));
            }
        }

        return Webhook.parse(webhooks).stream()
                .collect(Collectors.toMap(Webhook::getName, w -> w));
    }

    /**
     * function map
     */
    private Map<String, Function> _parseFunctions(JSONObject data, JSONObject pythonFunctions) {
        Map<String, JSONObject> functions = new HashMap<>(16);
        for (String key : data.keySet()) {
            Object obj = data.get(key);
            if (obj instanceof JSONObject) {
                JSONObject item = (JSONObject) data.get(key);
                String type = item.getString("type");
                JSONArray args = item.getJSONArray("args");
                if (ZipContainer.TYPE_FUNCTION.equals(type) && !Python.CONTEXT_VARIABLES_NAME.equals(key)) {

                    // python 代码
                    String body = pythonFunctions.getString(key);
                    if (StringUtils.isNotBlank(body)) {

                        // 这里重新包装Function -- 如果args存在，当前一定是webhook
                        JSONObject function = new JSONObject();
                        function.put("body", body);
                        function.put("args", args);
                        functions.put(key, function);
                        continue;
                    }

                    LOG.warn("[function:{} not exist]", key);
                }
            }
        }

        // functions 在 agents.yml中未注册（部分或全部），那么直接使用 pythons
        if (pythonFunctions.size() != functions.size()) {
            LOG.warn("[functions not registry in yml, using pythonFunctions build functions]");
            functions.clear();

            for (String key : pythonFunctions.keySet()) {
                JSONObject function = new JSONObject();
                function.put("body", pythonFunctions.get(key));
                function.put("args", Set.of());
                functions.put(key, function);
            }
        }

        return Function.parse(functions).stream()
                .collect(Collectors.toMap(Function::getName, f -> f));
    }

}