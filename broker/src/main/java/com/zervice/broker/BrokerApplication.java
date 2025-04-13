package com.zervice.broker;

import com.zervice.common.utils.LayeredConf;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.FileSystemResource;

import java.io.IOException;

/**
 * @author Peng Chen
 * @date 2022/7/5
 */
@SpringBootApplication(scanBasePackages = "com.zervice")
@ServletComponentScan(basePackages = "com.zervice")
public class BrokerApplication {

    public static void main(String[] args) {
        try {
            String confFile = System.getProperty("broker.config");
            if (!StringUtils.isEmpty(confFile)) {
                System.out.println("Loading configuration from environment variable broker.config - " + confFile);
                LayeredConf.getInstance().load(confFile);
            }
            else {
                throw new IllegalArgumentException("No conf file broker.config found. run with -Dbroker.config=WhereIsBroker.config");
            }
        }
        catch (IOException ioe) {
            System.err.println("Cannot load configuration file. Using defaults");
            ioe.printStackTrace();
        }

        SpringApplication.run(BrokerApplication.class, args);
    }

    @Bean
    public PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        PropertySourcesPlaceholderConfigurer properties =
                new PropertySourcesPlaceholderConfigurer();
        properties.setLocation(new FileSystemResource(System.getProperty("broker.config")));
        properties.setIgnoreResourceNotFound(false);
        return properties;
    }
}
