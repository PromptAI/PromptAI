package com.zervice.agent.published.project;

import com.alibaba.fastjson.JSONObject;
import com.zervice.agent.ai.AiClient;
import lombok.*;
import lombok.extern.log4j.Log4j2;

/**
 * @author Peng Chen
 * @date 2022/8/4
 */
@Log4j2
@Builder
@ToString
@Setter@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PublishedProjectPojo {

    private String _id;

    private String _status;

    private String _agentId;

    private String _accountName;

    private String _projectId;

    private AiClient _aiClient;
    public AiClient aiClient() {
        if (_aiClient == null) {
            synchronized (this) {
                if (_aiClient == null) {
                    _aiClient = new AiClient(this);
                }
            }
        }

        return _aiClient;
    }


    /**
     * 最后访问时间 - 默认为创建时间
     */
    @Builder.Default
    private Long _lastVisitTime = System.currentTimeMillis();

    public static PublishedProjectPojo parse(JSONObject body) {
        PublishedProjectPojo pojo = new PublishedProjectPojo();

        JSONObject data = body.getJSONObject("data");
        pojo.setId(data.getString("id"));
        pojo.setStatus(data.getString("status"));
        pojo.setAgentId(data.getString("agentId"));
        pojo.setAccountName(data.getString("dbName"));
        pojo.setProjectId(data.getJSONObject("properties").getString("projectId"));

        // init client
        AiClient client = new AiClient(pojo);
        pojo.setAiClient(client);
        return pojo;
    }

    public void refreshLastVisitTime() {
        _lastVisitTime = System.currentTimeMillis();
    }
}
