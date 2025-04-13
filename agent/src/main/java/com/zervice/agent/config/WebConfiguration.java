package com.zervice.agent.config;

import com.zervice.agent.rest.interceptor.AuthInterceptor;
import com.zervice.agent.rest.interceptor.PublishedProjectInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author Peng Chen
 * @date 2022/6/29
 */
@Configuration
public class WebConfiguration implements WebMvcConfigurer {

    @Autowired
    private AuthInterceptor interceptor;
    @Autowired
    private PublishedProjectInterceptor publishedProjectInterceptor;
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptor)
                .addPathPatterns("/**")
                .excludePathPatterns()
                .excludePathPatterns("/api/health", "/api/version", "/actuator/*");
        registry.addInterceptor(publishedProjectInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/api/health", "/api/version", "/api/publish/project", "/actuator/*");
    }
}
