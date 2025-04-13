package com.zervice.kbase.ai.convert.pojo;

import com.alibaba.fastjson.JSONObject;
import com.zervice.kbase.ai.convert.PublishContext;
import com.zervice.kbase.api.restful.pojo.RestProjectComponentGpt;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Gpt Node has related functions, which can be used at converting process.
 *  Here is a sample fragment of yml
 *  *
 *  *********************************************************************************************
 *    llm_agents:
 *     Query_Weather:
 *       description: Get  weather forecast,
 *       prompt: " You are a WeatherQueryAgent. Your job is to provide users with accurate\
 *         \ weather forecasts based on their queries. \n
 *         \ When a user asks about the weather, extract the city from their question and determine the\
 *
 *         #####
 *         ##### point out the function witch can be used to check the format.
 *         #####
 *
 *         \ desired temperature format. Once you know the format, call \"check_format\" to verify the format. Once all the information\
 *         \ is collected, the task is complete."
 *       args:
 *       - format
 *       - location
 *       uses: ["check_format"] // set the functions that this llm agent depends on
 *   functions:
 *     check_format:
 *       body: |-
 *
 *       #####
 *       ##### The Python codes need 4 indents
 *       #####
 *
 *         def check_format(**kwargs):
 *             format = kwargs.get("format")
 *             print("check format:{format}")
 *             if format.lower() not in ['celsius', 'fahrenheit']:
 *                 return [f"Invalid format: {format}. Please provide 'Celsius' or 'Fahrenheit'."] #####  return an array of message
 *             return ["Format is valid."]
 *  *********************************************************************************************
 * @author chenchen
 * @Date 2024/9/10
 */
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Function {
    private String _name;

    private String _body;


    public Function(RestProjectComponentGpt.FunctionCalling functionCalling) {
        this._name = functionCalling.getName();
        this._body = functionCalling.getCode();
    }

    public static List<Function> buildFunctions(PublishContext context) {
        List<Agent> agents = Agent.buildAgents(context);
        return agents.stream()
                .filter(a -> CollectionUtils.isNotEmpty(a.getFunctionCallings()))
                .flatMap(a -> a.getFunctionCallings().stream())
                .map(Function::new)
                .collect(Collectors.toList());
    }

    public JSONObject toPublishFunction() {
        JSONObject function = new JSONObject(true);
        function.put("name", _name);
        function.put("body", _body);
        return function;
    }
}
