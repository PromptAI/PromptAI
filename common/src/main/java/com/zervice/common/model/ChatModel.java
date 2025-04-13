package com.zervice.common.model;

import com.alibaba.fastjson.JSONObject;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * @author xh
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatModel {

    private String _chatId;

    @NotNull
    private String _projectId;
    @NotNull(message = "publishedProjectId required")
    private String _publishedProjectId;
    @NotNull(message = "requestIp required")
    private String _ip;
    /**
     * chat scene
     */
    @NotNull
    private String _scene;

    /**
     * faq/flow id, 可选值
     */
    private String _componentId;

    /**
     * 需要填充的slot
     *  - id - val
     */
    private Map<String, Object> _slots;

    /**
     * 需要存储的变量
     * - name - val
     */
    private Map<String, Object> _variables;

    private String _browser;
    private String _os;
    private String _platform;
    private String _mobile;

    public JSONObject getUserAgent() {
        JSONObject userAgent = new JSONObject();
        userAgent.put("browser", _browser);
        userAgent.put("os", _os);
        userAgent.put("platform", _platform);
        userAgent.put("mobile", _mobile);
        return userAgent;
    }
}
