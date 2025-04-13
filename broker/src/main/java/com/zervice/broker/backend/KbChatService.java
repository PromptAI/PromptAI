package com.zervice.broker.backend;

import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import com.alibaba.fastjson.JSONObject;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.zervice.broker.restful.pojo.CreateChatReq;
import com.zervice.common.model.ChatModel;
import com.zervice.common.pojo.chat.ChatPojo;
import com.zervice.common.pojo.chat.ProjectComponentEntityPojo;
import com.zervice.common.restful.exception.RestException;
import com.zervice.common.restful.exception.StatusCodes;
import com.zervice.common.utils.NetworkUtils;
import lombok.extern.log4j.Log4j2;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

@Log4j2
public class KbChatService {
    /** 未知 **/
    public static final String UNKNOWN = "unknown";
    /** 移动端 **/
    public static final String MOBILE = "mobile";
    /** 其他 **/
    public static final String MOBILE_OTHER = "other";

    /** 未知faq/conversation id **/
    public static final String PROJECT_BRANCH_ID_UNKNOWN = UNKNOWN;

    public static KbChatService _instance = new KbChatService();
    private ProjectComponentService _componentService = ProjectComponentService.getInstance();

    private BackendClient _backendClient = null;

    private static final String _URI_CHAT = "/rpc/chat";
    private static final String _URI_CHAT_SETTINGS = "/rpc/chat/settings";

    public static KbChatService getInstance() {
        return _instance;
    }

    private KbChatService() {
    }

    /**
     * chat过期时间
     */
    private final Integer expirationInSec = 30 * 60;

    /**
     * 按照account 区分load cache...
     * 这里只保存自驱动的chat： key:chat, value:chat pojo
     */
    private final Map<String, Map<String, ChatPojo>> chatCaches = new ConcurrentHashMap<>();

    public void init(BackendClient backendClient) {
        this._backendClient = backendClient;

        _scheduleRemoveExpireChatTask();
    }

    private void _scheduleRemoveExpireChatTask() {
        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setDaemon(true)
                .setNameFormat("cleanup-expire-chat")
                .build();

        Executors.newSingleThreadScheduledExecutor(threadFactory).scheduleAtFixedRate(() -> {
            try {

                for (Map.Entry<String /*accountName*/, Map<String  /*chat id*/, ChatPojo>> accountChatCache : chatCaches.entrySet()) {
                    accountChatCache.getValue()
                            .entrySet()
                            .removeIf(accountChat -> accountChat.getValue().expired(expirationInSec));
                }
            } catch (Exception e) {
                LOG.error("Fail to remove expiring chats", e);
            }
        }, 10, expirationInSec, TimeUnit.SECONDS);
    }

    public ChatPojo createChatIfNotExist(String chatId, String projectId, String publishedProjectId,
                                         String scene, String accountName, HttpServletRequest request) {
        ChatPojo chatPojo = searchChat(accountName, chatId);
        if (chatPojo != null) {
            return chatPojo;
        }

        CreateChatReq chatReq = CreateChatReq.builder()
                .chatId(chatId).scene(scene)
                .projectId(projectId)
                .publishedProjectId(publishedProjectId)
                .build();

        chatPojo = apply(chatReq, request, accountName);

        LOG.info("[{}:{}][success search chat from backend]", accountName, chatId);
        return chatPojo;
    }

    public JSONObject settings(String publishedProjectId, String projectId, String scene, String accountName) {
        String uri = _URI_CHAT_SETTINGS + "?publishedProjectId=" + publishedProjectId + "&projectId=" + projectId + "&scene=" + scene;
        return _backendClient.getJson(accountName, uri);
    }

    /**
     * 在kb那边先搜索，没搜索到就创建
     */
    public ChatPojo apply(CreateChatReq chatReq, HttpServletRequest request,
                          String accountName) {
        LOG.info("[{}:{}][try to apply chat with param:{}]", accountName,
                chatReq.getPublishedProjectId(), JSONObject.toJSONString(chatReq));

        String chatId = chatReq.getChatId();
        String ip = NetworkUtils.getRemoteIP(request);
        String componentId = chatReq.getComponentId();
        String projectId = chatReq.getProjectId();
        String publishedProjectId = chatReq.getPublishedProjectId();
        String scene = chatReq.getScene();

        ChatModel chatModel = ChatModel.builder()
                .chatId(chatId)
                .scene(scene)
                .componentId(componentId)
                .projectId(projectId).ip(ip)
                .publishedProjectId(publishedProjectId)
                .slots(chatReq.getSlots()).variables(chatReq.getVariables())
                .build();

        _addInfoFromUserAgent(chatModel, accountName, request);

        return _creatChat(chatModel, accountName);
    }

