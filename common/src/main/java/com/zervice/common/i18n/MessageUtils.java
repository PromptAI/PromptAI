package com.zervice.common.i18n;

import com.zervice.common.restful.exception.StatusCodes;
import com.zervice.common.utils.ServletUtils;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.util.Locale;

/**
 * 国际化工具类
 */
public class MessageUtils {

    private static final String I18N_TYPE_MESSAGE = "i18n/messages";
    private static final String I18N_TYPE_REST_VALIDATOR_MESSAGE = "restValidator/messages";
    private static final String I18N_TYPE_EXCEPTION_MESSAGE = "messages/error";
    private static final String I18N_TYPE_COMPONENT_VALIDATOR_MESSAGE = "componentValidator/error";

    private static MessageSource messageSource = buildMessageSource(I18N_TYPE_MESSAGE);
    private static MessageSource restValidatorMessageSource = buildMessageSource(I18N_TYPE_REST_VALIDATOR_MESSAGE);
    private static MessageSource exceptionMessageSource = buildMessageSource(I18N_TYPE_EXCEPTION_MESSAGE);
    private static MessageSource componentValidatorMessageSource = buildMessageSource(I18N_TYPE_COMPONENT_VALIDATOR_MESSAGE);


    private static MessageSource buildMessageSource(String type) {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasenames(type);
        messageSource.setUseCodeAsDefaultMessage(true);
        messageSource.setDefaultEncoding("UTF-8");
        return messageSource;
    }

    public static String get(String msgKey, Object... args) {
        return get(msgKey, (Locale) null, args);
    }


    public static Locale getLocale(String name) {
        if (Locale.ENGLISH.getLanguage().equalsIgnoreCase(name)) {
            return Locale.ENGLISH;
        }

        // 默认支持中文
        return Locale.SIMPLIFIED_CHINESE;
    }

    public static String get(String msgKey, String locale, Object... args) {
        return get(msgKey, getLocale(locale), args);
    }

    /**
     * 从请求头/请求参数的国际化地区获取message
     */
    public static String getMessage(String msgKey, Object... args) {
        Locale locale = ServletUtils.getLocale();
        if (locale == null) {
            locale = LocaleContextHolder.getLocale();
        }
        try {
            return messageSource.getMessage(msgKey, args, locale);
        } catch (Exception e) {
            return msgKey;
        }
    }

    /**
     * 获取单个国际化翻译值
     */
    public static String get(String msgKey, Locale locale, Object... args) {
        if (locale == null) {
            locale = LocaleContextHolder.getLocale();
        }
        try {
            return messageSource.getMessage(msgKey, args, locale);
        } catch (Exception e) {
            return msgKey;
        }
    }

    public static String getExceptionMessage(String msgKey, Locale locale, Object... args) {
        if (locale == null) {
            locale = LocaleContextHolder.getLocale();
        }

        try {
            return exceptionMessageSource.getMessage(msgKey, args, locale);
        } catch (Exception e) {
            return msgKey;
        }
    }

    public static String getExceptionMessage(String msgKey, Object... args) {
        return getExceptionMessage(msgKey, ServletUtils.getLocale(), args);
    }

    public static String getRestValidatorMessage(String msgKey, Locale locale, Object... args) {
        if (locale == null) {
            locale = LocaleContextHolder.getLocale();
        }
        try {
            return restValidatorMessageSource.getMessage(msgKey, args, locale);
        } catch (Exception e) {
            return msgKey;
        }
    }

    public static String getComponentValidatorMessage(String msgKey, Locale locale, Object... args) {
        if (locale == null) {
            locale = LocaleContextHolder.getLocale();
        }
        try {
            return componentValidatorMessageSource.getMessage(msgKey, args, locale);
        } catch (Exception e) {
            return msgKey;
        }
    }


    // bizz
    public static String tokenNotEnoughErrorMessage() {
        String messageKey = "error." + StatusCodes.FastProjectTokenNotEnoughInChat.getInternalStatusCode();
        return MessageUtils.getExceptionMessage(messageKey, Locale.ENGLISH);
    }
}