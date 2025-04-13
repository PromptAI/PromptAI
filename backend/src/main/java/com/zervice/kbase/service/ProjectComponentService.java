package com.zervice.kbase.service;

import com.alibaba.fastjson.JSONObject;
import com.zervice.common.i18n.MessageUtils;
import com.zervice.common.restful.exception.RestException;
import com.zervice.common.restful.exception.StatusCodes;
import com.zervice.common.utils.Constants;
import com.zervice.kbase.ZBotRuntime;
import com.zervice.kbase.api.restful.pojo.*;
import com.zervice.kbase.api.restful.pojo.mica.Button;
import com.zervice.kbase.api.restful.pojo.mica.ConditionPojo;
import com.zervice.kbase.api.restful.pojo.mica.SlotPojo;
import com.zervice.kbase.database.criteria.ProjectComponentCriteria;
import com.zervice.kbase.database.dao.CommonBlobDao;
import com.zervice.kbase.database.dao.ProjectComponentDao;
import com.zervice.kbase.database.dao.ProjectDao;
import com.zervice.kbase.database.dao.PublishedProjectDao;
import com.zervice.kbase.database.pojo.CommonBlob;
import com.zervice.kbase.database.pojo.Project;
import com.zervice.kbase.database.pojo.ProjectComponent;
import com.zervice.kbase.database.pojo.PublishedProject;
import com.zervice.kbase.database.utils.DaoUtils;
import com.zervice.kbase.validator.ComponentValidatorHelper;
import lombok.Cleanup;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.sql.Connection;
import java.util.*;
import java.util.stream.Collectors;

/**
 * project component service
 */
@Log4j2
public class ProjectComponentService {

    private static ProjectComponentService _instance = null;

    public static ProjectComponentService getInstance() {
        if (_instance == null) {
            synchronized (ProjectComponentService.class) {
                if (_instance == null) {
                    _instance = new ProjectComponentService();
                }
            }
        }
        return _instance;
    }

    private ProjectComponentService() {
    }

    /**
     * 能处理的会话根类型
     */
    private Set<String> _canHandleableRootTypes = new HashSet<>();

    /**
     * 能处理会话的节点类型
     */
    private Set<String> _canHandleableSubTypes = new HashSet<>();

    {
        _canHandleableRootTypes.add(ProjectComponent.TYPE_CONVERSATION);
        _canHandleableSubTypes.add(ProjectComponent.TYPE_BOT);
    }

    public Set<ProjectComponent.Label> getLabels(String projectId, String componentType, String dbName) throws Exception {
        @Cleanup Connection conn = DaoUtils.getConnection();
        List<ProjectComponent> projectComponents = ProjectComponentDao.getByProjectIdAndType(conn, dbName, projectId, componentType);
        Set<ProjectComponent.Label> labels = new HashSet<>();
        projectComponents.forEach(item -> {
            labels.addAll(item.parseLabels());
        });
        return labels;
    }

    /**
     * 获取其子节点
     *
     * @param componentId 当前节点id
     * @param dbName      db
     * @return component list 如果返回null， 表示faq当前命中的bot 未开启使用
     * @throws Exception e
     */
    public List<ProjectComponent> loadChildren(String componentId, String dbName) throws Exception {
        @Cleanup Connection conn = DaoUtils.getConnection(true);
        ProjectComponent currentComponent = ProjectComponentDao.get(conn, dbName, componentId);
        if (currentComponent == null) {
            return null;
        }

       return loadChildren(currentComponent, conn, dbName);
    }

    /**
     * 返回结果不包含Root节点
     */
    public List<ProjectComponent> loadChildren(ProjectComponent rootComponent, Connection conn, String dbName) throws Exception {
        List<ProjectComponent> projectComponents = ProjectComponentDao.getByProjectId(conn, dbName, rootComponent.getProjectId());

        return loadChildren(rootComponent, projectComponents);
    }

    /**
     * 返回结果不包含Root节点
     */
    public List<ProjectComponent> loadChildren(ProjectComponent rootComponent, List<ProjectComponent> projectComponents) {
        List<ProjectComponent> children = new ArrayList<>();
        // 原来的递归会在数据量很大时效率严重下降
        // 这换成循环的方式： 这不停append child 节点，极大的减少了方法调用的stack
        Map<String, List<ProjectComponent>> parentChildMap = projectComponents.stream()
                .filter(p -> StringUtils.isNotBlank(p.parseParentId()))
                .collect(Collectors.groupingBy(ProjectComponent::parseParentId));

        // 用来记录parentId,
        // 这一直保存parentId，child查到后，将childId push 进去，直道没有child
        Queue<String> parentIdQueue = new LinkedList<>();
        parentIdQueue.add(rootComponent.getId());

        while (!parentIdQueue.isEmpty()) {
            String parentId = parentIdQueue.poll();
            if (StringUtils.isNotBlank(parentId)) {

                List<ProjectComponent> cd = parentChildMap.get(parentId);
                if (CollectionUtils.isNotEmpty(cd)) {
                    children.addAll(cd);

                    // 将children的id添加到queue里面
                    for (ProjectComponent c : cd) {
                        parentIdQueue.add(c.getId());
                    }
                }
            }
        }

        return children;
    }

    /**
     * 获取有效的节点：
     * - 流图中的节点
     * - FAQ节点
     * <p>
     * 即：出现在FAq列表和图中的节点，
     * - 不包含回收站、收藏中
     * - Webhook、global、entity、action（共享的节点，不在图中的）
     */
    public List<ProjectComponent> loadValidComponents(String projectId, Connection conn, String dbName) throws Exception {
        List<ProjectComponent> projectComponents = ProjectComponentDao.getByProjectId(conn, dbName, projectId);
        return loadValidComponents(projectComponents);
    }

    /**
     * 获取有效的节点：
     * - 流图中的节点
     * - FAQ节点
     * <p>
     * 即：出现在FAq列表和图中的节点，
     * - 不包含回收站、收藏中
     * - Webhook、global、entity、action（共享的节点，不在图中的）
     */
    public List<ProjectComponent> loadValidComponents(List<ProjectComponent> projectComponents) {
        if (CollectionUtils.isEmpty(projectComponents)) {
            return new ArrayList<>();
        }

        // 1、filter roots
        List<ProjectComponent> roots = projectComponents.stream()
                .filter(p -> ProjectComponent.TYPE_ROOTS.contains(p.getType()))
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(roots)) {
            return roots;
        }

        List<ProjectComponent> results = new ArrayList<>(projectComponents.size());

        // 2、find children
        for (ProjectComponent root : roots) {
            List<ProjectComponent> children = loadChildren(root, projectComponents);
            children.add(root);

            results.addAll(children);
        }


