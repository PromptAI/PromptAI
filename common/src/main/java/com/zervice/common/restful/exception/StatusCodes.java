package com.zervice.common.restful.exception;

import org.apache.commons.lang3.StringUtils;

import static jakarta.servlet.http.HttpServletResponse.*;


public enum StatusCodes {
    /**
     * http basic
     */
    BadRequest(1400, SC_BAD_REQUEST),
    Unauthorized(1401, SC_UNAUTHORIZED),
    Forbidden(1403, SC_FORBIDDEN),
    NotFound(1404, SC_NOT_FOUND),

    /**
     * 请求参数校验失败
     */
    RequestParamValidateFail(1405, SC_BAD_REQUEST),

    InternalError(1500, SC_INTERNAL_SERVER_ERROR),

    /**
     * env
     */
    MUST_ON_MASTER_NODE(1502, SC_BAD_REQUEST),

    /**
     * common
     */
    CLICK_TOO_FAST(1600, SC_BAD_REQUEST),
    NOT_ALLOWED_FILE_TYPE(1601, SC_BAD_REQUEST),
    FILE_IS_TOO_LARGE(1602, SC_BAD_REQUEST),
    NotFoundStock(1603, SC_NOT_FOUND),
    NotSupportStockMarket(1604, SC_NOT_FOUND),
    NotFoundStockPrice(1605, SC_NOT_FOUND),


    /**
     * zp related
     */
    CONNECT_ZP_FAILED(1700, SC_INTERNAL_SERVER_ERROR),

    /**
     * accounts and users
     */
    ACCOUNT_NOT_EXISTS(2100, SC_BAD_REQUEST),
    USER_NOT_EXISTS(2101, SC_BAD_REQUEST),
    ROLE_NOT_EXISTS(2102, SC_BAD_REQUEST),
    USER_PASS_OR_NAME_FAILED(2103, SC_BAD_REQUEST),
    VERIFICATION_CODE_FAILED(2104, SC_BAD_REQUEST),
    INVALID_PASS_LENGTH(2105, SC_BAD_REQUEST),
    INVALID_PASS(2106, SC_BAD_REQUEST),
    ACCOUNT_ALREADY_EXISTS(2107, SC_BAD_REQUEST),
    USER_ALREADY_EXISTS(2108, SC_BAD_REQUEST),
    MOBILE_ALREADY_EXISTS(2109, SC_BAD_REQUEST),
    UNSUPPORTED_LOGIN_TYPE(2110, SC_BAD_REQUEST),
    ACCOUNT_ALREADY_TRAILED(2111, SC_FORBIDDEN),
    ACCOUNT_SUSPEND(2112, SC_FORBIDDEN),
    ACCOUNT_PARAMS_ERROR(2113, SC_BAD_REQUEST),
    ACCOUNT_NOT_AVAILABLE(2114, SC_BAD_REQUEST),
    FileNotFound(2115, SC_BAD_REQUEST),
    EMAIL_ALREADY_EXISTS(2116, SC_BAD_REQUEST),
    INVALID_USER_MOBILE_OR_EMAIL(2117, SC_BAD_REQUEST),
    INVALID_USER_MOBILE(2118, SC_BAD_REQUEST),
    INVALID_USER_EMAIL(2119, SC_BAD_REQUEST),
    INVALID_USER_CONFIG_LANGUAGE(2120, SC_BAD_REQUEST),
    INVALID_USER_CONFIG_TIME_ZONE(2121, SC_BAD_REQUEST),
    NotAllowDeleteDefaultUser(2122, SC_FORBIDDEN),
    NotAllowDeleteSelf(2123, SC_FORBIDDEN),
    FileAlreadyExists(2124, SC_BAD_REQUEST),
    CurrentPassError(2125, SC_BAD_REQUEST),
    NotAllowRegistry(2126, SC_BAD_REQUEST),
    NotAccountOwner(2127, SC_BAD_REQUEST),
    NotAllowInactiveAccountLogin(2128, SC_BAD_REQUEST),
    RestTokenNotEnough(2129, SC_BAD_REQUEST),
    FuncNotEnable(2130, SC_BAD_REQUEST),


