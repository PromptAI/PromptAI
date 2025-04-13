package com.zervice.kbase.ai.convert.pojo;

import com.zervice.common.utils.EnvUtil;
import com.zervice.kbase.ServerInfo;
import com.zervice.kbase.ZBotRuntime;
import com.zervice.kbase.ai.convert.PublishContext;
import com.zervice.kbase.api.rpc.RpcFilter;
import lombok.*;

import java.util.HashMap;
import java.util.Map;

@Builder
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class LlmConfig {

    private Map<String, String> _headers;
    private String _server;

    public static String getServerAddr() {
        String server = ServerInfo.getServerAddr();
        // Docker 模式下，需要将docker安装expose的port通过ENV注入进来，否则LLM请求不到GPT的api
        if (ZBotRuntime.runInDockerModel()) {
            server = "http://host.docker.internal:" + EnvUtil.getEnvOrProp("EXPOSE_PORT", "80");
        }

        return server;
    }

    public static LlmConfig factory(PublishContext context) {
        // prepare headers
        Map<String, String> headers = new HashMap<>();
        headers.put(RpcFilter.X_EXTERNAL_PUBLISHED_PROJECT_ID_HEADER, context.publishedProjectId());
        headers.put(RpcFilter.X_EXTERNAL_PROJECT_ID_HEADER, context.projectId());
        headers.put(RpcFilter.X_EXTERNAL_ACCOUNT_ID_HEADER, context.dbName());
        headers.put("Content-Type", "application/json");

        String server = getServerAddr();

        return LlmConfig.builder()
                .server(server)
                .headers(headers)
                .build();
    }
}