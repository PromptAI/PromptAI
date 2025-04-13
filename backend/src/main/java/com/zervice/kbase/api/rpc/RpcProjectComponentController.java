package com.zervice.kbase.api.rpc;

import com.zervice.common.pojo.chat.ProjectComponentUserPojo;
import com.zervice.common.pojo.chat.SimilarQuestionPojo;
import com.zervice.common.pojo.common.EmptyResponse;
import com.zervice.kbase.api.restful.AuthFilter;
import com.zervice.kbase.api.restful.pojo.RestBaseProjectComponent;
import com.zervice.kbase.api.restful.pojo.RestProjectComponentConversation;
import com.zervice.kbase.api.restful.pojo.RestProjectComponentEntity;
import com.zervice.kbase.api.restful.pojo.RestProjectComponentUser;
import com.zervice.kbase.api.rpc.helper.ChatHelper;
import com.zervice.kbase.database.pojo.Project;
import com.zervice.kbase.database.pojo.ProjectComponent;
import com.zervice.kbase.service.ProjectComponentService;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author chen
 * @date 2022/8/30
 */
@Log4j2
@RestController
@RequestMapping("/rpc/project/component")
public class RpcProjectComponentController {

    private ProjectComponentService _projectComponentService = ProjectComponentService.getInstance();

    /**
     * click id 可能是 componentId 或 user.name这里尝试搜索对应的id
     */
    @GetMapping("search")
    public Object search(@RequestParam String componentIdOrName,
                         @RequestParam String chatId,
                         @RequestParam String publishedProjectId,
                         @RequestParam String projectId,
                         @RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName) throws Exception{
        LOG.info("[{}][search component via:{}]", dbName, componentIdOrName);
        ProjectComponent component = ChatHelper.component(chatId, componentIdOrName, dbName);
        if (component == null) {
            LOG.info("[{}][try to search component via idOrName:{}]", dbName, componentIdOrName);
            // try via name
            List<ProjectComponent> users = ChatHelper.getByType(ProjectComponent.TYPE_USER, projectId, chatId, dbName);
            if (CollectionUtils.isEmpty(users)) {
                return EmptyResponse.empty();
            }

            Optional<ProjectComponent> userOp = users.stream()
                    .filter(p -> {
                        RestProjectComponentUser u = new RestProjectComponentUser(p);
                        String name = u.getData().getName();
                        return StringUtils.isNotBlank(name) && Objects.equals(componentIdOrName, name);
                    })
                    .findAny();
            if (userOp.isPresent()) {
                component = userOp.get();
            }
        }

        if (component == null) {
            LOG.info("[{}][no component searched component via idOrName:{}]",dbName, componentIdOrName);
            return EmptyResponse.empty();
        }

        Project project = ChatHelper.project(chatId, projectId, dbName);
        List<ProjectComponent> allComponents = ChatHelper.components(chatId, projectId, dbName);
        return _projectComponentService.convertToRest(component, allComponents, project, dbName);
    }

    /**
     * 根据输入匹配user/conversation节点
     */
    @GetMapping("query")
    public Object query(@RequestParam String input,
                        // 是否精确匹配
                        @RequestParam(required = false, defaultValue = "true") Boolean exact,
                        @RequestParam String chatId,
                        @RequestParam String publishedProjectId,
                        @RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName) throws Exception {
        // 查询发布后的组件
        List<ProjectComponent> components = ChatHelper.getDeployedComponents(chatId, publishedProjectId, dbName);
        List<ProjectComponent> userOrConversations = components.stream()
                .filter(c -> ProjectComponent.TYPE_USER.equals(c.getType()) || ProjectComponent.TYPE_CONVERSATION.equals(c.getType()))
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(userOrConversations)) {
            return Collections.emptyList();
        }