    /**
     * configuration related
     */
    CONFIGURATION_NOT_EXISTS(2200, SC_BAD_REQUEST),

    /**
     * dept related
     */
    DEPT_NAME_EXISTS(2300, SC_BAD_REQUEST),

    /***
     *ai agent
     */
    AiAgentBadRequest(2400, SC_BAD_REQUEST),
    AiAgentForbidden(2401, SC_FORBIDDEN),
    AiAgentNotFound(2402, SC_BAD_REQUEST),
    AiAgentInternalError(2403, SC_INTERNAL_SERVER_ERROR),
    AiAgentNotSupport(2404, SC_INTERNAL_SERVER_ERROR),
    AiAgentCanNotReach(2405, SC_INTERNAL_SERVER_ERROR),
    AiAgentIsBusy(2406, SC_INTERNAL_SERVER_ERROR),
    AiAgentNotAvailable(2407, SC_INTERNAL_SERVER_ERROR),
    AiAgentInvalidName(2408, SC_INTERNAL_SERVER_ERROR),

    /**
     * ai project
     */
    AiProjectBadRequest(2500, SC_BAD_REQUEST),
    AiProjectNameExists(2501, SC_BAD_REQUEST),
    AiProjectIsBusy(2502, SC_BAD_REQUEST),
    AiProjectNotFound(2503, SC_BAD_REQUEST),
    AiProjectNotSupport(2504, SC_BAD_REQUEST),
    AiProjectNoAgentAvailable(2505, SC_INTERNAL_SERVER_ERROR),
    InvalidProjectId(2506, SC_BAD_REQUEST),
    AiProjectNotDebug(2507, SC_BAD_REQUEST),
    AiProjectUnsupportedFallbackType(2508, SC_BAD_REQUEST),
    AiProjectForbidden(2509, SC_BAD_REQUEST),
    InvalidFallbackButton(2510, SC_BAD_REQUEST),
    InvalidProjectType(2511, SC_BAD_REQUEST),

    /**
     * ai model
     */
    AiModelBadRequest(2600, SC_BAD_REQUEST),
    AiModelForbidden(2601, SC_FORBIDDEN),
    AiModelNotFound(2602, SC_BAD_REQUEST),
    AiModelInternalError(2604, SC_INTERNAL_SERVER_ERROR),
    AiModelNotFoundAgentTaskTemplate(2605, SC_BAD_REQUEST),


    /***
     * ai agent task
     */
    AiAgentTaskBadRequest(2700, SC_BAD_REQUEST),
    AiAgentTaskForbidden(2701, SC_FORBIDDEN),
    AiAgentTaskNotFound(2702, SC_BAD_REQUEST),
    AiAgentTaskInternalError(2703, SC_INTERNAL_SERVER_ERROR),
    AiAgentTaskNotSupport(2704, SC_INTERNAL_SERVER_ERROR),
    AiAgentTaskNotExecutingOrSchedule(2705, SC_BAD_REQUEST),

