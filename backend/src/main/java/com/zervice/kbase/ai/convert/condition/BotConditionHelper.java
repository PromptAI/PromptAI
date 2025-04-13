package com.zervice.kbase.ai.convert.condition;

import com.zervice.common.restful.exception.RestException;
import com.zervice.common.restful.exception.StatusCodes;
import com.zervice.kbase.api.restful.pojo.mica.ConditionPojo;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author chenchen
 * @Date 2024/11/29
 */
public class BotConditionHelper {


    /**
     * 将 yaml中的 bot.condition 转换为 bot 节点的回复条件
     * xx == None and xx != None
     */
    public static List<ConditionPojo> parseConditions(String input) {
        // 拆分多个条件
        String[] conditions = input.split("\\s+and\\s+");
        List<ConditionPojo> results = new ArrayList<>();

        for (String condition : conditions) {
            results.add(_parseCondition(condition));
        }

        return results;
    }

    /**
     * 将 yaml中的 bot.condition 转换为 bot 节点的回复条件
     * xx == None
     */
    private static ConditionPojo _parseCondition(String input) {
        String slot, conditionType, value = "";

        // Patterns for matching different conditions
        if (input.matches("^\\w+ == None$")) {
            slot = input.split(" ")[0];
            conditionType = ConditionPojo.TYPE_IS_EMPTY;
        } else if (input.matches("^\\w+ != None$")) {
            slot = input.split(" ")[0];
            conditionType = ConditionPojo.TYPE_IS_NOT_EMPTY;
        } else if (input.matches("^\\w+ == .*")) {
            String[] parts = input.split(" == ");
            slot = parts[0];
            conditionType = ConditionPojo.TYPE_EQUAL;
            value = parts[1];
        } else if (input.matches("^\\w+ != .*")) {
            String[] parts = input.split(" != ");
            slot = parts[0];
            conditionType = ConditionPojo.TYPE_NOT_EQUAL;
            value = parts[1];
        } else if (input.matches("^.* in \\w+$")) {
            String[] parts = input.split(" in ");
            slot = parts[1];
            conditionType = ConditionPojo.TYPE_CONTAINS;
            value = parts[0];
        } else if (input.matches("^.* not in \\w+$")) {
            String[] parts = input.split(" not in ");
            slot = parts[1];
            conditionType = ConditionPojo.TYPE_NOT_CONTAINS;
            value = parts[0];
        } else if (input.matches("^\\w+ < .*")) {
            String[] parts = input.split(" < ");
            slot = parts[0];
            conditionType = ConditionPojo.TYPE_LESS_THAN;
            value = parts[1];
        } else if (input.matches("^\\w+ <= .*")) {
            String[] parts = input.split(" <= ");
            slot = parts[0];
            conditionType = ConditionPojo.TYPE_LESS_THAN_OR_EQUAL;
            value = parts[1];
        } else if (input.matches("^\\w+ > .*")) {
            String[] parts = input.split(" > ");
            slot = parts[0];
            conditionType = ConditionPojo.TYPE_GREATER_THAN;
            value = parts[1];
        } else if (input.matches("^\\w+ >= .*")) {
            String[] parts = input.split(" >= ");
            slot = parts[0];
            conditionType = ConditionPojo.TYPE_GREATER_THAN_OR_EQUAL;
            value = parts[1];
        } else if (input.matches("^re\\.match\\(.*\\, \\w+\\)$")) {
            Pattern pattern = Pattern.compile("^re\\.match\\((.*), (\\w+)\\)$");
            Matcher matcher = pattern.matcher(input);
            if (matcher.find()) {
                value = matcher.group(1);
                slot = matcher.group(2);
                conditionType = ConditionPojo.TYPE_REGEX;
            } else {
                throw new IllegalArgumentException("Invalid regex format");
            }
        } else if (input.matches("^\\w+\\.startswith\\(.*\\)$")) {
            Pattern pattern = Pattern.compile("^(\\w+)\\.startswith\\((.*)\\)$");
            Matcher matcher = pattern.matcher(input);
            if (matcher.find()) {
                slot = matcher.group(1);
                value = matcher.group(2);
                conditionType = ConditionPojo.TYPE_STARTS_WITH;
            } else {
                throw new IllegalArgumentException("Invalid startsWith format");
            }
        } else if (input.matches("^\\w+\\.endswith\\(.*\\)$")) {
            Pattern pattern = Pattern.compile("^(\\w+)\\.endswith\\((.*)\\)$");
            Matcher matcher = pattern.matcher(input);
            if (matcher.find()) {
                slot = matcher.group(1);
                value = matcher.group(2);
                conditionType = ConditionPojo.TYPE_ENDS_WITH;
            } else {
                throw new IllegalArgumentException("Invalid endsWith format");
            }
        } else {
            throw new IllegalArgumentException("Unsupported condition format");
        }

        return ConditionPojo.builder()
                .slotName(slot)
                .type(conditionType)
                .value(value)
                .build();
    }

    /**
     * build value
     */
    public static String buildConditionValue(String name, ConditionPojo conditionPojo) {
        String type = conditionPojo.getType();
        String val = conditionPojo.getValue();
        switch (type) {
            case ConditionPojo.TYPE_IS_EMPTY:
                return name + " == None";
            case ConditionPojo.TYPE_IS_NOT_EMPTY:
                return name + " != None";
            case ConditionPojo.TYPE_EQUAL:
                return name + " == " + val;
            case ConditionPojo.TYPE_NOT_EQUAL:
                return name + " != " + val;
            case ConditionPojo.TYPE_CONTAINS:
                return val + " in " + name;
            case ConditionPojo.TYPE_NOT_CONTAINS:
                return val + " not in " + name;
            case ConditionPojo.TYPE_LESS_THAN:
                return name + " < " + val;
            case ConditionPojo.TYPE_LESS_THAN_OR_EQUAL:
                return name + " <= " + val;
            case ConditionPojo.TYPE_GREATER_THAN:
                return name + " > " + val;
            case ConditionPojo.TYPE_GREATER_THAN_OR_EQUAL:
                return name + " >= " + val;
            case ConditionPojo.TYPE_REGEX:
                return "re.match(" + val + ", " + name + ")";
            case ConditionPojo.TYPE_STARTS_WITH:
                return name + ".startswith(" + val + ")";
            case ConditionPojo.TYPE_ENDS_WITH:
                return name + ".endswith(" + val + ")";
            default:
                throw new RestException(StatusCodes.BadRequest, "unsupported condition type:" + type);
        }
    }
}
