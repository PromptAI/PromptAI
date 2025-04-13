package com.zervice.kbase.api.rpc;

import com.alibaba.fastjson.JSONObject;
import com.maxmind.geoip2.model.CityResponse;
import com.zervice.common.model.ChatModel;
import com.zervice.common.pojo.chat.ButtonPojo;
import com.zervice.common.pojo.chat.ChatPojo;
import com.zervice.common.utils.JSONUtils;
import com.zervice.kbase.AccountCatalog;
import com.zervice.kbase.api.restful.AuthFilter;
import com.zervice.kbase.api.restful.pojo.RestBaseProjectComponent;
import com.zervice.kbase.api.restful.pojo.RestProjectComponentConversation;
import com.zervice.kbase.api.restful.pojo.RestProjectComponentEntity;
import com.zervice.kbase.api.rpc.helper.ChatHelper;
import com.zervice.kbase.api.rpc.pojo.IpPojo;
import com.zervice.kbase.database.dao.ChatDao;
import com.zervice.kbase.database.pojo.Chat;
import com.zervice.kbase.database.pojo.Project;
import com.zervice.kbase.database.pojo.ProjectComponent;
import com.zervice.kbase.database.pojo.PublishedProject;
import com.zervice.kbase.service.ChatService;
import com.zervice.kbase.utils.DbIpUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * @author xh
 */
@Log4j2
@RestController
@RequestMapping("/rpc/chat")
public class RpcChatController {

    @Autowired
    private ChatService chatService;

    @Value("${trial.chat.count.per.day:20}")
    private Integer _chatCountPerDay;

    @GetMapping("settings")
    public Object settings(@RequestParam String projectId,
                           @RequestParam String publishedProjectId,
                           @RequestParam String scene,
                           @RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName) throws Exception {
        Project project = ChatHelper.project(scene, projectId, publishedProjectId, dbName);
        if (project == null) {
            return "{}";
        }
        Project.ChatBotSetting setting = project.getProperties().getChatBotSettings();

        _resetChatSettingSlots(setting, scene, projectId, publishedProjectId, dbName);
        return setting;
    }

    /**
     * 动态设置 slots
     * - filter entities witch default value enabled
     *
     * LLM Project不使用系统变量，所以在筛选的时候过滤掉了系统变量
     */
    private void _resetChatSettingSlots(Project.ChatBotSetting setting, String scene, String projectId, String publishedProjectId, String dbName) {
        try {
            Set<ProjectComponent> entities = ChatHelper.projectEntities(scene, projectId, publishedProjectId, dbName);

            List<RestProjectComponentEntity> restEntities = entities.stream()
                    .map(e -> new RestProjectComponentEntity(e))
                    .filter(e -> Boolean.TRUE.equals(e.getDefaultValueEnable())
                            && StringUtils.isNotBlank(e.getDefaultValue())
                            && StringUtils.isNotBlank(e.getDefaultValueType())
                    )
                    .collect(Collectors.toList());
            if (restEntities.isEmpty()) {
                return;
            }
            List<Project.Slot> slots = restEntities.stream()
                    .map(Project.Slot::factory)
                    .collect(Collectors.toList());

            setting.setSlots(slots);
        } catch (Exception e) {
            LOG.error("[{}][reset chat setting slots with error:{}]", dbName, e.getMessage(), e);
        }
    }

    @PostMapping
    public Object save(@RequestBody @Validated ChatModel chatModel,
                       @RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName) throws Exception {
        ChatPojo chatPojo = _searchChatFromDb(chatModel, dbName);
        if (chatPojo != null) {
            return chatPojo;
        }

        // published project 不在运行状态就没必要创建chat了
        PublishedProject publishedProject = AccountCatalog.ensure(dbName).getPublishedProjects().get(chatModel.getPublishedProjectId());
        if (publishedProject == null || !PublishedProject.STATUS_RUNNING.equals(publishedProject.getStatus())) {
            JSONObject res = new JSONObject();
            res.put("ok", false);
            return res;
        }

        return _save(chatModel, publishedProject, dbName);
    }

    /**
     * 将Project的entity做快照
     */
    private List<Chat.SimpleEntity> _buildProjectEntitySnapshot(String scene, String projectId, String publishedProjectId, String dbName) throws Exception {
        Set<ProjectComponent> projectEntities = ChatHelper.projectEntities(scene, projectId, publishedProjectId, dbName);
        return Chat.buildEntitySnapshot(projectEntities);
    }


    private ChatPojo _searchChatFromDb(ChatModel chatModel, String dbName) {
        String chatId = chatModel.getChatId();
        if (StringUtils.isBlank(chatId)) {
            return null;
        }
        Chat chat = ChatDao.get(dbName, chatModel.getChatId());
        if (chat == null) {
            return null;
        }

        LOG.info("[{}:{}][success get chat from db]", dbName, chatId);
        return JSONObject.parseObject(JSONObject.toJSONString(chat), ChatPojo.class);
    }