    /***
     *ai project component
     */
    AiProjectComponentIdExists(2800, SC_BAD_REQUEST),
    AiProjectComponentForbidden(2801, SC_FORBIDDEN),
    AiProjectComponentNotFound(2802, SC_BAD_REQUEST),
    AiProjectComponentIdRequired(2803, SC_INTERNAL_SERVER_ERROR),
    AiProjectComponentNotSupport(2804, SC_INTERNAL_SERVER_ERROR),
    AiProjectComponentNameExists(2805, SC_BAD_REQUEST),
    AiProjectComponentInUse(2806, SC_INTERNAL_SERVER_ERROR),
    AiProjectComponentExampleExists(2807, SC_BAD_REQUEST),
    AiProjectComponentUserAndBotNotPair(2808, SC_BAD_REQUEST),
    AiProjectComponentBadRequest(2809, SC_BAD_REQUEST),
    AiProjectComponentNameEmpty(2810, SC_BAD_REQUEST),
    AiProjectComponentExampleTextEmpty(2811, SC_BAD_REQUEST),
    AiProjectComponentUserGlobalExampleExists(2812, SC_BAD_REQUEST),
    AiProjectComponentRhetoricalNeedAUserNode(2813, SC_BAD_REQUEST),
    AiProjectComponentInvalidParentId(2814, SC_BAD_REQUEST),
    AiProjectComponentSlotNameRepeat(2815, SC_BAD_REQUEST),
    AiProjectComponentGlobalNotExists(2816, SC_BAD_REQUEST),
    AiProjectComponentNotPermitName(2817, SC_BAD_REQUEST),
    AiProjectComponentNotPermitted(2818, SC_BAD_REQUEST),
    AiProjectComponentCanNotBeTrash(2819, SC_BAD_REQUEST),
    AiProjectComponentTrashFailed(2820, SC_BAD_REQUEST),
    AiProjectComponentInvalidParent(2821, SC_BAD_REQUEST),
    EntityNotFound(2822, SC_BAD_REQUEST),
    UnsupportedImportEntityDictionaryFormat(2823, SC_BAD_REQUEST),
    UnsupportedExportEntityDictionaryFormat(2824, SC_BAD_REQUEST),
    AiProjectComponentSortNotSupport(2825, SC_BAD_REQUEST),
    UnsupportedUserDisplay(2826, SC_BAD_REQUEST),
    AiProjectComponentNotCompleteTree(2827, SC_BAD_REQUEST),
    AiProjectComponentInvalidRootCompleteId(2828, SC_BAD_REQUEST),
    AiProjectComponentSynonymsOriginalRepeat(2829, SC_BAD_REQUEST),
    AiProjectComponentInvalidRootComponent(2830, SC_BAD_REQUEST),
    AiProjectComponentNotSupportPutBackTrash(2831, SC_INTERNAL_SERVER_ERROR),
    AiProjectComponentUserGlobalExists(2832, SC_BAD_REQUEST),
    AiProjectComponentBotGlobalExists(2833, SC_BAD_REQUEST),
    AiProjectComponentEntityExists(2834, SC_BAD_REQUEST),
    AiProjectComponentWebhookExists(2835, SC_BAD_REQUEST),
    AiProjectComponentInvalidUrl(2836, SC_BAD_REQUEST),

    AiProjectComponentActionNoExists(2837, SC_BAD_REQUEST),
    AiProjectComponentActionCodeEmpty(2838, SC_BAD_REQUEST),
    AiProjectComponentKbTextExists(2839, SC_BAD_REQUEST),
    AiProjectComponentKbPdfExists(2840, SC_BAD_REQUEST),
    AiProjectComponentKbUrlExists(2841, SC_BAD_REQUEST),

    AiProjectComponentNotAllowDeleteInPublish(2842, SC_BAD_REQUEST),
    AiProjectComponentKbUrlPareFailed(2843, SC_BAD_REQUEST),

    /**
     * published project
     */
    PublishedProjectNotPublish(2900, SC_BAD_REQUEST),
    PublishedProjectNotRunning(2901, SC_BAD_REQUEST),
    PublishedProjectTransferProjectFailed(2902, SC_BAD_REQUEST),
    PublishedProjectNotExistsOrRunning(2903, SC_BAD_REQUEST),
    PublishedProjectTransferProjectRootRequired(2904, SC_BAD_REQUEST),
    PublishedProjectCanNotBePublish(2905, SC_BAD_REQUEST),

    /**
     * sms
     */
    SmsRateLimit(3000, SC_BAD_REQUEST),
    InvalidCode(3001, SC_BAD_REQUEST),
    InvalidSmsConf(3002, SC_BAD_REQUEST),
    SendSmsFailed(3003, SC_BAD_REQUEST),

    /**
     * registry code
     */
    InvalidRegistryCode(3100, SC_BAD_REQUEST),
    RegistryCodeAlreadyUsed(3101, SC_BAD_REQUEST),

