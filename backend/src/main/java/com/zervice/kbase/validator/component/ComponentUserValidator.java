package com.zervice.kbase.validator.component;

import com.zervice.kbase.api.restful.pojo.RestBaseProjectComponent;
import com.zervice.kbase.api.restful.pojo.RestProjectComponentUser;
import com.zervice.kbase.api.restful.pojo.mica.SlotPojo;
import com.zervice.kbase.validator.error.ErrorCode;
import com.zervice.kbase.validator.error.ValidatorError;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

/**
 * 要求：
 * 1、用户输入间不能重复
 * 2、用户输入间不能与模板重复
 * 3、引用的entity丢失
 * 4、引用的模板丢失
 * 5、引用的重置变量值丢失
 */
@Log4j2
public class ComponentUserValidator extends BaseComponentValidator {

    public ComponentUserValidator(RestBaseProjectComponent component) {
        super(component);
    }

    @Override
    public ValidatorError validate(ComponentValidatorContext context) {
        RestProjectComponentUser user = (RestProjectComponentUser) _component;

        List<RestProjectComponentUser.Example> examples = user.getData().getExamples();
        if (CollectionUtils.isEmpty(examples)) {
            return ValidatorError.factory(ErrorCode.EmptyExample);
        }
        for (RestProjectComponentUser.Example example : examples) {
            String text = example.getText();
            if (StringUtils.isBlank(text)) {
                return ValidatorError.factory(ErrorCode.EmptyExample);
            }
        }

        ValidatorError repeatError = _repeatExample(context);
        if (repeatError != null) {
            return repeatError;
        }

        // 引用的entity 丢了
        boolean lostEntity = _lostEntity(user, context);
        if (lostEntity) {
            return ValidatorError.factory(ErrorCode.EntityNotExists);
        }


        boolean lostResetSlot = _lostResetSlots(user, context);
        if (lostResetSlot) {
            return ValidatorError.factory(ErrorCode.ResetSlotNotExists);
        }


        return _repeatIntentName(user, context);
    }

    private ValidatorError _repeatIntentName(RestProjectComponentUser user, ComponentValidatorContext context) {
        String intentName = user.getData().getName();
        if (StringUtils.isBlank(intentName)) {
            return ValidatorError.factory(ErrorCode.MissingIntentName);
        }

        ValidatorError repeatIntent = _repeatIntentNameWithUser(user, context);
        if (repeatIntent != null) {
            return repeatIntent;
        }

        return null;
    }

    private ValidatorError _repeatIntentNameWithUser(RestProjectComponentUser user, ComponentValidatorContext context) {
        String intentName = user.getData().getName();

        String id = user.getId();

        Map<String, Set<String>> notTrashedExtUserIdMap = context.notTrashedIntentNameUserIdMap();
        if (notTrashedExtUserIdMap.containsKey(intentName)) {
            Set<String> userIdSet = notTrashedExtUserIdMap.get(intentName);
            if (userIdSet.contains(id) && userIdSet.size() > 1) {
                LOG.error("[{}][user:{} intent name repeat with:{}]", context.getDbName(), user.getId(), userIdSet);

                String repeatUserId = id;
                Iterator<String> iterator = userIdSet.iterator();
                while (iterator.hasNext()) {
                    repeatUserId = iterator.next();
                    if (!repeatUserId.equals(id)) {
                        break;
                    }
                }
                RestProjectComponentUser repeatUser = (RestProjectComponentUser) context.getById(repeatUserId);

                ErrorCode errorCode = Objects.equals(repeatUser.getRootComponentId(), user.getRootComponentId()) ?
                        ErrorCode.RepeatIntentNameInCurrentFlow : ErrorCode.RepeatIntentNameInOtherFlow;
                // 与用户节点{}的{}训练例句重复
                return ValidatorError.factory(errorCode, buildUserRepeatArgs(repeatUser, intentName), userIdSet);
            }
        }
        return null;
    }

    private boolean _lostEntity(RestProjectComponentUser user, ComponentValidatorContext context) {
        // 启用了变量提取，但是没有mapping，设置不正确
        RestProjectComponentUser.Data data = user.getData();
        if (data.getMappingsEnable() != null && data.getMappingsEnable()
            && CollectionUtils.isEmpty(data.getMappings())) {
            return true;
        }

        // 检查mapping的变量是否存在
        if (CollectionUtils.isNotEmpty(data.getMappings())) {
            for (RestProjectComponentUser.Mapping mapping : data.getMappings()) {
                String entityId = mapping.getSlotId();
                RestBaseProjectComponent entity = context.getById(entityId);
                if (entity == null) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean _lostResetSlots(RestProjectComponentUser user, ComponentValidatorContext context) {
        RestProjectComponentUser.Data data = user.getData();
        if (CollectionUtils.isEmpty(data.getSetSlots())) {
            return false;
        }

        for (SlotPojo slotPojo : data.getSetSlots()) {
            String entityId = slotPojo.getSlotId();
            if (StringUtils.isBlank(entityId) || context.getById(entityId) == null) {
                return true;
            }
        }

        return false;
    }

    private ValidatorError _repeatExample(ComponentValidatorContext context) {
        RestProjectComponentUser user = (RestProjectComponentUser) _component;

        return _repeatExampleWithUser(user, context);
    }

    private ValidatorError _repeatExampleWithUser(RestProjectComponentUser user, ComponentValidatorContext context) {
        List<String> exampleTexts = user.examples();

        String id = user.getId();

        Map<String, Set<String>> notTrashedExtUserIdMap = context.notTrashedExtUserIdMap();
        for (String example : exampleTexts) {
            if (notTrashedExtUserIdMap.containsKey(example)) {
                Set<String> userIdSet = notTrashedExtUserIdMap.get(example);
                if (userIdSet.contains(id) && userIdSet.size() > 1) {
                    LOG.error("[{}][user:{} intent example repeat with:{}]", context.getDbName(), user.getId(), userIdSet);

                    String repeatUserId = id;
                    Iterator<String> iterator = userIdSet.iterator();
                    while (iterator.hasNext()) {
                        repeatUserId = iterator.next();
                        if (!repeatUserId.equals(id)) {
                            break;
                        }
                    }
                    RestProjectComponentUser repeatUser = (RestProjectComponentUser) context.getById(repeatUserId);

                    ErrorCode errorCode = Objects.equals(repeatUser.getRootComponentId(), user.getRootComponentId()) ?
                            ErrorCode.RepeatUserExampleInCurrentFlow : ErrorCode.RepeatUserExampleInOtherFlow;
                    // 与用户节点{}的{}训练例句重复
                    return ValidatorError.factory(errorCode, buildUserRepeatArgs(repeatUser, example), userIdSet);
                }
            }
        }
        return null;
    }

    public static Object[] buildUserRepeatArgs(RestProjectComponentUser repeatUser, String example) {
        return new Object[]{repeatUser.getExamples().get(0), example};
    }


}
