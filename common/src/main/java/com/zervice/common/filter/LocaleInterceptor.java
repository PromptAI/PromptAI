package com.zervice.common.filter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import java.util.Locale;


public class LocaleInterceptor extends LocaleChangeInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 提取locale信息
        String localeStr = request.getHeader("Accept-Language");
        if (StringUtils.isNotBlank(localeStr)) {
            Locale locale = Locale.forLanguageTag(localeStr);

            // 设置locale为当前上下文
            LocaleContextHolder.setLocale(locale);
        }
        return true;
    }


}