    FastProjectExceedTokenLimitWithoutLogin(3200, SC_BAD_REQUEST),
    FastProjectParsUrlFail(3201, SC_BAD_REQUEST),
    FastProjectNoContentParsed(3202, SC_BAD_REQUEST),
    FastProjectCountLimit(3203, SC_BAD_REQUEST),
    FastProjectMessageCountLimit(3204, SC_BAD_REQUEST),
    FastProjectParsContentFail(3205, SC_BAD_REQUEST),
    FastProjectTokenNotEnough(3206, SC_BAD_REQUEST),
    FastProjectExceedTokenLimitWithLogin(3207, SC_BAD_REQUEST),
    FastProjectTokenNotEnoughInChat(3208, SC_BAD_REQUEST),
    FastProjectChatCountLimit(3209, SC_BAD_REQUEST),


    /**
     * talk 2 bits
     */
    Talk2BitsParseFail(3300, SC_BAD_REQUEST),
    Talk2BitsNotReachable(3301, SC_BAD_REQUEST),

    /**
     * schedule task
     */
    ScheduleTaskIntervalRequired(3400, SC_BAD_REQUEST),
    ScheduleTaskIntervalTooLow(3401, SC_BAD_REQUEST),
    ScheduleTaskInvalidCron(3402, SC_BAD_REQUEST),
    ScheduleTaskUnsupportedTaskType(3403, SC_BAD_REQUEST),


    /**
     * llm
     */
    LlmInvalidSystemConfig(3500, SC_BAD_REQUEST),
    LlmInvalidOpenAIConfig(3501, SC_BAD_REQUEST),
    LlmUnknownConfig(3502, SC_BAD_REQUEST),
    LlmInvalidAIOConfig(3503, SC_BAD_REQUEST),

    LicenseInvalid(3600, SC_BAD_REQUEST),
    LicenseExpired(3601, SC_BAD_REQUEST),
    LicenseLimit4Project(3602, SC_BAD_REQUEST),
    LicenseLimit4Flow(3603, SC_BAD_REQUEST),

    /**
     * chat related
     */
    ChatNotFound(3700, SC_BAD_REQUEST),
    ChatNotSupport(3701, SC_BAD_REQUEST),
    ChatUpdateFailed(3702, SC_BAD_REQUEST),

    /**
     * ai train
     */
    AiTrainFileInvalidEmptyFile(4001, SC_BAD_REQUEST),
    @Deprecated
    AiTrainFileInvalidBotResponsesEmpty(4002, SC_BAD_REQUEST),
    @Deprecated
    AiTrainFileInvalidBotResponseTextEmpty(4003, SC_BAD_REQUEST),
    AiTrainFileInvalidChildEmpty(4004, SC_BAD_REQUEST),
    @Deprecated
    AiTrainFileInvalidSlotNameEmpty(4005, SC_BAD_REQUEST),
    @Deprecated
    AiTrainFileInvalidRhetoricalContentEmpty(4006, SC_BAD_REQUEST),
    @Deprecated
    AiTrainFileInvalidExampleEmpty(4007, SC_BAD_REQUEST),
    @Deprecated
    AiTrainFileInvalidExampleResponseEmpty(4008, SC_BAD_REQUEST),
    AiTrainFileInvalidMissingChild(4009, SC_BAD_REQUEST),
    AiTrainPublishedProjectOverFlowLimit(4010, SC_BAD_REQUEST),
    @Deprecated
    AiTrainFileInvalidMissingChildUser(4011, SC_BAD_REQUEST),
    AiTrainFileInvalidMissingChildSlot(4012, SC_BAD_REQUEST),
    @Deprecated
    AiTrainFileInvalidMissingChildConfirm(4013, SC_BAD_REQUEST),
    AiTrainFileInvalidMissingChildInterrupt(4014, SC_BAD_REQUEST),
    AiTrainFileInvalidMissingChildBot(4015, SC_BAD_REQUEST),
    @Deprecated
    AiTrainFileInvalidWelcomeRequired(4016, SC_BAD_REQUEST),
    @Deprecated
    AiTrainFileInvalidMissingChildFiled(4017, SC_BAD_REQUEST),
    AiTrainFileInvalidSlot(4018, SC_BAD_REQUEST),
    AiTrainFileInvalidEntityNotExists(4019, SC_BAD_REQUEST),
    AiTrainFileInvalidNotSupportValidatorType(4020, SC_BAD_REQUEST),
    AiTrainFileInvalidOptionExampleEmpty(4021, SC_BAD_REQUEST),
    AiTrainFileInvalidOptionExampleResponseEmpty(4022, SC_BAD_REQUEST),
    AiTrainNotReady(4023, SC_BAD_REQUEST),
    @Deprecated
    AiTrainFileInvalidBotResponseTextExceedLimitSize(4024, SC_BAD_REQUEST),
    AiTrainFileInvalidUserGlobalNotExists(4025, SC_BAD_REQUEST),
    AiTrainFileInvalidBotGlobalNotExists(4026, SC_BAD_REQUEST),
    AiTrainFileInvalidWebhookNotExists(4027, SC_BAD_REQUEST),
    @Deprecated
    AiTrainFileInvalidResetSlotNotExists(4028, SC_BAD_REQUEST),
    @Deprecated
    AiTrainFileInvalidReplyConditionNotExists(4029, SC_BAD_REQUEST),
    FlowLLMModelRequired(4030, SC_BAD_REQUEST),

