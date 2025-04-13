package com.zervice.agent;

import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @author Peng Chen
 * @date 2022/6/15
 */
@Log4j2
@EnableAsync
@EnableScheduling
@SpringBootApplication(scanBasePackages = {"com.zervice"})
public class AgentApplication {

    public static boolean isReady = false;

    public static void main(String[] args) {
        SpringApplication.run(AgentApplication.class, args);
    }
}
