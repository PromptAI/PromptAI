package com.zervice.broker;

import com.zervice.broker.agent.AgentClientSelector;
import com.zervice.broker.backend.*;
import com.zervice.common.ShareConfig;
import com.zervice.common.utils.IdGenerator;
import com.zervice.common.utils.LayeredConf;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@Log4j2
public class BrokerApplicationRunner implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) throws Exception {
        IdGenerator.init(LayeredConf.getInt("nodeId", 0));

        String kbAuthCode = LayeredConf.getString(ShareConfig.KEY_RPC_AUTH_CODE, ShareConfig.DEFAULT_AUTH_CODE);
        if (StringUtils.isEmpty(kbAuthCode)) {
            throw new IllegalArgumentException("No valid auth code found");
        }

        BackendClientSelector.init(kbAuthCode);
        LOG.info("Backend clients inited");

        AgentClientSelector.getInstance().init();
        BackendClient client = BackendClientSelector.getMainBackendClient();
        KbChatService.getInstance().init(client);
        ProjectService.getInstance().init(client);
        KbMessageService.getInstance().init(client);
        ProjectComponentService.getInstance().init(client);
        PublishedProjectService.getInstance().init(client);
        LOG.info("Broker is ready ... ");
    }
}
