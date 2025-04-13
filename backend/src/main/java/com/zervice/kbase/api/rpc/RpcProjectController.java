package com.zervice.kbase.api.rpc;

import com.google.common.collect.Maps;
import com.zervice.common.i18n.MessageUtils;
import com.zervice.common.utils.Constants;
import com.zervice.kbase.api.BaseController;
import com.zervice.kbase.api.restful.AuthFilter;
import com.zervice.kbase.api.restful.controller.ProjectController;
import com.zervice.kbase.api.restful.pojo.RestPublishedProject;
import com.zervice.kbase.api.restful.pojo.mica.Button;
import com.zervice.kbase.api.rpc.helper.ChatHelper;
import com.zervice.kbase.database.pojo.Chat;
import com.zervice.kbase.database.pojo.Project;
import com.zervice.kbase.database.pojo.ProjectComponent;
import com.zervice.kbase.database.pojo.PublishedProject;
import com.zervice.kbase.service.ChatService;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Log4j2
@RestController
@RequestMapping("/rpc/project")
public class RpcProjectController extends BaseController {

    @Autowired
    private ChatService chatService;

    @GetMapping("welcome")
    public Object welcome(@RequestParam String publishedProjectId,
                          @RequestParam String chatId,
                          @RequestParam String projectId,
                          @RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName) throws Exception {
        if (StringUtils.isNotEmpty(chatId)) {
            Chat chat = chatService.get(dbName, chatId);
            if (chat != null) {
                projectId = chat.getProperties().getProjectId();
            }
            return _getWelcomeFromProject(chatId, projectId, dbName);
        }
        if (StringUtils.isNotEmpty(projectId)) {
            return _getWelcomeFromProject(chatId, projectId, dbName);
        }
        PublishedProject publishedProject = ChatHelper.publishedProject(publishedProjectId, dbName);
        if (publishedProject == null) {
            return MessageUtils.get(Constants.I18N_DEFAULT_PROJECT_WELCOME);
        }
        return _getWelcomeFromProject(chatId, publishedProject.getProperties().getProjectId(), dbName);
    }

    @GetMapping("fallback")
    public Object fallback(@RequestParam String publishedProjectId,
                           @RequestParam String chatId,
                           @RequestParam(required = false) String projectId,
                           @RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName) throws Exception {
        PublishedProject publishedProject = ChatHelper.publishedProject(publishedProjectId, dbName);
        if (publishedProject == null) {
            String defaultFallback = MessageUtils.get(Constants.I18N_DEFAULT_PROJECT_FALLBACK);
            return Map.of("fallback", defaultFallback);
        }
        return _getFallBackFromProject(chatId, publishedProject, dbName);
    }

    private Object _getWelcomeFromProject(String chatId, String projectId, String dbName) throws Exception {
        // 读取project 中的欢迎语
        Map<String, String> result = Maps.newHashMapWithExpectedSize(2);
        Project project = ChatHelper.project(chatId, projectId, dbName);
        String welcome;
        if (project == null) {
            LOG.warn("[{}][get welcome from project fail, project not found:{}]", dbName, projectId);
            welcome = MessageUtils.get(Constants.I18N_DEFAULT_PROJECT_WELCOME, Locale.SIMPLIFIED_CHINESE);
            result.put("fallback", welcome);
        } else {
            welcome = project.getProperties().getWelcome();
            if (StringUtils.isBlank(welcome)) {
                welcome = MessageUtils.get(Constants.I18N_DEFAULT_PROJECT_WELCOME, project.getProperties().getLocale());
                LOG.warn("[{}][get welcome from project:{} fail, user default welcome:{}]", dbName, projectId, welcome);
            }

            result.put("welcome", welcome);
        }
        return result;
    }

    private Object _getFallBackFromProject(String chatId, PublishedProject publishedProject, String dbName) throws Exception {
        String projectId = publishedProject.getProperties().getProjectId();

        // 读取project 中的欢迎语
        Map<String, Object> result = Maps.newHashMapWithExpectedSize(2);
        Project project = ChatHelper.project(chatId, projectId, dbName);
        String fallback;
        if (project == null) {
            LOG.warn("[{}][get fallback from project fail, project not found:{}]", dbName, projectId);
            fallback = MessageUtils.get(Constants.I18N_DEFAULT_PROJECT_FALLBACK, Locale.SIMPLIFIED_CHINESE);
            result.put("fallback", fallback);
        } else {
            fallback = project.getProperties().getFallback();
            if (StringUtils.isBlank(fallback)) {
                fallback = MessageUtils.get(Constants.I18N_DEFAULT_PROJECT_FALLBACK, project.getProperties().getLocale());
                LOG.warn("[{}][get fallback from project:{} fail, user default fallback:{}]", dbName, projectId, fallback);
            }

        }

        result.put("fallback", fallback);
        return result;
    }
}
