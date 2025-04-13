package com.zervice.kbase.validator.component;

import com.zervice.kbase.api.restful.pojo.RestBaseProjectComponent;
import com.zervice.kbase.api.restful.pojo.RestProjectComponentGpt;
import com.zervice.kbase.validator.error.ErrorCode;
import com.zervice.kbase.validator.error.ValidatorError;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * GPT 节点
 */
@Log4j2
public class ComponentGptValidator extends BaseComponentValidator {
    private final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$");

    public ComponentGptValidator(RestBaseProjectComponent component) {
        super(component);
    }

    @Override
    public ValidatorError validate(ComponentValidatorContext context) {
        RestProjectComponentGpt gpt = (RestProjectComponentGpt) this._component;

        String name = gpt.getData().getName();
        if (StringUtils.isBlank(name)) {
            return ValidatorError.factory(ErrorCode.MissingName);
        }

        Matcher matcher = NAME_PATTERN.matcher(name);
        if (!matcher.matches()) {
            return ValidatorError.factory(ErrorCode.InvalidGptName);
        }

        /// check function names
        // 与自身重复
        ValidatorError repeat = _checkFunctionCallingRepeat(gpt.getData().getFunctionCalling());
        if (repeat != null) {
            return repeat;
        }
        // 与其他点重复
        return _checkFunctionCallingRepeatWithOthers(gpt, context);
    }

    /**
     * gpt 节点数量不多，可以后续再来优化
     * TODO： 如果有大量gpt，可导致这里出现性能问题，参考 User.intents 优化
     * @param gpt
     * @param context
     * @return
     */
    private ValidatorError _checkFunctionCallingRepeatWithOthers(RestProjectComponentGpt gpt,
                                                                 ComponentValidatorContext context) {
        if (CollectionUtils.isEmpty(gpt.getData().getFunctionCalling())) {
            return null;
        }

        Set<String> functions = gpt.getData().getFunctionCalling().stream()
                .map(RestProjectComponentGpt.FunctionCalling::getName)
                .collect(Collectors.toSet());

        List<RestProjectComponentGpt> gpts = context.getNotTrashedGPT();
        for (RestProjectComponentGpt g : gpts) {
            if (gpt.getId().equals(g.getId()) || CollectionUtils.isEmpty(g.getData().getFunctionCalling())) {
                continue;
            }

            Set<String> names = g.getData().getFunctionCalling().stream()
                    .map(RestProjectComponentGpt.FunctionCalling::getName)
                    .collect(Collectors.toSet());

            for (String n : functions) {
                if (names.contains(n)) {
                    LOG.error("[{}:{}][gpt's function name :{} repeat with:{}]", context.getDbName(), gpt.getId(), n, g.getId());
                    return ValidatorError.factory(ErrorCode.FunctionCallingRepeatWithOtherNode, new String[]{g.getData().getName(), n});
                }
            }

        }

        return null;
    }
    private ValidatorError _checkFunctionCallingRepeat(List<RestProjectComponentGpt.FunctionCalling> functionCallings) {
        if (CollectionUtils.isEmpty(functionCallings)) {
            return null;
        }
        Set<String> names = new HashSet<>();
        for (RestProjectComponentGpt.FunctionCalling f : functionCallings) {
            if (!names.add(f.getName())) {
                return ValidatorError.factory(ErrorCode.FunctionCallingRepeat, new String[]{f.getName()});
            }
        }

        return null;
    }
}