    /**
     * 解析user agent中可用的信息
     */
    private void _addInfoFromUserAgent(ChatModel chatModel, String accountName, HttpServletRequest request) {
        String userAgentFromRequest = request.getHeader("User-Agent");

        String browser = UNKNOWN;
        String os = UNKNOWN;
        String platform =UNKNOWN;
        String mobile =UNKNOWN;

        UserAgent userAgent = UserAgentUtil.parse(userAgentFromRequest);
        if (userAgent == null) {
            LOG.warn("[{}][parse user agent:{} fail from request]", accountName, userAgentFromRequest);
        } else {
            browser = userAgent.getBrowser().getName();
            os = userAgent.getOs().getName();
            platform = userAgent.getPlatform().getName();
            mobile = userAgent.isMobile() ? MOBILE : MOBILE_OTHER;
        }

        chatModel.setBrowser(browser);
        chatModel.setOs(os);
        chatModel.setPlatform(platform);
        chatModel.setMobile(mobile);
    }

    private ChatPojo _creatChat(ChatModel chatModel, String accountName) {
        String publishedProjectId = chatModel.getPublishedProjectId();
        String projectId = chatModel.getProjectId();
        JSONObject chatRes = _backendClient.postJson(accountName, _URI_CHAT, chatModel);

        // 有自定义错误
        if (chatRes.containsKey("error") && chatRes.getString("error") != null) {
            String error = chatRes.getString("error");
            try {
                StatusCodes statusCodes = StatusCodes.valueOf(error);
                throw new RestException(statusCodes);
            } catch (RestException e) {
                throw e;
            } catch (Exception e) {
                LOG.warn("[{}][unknown error name:{} with message:{}]", accountName, error, e.getMessage(), e);
                throw new RestException(StatusCodes.InternalError);
            }
        }

        //检查published project是否处于运行状态
        if (chatRes.containsKey("ok") && !chatRes.getBoolean("ok")) {
            throw new RestException(StatusCodes.PublishedProjectNotExistsOrRunning);
        }

        ChatPojo chatPojo = new ChatPojo(chatRes);

        _afterApplied(accountName, chatPojo);
        LOG.info("[{}:{}][apply chat success. projectId:{}, id: {}]",
                accountName, publishedProjectId, projectId, chatPojo.getId());
        return chatPojo;
    }

    /**
     * backend驱动失败的时候清空components
     * @param chatId
     * @param accountName
     */
    public void clearChatComponents(String chatId, String accountName) {
        Map<String, ChatPojo> accountChatCache = chatCaches.get(accountName);
        if (accountChatCache != null) {
            ChatPojo chatPojo = accountChatCache.get(chatId);
            if (chatPojo == null) {
                return;
            }
            chatPojo.setComponents(null);
        }
    }

    private void _afterApplied(String accountName, ChatPojo chatPojo) {
        Map<String, ChatPojo> accountChatCache = chatCaches.get(accountName);
        if (accountChatCache == null) {
            accountChatCache = new ConcurrentHashMap<>();
            chatCaches.put(accountName, accountChatCache);
        }

        accountChatCache.put(chatPojo.getId(), chatPojo);
        LOG.debug("add chat:{} to cache", chatPojo.getId());
    }

    public ChatPojo searchChat(String accountName, String chatId) {
        Map<String, ChatPojo> accountChatCache = chatCaches.get(accountName);
        if (accountChatCache == null) {
            return null;
        }

        try {
            ChatPojo chatPojo = accountChatCache.get(chatId);
            if (chatPojo != null) {
                chatPojo.refreshLastReadTime();
            }

            return chatPojo;
        } catch (Exception e) {
            LOG.error("[{}][search chat:{} from cache error]", accountName, chatId);
            return null;
        }
    }

    public ChatPojo searchChatWithEntity(String accountName, String chatId) {
        ChatPojo chatPojo = searchChat(accountName, chatId);
        if (chatPojo == null) {
            return null;
        }

        if (chatPojo.existEntities()) {
            return chatPojo;
        }

        String projectId = chatPojo.getProperties().getProjectId();

        // query entities
        List<ProjectComponentEntityPojo> entityPojos = _componentService.entities(chatId, projectId, accountName);
        chatPojo.getProperties().setEntities(entityPojos);

        return chatPojo;
    }
}