        String projectId = userOrConversations.get(0).getProjectId();
        Project project = ChatHelper.project(chatId, projectId, dbName);
        List<ProjectComponent> allComponents = ChatHelper.components(chatId, projectId, dbName);
        List<RestBaseProjectComponent> restUsers = _projectComponentService.convertToRest(userOrConversations, allComponents, project, dbName);
        return _filterUserOrConversations(input, exact, restUsers);
    }

    /**
     * decode 相似问
     */
    @GetMapping("similar/question")
    public Object similarQuestion(@RequestParam Set<String> ids,
                                  @RequestParam String chatId,
                                  @RequestParam String projectId,
                                  @RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName) throws Exception {
        List<RestProjectComponentUser> users = ChatHelper.components(chatId, ids, dbName).stream()
                .filter(p -> projectId.equals(p.getProjectId()) && ProjectComponent.TYPE_USER.equals(p.getType()))
                .map(RestProjectComponentUser::new)
                .collect(Collectors.toList());

        return users.stream()
                .filter(u -> CollectionUtils.isNotEmpty(u.getData().getExamples()))
                .map(u -> new SimilarQuestionPojo(u.getId(), u.getData().getExamples().get(0).getText()))
                .collect(Collectors.toList());
    }

    @GetMapping("entity/{projectId}")
    public Object entity(@PathVariable String projectId,
                         @RequestParam String chatId,
                         @RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName) throws Exception {
        Set<ProjectComponent> projectEntity = ChatHelper.projectEntities(chatId, projectId, dbName);
        return projectEntity.stream()
                .distinct()
                .map(e -> new RestProjectComponentEntity(e))
                .collect(Collectors.toList());
    }

    @GetMapping("{id}")
    public Object get(@PathVariable String id,
                      @RequestParam String chatId,
                      @RequestParam String projectId,
                      @RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName) throws Exception {
        ProjectComponent projectComponent = ChatHelper.component(chatId, id, dbName);
        if (projectComponent == null) {
            LOG.warn("[{}][project component:{} not found]", dbName, id);
            return EmptyResponse.empty();
        }

        Project project = ChatHelper.project(chatId, projectId, dbName);
        List<ProjectComponent> allComponents = ChatHelper.components(chatId, projectId, dbName);

        return _projectComponentService.convertToRest(projectComponent, allComponents, project, dbName);
    }

    /**
     * 获取当前节点及其子节点
     *
     * @param componentId     当前节点id
     * @param types           期望的节点类型。如果为空则不限制。如果不为空，当前节点或其子节点不符合要求则返回空list..
     */
    @GetMapping("children")
    public Object children(@RequestParam String componentId,
                           // 期望节点的类型
                           @RequestParam(required = false) List<String> types,
                           @RequestParam String chatId,
                           @RequestParam String publishedProjectId,
                           @RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName) throws Exception {
        // faq 由Broker启动 发布后的faq 如果user未启用，则返回null
        List<ProjectComponent> children = ChatHelper.children(componentId, chatId, dbName);

        // 添加自身节点
        children.add(ChatHelper.component(chatId, componentId, dbName));

        children = _afterChildrenLoaded(chatId, publishedProjectId, componentId, children, dbName);

        // 表示当前faq命中bot的user 未启用
        if (children == null) {
            return ProjectComponentUserPojo.NOT_ENABLE_RES;
        }

        if (CollectionUtils.isEmpty(children)) {
            return Collections.emptyList();
        }

        String projectId = children.get(0).getProjectId();

        Project project = ChatHelper.project(chatId, projectId, dbName);
        List<ProjectComponent> components = ChatHelper.components(chatId, projectId, dbName);

        if (CollectionUtils.isEmpty(types)) {
            return _projectComponentService.convertToRest(children, components, project, dbName);
        }

        Optional<ProjectComponent> notAllowedComponentOp = children.stream()
                .filter(p -> !types.contains(p.getType()))
                .findAny();

        return notAllowedComponentOp.isPresent() ? Collections.emptyList() :
                _projectComponentService.convertToRest(children, components, project, dbName);
    }

    /**
     * 这里做两件事
     * 1、 faq 节点下的bot 对应的user没有启用，则返回null
     * 2、如果只发布了faq，如果faq设置的有再次提示语，那么使用再次提示语造一个加的bot返回回去
     */
    private List<ProjectComponent> _afterChildrenLoaded(String chatId, String publishedProjectId,
                                                        String componentId, List<ProjectComponent> children,
                                                        String dbName) throws Exception {

        ProjectComponent currentComponent = ChatHelper.component(chatId, componentId ,dbName);
        if (currentComponent == null) {
            LOG.warn("[{}:{}][component can't find , componentId:{}]", dbName, publishedProjectId, componentId);
            return null;
        }

        // flow节点，无需处理
        if (ProjectComponent.TYPE_CONVERSATION.equals(currentComponent.getType())) {
            return children;
        }

        String projectId = currentComponent.getProjectId();

        List<ProjectComponent> allProjects = ChatHelper.components(chatId, projectId, dbName);
        ProjectComponent rootComponent = null;
        if (currentComponent.getRootComponentId() != null) {
            rootComponent = ChatHelper.component(chatId, currentComponent.getRootComponentId(), dbName);
            if (rootComponent == null) {
                rootComponent = _projectComponentService.findRootComponent(currentComponent, allProjects);

            }
        }

        if (rootComponent == null) {
            return null;
        }
        return children;
    }


    private boolean _userEnabled(ProjectComponent component) {
        RestProjectComponentUser user = new RestProjectComponentUser(component);
        return Boolean.TRUE.equals(user.getData().getEnable());
    }

    /**
     * 根据输入匹配user节点
     */
    private List<RestBaseProjectComponent> _filterUserOrConversations(String input, Boolean exact, List<RestBaseProjectComponent> components) {
        String inputNoBlackSpace = _removeBlackSpaceAnd2LowerCase(input);
        return components.stream()
                .filter(c -> c instanceof RestProjectComponentUser || c instanceof RestProjectComponentConversation)
                .filter(c -> {
                    if (c instanceof RestProjectComponentUser) {
                        RestProjectComponentUser user = (RestProjectComponentUser) c;
                        List<RestProjectComponentUser.Example> examples = user.getData().getExamples();
                        if (CollectionUtils.isEmpty(examples)) {
                            return false;
                        }

                        for (RestProjectComponentUser.Example example : examples) {
                            String text = _removeBlackSpaceAnd2LowerCase(example.getText());
                            // 精确匹配
                            if (exact) {
                                if (Objects.equals(inputNoBlackSpace, text)) {
                                    return true;
                                }

                                continue;
                            }

                            // 模糊匹配
                            if (ProjectComponentUserPojo.fuzzyMatch(inputNoBlackSpace, text)) {
                                return true;
                            }
                        }
                        return false;
                    }

                    if (c instanceof RestProjectComponentConversation) {
                        RestProjectComponentConversation conversation = (RestProjectComponentConversation) c;

                        String name = conversation.getData().getName();
                        if (StringUtils.isBlank(name)) {
                            return false;
                        }

                        name = _removeBlackSpaceAnd2LowerCase(name);
                        // 精确匹配
                        if (exact) {
                            if (Objects.equals(inputNoBlackSpace, name)) {
                                return true;
                            }
                            return false;
                        }

                        // 模糊匹配
                        if (ProjectComponentUserPojo.fuzzyMatch(inputNoBlackSpace, name)) {
                            return true;
                        }
                    }
                    return false;
                })
                .collect(Collectors.toList());
    }

    private String _removeBlackSpaceAnd2LowerCase(String text) {
        if (StringUtils.isBlank(text)) {
            return text;
        }

        return StringUtils.remove(text, " ").toLowerCase(Locale.ROOT);
    }


}
