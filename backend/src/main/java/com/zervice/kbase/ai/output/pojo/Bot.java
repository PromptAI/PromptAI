package com.zervice.kbase.ai.output.pojo;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import com.zervice.common.restful.exception.RestException;
import com.zervice.common.restful.exception.StatusCodes;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.stream.Collectors;

/**
 * @author chenchen
 * @Date 2025/1/3
 */
@Setter@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Bot {

    protected String _id;

    @JSONField(name = "bot_name")
    protected String _botName;
    @JSONField(name = "llm_config")
    protected LlmConfig _llmConfig;

    protected Data _data;

    protected JSONArray _buildFlowFromStepsAndSubflows(JSONArray steps, JSONArray subflows) {
        JSONArray r = new JSONArray();
        r.addAll(_buildFlowSteps(steps, true));
        for (int i = 0; i < subflows.size(); i++) {
            JSONObject subflow = subflows.getJSONObject(i);
            r.addAll(_buildFlowSteps(subflow.getJSONArray("steps"), true));
        }

        return r;
    }

    protected JSONArray _buildFlowSteps(JSONArray steps, boolean needEnd) {
        JSONArray r = new JSONArray();
        for (int i = 0; i < steps.size(); i++) {
            JSONObject item = steps.getJSONObject(i);
            String action = item.getString("action");
            switch (action) {
                case "begin":
                    JSONObject begin = new JSONObject();
                    begin.put("begin", item.getString("value"));
                    r.add(begin);
                    break;
                case "bot":
                    JSONObject bot = new JSONObject();
                    bot.put("bot", item.getString("text"));
                    r.add(bot);
                    break;
                case "user":
                    JSONObject user = new JSONObject();
                    user.put("user", "");
                    r.add(user);
                    break;

                case "next":
                    JSONObject next = new JSONObject();
                    next.put("next", item.getString("value"));
                    r.add(next);
                    break;

                case "if":
                    JSONArray ifStep = _buildBotIfSteps(item);
                    r.addAll(ifStep);
                    break;
                case "set":
                    JSONObject setStp = _buildSetStep(item);
                    r.add(setStp);
                    break;
                case "label":
                    JSONObject label = new JSONObject();
                    label.put("label", item.getString("value"));
                    r.add(label);
                    break;
                case "call":
                    JSONObject call = new JSONObject();
                    call.put("call", item.getString("value"));
                    if (item.get("args") != null) {
                        call.put("args", item.getJSONObject("args"));
                    }
                    r.add(call);
                    break;

                default:
                    throw new RestException(StatusCodes.BadRequest, "unsupported action:", action);
            }
        }

        // add end
        if (needEnd) {
            JSONObject end = new JSONObject();
            end.put("end", "");

            r.add(end);
        }
        return r;
    }

    /**
     * convert set step
     *
     * @param setNode
     * @return
     */
    private JSONObject _buildSetStep(JSONObject setNode) {
        JSONObject setValues = new JSONObject();

        // obj -> array
        JSONObject setValuesObj = setNode.getJSONObject("value");
        for (String key : setValuesObj.keySet()) {
            setValues.put(key, setValuesObj.getString(key));
        }

        JSONObject set = new JSONObject();

        set.put("set", setValues);
        return set;
    }


    /**
     *    {
     *               "else": [
     *                   {
     *                       "action": "next",
     *                       "value": "sub_bot_else_4"
     *                   }
     *               ],
     *               "else if": [
     *                   {
     *                       "then": [
     *                           {
     *                               "action": "next",
     *                               "value": "sub_bot_else_if_3"
     *                           }
     *                       ],
     *                       "conditions": [
     *                           {
     *                               "action": "bot_condition",
     *                               "value": "problem == xxx"
     *                           }
     *                       ]
     *                   }
     *               ],
     *               "action": "if",
     *               "then": [
     *                   {
     *                       "action": "next",
     *                       "value": "sub_bot_if_2"
     *                   }
     *               ],
     *               "conditions": [
     *                   {
     *                       "action": "bot_condition",
     *                       "value": "count == Not null"
     *                   }
     *               ]
     *           }
     */
    private JSONArray _buildBotIfSteps(JSONObject ifNode) {
        JSONArray ifSteps = new JSONArray();
        JSONObject ifStep = new JSONObject(true);
        // if condition
        ifStep.put("if", _buildConditionStr(ifNode.getJSONObject("condition")));

        // if then
        ifStep.put("then", _buildFlowSteps(ifNode.getJSONArray("then"), false));
        ifSteps.add(ifStep);

        // else if
        if (ifNode.getJSONArray("else if") != null) {
            JSONArray elseIfs = ifNode.getJSONArray("else if");
            for (int i = 0; i < elseIfs.size(); i++) {
                JSONObject elseIfNode = elseIfs.getJSONObject(i);

                JSONObject elseIfStep = new JSONObject();
                elseIfStep.put("else if", _buildConditionStr(elseIfNode.getJSONObject("condition")));
                elseIfStep.put("then", _buildFlowSteps(elseIfNode.getJSONArray("then"), false));

                ifSteps.add(elseIfStep);
            }
        }

        // else
        if (ifNode.getJSONArray("else") != null) {
            JSONObject elseStep = new JSONObject();
            elseStep.put("else", _buildFlowSteps(ifNode.getJSONArray("else"), false));
            ifSteps.add(elseStep);
        }

        return ifSteps;
    }

    private String _buildConditionStr(JSONObject condition) {
        StringBuilder conditionStr = new StringBuilder();

        String action = condition.getString("action");

        JSONArray values = condition.getJSONArray("values");

        if ("bot_condition".equals(action)) {
            String value = String.join(" and ", values.toJavaList(String.class));
            conditionStr.append(value);
        }

        if ("the user claims".equals(action)) {
            // string.join时，给前后添加 "
//                String value = String.join(,",", );
            String value = values.toJavaList(String.class).stream()
                    .collect(Collectors.joining(",", "\"", "\""));

            conditionStr.append(action).append(" ").append(value);
        }

        return conditionStr.toString();
    }

    protected JSONArray _buildFlowSates(JSONArray states) {
        JSONArray r = new JSONArray();
        for (int i = 0; i < states.size(); i++) {
            r.add(states.getString(i));
        }
        return r;
    }

}
