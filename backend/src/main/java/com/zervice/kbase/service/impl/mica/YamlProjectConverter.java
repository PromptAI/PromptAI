package com.zervice.kbase.service.impl.mica;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.zervice.common.i18n.MessageUtils;
import com.zervice.common.restful.exception.RestException;
import com.zervice.common.restful.exception.StatusCodes;
import com.zervice.common.utils.Constants;
import com.zervice.kbase.ai.convert.condition.BotConditionHelper;
import com.zervice.kbase.ai.convert.pojo.Python;
import com.zervice.kbase.api.restful.pojo.*;
import com.zervice.kbase.api.restful.pojo.mica.ConditionPojo;
import com.zervice.kbase.database.pojo.Project;
import com.zervice.kbase.database.pojo.ProjectComponent;
import com.zervice.kbase.service.impl.mica.pojo.Flow;
import com.zervice.kbase.service.impl.mica.pojo.Function;
import com.zervice.kbase.service.impl.mica.pojo.Gpt;
import com.zervice.kbase.service.impl.mica.pojo.Webhook;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * import from mica yaml
 * - This convert need to be fixed. Some logic needs to be synchronized from {@link ZipProjectConverter}
 */
@Setter
@Getter
@Log4j2
@Deprecated
@AllArgsConstructor
@NoArgsConstructor
public class YamlProjectConverter {
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
    private String _locale;

    public YamlProjectConverter(JSONObject yaml, String locale) {
        this._gpts = _parseGpts(yaml);
        this._flows = _parseFlows(yaml);
        this._webhooks = _parseWebhooks(yaml);
        this._functions = _parseFunctions(yaml);

        this._locale = locale;
    }

    public List<ProjectComponent> generate(Project project) {
        String projectId = project.getId();
        // generate entity
        Map<String, RestProjectComponentEntity> entities = _generateEntities(projectId);

        // generate webhook
        Map<String, RestProjectComponentWebhook> webhooks = _generateWebhooks(projectId);

        // generate flow
        return _generateFlows(project.getId(), entities, webhooks);
    }

    /**
     * 当需要用户输入时，默认使用模板，并自动生成用户编号
     */
    private String _applyUserSay() {
        return MessageUtils.get(Constants.I18N_YAML_USER_TEMPLATE, _locale) + " - " + _userCounter.getAndIncrement();
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
        JSONArray steps = flow.getSteps();

        List<ProjectComponent> projectComponents = new ArrayList<>();
        projectComponents.add(root.toProjectComponent());

        String rootId = root.getId();
        RestBaseProjectComponent parent = root;

        _convertSteps(projectComponents, projectId, rootId, parent.getId(), steps, entities, webhooks);

        return projectComponents;
    }

    private void _convertSteps(List<ProjectComponent> components, String projectId, String rootId,
                               String parentId, JSONArray steps,
                               Map<String, RestProjectComponentEntity> entities,
                               Map<String, RestProjectComponentWebhook> webhooks) {
        for (int i = 0; i < steps.size(); i++) {
            Object item = steps.get(i);
            if (item instanceof JSONObject) {
                JSONObject step = (JSONObject) item;
                String newParentId = handleJsonStep(step, components, projectId, rootId, parentId, entities, webhooks, steps, i);
                if (newParentId != null) {
                    parentId = newParentId;
                }
                continue;
            }

            // handle string item
            String itemStr = item.toString();
            if (itemStr.equals("user") && !_nextIsIF(steps, i)) {
                RestProjectComponentUser user = RestProjectComponentUser.newInstance(projectId, parentId, rootId, List.of(_applyUserSay()));
                components.add(user.toProjectComponent());
                parentId = user.getId();
                continue;
            }

            LOG.warn("[ignore step:{}]", item);
        }
    }

    private String handleJsonStep(JSONObject step, List<ProjectComponent> components, String projectId, String rootId,
                                  String parentId, Map<String, RestProjectComponentEntity> entities,
                                  Map<String, RestProjectComponentWebhook> webhooks, JSONArray steps, int currentIndex) {
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
            handleCondition(step, "if", components, projectId, rootId, parentId, entities, webhooks);
            return null;
        }

        // Handle else if condition
        if (step.containsKey("else if")) {
            handleCondition(step, "else if", components, projectId, rootId, parentId, entities, webhooks);
            return null;
        }

        return null;
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

        // Try webhook first
        RestProjectComponentWebhook webhook = webhooks.get(gptOrWebhook);
        if (webhook != null) {
            RestProjectComponentBot bot = RestProjectComponentBot.newInstance(projectId, parentId, rootId, webhook);
            components.add(bot.toProjectComponent());
            return bot.getId();
        }

