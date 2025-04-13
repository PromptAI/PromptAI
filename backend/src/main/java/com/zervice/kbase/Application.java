package com.zervice.kbase;

import com.zervice.common.utils.LayeredConf;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.Banner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.FileSystemResource;
import org.springframework.scheduling.annotation.EnableAsync;

@Log4j2
@EnableAsync
@SpringBootApplication(scanBasePackages = {"com.zervice"}, exclude = {
        MongoAutoConfiguration.class,
        QuartzAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
})

@ServletComponentScan(basePackages = {"com.zervice"})
public class Application {

    public static boolean isReady = false;

    @Bean
    public PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        PropertySourcesPlaceholderConfigurer properties =
                new PropertySourcesPlaceholderConfigurer();
        properties.setLocation(new FileSystemResource(System.getProperty("kb.config")));
        properties.setIgnoreResourceNotFound(false);
        return properties;
    }

    public static void main(String[] args) {
        try {
            String confFile = System.getProperty("kb.config");
            if (!StringUtils.isEmpty(confFile)) {
                LOG.info("Loading configuration from environment variable kb.config - " + confFile);
                LayeredConf.getInstance().load(confFile);
            } else {
                throw new IllegalArgumentException("No conf file kb.config found.  run with -Dkb.config=WhereIsKb.config");
            }
        } catch (Exception ioe) {
            LOG.error("Cannot load configuration file. Exit!");
            ioe.printStackTrace();
            return;
        }

        SpringApplicationBuilder builder = new SpringApplicationBuilder(Application.class)
                .bannerMode(Banner.Mode.OFF);
        builder.run(args);
    }


}