    PaymentNotEnable(4100, SC_BAD_REQUEST),
    PaymentPackageNotExists(4101, SC_BAD_REQUEST),
    PaymentInvalidPackage(4102, SC_BAD_REQUEST),
    PaymentPackageRepeat(4103, SC_BAD_REQUEST),
    PaymentCancelSubscriptionFailed(4104, SC_BAD_REQUEST),
    PaymentPackageInUse(4105, SC_BAD_REQUEST),


    /**
     * broker
     */
    ChatProjectNotFound(4200,SC_BAD_REQUEST),

    /**
     * qrcode
     */
    UNSUPPORTED_QRCODE_TYPE(5001, SC_BAD_REQUEST),
    UNSUPPORTED_CONTENT_TYPE(5002, SC_BAD_REQUEST),

    /**
     * trial apply
     */
    MOBILE_ALREADY_APPLY(5003, SC_BAD_REQUEST),
    EMAIL_ALREADY_APPLY(5004, SC_BAD_REQUEST),
    INVALID_APPLY_MOBILE_OR_EMAIL(5005, SC_BAD_REQUEST),

    TRIAL_APPLY_NOT_EXITS(5006, SC_BAD_REQUEST),
    TRIAL_APPLY_ALREADY_PROCESS(5007, SC_BAD_REQUEST),
    TRIAL_APPLY_NOT_SUPPORT_OP(5008, SC_BAD_REQUEST),
    CONTACT_COMMENT_RATE_LIMIT(5009, SC_BAD_REQUEST),
    TRIAL_APPLY_RATE_LIMIT(5010, SC_BAD_REQUEST),

    /**
     * component collection
     */
    ComponentCollectionNotFound(5100, SC_BAD_REQUEST),

    /**
     * others
     */
    UnsupportedFileFormat(5200, SC_BAD_REQUEST);

    private int httpStatusCode;
    private int internalStatusCode;

    StatusCodes(int internalStatusCode, int httpStatusCode) {
        this.httpStatusCode = httpStatusCode;
        this.internalStatusCode = internalStatusCode;
    }

    public int getInternalStatusCode() {
        return internalStatusCode;
    }

    public int getHttpStatusCode() {
        return httpStatusCode;
    }

    public static StatusCodes parse(Integer internalStatusCode) {
        for (StatusCodes statusCodes : StatusCodes.values()) {
            if (statusCodes.internalStatusCode == internalStatusCode) {
                return statusCodes;
            }
        }
        return InternalError;
    }

    public static StatusCodes parse(String status) {
        if (StringUtils.isBlank(status)) {
            return InternalError;
        }

        int intCode = Integer.parseInt(status);
        switch (intCode) {
            case SC_BAD_REQUEST:
                return BadRequest;
            case SC_UNAUTHORIZED:
                return Unauthorized;
            case SC_FORBIDDEN:
                return Forbidden;
            case SC_NOT_FOUND:
                return NotFound;
            default:
                return InternalError;
        }

    }
}
