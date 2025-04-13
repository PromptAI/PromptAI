package com.zervice.common;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * https://codippa.com/how-to-autowire-objects-in-non-spring-classes/
 */
@Component
public class SpringContext implements ApplicationContextAware {
    @Autowired
    static ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }

    public static ApplicationContext getAppContext() {
        return context;
    }

    public static String getProperty(String key, String defaultValue) {
        return getAppContext().getEnvironment().getProperty(key, defaultValue);
    }

    public static String getProperty(String key) {
        return  getAppContext().getEnvironment().getProperty(key);
    }
}