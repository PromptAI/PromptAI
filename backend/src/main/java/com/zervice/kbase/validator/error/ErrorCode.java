package com.zervice.kbase.validator.error;

import lombok.Getter;

/**
 * 校验阶段使用的错误码
 *
 * @author chenchen
 * @Date 2023/10/8
 */
@Getter
public enum ErrorCode {
    ErrorExist(9999),
    EmptyResponse(1000),
    EmptyTextResponse(1001),
    TextResponseExceedLimitSize(1002),
    BotGlobalNotExists(1003),
    WebhookNotExists(1004),
    ResetSlotNotExists(1005),
    ReplyConditionNotExists(1006),
    MissingChild(1007),
    MissingWelcome(1008),
    EntityNotExists(1009),
    MissingChildSlots(1010),
    MissingChildConfirm(1011),
    MissingChildInterrupt(1012),
    EmptyRhetorical(1013),
    MissingChildUser(1014),
    MissingChildFiled(1015),
    RepeatUserExample(1016),
    RepeatGlobalUserExample(1017),
    EmptyExample(1018),
    UserGlobalNotExists(1019),
    RepeatUserExampleInCurrentFlow(1020),
    RepeatUserExampleInOtherFlow(1021),
    MissingIntentName(1022),
    RepeatIntentNameInCurrentFlow(1023),
    RepeatIntentNameInOtherFlow(1024),
    RepeatGlobalIntentName(1025),
    MissingName(1026),
    InvalidGptName(1027),
    FunctionCallingRepeat(1028),
    FunctionCallingRepeatWithOtherNode(1029);



    private Integer _code;

    ErrorCode(Integer code) {
        this._code = code;
    }

}
