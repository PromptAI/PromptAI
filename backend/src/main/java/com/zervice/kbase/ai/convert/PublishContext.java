package com.zervice.kbase.ai.convert;

import com.zervice.common.pojo.common.Account;
import com.zervice.kbase.AccountCatalog;
import com.zervice.kbase.api.restful.pojo.RestBaseProjectComponent;
import com.zervice.kbase.api.restful.pojo.RestProjectComponentEntity;
import com.zervice.kbase.api.restful.pojo.RestProjectComponentGpt;
import com.zervice.kbase.database.pojo.Project;
import com.zervice.kbase.database.pojo.ProjectComponent;
import com.zervice.kbase.database.pojo.PublishedProject;
import lombok.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author chenchen
 * @Date 2024/8/19
 */
@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PublishContext {

    private AccountCatalog _account;
    private Project _project;
    private PublishedProject _publishedProject;
    private List<ProjectComponent> _components;
    private List<String> _publishedRoots;
    private List<RestBaseProjectComponent> _roots;
    private Map<String, ProjectComponent> _comMap;

    /**
     * 需要在ensemble_agent注册llm gpt节点
     */
    @Builder.Default
    private List<RestProjectComponentGpt> _needRegisterGpts = new ArrayList<>();

    public static PublishContext init(AccountCatalog account, Project project,
                                      PublishedProject publishedProject,
                                      List<String> publishedRoots,
                                      List<RestBaseProjectComponent> roots,
                                      List<ProjectComponent> components) {
        // build component map
        Map<String, ProjectComponent> comMap = components.stream()
                .collect(Collectors.toMap(ProjectComponent::getId, v -> v));

        return PublishContext.builder()
                .account(account)
                .project(project)
                .publishedProject(publishedProject)
                .publishedRoots(publishedRoots)
                .roots(roots)
                .comMap(comMap)
                .components(components)
                .build();
    }

    /**
     * 满足以下情况不转换flow，同时需要在ensemble_agent注册
     * -  FlowAgent + 单个 GPT 时此 GPT 单独工作，
     * - Flow 为 llm 类型
     */
    public void needRegister(RestProjectComponentGpt gpt) {
        _needRegisterGpts.add(gpt);
    }

    public String projectName() {
        return _project.getName();
    }

    public String botName() {
        return publishedProjectId();
    }
    public String schedule() {
        return _project.getProperties().getSchedule();
    }

    public String publishedProjectId() {
        return _publishedProject.getId();
    }

    public String projectId() {
        return _project.getId();
    }

    public String dbName() {
        return _account.getDBName();
    }

    public String accountId() {
        return Account.getExternalId(_account.getId());
    }

    public String welcome() {
        return _project.getProperties().getWelcome();
    }

    public String fallback() {
        return _project.getProperties().getFallback();
    }

    public List<ProjectComponent> type(String type) {
        return _components.stream()
                .filter(c -> type.equals(c.getType()))
                .collect(Collectors.toList());
    }

    /**
     * 配置了动态变量的实体
     *
     * @return list of RestProjectComponentEntity
     */
    public List<RestProjectComponentEntity> dynamicSlots() {
      return _components.stream()
              .filter(e -> ProjectComponent.TYPE_ENTITY.equals(e.getType()))
                .map(e -> new RestProjectComponentEntity(e))
                .filter(e -> Boolean.TRUE.equals(e.getDefaultValueEnable())
                             && StringUtils.isNotBlank(e.getDefaultValue())
                             && StringUtils.isNotBlank(e.getDefaultValueType())
                )
                .collect(Collectors.toList());
    }

    /**
     * 将树结构变为线结构
     *
     * @param root tree root
     * @return children
     */
    public static List<RestBaseProjectComponent> flatChildren(RestBaseProjectComponent root) {
        List<RestBaseProjectComponent> children = new ArrayList<>();
        flatChildren(root, children);
        return children;
    }

    /**
     * 将树结构变为线结构
     *
     * @param root     tree root
     * @param children children container
     */
    private static void flatChildren(RestBaseProjectComponent root, List<RestBaseProjectComponent> children) {
        List<RestBaseProjectComponent> c = root.getChildren();
        if (CollectionUtils.isNotEmpty(c)) {
            for (RestBaseProjectComponent child : c) {
                children.add(child);
                flatChildren(child, children);
            }
        }
    }

    public List<ProjectComponent> get(List<String> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return List.of();
        }

        List<ProjectComponent> result = new ArrayList<>(ids.size());
        for (String id : ids) {
            if (_comMap.containsKey(id)) {
                result.add(_comMap.get(id));
            }
        }

        return result;
    }

    public ProjectComponent get(String id) {
        if (id == null) {
            return null;
        }

        return _comMap.get(id);
    }

    public List<ProjectComponent> children(String id) {
        if (StringUtils.isBlank(id)) {
            return List.of();
        }

        return _components.stream()
                .filter(c -> id.equals(c.parseParentId()))
                .collect(Collectors.toList());
    }
}

