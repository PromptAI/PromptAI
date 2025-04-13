package com.zervice.kbase.validator.component;

import com.alibaba.fastjson.JSONObject;
import com.zervice.kbase.api.restful.pojo.RestBaseProjectComponent;
import com.zervice.kbase.api.restful.pojo.RestProjectComponentBot;
import com.zervice.kbase.api.restful.pojo.mica.ConditionPojo;
import com.zervice.kbase.api.restful.pojo.mica.SlotPojo;
import com.zervice.kbase.validator.error.ErrorCode;
import com.zervice.kbase.validator.error.ValidatorError;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Set;

/**
 * 在这补齐校验规则、优先级
 * <p>
 * 1、bot的response至少要有一个
 * 2、单个文本长度不能超过512
 * 3、set slot
 * 4、reply condition
 */
public class ComponentBotValidator extends BaseComponentValidator {

    private final Set<String> notTextResTypeSet = Set.of(RestProjectComponentBot.BotResponse.TYPE_WEBHOOK,
            RestProjectComponentBot.BotResponse.TYPE_ACTION);

    public ComponentBotValidator(RestBaseProjectComponent component) {
        super(component);
    }

    @Override
    public ValidatorError validate(ComponentValidatorContext context) {
        RestProjectComponentBot bot = (RestProjectComponentBot) _component;

        // 空回复
        boolean emptyResponse = _emptyResponse(bot);
        if (emptyResponse) {
            return ValidatorError.factory(ErrorCode.EmptyResponse);
        }

        // text 需要有值
        boolean noText = _noTextRes(bot);
        if (noText) {
            return ValidatorError.factory(ErrorCode.EmptyTextResponse);
        }

        // 文本超过最大长度
        boolean textOverflow = _textOverflow(bot, context);
        if (textOverflow) {
            return ValidatorError.factory(ErrorCode.TextResponseExceedLimitSize);
        }

        // webhook 丢了
        boolean lostWebhook = _lostWebhook(bot, context);
        if (lostWebhook) {
            return ValidatorError.factory(ErrorCode.WebhookNotExists);
        }

        // set slot 丢了
        boolean lostResetSlot = _lostResetSlot(bot, context);
        if (lostResetSlot) {
            return ValidatorError.factory(ErrorCode.ResetSlotNotExists);
        }

        // condition 丢了
        boolean lostCondition = _lostReplyCondition(bot, context);
        if (lostCondition) {
            return ValidatorError.factory(ErrorCode.ReplyConditionNotExists);
        }

        return null;
    }

    private boolean _lostResetSlot(RestProjectComponentBot bot, ComponentValidatorContext context) {
        RestProjectComponentBot.Data data = bot.getData();
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

    private boolean _lostReplyCondition(RestProjectComponentBot bot, ComponentValidatorContext context) {
        RestProjectComponentBot.Data data = bot.getData();
        if (CollectionUtils.isEmpty(data.getConditions())) {
            return false;
        }

        for (ConditionPojo condition : data.getConditions()) {
            String entityId = condition.getSlotId();
            if (StringUtils.isBlank(entityId) || context.getById(entityId) == null) {
                return true;
            }
        }

        return false;
    }


    private boolean _lostWebhook(RestProjectComponentBot bot, ComponentValidatorContext context) {
        List<RestProjectComponentBot.BotResponse> responses = bot.getData().getResponses();
        for (RestProjectComponentBot.BotResponse r : responses) {
            if (RestProjectComponentBot.BotResponse.TYPE_WEBHOOK.equals(r.getType())) {
                String webhookId = r.getContent() == null ? null : r.getContent().getString("id");
                if (StringUtils.isBlank(webhookId) || context.getById(webhookId) == null) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean _emptyResponse(RestProjectComponentBot bot) {
        List<RestProjectComponentBot.BotResponse> responses = bot.getData().getResponses();
        return CollectionUtils.isEmpty(responses);
    }

    private boolean _textOverflow(RestProjectComponentBot bot, ComponentValidatorContext context) {
        List<RestProjectComponentBot.BotResponse> responses = bot.getData().getResponses();

        for (RestProjectComponentBot.BotResponse res : responses) {
            JSONObject content = res.getContent();
            String text = content == null ? null : content.getString("text");
            if (StringUtils.isNotBlank(text) && text.length() > RestProjectComponentBot.MAX_BOT_RESPONSE_SIZE) {
                return true;
            }
        }

        return false;
    }

    private boolean _noTextRes(RestProjectComponentBot bot) {
        List<RestProjectComponentBot.BotResponse> responses = bot.getData().getResponses();

        for (RestProjectComponentBot.BotResponse res : responses) {
            JSONObject content = res.getContent();
            String text = content == null ? null : content.getString("text");
            if (StringUtils.isBlank(text) && res.getType() != null && !notTextResTypeSet.contains(res.getType())) {
                return true;
            }
        }

        return false;
    }
}