    private ChatPojo _save(ChatModel chatModel, PublishedProject project, String dbName) throws Exception {
        LOG.info("[{}:{}][try save chat. ip:{},publishProjectId:{}]", dbName, chatModel.getProjectId(),
                chatModel.getIp(), chatModel.getPublishedProjectId());

        String ip = chatModel.getIp();
        String projectId = chatModel.getProjectId();
        String publishedProjectId = chatModel.getPublishedProjectId();
        String scene = chatModel.getScene();

        // 如果有 components， 那么可以由java 端驱动
        String handleBy = Chat.HANDLE_BY_MODEL;

        IpPojo ipPojo = _queryIp(ip);
        List<Chat.SimpleEntity> entitySnapshot = _buildProjectEntitySnapshot(scene, projectId, publishedProjectId, dbName);

        Chat chat = Chat.factory(ip, scene, projectId, publishedProjectId, handleBy);
        // using provided id if exits
        chat.setId(chatModel.getChatId());
        chat.getProperties().setEntitySnapshot(entitySnapshot);
        chat.getProperties().setIpExtra(JSONUtils.toJsonObject(ipPojo));

        // save slots and variables
        chat.getProperties().setSlots(chatModel.getSlots());
        chat.getProperties().setVariables(chatModel.getVariables());
        chat.getProperties().setUserAgent(chatModel.getUserAgent());

        // 设置其他属性
        _afterChatGenerated(chat, project, dbName);

        // save 2 db
        chat = chatService.save(dbName, chat);

        ChatPojo chatPojo = JSONObject.parseObject(JSONObject.toJSONString(chat), ChatPojo.class);

        LOG.info("[{}:{}][success generate chat from ip:{}, publishProjectId:{}]",
                dbName, chatModel.getProjectId(), chatModel.getIp(), chatPojo.getId());

        return chatPojo;
    }

    private IpPojo _queryIp(String ip) {
        CityResponse cityResponse = DbIpUtils.query(ip);
        return cityResponse == null ? IpPojo.empty(ip) : IpPojo.parse(cityResponse);
    }

    private void _afterChatGenerated(Chat chat, PublishedProject publishedProject, String dbName) throws Exception {
        // 1、 设置按钮属性
        String scene = chat.getProperties().getScene();
        String projectId = chat.getProperties().getProjectId();
        String publishedProjectId = chat.getProperties().getPublishedProjectId();

        //设置分支作为button展示的时候是否隐藏
        Project project = ChatHelper.project(scene, projectId, publishedProjectId, dbName);

        Chat.Prop prop = chat.getProperties();

        // 欢迎语
        String welcome = project.getProperties().getWelcome();
        prop.setWelcome(welcome);

        // 默认回答
        String fallback = project.getProperties().getFallback();
        prop.setFallback(fallback);

        // locale
        String locale = project.getProperties().getLocale();
        prop.setLocale(locale);

        // published roots
        List<String> publishedComponents = publishedProject.getProperties().getComponentIds();
        prop.setPublishedComponents(publishedComponents);

        JSONObject chatBotSettings = JSONObject.parseObject(JSONObject.toJSONString(project.getProperties().getChatBotSettings()));
        prop.setChatBotSettings(chatBotSettings);

        // 需要将conversation展示为节点
        _setConversationButtons(project, prop, scene, publishedProjectId, dbName);
    }


    private void _setConversationButtons(Project project, Chat.Prop prop,String scene,
                                         String publishedProjectId, String dbName) throws Exception {
        String showSubNodes = project.getProperties().getShowSubNodesAsOptional();
        prop.setShowSubNodesAsOptional(showSubNodes);

        // 不展示任何节点
        if (Project.Prop.SHOW_SUB_NODES_AS_OPTIONAL_NONE.equals(showSubNodes)) {
            return;
        }

        // 查询部署的conversations
        List<RestProjectComponentConversation> deployedConversations = ChatHelper.deployedConversations(scene, publishedProjectId, dbName);

        // 展示全部
        if (Project.Prop.SHOW_SUB_NODES_AS_OPTIONAL_ALL.equals(showSubNodes)) {
            // 转成button
            List<ButtonPojo> buttonPojos = deployedConversations.stream()
                    .map(c -> ButtonPojo.factory(c.getId(), c.getData().getName(), Boolean.FALSE))
                    .collect(Collectors.toList());

            prop.setButtons(buttonPojos);
            return;
        }

        // 展示未隐藏的
        if (Project.Prop.SHOW_SUB_NODES_AS_OPTIONAL_CUSTOM.equals(showSubNodes)) {
            List<ButtonPojo> buttonPojos = deployedConversations.stream()
                    // 筛选未隐藏的
                    .filter(c -> Boolean.FALSE.equals(c.getData().getHidden()))
                    .map(c -> ButtonPojo.factory(c.getId(), c.getData().getName(), c.getData().getHidden()))
                    .collect(Collectors.toList());

            prop.setButtons(buttonPojos);
        }

        LOG.warn("[{}][unknown type:{} of project:{} show sub nodes as optional]", dbName, showSubNodes, project.getId());
    }

    private List<JSONObject> _convertToJSONObjects(List<RestBaseProjectComponent> components) {
        if (CollectionUtils.isEmpty(components)) {
            return Collections.emptyList();
        }

        return components.stream()
                .map(c -> JSONObject.parseObject(JSONObject.toJSONString(c)))
                .collect(Collectors.toList());
    }
}
