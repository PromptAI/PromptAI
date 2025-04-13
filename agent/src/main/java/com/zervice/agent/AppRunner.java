/**
 * Copyright (C) 2015, Promptai
 * All rights reserved.
 */

package com.zervice.agent;

import com.zervice.agent.published.project.PublishedProjectManager;
import com.zervice.agent.service.BackendClient;
import com.zervice.agent.utils.ConfConstant;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

/**
 * @author Peng Chen
 * @date 2022/6/16
 */
@Log4j2
@Component
public class AppRunner implements ApplicationRunner {

    private BackendClient client = BackendClient.getInstance();

    @Value("${public.url:}")
    private String publicUrl;

    @Override
    public void run(ApplicationArguments args) {
        if (StringUtils.isBlank(ConfConstant.AGENT_ID)) {
            throw new RuntimeException("invalid Agent id");
        }

        LOG.info("using default public url:{}", publicUrl);
        ConfConstant.PUBLIC_URL = publicUrl;

        // init Published Project...
        PublishedProjectManager.getInstance().init();

        AgentApplication.isReady = true;
        client.registry();
    }
}
