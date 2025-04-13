package com.zervice.kbase.api.rpc;

import com.alibaba.fastjson.JSONObject;
import com.zervice.common.pojo.common.EmptyResponse;
import com.zervice.common.restful.exception.RestException;
import com.zervice.common.restful.exception.StatusCodes;
import com.zervice.kbase.AccountCatalog;
import com.zervice.kbase.ai.convert.pojo.LlmConfig;
import com.zervice.kbase.api.restful.pojo.RestProjectComponentEntity;
import com.zervice.kbase.api.rpc.helper.ChatHelper;
import com.zervice.kbase.database.dao.ChatDao;
import com.zervice.kbase.database.pojo.Chat;
import com.zervice.kbase.database.pojo.ProjectComponent;
import com.zervice.kbase.database.pojo.PublishedProject;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * llmChatbot 使用的APIs
 *
 * @author chenchen
 * @Date 2024/11/18
 */
@Log4j2
@RestController
@RequestMapping("/rpc/llm/chat")
public class RpcLlmChatController {

    public static final Map<String, String> AUTH_HEADERS = new ConcurrentHashMap<>();

    static {
        AUTH_HEADERS.put("X-llmChatbot-Authorization", "Basic " + "aFxElzQvOEyY2hMjbWJ1NbCC==");
    }

    public static String llmChatDynamicVariablesWebhookUrl() {
        return LlmConfig.getServerAddr() + "/rpc/llm/chat/variables";
    }

    /**
     * 配合llm chat调用的webhook变量获取接口
     * <p>
     * - 确保这个接口在发生错误时也返回一个空对象（401除外），否则将导致 llmChatbot 无法正常工作
     */
    @PostMapping("variables")
    public Object variables(@RequestBody JSONObject data,
                            HttpServletRequest request) throws Exception {
        _checkAuth(request);

        String chatId = data.getString("chatId");
        String dbName = data.getString("accountName");
        String publishedProjectId = data.getString("publishedProjectId");
        LOG.info("[llm chatbot variables request:{}]", JSONObject.toJSONString(data));

        // check account && published project
        PublishedProject publishedProject = AccountCatalog.ensure(dbName).getPublishedProjects().get(publishedProjectId);
        if (publishedProject == null) {
            LOG.error("[{}:{}][llm chatbot variables request, published project:{} not found]", dbName, chatId, publishedProjectId);
            return EmptyResponse.empty();
        }

        // read chat variables
        Chat chat = ChatDao.get(dbName, chatId);
        if (chat == null) {
            LOG.error("[{}:{}][llm chatbot variables request, chat not found]", dbName, chatId);
            return EmptyResponse.empty();
        }

        Map<String/*id of entity*/, Object> slots = chat.getProperties().getSlots();
        if (slots == null) {
            LOG.warn("[{}:{}][llm chatbot variables request, slots is empty]", dbName, chatId);
            return EmptyResponse.empty();
        }

        // convert to  name -> value
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<String, Object> entry : slots.entrySet()) {
            String entityId = entry.getKey();
            Object value = entry.getValue();
            ProjectComponent component = ChatHelper.component(chatId, entityId, dbName);
            if (component != null && ProjectComponent.TYPE_ENTITY.equals(component.getType())) {
                RestProjectComponentEntity e = new RestProjectComponentEntity(component);

                result.put(e.getName(),value);
            }
        }

        LOG.info("[{}:{}][llm chatbot read variables:{}]", dbName, chatId, JSONObject.toJSONString(result));
        return result;
    }


    private void _checkAuth(HttpServletRequest request) {
        for (String key : AUTH_HEADERS.keySet()) {
            if (request.getHeader(key) == null || !request.getHeader(key).equals(AUTH_HEADERS.get(key))) {
                LOG.error("[Unauthorized access llm chat api, missing header:{}]", key);
                throw new RestException(StatusCodes.Unauthorized);
            }
        }
    }
}