        return results;
    }


    /**
     * 判断是否能被broker驱动
     * 即：flow下只包含_canHandleableSubTypes的节点类型
     */
    private boolean _canDrive(List<ProjectComponent> children) {
        List<ProjectComponent> notAllowedComponents = children.stream()
                .filter(component -> !_canHandleableSubTypes.contains(component.getType()))
                .collect(Collectors.toList());

        return notAllowedComponents.isEmpty();
    }

    public RestBaseProjectComponent convertBodyRest(JSONObject data) {
        String type = data.getString("type");

        switch (type) {
            case RestProjectComponentWebhook.TYPE_NAME:
                return data.toJavaObject(RestProjectComponentWebhook.class);
            case RestProjectComponentUser.TYPE_NAME:
                return data.toJavaObject(RestProjectComponentUser.class);
            case RestProjectComponentConversation.TYPE_NAME:
                return data.toJavaObject(RestProjectComponentConversation.class);
            case RestProjectComponentBot.TYPE_NAME:
                return data.toJavaObject(RestProjectComponentBot.class);
            case RestProjectComponentGoto.TYPE_NAME:
                return data.toJavaObject(RestProjectComponentGoto.class);
            case RestProjectComponentEntity.TYPE_NAME:
                return data.toJavaObject(RestProjectComponentEntity.class);
            case RestProjectComponentGpt.TYPE_NAME:
                return data.toJavaObject(RestProjectComponentGpt.class);
            default:
                throw new RestException(StatusCodes.AiProjectComponentNotSupport, type);
        }
    }


    public ProjectComponent convertBodyToComponent(JSONObject data) {
        return convertBodyRest(data).toProjectComponent();
    }


    public List<? extends RestBaseProjectComponent> get(String dbName, String type, String projectId, ProjectComponentCriteria criteria) throws Exception {
        @Cleanup Connection conn = DaoUtils.getConnection(true);

        List<ProjectComponent> components = ProjectComponentDao.getByProjectIdAndType(conn, dbName, projectId, type);
        if (components.isEmpty() && !ProjectComponent.TYPE_ENTITY.equals(type)) {
            return Collections.emptyList();
        }
        switch (type) {
            case RestProjectComponentUser.TYPE_NAME:
                //list user component api 接口增加一个参数hasName
                //1.当hasName = null 时候，查询所有
                //2.当hasName = false 时候，查询所有不带name的“user component” （name情况：null， 空字符串 为“不带name”）
                //3.当hasName = true 时候，查询所有带name的“user component”
                components = components.stream()
                        .map(RestProjectComponentUser::new)
                        .filter(u -> {
                            if (criteria.getHasName() == null) {
                                return true;
                            }

                            if (criteria.getHasName()) {
                                return StringUtils.isNotBlank(u.getData().getName());
                            }

                            return StringUtils.isBlank(u.getData().getName());
                        })
                        .map(RestProjectComponentUser::toProjectComponent)
                        .collect(Collectors.toList());
                break;
            case RestProjectComponentWebhook.TYPE_NAME:
                // 过滤掉特殊的 webhook
                // - talk2bits
                components = components.stream()
                        .map(RestProjectComponentWebhook::new)
                        .filter(w -> {
                            if (RestProjectComponentWebhook.NORMAL_WEBHOOK.equals(criteria.getWebhookType())) {
                                return !w.blnTalk2bits();
                            }

                            if (RestProjectComponentWebhook.TALK2BITS_WEBHOOK.equals(criteria.getWebhookType())) {
                                return w.blnTalk2bits();
                            }

                            return true;
                        })
                        .map(RestProjectComponentWebhook::toProjectComponent)
                        .collect(Collectors.toList());
        }

        return convertToRest(components, projectId, conn, dbName);

    }

    /**
     * 查询当前project和系统内置的any
     */
    private List<RestProjectComponentEntity> _loadEntityInProjectAndInternal(List<ProjectComponent> components,
                                                                             Connection conn, String dbName) throws Exception {
        List<RestProjectComponentEntity> accountsEntities = components.stream()
                .map(c -> new RestProjectComponentEntity(c))
                .collect(Collectors.toList());


        // 去重
        Set<RestProjectComponentEntity> distinct = new HashSet<>();
        distinct.addAll(accountsEntities);

        return new ArrayList<>(distinct);
    }

    /**
     * TODO fix me:低效实现
     *
     * @param components
     * @param projectId
     * @param conn
     * @param dbName
     * @return
     */
    public List<RestBaseProjectComponent> convertToRest(List<ProjectComponent> components,
                                                        String projectId, Connection conn, String dbName) {
        if (CollectionUtils.isEmpty(components)) {
            return Collections.emptyList();
        }
        List<ProjectComponent> projectComponents = Collections.emptyList();
        try {
            projectComponents = getProjectComponentWithInternalEntity(projectId, conn, dbName);
        } catch (Exception e) {
            LOG.error(e);
        }

        Project project = null;
        try {
            project = ProjectDao.get(conn, dbName, projectId);
        } catch (Exception e) {
            LOG.error(e);
        }

        return convertToRest(components, projectComponents, project, dbName);
    }

    public List<RestBaseProjectComponent> convertToRest(List<ProjectComponent> components,
                                                        List<ProjectComponent> projectComponents,
                                                        Project project, String dbName) {
        if (CollectionUtils.isEmpty(components)) {
            return Collections.emptyList();
        }

        Convert2RestContext context = Convert2RestContext.build(projectComponents);
        List<RestBaseProjectComponent> rests = new ArrayList<>();
        for (ProjectComponent component : components) {
            rests.add(convertToRest(component, context, project, dbName));
        }

        return rests;
    }

    public RestBaseProjectComponent convertToRest(ProjectComponent component,
                                                  List<ProjectComponent> projectComponents,
                                                  Project project, String dbName) {
        Convert2RestContext context = Convert2RestContext.build(projectComponents);
        return convertToRest(component, context, project, dbName);
    }

    public RestBaseProjectComponent convertToRest(ProjectComponent component, Convert2RestContext context,
                                                  Project project, String dbName) {
        List<ProjectComponent> allProjectComponents = context.getNotTrashedComponents();

        Map<String, RestProjectComponentEntity> entities = context.getEntitiesMap(dbName);

        Map<String, RestProjectComponentWebhook> webhooks = context.webhooks();

        String type = component.getType();
        switch (type) {
            case RestProjectComponentWebhook.TYPE_NAME:
                return new RestProjectComponentWebhook(component);
            case RestProjectComponentUser.TYPE_NAME:
                RestProjectComponentUser user = new RestProjectComponentUser(component);
                _setSlots(user.getData().getSetSlots(), entities, dbName);
                setMappings(user.getData().getMappings(), user.getId(), entities, dbName);
                return user;
            case RestProjectComponentConversation.TYPE_NAME:
                RestProjectComponentConversation conversation = new RestProjectComponentConversation(component);
                _setConversation(conversation, project);
                return conversation;
            case RestProjectComponentBot.TYPE_NAME:
                RestProjectComponentBot bot = new RestProjectComponentBot(component);
                _setSlots(bot.getData().getSetSlots(), entities, dbName);
                _setConditions(bot.getData().getConditions(), entities, dbName);
                setWebhook(bot.getData().getResponses(), webhooks);
                return bot;
            case RestProjectComponentGoto.TYPE_NAME:
                return new RestProjectComponentGoto(component);
            case RestProjectComponentEntity.TYPE_NAME:
                RestProjectComponentEntity e = new RestProjectComponentEntity(component);
                _setEntityRelations(e, allProjectComponents, dbName);
                return e;
            case RestProjectComponentGpt.TYPE_NAME:
                return new RestProjectComponentGpt(component);
            default:
                throw new RestException(StatusCodes.AiProjectComponentNotSupport, type);
        }
    }

    /**
     * user 模板，设置引用的地方
     */
    private void _setEntityRelations(RestProjectComponentEntity entity, List<ProjectComponent> allProjectComponents, String dbName) {
        ProjectComponent.ComponentRelation relation = ProjectComponent.ComponentRelation.empty();
        try {
            // 展示名称
            String entityId = entity.getId();

            Map<String, ProjectComponent> componentMap = allProjectComponents.stream()
                    .collect(Collectors.toMap(ProjectComponent::getId, p -> p));

            // global、webhook节点不统计，统计flow中的，因为无法点击跳转..
            Set<String> canLinkEntityComponentTypes = Set.of(ProjectComponent.TYPE_USER, ProjectComponent.TYPE_BOT);

            // 查找引用的的user节点，转换成ComponentRelationInfo
            List<ProjectComponent.ComponentRelationInfo> linkedComponents = allProjectComponents.stream()
                    .filter(c -> canLinkEntityComponentTypes.contains(c.getType()))
                    .map(c -> {
                        String type = c.getType();

                        if (ProjectComponent.TYPE_USER.equals(type)) {
                            RestProjectComponentUser user = new RestProjectComponentUser(c);
                            if (user.entityInUse(entityId)) {
                                ProjectComponent rootComponent = findRootComponent(c, componentMap);
                                return ProjectComponent.ComponentRelationInfo.factory(rootComponent, c.getId(), c.parseName());
                            }

                            return null;
                        }

                        if (ProjectComponent.TYPE_BOT.equals(type)) {
                            RestProjectComponentBot bot = new RestProjectComponentBot(c);
                            if (bot.entityInUse(entityId)) {
                                ProjectComponent rootComponent = findRootComponent(c, componentMap);
                                return ProjectComponent.ComponentRelationInfo.factory(rootComponent, c.getId(), c.parseName());
                            }
                            return null;
                        }

                        return null;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            relation.setUsedByComponentRoots(linkedComponents);
            entity.setComponentRelation(relation);
        } catch (Exception e) {
            LOG.error("[{}][query entity:{} link fail:{}]", dbName, entity.getId(), e.getMessage(), e);
        }
    }

    private void _setConversation(RestProjectComponentConversation conversation, Project project) {
        try {
            String showSubNodesAsOptional = project.getProperties().getShowSubNodesAsOptional();

            // 是否能编辑
            boolean canEditorHidden = Project.Prop.SHOW_SUB_NODES_AS_OPTIONAL_CUSTOM.equals(showSubNodesAsOptional);
            // 如果可以编辑，则使用个conversation自己的内容
            // 如果不可以编辑，则根据状态来判断：all，都是启用，false 都是禁用;自定义时，试用conversation配置的
            Boolean hidden = canEditorHidden ? null : Project.Prop.SHOW_SUB_NODES_AS_OPTIONAL_ALL.equals(showSubNodesAsOptional);
            conversation.getData().setCanEditorHidden(canEditorHidden);

            if (hidden != null) {
                // 这里要取个反， true 是隐藏
                hidden = !hidden;
                conversation.getData().setHidden(hidden);
            }

        } catch (Exception ignored) {
        }
    }

    public void setWebhook(List<RestProjectComponentBot.BotResponse> responses,
                           Map<String, RestProjectComponentWebhook> webhooks) {
        if (CollectionUtils.isEmpty(responses)) {
            return;
        }

        for (RestProjectComponentBot.BotResponse response : responses) {
            boolean isWebhook = RestProjectComponentBot.BotResponse.TYPE_WEBHOOK.equals(response.getType());
            if (isWebhook) {
                String webhookId = response.getContent().getString("id");
                RestProjectComponentWebhook webhook = webhooks.get(webhookId);
                if (webhook != null) {
                    response.setContent(JSONObject.parseObject(JSONObject.toJSONString(webhook)));
                }
            }
        }
    }

    /**
     * 为mapping 设置slot name & display
     */
    public void setMappings(List<RestProjectComponentUser.Mapping> mappings,
                            String componentId,
                            Map<String, RestProjectComponentEntity> entityMap,
                            String dbName) {
        if (CollectionUtils.isEmpty(mappings)) {
            return;
        }

        try {
            Iterator<RestProjectComponentUser.Mapping> iterator = mappings.iterator();
            while (iterator.hasNext()) {
                RestProjectComponentUser.Mapping mapping = iterator.next();
                String entityId = mapping.getSlotId();
                if (StringUtils.isBlank(mapping.getSlotId()) || entityMap.get(entityId) == null) {
                    LOG.warn("[{}][invalid mapping found, slotId:{} in :{}]", dbName, entityId, componentId);
                    iterator.remove();
                    continue;
                }

                RestProjectComponentEntity entity = entityMap.get(mapping.getSlotId());
                String deleted = MessageUtils.get(Constants.I18N_DELETED);
                String name = entity != null ? entity.getName() : deleted;
                String disPlay = entity != null ? entity.getDisplay() : deleted;
                mapping.setSlotDisplay(disPlay);
                mapping.setSlotName(name);
            }

        } catch (Exception e) {
            LOG.error("[{}][set user mapping error:{}]", dbName, e.getMessage(), e);
        }
    }

    /**
     * 这里需要渲染ConditionPojo中的entity信息
     */
    private void _setConditions(List<ConditionPojo> conditionPojos,
                                Map<String, RestProjectComponentEntity> entityMap,
                                String dbName) {
        if (CollectionUtils.isEmpty(conditionPojos)) {
            return;
        }

        try {
            Iterator<ConditionPojo> iterator = conditionPojos.iterator();
            while (iterator.hasNext()) {
                ConditionPojo conditionPojo = iterator.next();
                String entityId = conditionPojo.getSlotId();

                if (StringUtils.isBlank(entityId) || entityMap.get(entityId) == null) {
                    iterator.remove();

                    LOG.warn("[{}][invalid condition found, slotId:{}]", dbName, entityId);
                    continue;
                }

                RestProjectComponentEntity entity = entityMap.get(entityId);

                String name = entity.getName();
                String display = entity.getDisplay();
                conditionPojo.setSlotName(name);
                conditionPojo.setSlotDisplay(display);
            }
        } catch (Exception e) {
            LOG.error("[{}][set condition error:{}]", dbName, e.getMessage(), e);
        }

    }

    /**
     * 这里需要渲染 SlotPojo 中的entity信息
     *
     * @param slotPojos
     * @param dbName
     */
    private void _setSlots(List<SlotPojo> slotPojos,
                           Map<String, RestProjectComponentEntity> entityMap,
                           String dbName) {
        if (CollectionUtils.isEmpty(slotPojos)) {
            return;
        }
        try {
            Iterator<SlotPojo> iterator = slotPojos.iterator();
            while (iterator.hasNext()) {
                SlotPojo slotPojo = iterator.next();
                String entityId = slotPojo.getSlotId();
                if (StringUtils.isBlank(entityId) || entityMap.get(entityId) == null) {
                    LOG.warn("[{}][invalid set slots found, slotId:{}]", dbName, entityId);

                    iterator.remove();
                    continue;
                }

                RestProjectComponentEntity entity = entityMap.get(slotPojo.getSlotId());
                String name = entity.getName();
                String display = entity.getDisplay();
                slotPojo.setSlotName(name);
                slotPojo.setSlotDisplay(display);
            }
        } catch (Exception e) {
            LOG.error("[{}][set slot error:{}]", dbName, e.getMessage(), e);
        }
    }

    public RestBaseProjectComponent convertToRest(ProjectComponent component, Connection conn, String dbName) {
        String projectId = component.getProjectId();
        List<ProjectComponent> allProjectComponents = Collections.emptyList();
        try {
            allProjectComponents = getProjectComponentWithInternalEntity(projectId, conn, dbName);
        } catch (Exception e) {
            LOG.error(e);
        }
        Project project = null;
        try {
            project = ProjectDao.get(conn, dbName, projectId);
        } catch (Exception e) {
            LOG.error(e);
        }

        Convert2RestContext context = Convert2RestContext.build(allProjectComponents);
        return convertToRest(component, context, project, dbName);
    }

    /**
     * 获取root节点
     */
    public ProjectComponent findRootComponent(ProjectComponent component,
                                              Connection conn, String dbName) throws Exception {
        if (ProjectComponent.TYPE_ROOTS.contains(component.getType())) {
            return component;
        }

        ProjectComponent rootComponent = null;

        // 尝试从rootComponent获取
        String rootComponentId = component.getRootComponentId();
        if (StringUtils.isNotBlank(rootComponentId)) {
            rootComponent = ProjectComponentDao.get(conn, dbName, component.getRootComponentId());
        }

        if (rootComponent != null) {
            return rootComponent;
        }

        List<ProjectComponent> allProjectComponent = ProjectComponentDao.getByProjectId(conn, dbName, component.getProjectId());
        return findRootComponent(component, allProjectComponent);

    }

    public ProjectComponent findRootComponent(ProjectComponent component, Map<String, ProjectComponent> componentMap) {
        String parentId = component.parseParentId();

        // 没有parent了，就是自己
        if (StringUtils.isBlank(parentId)) {
            return component;
        }

        ProjectComponent parent = componentMap.get(parentId);
        if (parent == null || component.getId().equals(parent.parseParentId())) {
            return component;
        }
        return findRootComponent(parent, componentMap);
    }

    public ProjectComponent findRootComponent(ProjectComponent component, List<ProjectComponent> components) {
        Map<String, ProjectComponent> componentMap = components.stream()
                .collect(Collectors.toMap(ProjectComponent::getId, p -> p));
        return findRootComponent(component, componentMap);
    }

    public void delete(String projectId, List<String> components, Connection conn, String dbName) throws Exception {
        // 需要删除的节点id
        Set<String> ids = new HashSet<>();
        List<ProjectComponent> projectComponents = ProjectComponentDao.getByProjectId(conn, dbName, projectId);

        // 需要交验发布项目，如果flow处在发布中，禁止删除
        PublishedProject publishedProject = PublishedProjectDao.get(conn, dbName, PublishedProject.generateId(dbName, projectId));

        // 发布/部署中
        if (publishedProject != null && PublishedProject.VALID_STATUS.contains(publishedProject.getStatus()) && CollectionUtils.isNotEmpty(publishedProject.getProperties().getComponentIds())) {

            List<String> publishedProjectComponents = publishedProject.getProperties().getComponentIds();

            // 已发布的
            List<String> usingComponentIds = new ArrayList<>(publishedProjectComponents);

            // 发布中
            if (PublishedProject.STATUS_DEPLOYING.equals(publishedProject.getStatus())) {
                usingComponentIds.addAll(publishedProject.getProperties().getPublishingIds());
            }

            if (CollectionUtils.isNotEmpty(usingComponentIds)) {
                for (String deletedId : components) {
                    if (usingComponentIds.contains(deletedId)) {
                        LOG.info("[{}][can't delete component:{} in publishing from project:{}]", dbName, deletedId, projectId);
                        throw new RestException(StatusCodes.AiProjectComponentNotAllowDeleteInPublish);
                    }
                }
            }
        }

        Set<String> rootIds = new HashSet<>();
        for (String id : components) {
            ProjectComponent component = ProjectComponentDao.get(conn, dbName, id);
         
            // 这里可能是系统用户删除系统变量，projectId会不一样
            if (component == null ||
                    ((!component.getProjectId().equals(projectId)))
                            && !ProjectComponent.TYPE_ENTITY.equals(component.getType())) {
                LOG.warn("[{}][delete project:{} component:{} fail, not found]", dbName, projectId, id);
                throw new RestException(StatusCodes.AiProjectComponentNotFound);
            }
            //如果删除的是conversation，需要将回收站的组件id加进来
            if (ProjectComponent.TYPE_ROOTS.contains(component.getType())) {

                List<String> trashComponentIds = getRootComponentTrashComponents(projectComponents, component.getId())
                        .stream()
                        .map(ProjectComponent::getId)
                        .collect(Collectors.toList());
                ids.addAll(trashComponentIds);
            }
            ids.add(id);
            ProjectComponent rootComponent = findRootComponent(component, projectComponents);
            rootIds.add(rootComponent.getId());
        }
        Map<String, ProjectComponent> projectComponentMap = projectComponents.stream()
                .collect(Collectors.toMap(ProjectComponent::getId, m -> m));

        _filterChildrenIds(ids, projectComponents);

        _beforeDelete(projectComponentMap, ids, projectId, conn, dbName);

        ProjectComponentDao.deleteByIds(conn, dbName, ids);

        updateValidateInfo(projectId, conn, dbName);
    }

    private void _filterChildrenIds(Set<String> ids, List<ProjectComponent> all) {
        List<ProjectComponent> children = ProjectComponent.queryChildren(new ArrayList<>(ids), all);
        Set<String> childrenIds = children.stream().map(ProjectComponent::getId).collect(Collectors.toSet());
        ids.addAll(childrenIds);
    }

    /**
     * 节点可能有关联的文件
     */
    private void _deleteRelatedFile(ProjectComponent component, Connection conn, String dbName) {
        String fileId = component.parseFileId();
        if (StringUtils.isBlank(fileId)) {
            return;
        }

        try {
            CommonBlobDao.delete(conn, dbName, CommonBlob.idFromExternalId(fileId));
        } catch (Exception e) {
            LOG.error("[{}][delete component:{} related file:{} fail:{} from ]", dbName, component.getId(), fileId, e.getMessage(), e);
        }
    }

    private void _beforeDelete(Map<String, ProjectComponent> projectComponentMap, Set<String> ids, String projectId,
                               Connection conn, String dbName) throws Exception {
        List<ProjectComponent> validaComponents = loadValidComponents(new ArrayList<>(projectComponentMap.values()));

        // 如果被删除的节点被"意图模板" -ProjectComponent.TYPE_USER_GLOBAL 引用，那么移除这个引用
        _onDeleteEntity(ids, projectId, projectComponentMap, validaComponents, dbName);
        _onDeleteWebhook(ids, projectId, projectComponentMap, validaComponents, conn, dbName);
        _onDeleteFallbackButton(ids, projectId, conn, dbName);
    }

    /**
     * fallback 按钮使用中
     */
    private void _onDeleteFallbackButton(Set<String> ids, String projectId,
                                         Connection conn, String dbName) throws Exception {
        Project project = ProjectDao.get(conn, dbName, projectId);
        if (CollectionUtils.isNotEmpty(project.getProperties().getFallbackButtons())) {
            List<Button> fallbackButton = project.getProperties().getFallbackButtons();
            for (Button button : fallbackButton) {
                if (ids.contains(button.getId())) {
                    throw new RestException(StatusCodes.AiProjectComponentInUse);
                }
            }
        }
    }

    private void _onDeleteWebhook(Set<String> ids, String projectId,
                                  Map<String, ProjectComponent> projectComponentMap,
                                  List<ProjectComponent> validComponents,
                                  Connection conn, String dbName) throws Exception {

        List<ProjectComponent> bots = validComponents.stream()
                .filter(p -> ProjectComponent.TYPE_BOT.equals(p.getType()))
                .collect(Collectors.toList());

        // project 中可能也有webhook
        Project project = ProjectDao.get(conn, dbName, projectId);

        List<RestProjectComponentBot.BotResponse> webhooksInProject = project.getProperties().getWebhooks();

        for (String componentId : ids) {
            ProjectComponent c = projectComponentMap.get(componentId);
            if (c == null || !ProjectComponent.TYPE_WEBHOOK.equals(c.getType())) {
                continue;
            }

            // 动态查询是否有bot使用
            if (bots.isEmpty()) {
                continue;
            }

            Optional<ProjectComponent> used = bots.stream()
                    .filter(b -> {
                        RestProjectComponentBot bot = new RestProjectComponentBot(b);

                        List<RestProjectComponentBot.BotResponse> responses = bot.getData().getResponses();
                        if (CollectionUtils.isEmpty(responses)) {
                            return false;
                        }

                        for (RestProjectComponentBot.BotResponse r : responses) {
                            if (RestProjectComponentBot.BotResponse.TYPE_WEBHOOK.equals(r.getType())) {
                                if (componentId.equals(r.getId())) {
                                    return true;
                                }
                            }
                        }

                        return false;
                    })
                    .findAny();

            if (used.isPresent()) {
                LOG.warn("[{}][can't delete :{} id:{} from project:{}, current component used by others:{}]",
                        dbName, c.getType(), c.getId(), projectId, used.get().getId());
                throw new RestException(StatusCodes.AiProjectComponentInUse);
            }

            // project fallback 使用到了
            if (CollectionUtils.isNotEmpty(webhooksInProject)) {
                for (RestProjectComponentBot.BotResponse webhook : webhooksInProject) {
                    String webhookId = webhook.getContent().getString("id");
                    if (componentId.equals(webhookId)) {
                        LOG.warn("[{}][can't delete :{} id:{} from project:{}, current component used by project fallbacks]",
                                dbName, c.getType(), c.getId(), projectId);
                        throw new RestException(StatusCodes.AiProjectComponentInUse);
                    }
                }
            }
        }
    }

    /**
     * project全局变量被引用时不能被删除
     */
    private void _onDeleteEntity(Set<String> ids, String projectId,
                                 Map<String, ProjectComponent> projectComponentMap,
                                 List<ProjectComponent> validComponents, String dbName) throws Exception {
        for (String componentId : ids) {
            ProjectComponent entity = projectComponentMap.get(componentId);
            if (entity == null || !ProjectComponent.TYPE_ENTITY.equals(entity.getType())) {
                continue;
            }

            RestProjectComponentEntity restEntity = new RestProjectComponentEntity(entity);
            boolean inUse = _entityInUse(componentId, validComponents);
            if (inUse) {
                LOG.warn("[{}][can't delete :{} id:{} from project:{}, current component used by others]",
                        dbName, entity.getType(), entity.getId(), projectId);
                throw new RestException(StatusCodes.AiProjectComponentInUse);
            }
        }
    }

    /**
     * 搜索系统变量是否使用中
     *
     * @param entityId 变量id
     */
    private boolean _entityInUse(String entityId, List<ProjectComponent> validComponents) {
        List<String> types = Arrays.asList(ProjectComponent.TYPE_USER, ProjectComponent.TYPE_BOT);

        for (String type : types) {

            //收集引用变量id
            List<String> slotIds = new ArrayList<>();
            //收集重置变量id
            List<String> conditionIds = new ArrayList<>();
            switch (type) {
                case ProjectComponent.TYPE_USER:
                    validComponents.stream()
                            .map(RestProjectComponentUser::new)
                            .forEach(item -> {
                                //user节点重置变量引用的slotId
                                List<SlotPojo> slotPojos = item.getData().getSetSlots();
                                if (CollectionUtils.isNotEmpty(slotPojos)) {

                                    List<String> slotPojosIds = slotPojos.stream()
                                            .map(SlotPojo::getSlotId)
                                            .collect(Collectors.toList());

                                    slotIds.addAll(slotPojosIds);
                                }
                                //user节点引用的slotId
                                List<RestProjectComponentUser.Mapping> mappings = item.getData().getMappings();
                                if (CollectionUtils.isNotEmpty(mappings)) {
                                    List<String> mappingIds = mappings.stream()
                                            .map(RestProjectComponentUser.Mapping::getSlotId)
                                            .collect(Collectors.toList());
                                    slotIds.addAll(mappingIds);
                                }
                            });
                    break;
                case ProjectComponent.TYPE_BOT:
                    validComponents.stream()
                            .map(RestProjectComponentBot::new)
                            .forEach(item -> {
                                //bot节点重置变量引用的slotId
                                List<SlotPojo> slotPojos = item.getData().getSetSlots();
                                if (CollectionUtils.isNotEmpty(slotPojos)) {
                                    List<String> slotPojosIds = slotPojos.stream()
                                            .map(SlotPojo::getSlotId)
                                            .collect(Collectors.toList());

                                    slotIds.addAll(slotPojosIds);
                                }

                                // entityReplies
                                if (CollectionUtils.isNotEmpty(item.getData().getEntityReplies())) {
                                    List<String> entityIds = item.getData().getEntityReplies().stream()
                                            .flatMap(e -> e.getEntities().keySet().stream())
                                            .collect(Collectors.toList());
                                    slotIds.addAll(entityIds);
                                }

                                //bot节点条件引用的slotId
                                List<ConditionPojo> conditionPojos = item.getData().getConditions();
                                if (CollectionUtils.isNotEmpty(conditionPojos)) {
                                    List<String> conditionPojoIds = conditionPojos.stream()
                                            .map(ConditionPojo::getSlotId)
                                            .collect(Collectors.toList());
                                    conditionIds.addAll(conditionPojoIds);
                                }
                            });
                    break;
            }
            if (slotIds.contains(entityId) || conditionIds.contains(entityId)) {
                return true;
            }

        }
        return false;
    }

    public void updateValidateInfo( String projectId, Connection conn, String dbName) throws Exception {
        ComponentValidatorHelper.validate(projectId, conn, dbName);
    }

    public ProjectComponent save(ProjectComponent component, boolean needRunAfterSaved, Connection conn, String dbName) throws Exception {
        ProjectComponent dbComponent = ProjectComponentDao.get(conn, dbName, component.getId());
        if (dbComponent != null) {
            throw new RestException(StatusCodes.AiProjectComponentIdExists, "id exist");
        }

        _beforeSave(component, conn, dbName);

        String parentId = component.parseParentId();
        if (StringUtils.isNotBlank(parentId)) {
            ProjectComponent parent = ProjectComponentDao.get(conn, dbName, parentId);
            if (parent == null) {
                LOG.error("[{}][save component:{} failed, parent:{} not found]", dbName, component.getId(), parentId);
                throw new RestException(StatusCodes.AiProjectComponentInvalidParent);
            }

            // 分配新的节点编号并保存
            Integer no = parent.generateNewChildNo();
            ProjectComponentDao.updateProp(conn, dbName, parent);

            // 保存新的编号
            component.getProperties().setNo(no);
        }

        ProjectComponentDao.add(conn, dbName, component);
        if (needRunAfterSaved) {
            _afterSaved(component, conn, dbName);
        }
        // 在执行 after save后，component数据可能不是最新的，这里从数据库load 一次
        return ProjectComponentDao.get(conn, dbName, component.getId());
    }

    private void _setRootComponentId(ProjectComponent component, Connection conn, String dbName) throws Exception {
        String parentId = component.parseParentId();
        if (StringUtils.isBlank(parentId)) {
            return;
        }

        ProjectComponent rootComponent = findRootComponent(component, conn, dbName);

        String rootComponentId = rootComponent == null ? null : rootComponent.getId();
        component.setRootComponentId(rootComponentId);
    }

    private void _beforeSave(ProjectComponent component, Connection conn, String dbName) throws Exception {
        _checkComponent(component, conn, dbName);

        _setRootComponentId(component, conn, dbName);
    }

    /**
     * 系统会内置一些entity, 我们规定使用特殊的前缀去分别
     * - 在a1下
     * - 使用规定的前缀
     * - 其他账号无权使用这个前缀
     *
     * @param name   Any 类型的名称
     * @param dbName account db
     */
    private void _checkEntityComponentName(String name, String dbName) {
        // 无权使用
        if (RestProjectComponentEntity.isInternalEntity(name) && !ZBotRuntime.DEFAULT_EXTERNAL_ACCOUNT_ID.equals(dbName)) {
            throw new RestException(StatusCodes.AiProjectComponentNotPermitName, RestProjectComponentEntity.INTERNAL_ENTITY_PREFIX);
        }
    }

    public Object update(ProjectComponent component, Connection conn, String dbName) throws Exception {
        ProjectComponent dbComponent = ProjectComponentDao.get(conn, dbName, component.getId());
        if (dbComponent == null) {
            LOG.error("[{}][component id not exists. projectId:{}, componentId:{}]",
                    dbName, component.getProjectId(), component.getId());
            throw new RestException(StatusCodes.AiProjectComponentNotFound, "not found");
        }
        dbComponent.setType(component.getType());
        dbComponent.getProperties().setData(component.getProperties().getData());
        dbComponent.getProperties().setUpdateBy(component.getProperties().getUpdateBy());
        dbComponent.getProperties().setUpdateTime(System.currentTimeMillis());

        // parent 变化了变化了，那么从新的parent里面申请一个新的no
        String parentId = component.parseParentId();
        if (StringUtils.isNotBlank(parentId) && !Objects.equals(parentId, dbComponent.parseParentId())) {
            ProjectComponent newParent = ProjectComponentDao.get(conn, dbName, parentId);
            if (newParent == null) {
                LOG.error("[{}][update component:{} failed, new parent:{} not found]", dbName, component.getId(), parentId);
                throw new RestException(StatusCodes.AiProjectComponentInvalidParent);
            }

            // 分配新的节点编号并保存
            Integer no = newParent.generateNewChildNo();
            ProjectComponentDao.updateProp(conn, dbName, newParent);

            // 保存新的编号
            dbComponent.getProperties().setNo(no);
        }

        _preUpdate(dbComponent, conn, dbName);
        ProjectComponentDao.update(conn, dbName, dbComponent);
        _afterUpdated(dbComponent, conn, dbName);
        return dbComponent;
    }


    private void _afterSaved(ProjectComponent component, Connection conn, String dbName) throws Exception {
        // add a default bot after conversation created
        if (ProjectComponent.TYPE_CONVERSATION.equals(component.getType())) {
            RestProjectComponentConversation conversation = new RestProjectComponentConversation(component);
            // llm Agent, build a gpt node
            if (RestProjectComponentConversation.Data.TYPE_LLM_AGENT.equals(conversation.getData().getType())) {
                String name = conversation.getData().getName();
                String description = conversation.getData().getDescription();
                String prompt = conversation.getData().getPrompt();

                String projectId = component.getProjectId();
                String rootComponentId = component.getId();
                RestProjectComponentGpt gpt = RestProjectComponentGpt.newInstance(projectId, rootComponentId, rootComponentId,
                        name, prompt, description, List.of(), List.of());
                ProjectComponentDao.add(conn, dbName, gpt.toProjectComponent());
            }
        }

        // 设置默认的引导语
        _serDefaultWelcome4Conversation(component, conn, dbName);
    }

    private void _preUpdate(ProjectComponent projectComponent, Connection conn, String dbName) throws Exception {
        _checkComponent(projectComponent, conn, dbName);

        _setRootComponentId(projectComponent, conn, dbName);
    }

    /**
     * 为会话设置默认的引导语： 会话名称+引导语
     */
    private void _serDefaultWelcome4Conversation(ProjectComponent component, Connection conn, String dbName) throws Exception {
        RestProjectComponentConversation conversation = null;
        if (ProjectComponent.TYPE_CONVERSATION.equals(component.getType())) {
            conversation = new RestProjectComponentConversation(component);
        } else {
            ProjectComponent root = findRootComponent(component, conn, dbName);

            // 没找到或root不是conversation
            if (root == null || !ProjectComponent.TYPE_CONVERSATION.equals(root.getType())) {
                return;
            }

            conversation = new RestProjectComponentConversation(root);
        }

        // 设置了引导语
        String welcome = conversation.getData().getWelcome();
        if (StringUtils.isNotBlank(welcome)) {
            return;
        }

        // 没有子节点，先不验证
        List<ProjectComponent> children = loadChildren(conversation.toProjectComponent(), conn, dbName);
        if (CollectionUtils.isEmpty(children)) {
            return;
        }

        // 要么是一个bot，要么是一个或多个user； 只有在user的情况下才要求设置引导语
        ProjectComponent firstChild = children.get(0);

        // bot 无需设置
        if (ProjectComponent.TYPE_BOT.equals(firstChild.getType())) {
            return;
        }

        // 需要设置引导语
        Project project = ProjectDao.get(conn, dbName, conversation.getProjectId());
        welcome = conversation.getData().getName() + MessageUtils.get(Constants.I18N_DEFAULT_CONVERSATION_WELCOME_SUFFIX, project.getProperties().getLocale());
        conversation.getData().setWelcome(welcome);

        // 更新到prop
        ProjectComponentDao.update(conn, dbName, conversation.toProjectComponent());

        LOG.info("[{}][set default welcome 4 conversation:{}]", dbName, conversation.getId());
    }
    private void _checkComponent(ProjectComponent projectComponent, Connection conn, String dbName) throws Exception {
        String projectId = projectComponent.getProjectId();
        String type = projectComponent.getType();

        // 不允许重名、普通account不能编辑内置变量
        if (ProjectComponent.TYPE_ENTITY.equals(type)) {
            _checkEntity(projectComponent, conn, dbName);
            return;
        }

        //普通user的example不能重
        if (ProjectComponent.TYPE_USER.equals(type)) {
            RestProjectComponentUser userComponent = new RestProjectComponentUser(projectComponent);

            if (!RestProjectComponentUser.validateMark(userComponent.getData().getExamples())) {
                LOG.error("[{}][user component mark invalid. componentId:{},projectId:{},properties.data:{}]",
                        dbName, userComponent.getId(), projectId, projectComponent.getProperties().getData().toJSONString());
                throw new RestException(StatusCodes.AiProjectComponentBadRequest);
            }
            return;
        }

        // 名称不能重复
        if (ProjectComponent.TYPE_WEBHOOK.equals(type)) {
            RestProjectComponentWebhook webhook = new RestProjectComponentWebhook(projectComponent);
            List<RestProjectComponentWebhook> webhooks = ProjectComponentDao.getByProjectIdAndType(conn, dbName,
                            projectComponent.getProjectId(), ProjectComponent.TYPE_WEBHOOK).stream()
                    .map(RestProjectComponentWebhook::new)
                    .collect(Collectors.toList());
            String text = webhook.getText();

            for (RestProjectComponentWebhook w : webhooks) {
                if (text.equals(w.getText()) && !projectComponent.getId().equals(w.getId())) {
                    throw new RestException(StatusCodes.AiProjectComponentWebhookExists, text);
                }
            }
            return;
        }

        // 根节点名不能互相重复
        if (ProjectComponent.TYPE_ROOTS.contains(type)) {
            _checkRootComponentName(projectComponent, conn, dbName);
            return;
        }
    }


    public void checkRootComponentId(String rootComponentId, Connection conn, String dbName) throws Exception {
        ProjectComponent root = ProjectComponentDao.get(conn, dbName, rootComponentId);
        if (root == null || !ProjectComponent.TYPE_ROOTS.contains(root.getType())) {
            LOG.error("[{}][invalid rootComponentId:{}]", dbName, rootComponentId);
            throw new RestException(StatusCodes.AiProjectComponentInvalidRootComponent);
        }
    }

    public void checkProjectId(String projectId, Connection conn, String dbName) throws Exception {
        Project project = ProjectDao.get(conn, dbName, projectId);
        if (project == null) {
            LOG.error("[{}][invalid projectId:{}]", dbName, projectId);
            throw new RestException(StatusCodes.AiProjectNotFound);
        }
    }


    private void _checkRootComponentName(ProjectComponent projectComponent, Connection conn, String dbName) throws Exception {
        List<ProjectComponent> roots = ProjectComponentDao.getByProjectIdAndTypes(conn, dbName,
                projectComponent.getProjectId(), ProjectComponent.TYPE_ROOTS);
        roots.forEach(item -> {
            String componentName = projectComponent.parseName();
            if (item.parseName().equals(componentName)
                    && !item.getId().equals(projectComponent.getId())) {
                LOG.error("[{}][{} component name repeated. componentId:{} ,componentName:{}]", dbName, projectComponent.getType(),
                        projectComponent.getId(), componentName);
                throw new RestException(StatusCodes.AiProjectComponentNameExists, componentName);
            }
        });
    }

    /**
     * 新增 / 更新时检查entity
     * <p>
     * 1、项目下名称不能重复
     * 2、普通用户不能操作系统变量
     */
    private void _checkEntity(ProjectComponent component, Connection conn, String dbName) throws Exception {
        String projectId = component.getProjectId();
        RestProjectComponentEntity rest = new RestProjectComponentEntity(component);
        String name = rest.getName();
        if (StringUtils.isBlank(name)) {
            throw new RestException(StatusCodes.AiProjectComponentNameEmpty);
        }

        // query project entity
        List<ProjectComponent> projectEntities = ProjectComponentDao.getByProjectIdAndType(conn, dbName,
                projectId, ProjectComponent.TYPE_ENTITY);

        List<RestProjectComponentEntity> entities = _loadEntityInProjectAndInternal(projectEntities, conn, dbName);

        List<RestProjectComponentEntity> existsEntities = entities.stream()
                .filter(item -> {
                    //新增
                    boolean blnAdd = StringUtils.isBlank(rest.getId()) && name.equals(item.getName());
                    //更新
                    boolean blnUpdate = name.equals(item.getName()) && !rest.getId().equals(item.getId());
                    return blnAdd || blnUpdate;
                })
                .collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(existsEntities)) {
            throw new RestException(StatusCodes.AiProjectComponentEntityExists, rest.getName());
        }
    }

    private void _afterUpdated(ProjectComponent projectComponent, Connection conn, String dbName) throws Exception {
        // llm flow的名称与 GPT 名称一致
        if (ProjectComponent.TYPE_GPT.equals(projectComponent.getType())) {
            String root = projectComponent.getRootComponentId();
            if (StringUtils.isNotBlank(root)) {
                ProjectComponent conversation = ProjectComponentDao.get(conn, dbName, root);
                if (conversation != null) {
                    RestProjectComponentConversation rest = new RestProjectComponentConversation(conversation);
                    // llm 的 flow，那么更新他的名称与 GPT 一致
                    if (RestProjectComponentConversation.Data.TYPE_LLM_AGENT.equals(rest.getData().getType())) {
                        String name = projectComponent.parseName();
                        rest.getData().setName(name);

                        ProjectComponentDao.update(conn, dbName, rest.toProjectComponent());
                    }
                }
            }
        }

        // 设置默认的引导语
        _serDefaultWelcome4Conversation(projectComponent, conn, dbName);
    }


    /**
     * 获取流图下回收站节点的根节点
     * 目前是把节点的parentId置空来实现回收站功能的
     * 通过parentId、rootComponentId、TYPE_CAN_TRASH来查找
     *
     * @param all             项目下的所有节点
     * @param rootComponentId 流图的根节点
     * @return
     */
    public List<ProjectComponent> getRootComponentTrashComponents(List<ProjectComponent> all, String rootComponentId) {
        return all.stream()
                .filter(c -> StringUtils.isBlank(c.parseParentId())
                        && ProjectComponent.TYPE_CAN_TRASH.contains(c.getType())
                        && rootComponentId.equals(c.getRootComponentId()))
                .collect(Collectors.toList());
    }

    /**
     * 获取项目下回收站节点的根节点
     * 目前是把节点的parentId置空来实现回收站功能的
     * 通过parentId、TYPE_CAN_TRASH来查找
     *
     * @param all 项目下的所有节点
     */
    private List<ProjectComponent> _getTrashRootComponents(List<ProjectComponent> all) {
        return all.stream()
                .filter(c -> StringUtils.isBlank(c.parseParentId())
                        && ProjectComponent.TYPE_CAN_TRASH.contains(c.getType()))
                .collect(Collectors.toList());
    }

    /**
     * 按照父->子节点排序
     * root ：
     * - 第一个节点
     * - condition节点
     * - 及第一个节点是condition节点
     */
    public List<ProjectComponent> sortByParentToChild(List<ProjectComponent> components) {
        List<ProjectComponent> sortedComponents = new ArrayList<>();


        if (components.size() != sortedComponents.size()) {
            throw new RestException(StatusCodes.AiProjectComponentNotCompleteTree);
        }
        return sortedComponents;
    }


    /**
     * 获取项目下回收站所有节点
     * 1、load trash root
     * 2、load trash children
     */
    public List<String> loadProjectTrashedComponentId(List<ProjectComponent> all) {
        List<ProjectComponent> trashedComponents = _getTrashRootComponents(all);
        List<ProjectComponent> allTrashedComponents = new ArrayList<>();
        for (ProjectComponent trashed : trashedComponents) {
            List<ProjectComponent> trashedChildren = loadTrashedComponentChildren(all, trashed);
            allTrashedComponents.addAll(trashedChildren);
        }
        allTrashedComponents.addAll(trashedComponents);
        return allTrashedComponents.stream()
                .map(ProjectComponent::getId)
                .collect(Collectors.toList());
    }

    /**
     * 筛选正常的节点
     */
    public List<ProjectComponent> notTrashedComponents(List<ProjectComponent> projectComponents) {
        List<String> trashedComponentIds = loadProjectTrashedComponentId(projectComponents);
        if (CollectionUtils.isEmpty(trashedComponentIds)) {
            return projectComponents;
        }

        //项目下所有不在回收站的节点
        return projectComponents.stream()
                .filter(item -> !trashedComponentIds.contains(item.getId()))
                .collect(Collectors.toList());
    }


    /**
     * 获取回收站节点的子节
     *
     * @param all     项目下的所有节点
     * @param trashed 回收站节点
     */
    public List<ProjectComponent> loadTrashedComponentChildren(List<ProjectComponent> all, ProjectComponent trashed) {
        return loadChildren(trashed, all);
    }

    /**
     * 查询参与项目的节点
     * 1、查询当前项目下的节点
     * 2、查询系统内置节点 - 目前有内置的Entity
     * <p>
     * 其中，包含在回收站中的节点
     */
    public List<ProjectComponent> getProjectComponentWithInternalEntity(String projectId,
                                                                        Connection conn, String dbName) throws Exception {
        // 1、query project components
        List<ProjectComponent> projectComponents = ProjectComponentDao.getByProjectId(conn, dbName, projectId);

        // 去重 -
        projectComponents = projectComponents.stream()
                .distinct()
                .collect(Collectors.toList());

        return projectComponents.stream()
                .distinct()
                .collect(Collectors.toList());
    }


    public static class Convert2RestContextFactory {
        private static ProjectComponentService _componentService = ProjectComponentService.getInstance();


        /**
         * 构造上下文
         */
        public static Convert2RestContext factory(String projectId, Connection conn, String dbName) throws Exception {
            List<ProjectComponent> projectComponents = _componentService.getProjectComponentWithInternalEntity(projectId, conn, dbName);
            return Convert2RestContext.build(projectComponents);
        }

    }

    /**
     * 将需转换的数据缓存起来，
     */
    public static class Convert2RestContext {

        private static ProjectComponentService _componentService = ProjectComponentService.getInstance();

        private Convert2RestContext() {
        }

        public static Convert2RestContext build(List<ProjectComponent> projectComponents) {
            Convert2RestContext context = new Convert2RestContext();
            context.setAllComponents(projectComponents);

            List<ProjectComponent> notTrashedComponents = _componentService.notTrashedComponents(projectComponents);
            context.setNotTrashedComponents(notTrashedComponents);
            return context;
        }

        /**
         * 所有项目用到的节点
         */
        @Setter
        @Getter
        private List<ProjectComponent> _allComponents;

        /**
         * 不包含在回收站的节点
         */
        @Setter
        @Getter
        private List<ProjectComponent> _notTrashedComponents;

        private List<ProjectComponent> _entities;

        private Map<String, RestProjectComponentWebhook> _webhookMap;

        private Map<String, ProjectComponent> _notTrashedProjectComponentMap;

        private Map<String, RestProjectComponentEntity> _entitiesMap;

        public Map<String, RestProjectComponentEntity> getEntitiesMap(String dbName) {
            if (_entitiesMap == null) {
                synchronized (this) {
                    if (_entitiesMap == null) {
                        _entitiesMap = getEntities().stream()
                                .map(c -> new RestProjectComponentEntity(c))
                                .collect(Collectors.toMap(RestProjectComponentEntity::getId, a -> a));
                    }
                }
            }

            return _entitiesMap;
        }

        public Map<String, ProjectComponent> getNotTrashedProjectComponents() {
            if (_notTrashedProjectComponentMap == null) {
                synchronized (this) {
                    if (_notTrashedProjectComponentMap == null) {
                        _notTrashedProjectComponentMap = _allComponents.stream()
                                .collect(Collectors.toMap(ProjectComponent::getId, p -> p));
                    }
                }
            }
            return _notTrashedProjectComponentMap;
        }

        public List<ProjectComponent> getEntities() {
            if (_entities == null) {
                synchronized (this) {
                    if (_entities == null) {
                        _entities = _allComponents.stream()
                                .filter(c -> ProjectComponent.TYPE_ENTITY.equals(c.getType()))
                                .collect(Collectors.toList());
                    }
                }
            }

            return _entities;
        }

        public Map<String, RestProjectComponentWebhook> webhooks() {
            if (_webhookMap == null) {
                synchronized (this) {
                    if (_webhookMap == null) {
                        _webhookMap = _allComponents.stream()
                                .filter(c -> ProjectComponent.TYPE_WEBHOOK.equals(c.getType()))
                                .map(RestProjectComponentWebhook::new)
                                .collect(Collectors.toMap(RestProjectComponentWebhook::getId, w -> w));
                    }
                }
            }

            return _webhookMap;
        }
    }


}

