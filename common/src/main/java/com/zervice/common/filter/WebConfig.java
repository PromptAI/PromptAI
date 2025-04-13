package com.zervice.common.filter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * @author Peng Chen
 * @date 2022/2/21
 */
@Component
public class WebConfig implements WebMvcConfigurer {
    private final List<Locale> supportedLocale = new ArrayList<>();
    {
        supportedLocale.add(Locale.CHINESE);
        supportedLocale.add(Locale.SIMPLIFIED_CHINESE);
        supportedLocale.add(Locale.US);
        supportedLocale.add(Locale.ENGLISH);
    }

    @Autowired
    private LogTraceInterceptor logTraceInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(logTraceInterceptor).addPathPatterns("/**");

        registry.addInterceptor(localeInterceptor());
    }

    @Bean
    public LocaleResolver localeResolver() {
        // i18n默认的转换器
        AcceptHeaderLocaleResolver acceptHeaderLocaleResolver = new AcceptHeaderLocaleResolver();
        acceptHeaderLocaleResolver.setDefaultLocale(Locale.ENGLISH);
        acceptHeaderLocaleResolver.setSupportedLocales(supportedLocale);
        return acceptHeaderLocaleResolver;
    }

    /**
     * LocaleChangeInterceptor 指定切换国际化语言的参数名。
     * 例如?lang=zh_CN 表示读取国际化文件messages_zh_CN.properties。
     */
    private LocaleChangeInterceptor localeInterceptor() {
        LocaleChangeInterceptor localeInterceptor = new LocaleInterceptor();
        localeInterceptor.setParamName("lang");

        return localeInterceptor;
    }
}