        // Try GPT
        Gpt gpt = _gpts.get(gptOrWebhook);
        if (gpt != null) {
            // 标记被 FlowAgent 使用了
            gpt.setUsedByFlow(true);

            return handleGptCall(gpt, components, projectId, parentId, rootId, entities);
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
                    return RestProjectComponentGpt.FunctionCalling.factory(function.getName(), function.getBody());
                }).collect(Collectors.toList());

        RestProjectComponentGpt gptC = RestProjectComponentGpt.newInstance(projectId, parentId, rootId,
                gpt.getName(), gpt.getPrompt(), gpt.getDescription(), slots, functionCallings);
        components.add(gptC.toProjectComponent());

        return gptC.getId();
    }

    private void handleCondition(JSONObject step, String conditionType, List<ProjectComponent> components,
                                 String projectId, String rootId, String parentId,
                                 Map<String, RestProjectComponentEntity> entities,
                                 Map<String, RestProjectComponentWebhook> webhooks) {
        String condition = step.getString(conditionType);

        if (condition.startsWith("the user claims")) {
            handleUserClaim(step, condition, components, projectId, rootId, parentId, entities, webhooks);
        } else {
            handleBotCondition(step, condition, components, projectId, rootId, parentId, entities, webhooks);
        }
    }

    private void handleUserClaim(JSONObject step, String condition, List<ProjectComponent> components,
                                 String projectId, String rootId, String parentId,
                                 Map<String, RestProjectComponentEntity> entities,
                                 Map<String, RestProjectComponentWebhook> webhooks) {
        Set<String> userExample = _parseUserIf(condition);
        RestProjectComponentUser user = RestProjectComponentUser.newInstance(projectId, parentId, rootId, new ArrayList<>(userExample));
        components.add(user.toProjectComponent());

        JSONArray then = step.getJSONArray("then");
        if (CollectionUtils.isNotEmpty(then)) {
            _convertSteps(components, projectId, rootId, user.getId(), then, entities, webhooks);
        }
    }

    private void handleBotCondition(JSONObject step, String condition, List<ProjectComponent> components,
                                    String projectId, String rootId, String parentId,
                                    Map<String, RestProjectComponentEntity> entities,
                                    Map<String, RestProjectComponentWebhook> webhooks) {
        List<ConditionPojo> conditionPojos = BotConditionHelper.parseConditions(condition);
        conditionPojos.forEach(c -> {
            RestProjectComponentEntity e = entities.get(c.getSlotName());
            c.setSlotId(e.getId());
        });

        JSONArray then = step.getJSONArray("then");
        JSONObject firstBot = then.getJSONObject(0);
        String text = firstBot.getString("bot");

        RestProjectComponentBot bot = RestProjectComponentBot.newInstance(projectId, parentId, rootId, text, conditionPojos);
        components.add(bot.toProjectComponent());

        if (then.size() > 1) {
            then = new JSONArray(then.subList(1, then.size()));
            _convertSteps(components, projectId, rootId, bot.getId(), then, entities, webhooks);
        }
    }

    /**
     * 判断下一个节点是否为 if
     *
     * @param steps current steps
     * @param i     current step index
     * @return Return ture if next node is an if
     */
    private boolean _nextIsIF(JSONArray steps, int i) {
        if (steps.size() >= i + 1) {
            Object next = steps.get(i + 1);
            if (next instanceof JSONObject) {
                return ((JSONObject) next).containsKey("if");
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

        for (Map.Entry<String, Webhook> entry : _webhooks.entrySet()) {
            String name = entry.getKey();
            Webhook webhook = _webhooks.get(name);

            // webhook 需要的 entity，产生的 entity 暂时不纳入
            entities.addAll(webhook.getRequired());
        }

        return entities.stream().map(e -> new RestProjectComponentEntity(e, projectId))
                .collect(Collectors.toMap(RestProjectComponentEntity::getName, e -> e));
    }

    private Map<String, Flow> _parseFlows(JSONObject yaml) {
        return Flow.parse(yaml.getJSONObject("data").getJSONObject("flow_agents")).stream()
                .collect(Collectors.toMap(Flow::getName, f -> f));
    }

    private Map<String, Gpt> _parseGpts(JSONObject yaml) {
        return Gpt.parse(yaml.getJSONObject("data").getJSONObject("llm_agents")).stream()
                .collect(Collectors.toMap(Gpt::getName, g -> g));
    }

    private Map<String, Webhook> _parseWebhooks(JSONObject yaml) {
        return Webhook.parse(yaml.getJSONObject("data").getJSONObject("pythons")).stream()
                .collect(Collectors.toMap(Webhook::getName, w -> w));
    }

    /**
     * function map
     */
    private Map<String, Function> _parseFunctions(JSONObject yaml) {
        return Function.parse(yaml.getJSONObject("data").getJSONObject("functions")).stream()
                .collect(Collectors.toMap(Function::getName, f -> f));
    }
}