package com.zervice.kbase.api.restful.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.google.common.collect.Maps;
import com.zervice.common.pojo.common.EmptyResponse;
import com.zervice.common.restful.exception.RestException;
import com.zervice.common.restful.exception.StatusCodes;
import com.zervice.kbase.api.BaseController;
import com.zervice.kbase.api.restful.AuthFilter;
import com.zervice.kbase.api.restful.pojo.*;
import com.zervice.kbase.api.restful.pojo.mica.SlotPojo;
import com.zervice.kbase.database.criteria.ProjectComponentCriteria;
import com.zervice.kbase.database.dao.ProjectComponentDao;
import com.zervice.kbase.database.dao.ProjectDao;
import com.zervice.kbase.database.pojo.Project;
import com.zervice.kbase.database.pojo.ProjectComponent;
import com.zervice.kbase.database.pojo.User;
import com.zervice.kbase.database.utils.DaoUtils;
import com.zervice.kbase.service.ProjectComponentService;
import lombok.Cleanup;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.sql.Connection;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Peng Chen
 * @date 2022/6/22
 */
@Log4j2
@RequestMapping("/api/project/component")
@RestController
public class ProjectComponentController extends BaseController {

    private ProjectComponentService _projectComponentService = ProjectComponentService.getInstance();


    @GetMapping("{projectId}")
    public Object get(@PathVariable String projectId,
                      @RequestParam String type,
                      ProjectComponentCriteria criteria,
                      @RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName,
                      @RequestHeader(AuthFilter.X_EXTERNAL_USER_ID_HEADER) String uid) throws Exception {
        long userId = User.fromExternalId(uid);
        return _projectComponentService.get(dbName, type, projectId, criteria);
    }

    /**
     * 获取单个组建详情
     */
    @GetMapping("single/{id}")
    public Object get(@PathVariable String id,
                      @RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName,
                      @RequestHeader(AuthFilter.X_EXTERNAL_USER_ID_HEADER) String uid) throws Exception {
        @Cleanup Connection conn = DaoUtils.getConnection(true);
        ProjectComponent component = ProjectComponentDao.get(conn, dbName, id);
        if (component == null) {
            throw new RestException(StatusCodes.AiProjectComponentNotFound);
        }

        return _projectComponentService.convertToRest(component, conn, dbName);
    }

    @GetMapping("{projectId}/{id}")
    public Object get(@PathVariable String projectId,
                      @PathVariable String id,
                      @RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName,
                      @RequestHeader(AuthFilter.X_EXTERNAL_USER_ID_HEADER) String uid) throws Exception {
        long userId = User.fromExternalId(uid);

        @Cleanup Connection conn = DaoUtils.getConnection(true);
        ProjectComponent component = ProjectComponentDao.get(conn, dbName, id);
        if (component == null || !component.getProjectId().equals(projectId)) {
            throw new RestException(StatusCodes.AiProjectComponentNotFound, "Not found");
        }

        return _handleDetailResult(component, conn, dbName);
    }

    @PostMapping("{projectId}")
    public Object save(@PathVariable String projectId,
                       @RequestBody String body,
                       @RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName,
                       @RequestHeader(AuthFilter.X_EXTERNAL_USER_ID_HEADER) String uid) throws Exception {
        long userId = User.fromExternalId(uid);

        JSONObject data = JSONObject.parseObject(body);

        ProjectComponent component = _projectComponentService.convertBodyToComponent(data);
        @Cleanup Connection conn = DaoUtils.getConnection(false);
        _projectComponentService.checkProjectId(projectId, conn, dbName);

        component.setProjectId(projectId);
        component.setId(ProjectComponent.generateId(component.getType()));
        component.getProperties().setCreateBy(userId);
        component.getProperties().setCreateTime(System.currentTimeMillis());

        component = _projectComponentService.save(component, true, conn, dbName);
        _projectComponentService.updateValidateInfo(projectId, conn, dbName);
        conn.commit();

        return _afterSaved(component, projectId, conn, dbName);
    }


    @PostMapping("{projectId}/{rootComponentId}/batch")
    public Object saveBatch(@PathVariable String projectId,
                            @PathVariable String rootComponentId,
                            @RequestBody String body,
                            @RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName,
                            @RequestHeader(AuthFilter.X_EXTERNAL_USER_ID_HEADER) String uid) throws Exception {
        long userId = User.fromExternalId(uid);

        JSONArray data = JSONObject.parseArray(body);

        @Cleanup Connection conn = DaoUtils.getConnection(false);
        _projectComponentService.checkProjectId(projectId, conn, dbName);
        _projectComponentService.checkRootComponentId(rootComponentId, conn, dbName);
        // 这里解决循环依赖
        data = _breakCircleReference(data);
        List<ProjectComponent> components = data.stream()
                .map(item -> (JSONObject) item)
                .map(item -> _projectComponentService.convertBodyToComponent(item))
                .collect(Collectors.toList());

        for (int i = 0; i < components.size(); i++) {
            ProjectComponent component = components.get(i);

            component.setProjectId(projectId);
            component.setRootComponentId(rootComponentId);
            component.getProperties().setCreateBy(userId);
            component.getProperties().setCreateTime(System.currentTimeMillis());
            _projectComponentService.save(component, false, conn, dbName);
        }
        _projectComponentService.updateValidateInfo(projectId, conn, dbName);
        conn.commit();
        return EmptyResponse.empty();
    }

    @PutMapping("{projectId}")
    public Object update(@PathVariable String projectId,
                         @RequestBody String body,
                         @RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName,
                         @RequestHeader(AuthFilter.X_EXTERNAL_USER_ID_HEADER) String uid) throws Exception {
        long userId = User.fromExternalId(uid);

        JSONObject data = JSONObject.parseObject(body);
        @Cleanup Connection conn = DaoUtils.getConnection(false);
        _projectComponentService.checkProjectId(projectId, conn, dbName);
        ProjectComponent component = _projectComponentService.convertBodyToComponent(data);
        if (StringUtils.isBlank(component.getId())) {
            throw new RestException(StatusCodes.AiProjectComponentIdRequired, "id required");
        }

        component.setProjectId(projectId);
        component.getProperties().setCreateBy(userId);

        _projectComponentService.update(component, conn, dbName);
        _projectComponentService.updateValidateInfo(projectId, conn, dbName);
        conn.commit();
        ProjectComponent savedComponent = ProjectComponentDao.get(conn, dbName, component.getId());

        return _projectComponentService.convertToRest(savedComponent, conn, dbName);
    }

    @PutMapping("resort")
    public Object resort(@RequestBody @Validated RestResortProjectComponent resort,
                         @RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName,
                         @RequestHeader(AuthFilter.X_EXTERNAL_USER_ID_HEADER) String uid) throws Exception {
        if (Objects.equals(resort.getC1(), resort.getC2())) {
            LOG.warn("[{}][ignore resort, components are same one]", dbName);
            return EmptyResponse.empty();
        }

        @Cleanup Connection conn = DaoUtils.getConnection(true);
        ProjectComponent c1 = ProjectComponentDao.get(conn, dbName, resort.getC1());
        if (c1 == null) {
            LOG.error("[{}][resort fail, sort component:{} not found]", dbName, resort.getC1());
            throw new RestException(StatusCodes.AiProjectComponentNotFound);
        }

        if (StringUtils.isBlank(c1.parseParentId())) {
            LOG.error("[{}][resort fail, component:{} parent not exists]", dbName, resort.getC1());
            throw new RestException(StatusCodes.AiProjectComponentInvalidParent);
        }

        ProjectComponent c2 = ProjectComponentDao.get(conn, dbName, resort.getC2());
        if (c2 == null) {
            LOG.error("[{}][resort fail, sort component:{} not found]", dbName, resort.getC2());
            throw new RestException(StatusCodes.AiProjectComponentNotFound);
        }

        if (StringUtils.isBlank(c2.parseParentId())) {
            LOG.error("[{}][resort fail, component:{} parent not exists]", dbName, resort.getC2());
            throw new RestException(StatusCodes.AiProjectComponentInvalidParent);
        }

        if (!StringUtils.equals(c1.parseParentId(), c2.parseParentId())) {
            LOG.error("[{}][resort fail, c1:{} and c2:{} not a same parent]", dbName, resort.getC1(), resort.getC2());
            throw new RestException(StatusCodes.AiProjectComponentSortNotSupport);
        }

        Integer c1No = c1.getProperties().getNo();
        Integer c2No = c2.getProperties().getNo();

        // 编号不一样，直接交换
        if (!c1No.equals(c2No)) {
            int c = c1No;
            c1No = c2No;
            c2No = c;

            c1.getProperties().setNo(c1No);
            c2.getProperties().setNo(c2No);
            ProjectComponentDao.updateProp(conn, dbName, c1);
            ProjectComponentDao.updateProp(conn, dbName, c2);
            return EmptyResponse.empty();
        }

        // 编号一样，那么c1 减去1 重新申请一个编号
        c1No--;
        c1.getProperties().setNo(c1No);

        ProjectComponentDao.updateProp(conn, dbName, c1);
        return EmptyResponse.empty();
    }


    @DeleteMapping("{projectId}")
    public Object delete(@PathVariable String projectId,
                         @RequestParam List<String> components,
                         @RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName,
                         @RequestHeader(AuthFilter.X_EXTERNAL_USER_ID_HEADER) String uid) throws Exception {
        long userId = User.fromExternalId(uid);
        @Cleanup Connection conn = DaoUtils.getConnection(false);
        _projectComponentService.delete(projectId, components, conn, dbName);
        conn.commit();
        return EmptyResponse.empty();
    }

    /**
     * 获取回收站列表
     */
    @GetMapping("{projectId}/{rootComponentId}/trash")
    public Object trash(@PathVariable String projectId,
                        @PathVariable String rootComponentId,
                        @RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName,
                        @RequestHeader(AuthFilter.X_EXTERNAL_USER_ID_HEADER) String uid) throws Exception {
        @Cleanup Connection conn = DaoUtils.getConnection(true);

        // 过滤parent is null && type can add 2 trash
        List<ProjectComponent> all = ProjectComponentDao.getByProjectId(conn, dbName, projectId);
        List<ProjectComponent> trashedComponents = _projectComponentService.getRootComponentTrashComponents(all, rootComponentId);

        JSONArray results = new JSONArray();
        for (ProjectComponent trashed : trashedComponents) {
            List<ProjectComponent> trashedChildren = _projectComponentService.loadTrashedComponentChildren(all, trashed);
            trashedChildren.add(trashed);
            JSONObject item = new JSONObject();
            item.put("top", _projectComponentService.convertToRest(trashed, conn, dbName));
            item.put("data", _projectComponentService.convertToRest(trashedChildren, projectId, conn, dbName));
            results.add(item);
        }

        return JSONArray.parseArray(results.toString(SerializerFeature.DisableCircularReferenceDetect));
    }

    /**
     * 添加到回收站
     */
    @PutMapping("{projectId}/{rootComponentId}/trash/{componentId}")
    public Object trash(@PathVariable String projectId,
                        @PathVariable String rootComponentId,
                        @PathVariable String componentId,
                        //含break的节点移到回收站的时候，需要由前端传递节点所在form的formId
                        @RequestParam(required = false) String formId,
                        @RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName,
                        @RequestHeader(AuthFilter.X_EXTERNAL_USER_ID_HEADER) String uid) throws Exception {

        @Cleanup Connection conn = DaoUtils.getConnection(false);

        ProjectComponent component = ProjectComponentDao.get(conn, dbName, componentId);
        if (component == null) {
            throw new RestException(StatusCodes.AiProjectComponentNotFound);
        }

        // 不能加入回收站
        if (!ProjectComponent.TYPE_CAN_TRASH.contains(component.getType())) {
            throw new RestException(StatusCodes.AiProjectComponentCanNotBeTrash);
        }

        component.move2Trash();
        ProjectComponentDao.update(conn, dbName, component);

        _projectComponentService.updateValidateInfo(projectId, conn, dbName);
        conn.commit();

        // 返回被删除的节点
        List<ProjectComponent> trashedChildren = _projectComponentService.loadChildren(component, conn, dbName);
        trashedChildren.add(component);

        return _projectComponentService.convertToRest(trashedChildren, projectId, conn, dbName);
    }

    /**
     * 从回收站中放回
     */
    @PutMapping("{projectId}/{rootComponentId}/trash/putback/{componentId}")
    public Object trashPutback(@PathVariable String projectId,
                               @PathVariable String rootComponentId,
                               @PathVariable String componentId,
                               // 接在谁后面
                               @RequestParam String parentId,
                               @RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName,
                               @RequestHeader(AuthFilter.X_EXTERNAL_USER_ID_HEADER) String uid) throws Exception {
        @Cleanup Connection conn = DaoUtils.getConnection(false);

        ProjectComponent component = ProjectComponentDao.get(conn, dbName, componentId);
        if (component == null) {
            throw new RestException(StatusCodes.AiProjectComponentNotFound);
        }

        // 不在回收站
        String parentIdInComponent = component.parseParentId();
        if (StringUtils.isNotBlank(parentIdInComponent)) {
            return EmptyResponse.empty();
        }

        ProjectComponent parent = ProjectComponentDao.get(conn, dbName, parentId);
        if (parent == null) {
            throw new RestException(StatusCodes.AiProjectComponentInvalidParent);
        }

        component.trashPutback(parentId);
        _projectComponentService.update(component, conn, dbName);
        _projectComponentService.updateValidateInfo(projectId, conn, dbName);
        conn.commit();
        return EmptyResponse.empty();
    }

    /**
     * 清空回收站
     */
    @DeleteMapping("{projectId}/{rootComponentId}/trash/empty")
    public Object trashEmpty(@PathVariable String projectId,
                             @PathVariable String rootComponentId,
                             @RequestParam List<String> components,
                             @RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName,
                             @RequestHeader(AuthFilter.X_EXTERNAL_USER_ID_HEADER) String uid) throws Exception {

        @Cleanup Connection conn = DaoUtils.getConnection(true);
        List<ProjectComponent> trashedComponent = new ArrayList<>();
        for (String componentId : components) {
            ProjectComponent component = ProjectComponentDao.get(conn, dbName, componentId);
            if (component == null) {
                LOG.warn("[{}][empty trash fail, component:{}, not found]", dbName, componentId);
                continue;
            }

            trashedComponent.add(component);
            trashedComponent.addAll(_projectComponentService.loadChildren(component, conn, dbName));
        }

        delete(projectId, components, dbName, uid);
        _projectComponentService.updateValidateInfo(projectId, conn, dbName);
        return _projectComponentService.convertToRest(trashedComponent, projectId, conn, dbName);
    }

    /**
     * 粘贴复制或收藏的节点
     */
    @PutMapping("{projectId}/{rootComponentId}/paste")
    public Object paste(@PathVariable String projectId,
                        @PathVariable String rootComponentId,
                        @RequestBody String body,
                        @RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName,
                        @RequestHeader(AuthFilter.X_EXTERNAL_USER_ID_HEADER) String uid) throws Exception {
        long userId = User.fromExternalId(uid);

        JSONArray data = JSONObject.parseArray(body);

        @Cleanup Connection conn = DaoUtils.getConnection(false);
        _projectComponentService.checkProjectId(projectId, conn, dbName);
        _projectComponentService.checkRootComponentId(rootComponentId, conn, dbName);

        // 这里可能导致循环依赖
        data = _breakCircleReference(data);

        // stage 1： 转换成节点
        List<ProjectComponent> components = data.stream()
                .map(item -> (JSONObject) item)
                .map(item -> _projectComponentService.convertBodyToComponent(item))
                .collect(Collectors.toList());

        List<ProjectComponent> sortedComponents = _projectComponentService.sortByParentToChild(components);

        // stage 2 查找引用的entity、webhook、globalUser、globalBot
        // 有可能会重名,导致添加失败

        List<ProjectComponent> usedComponentsInPaste = _pasteReferencedComponents(data, projectId, conn, dbName);
        sortedComponents.addAll(usedComponentsInPaste);

        // 这里需要新生成id
        Map<String /* old id */, String /* new id*/> idMap = sortedComponents.stream()
                .collect(Collectors.toMap(ProjectComponent::getId, p -> ProjectComponent.generateId(p.getType())));

        for (int i = 0; i < sortedComponents.size(); i++) {
            ProjectComponent component = sortedComponents.get(i);
            // 新的节点id
            String newId = idMap.get(component.getId());
            component.setId(newId);

            component.setProjectId(projectId);
            component.setRootComponentId(rootComponentId);
            component.getProperties().setCreateBy(userId);
            component.getProperties().setCreateTime(System.currentTimeMillis());

            // 更新关联关系
            component.updateRelations(idMap);

            _projectComponentService.save(component, false, conn, dbName);
        }
        conn.commit();

        _projectComponentService.updateValidateInfo(projectId, conn, dbName);
        conn.commit();

        // 前端需要把新增的节点返回回去，但是不能给前端entity等id,他只需要在图中的节点
        // 不然撤销的时候会讲这些不在图上 的id返回回来，导致提示使用中
        Set<String> usedComponentIdsInPaste = usedComponentsInPaste.stream()
                .map(ProjectComponent::getId)
                .collect(Collectors.toSet());
        sortedComponents = sortedComponents.stream()
                .filter(c -> !usedComponentIdsInPaste.contains(c.getId()))
                .collect(Collectors.toList());
        return _projectComponentService.convertToRest(sortedComponents, projectId, conn, dbName);
    }


    /**
     * a -> b - > c
     * <p>
     * 1、此时将 b - > c 进行收藏
     * 2、将b -> c 接到c 后面
     * 3、此时  b.parentId 将变成 c
     * 4、问题 ： paste的 b -> c相互引用
     * <p>
     * 解决 ： 将第一个节点的parent Id读取出来,如果后续节点有parentId==id的节点，重新生成一把id
     */
    private JSONArray _breakCircleReference(JSONArray data) {
        if (data.size() <= 1) {
            return data;
        }

        JSONObject first = data.getJSONObject(0);
        String fistParentId = first.getString("parentId");
        if (StringUtils.isBlank(fistParentId)) {
            return data;
        }

        // 为冲突的节点生成新的id
        Map<String, String> idMap = Maps.newHashMapWithExpectedSize(data.size());
        for (int i = 1; i < data.size(); i++) {
            JSONObject c = data.getJSONObject(i);
            String id = c.getString("id");
            if (Objects.equals(id, fistParentId)) {
                String type = c.getString("type");
                idMap.put(id, ProjectComponent.generateId(type));
            }
        }

        // 第一个节点不用替换
        data.remove(0);

        // 将后续节点转string，replace all
        String result = data.toJSONString();
        for (Map.Entry<String, String> entry : idMap.entrySet()) {
            result = result.replaceAll(entry.getKey(), entry.getValue());
        }

        // 再将后续节点还原回 array
        data = JSONArray.parseArray(result);
        // 增加第一个节点
        data.add(0, first);
        return data;
    }

    private List<ProjectComponent> _pasteReferencedComponents(JSONArray data, String projectId,
                                                              Connection conn, String dbName) throws Exception {
        List<RestBaseProjectComponent> pastedComponents = data.stream()
                .map(item -> (JSONObject) item)
                .map(item -> _projectComponentService.convertBodyRest(item))
                .collect(Collectors.toList());

        List<ProjectComponent> webhooks = _filterUsedWebhooks(pastedComponents, projectId, conn, dbName);
        List<ProjectComponent> entities = _filterUsedEntities(pastedComponents, projectId, conn, dbName);
        // 合并
        webhooks.addAll(entities);
        return webhooks;
    }


    private List<ProjectComponent> _filterUsedWebhooks(List<RestBaseProjectComponent> rests, String projectId,
                                                       Connection conn, String dbName) throws Exception {
        Set<String> containWebhookTypes = Set.of(ProjectComponent.TYPE_BOT);
        List<String> webhookIds = rests.stream()
                .filter(r -> containWebhookTypes.contains(r.getType()))
                .flatMap(r -> {
                    List<String> ids = new ArrayList<>();
                    RestProjectComponentBot bot = (RestProjectComponentBot) r;
                    List<RestProjectComponentBot.BotResponse> responses = bot.getData().getResponses();

                    if (CollectionUtils.isNotEmpty(responses)) {
                        List<String> idInRes = responses.stream()
                                .filter(item -> RestProjectComponentBot.BotResponse.TYPE_WEBHOOK.equals(item.getType()))
                                .map(item -> item.getContent().getString("id"))
                                .filter(StringUtils::isNotBlank)
                                .collect(Collectors.toList());
                        ids.addAll(idInRes);
                    }

                    return ids.stream();
                })
                .distinct()
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(webhookIds)) {
            return new ArrayList<>();
        }

        List<ProjectComponent> webhooks = ProjectComponentDao.getByIds(conn, dbName, webhookIds);
        return webhooks.stream()
                .filter(w -> !projectId.equals(w.getProjectId()))
                .collect(Collectors.toList());

    }

    private List<ProjectComponent> _filterUsedEntities(List<RestBaseProjectComponent> rests, String projectId,
                                                       Connection conn, String dbName) throws Exception {
        Set<String> containEntityTypes = Set.of(ProjectComponent.TYPE_USER, ProjectComponent.TYPE_BOT);

        List<String> entityIds = rests.stream()
                .filter(r -> containEntityTypes.contains(r.getType()))
                .flatMap(r -> {
                    List<String> ids = new ArrayList<>();
                    switch (r.getType()) {
                        case ProjectComponent.TYPE_USER:
                            RestProjectComponentUser user = (RestProjectComponentUser) r;
                            // 1、examples 里面
                            if (CollectionUtils.isNotEmpty(user.getData().getMappings())) {
                                List<String> idInMappings = user.getData().getMappings().stream()
                                        .map(RestProjectComponentUser.Mapping::getSlotId)
                                        .collect(Collectors.toList());
                                ids.addAll(idInMappings);
                            }

                            // 2、 mark 里面
                            if (CollectionUtils.isNotEmpty(user.getData().getExamples())) {
                                List<String> inInMarks = user.getData().getExamples().stream()
                                        .filter(e -> CollectionUtils.isNotEmpty(e.getMarks()))
                                        .flatMap(e -> e.getMarks().stream())
                                        .filter(m -> StringUtils.isNotBlank(m.getEntityId()))
                                        .map(RestProjectComponentUser.Mark::getEntityId)
                                        .collect(Collectors.toList());
                                ids.addAll(inInMarks);
                            }

                            // 3、set slots里面
                            if (CollectionUtils.isNotEmpty(user.getData().getSetSlots())) {
                                List<String> idInSetSlots = user.getData().getSetSlots().stream()
                                        .map(SlotPojo::getSlotId)
                                        .collect(Collectors.toList());
                                ids.addAll(idInSetSlots);
                            }
                            break;

                        case ProjectComponent.TYPE_BOT:
                            RestProjectComponentBot bot = (RestProjectComponentBot) r;
                            // 1、set slots里面
                            if (CollectionUtils.isNotEmpty(bot.getData().getSetSlots())) {
                                List<String> idInSetSlots = bot.getData().getSetSlots().stream()
                                        .map(SlotPojo::getSlotId)
                                        .collect(Collectors.toList());
                                ids.addAll(idInSetSlots);
                            }
                            break;

                        default:
                            break;
                    }
                    return ids.stream();
                })
                .distinct()
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(entityIds)) {
            return new ArrayList<>();
        }

        List<ProjectComponent> components = ProjectComponentDao.getByIds(conn, dbName, entityIds);
        return components.stream()
                .map(c -> new RestProjectComponentEntity(c))
                // 1、同一个项目，不用复制；
                .filter(e -> !e.getProjectId().equals(projectId) )
                .map(RestProjectComponentEntity::toProjectComponent)
                .collect(Collectors.toList());
    }

    private Object _handleDetailResult(ProjectComponent component, Connection conn, String dbName) throws Exception {
        // special handle  conversation
        switch (component.getType()) {
            case ProjectComponent.TYPE_CONVERSATION:
                String projectId = component.getProjectId();
                // build context
                ProjectComponentService.Convert2RestContext context = ProjectComponentService.Convert2RestContextFactory
                        .factory(projectId, conn, dbName);
                Project project = ProjectDao.get(conn, dbName, projectId);

                List<ProjectComponent> children = _projectComponentService.loadChildren(component, context.getAllComponents());
                children.add(component);
                return children.stream()
                        .map(item -> _projectComponentService.convertToRest(item, context, project, dbName))
                        .filter(r -> {
                            // llm 的 conversation
                            if (r instanceof RestProjectComponentConversation) {
                                RestProjectComponentConversation c = (RestProjectComponentConversation) r;
                                return !RestProjectComponentConversation.Data.TYPE_LLM_AGENT.equals(c.getData().getType());
                            }
                            return true;
                        })
                        .collect(Collectors.toList());
            default:
                return _projectComponentService.convertToRest(component, conn, dbName);
        }
    }

    /**
     * 这里需要把新增后的节点返回给前端
     */
    private List<RestBaseProjectComponent> _afterSaved(ProjectComponent component, String projectId,
                                                       Connection conn, String dbName) throws Exception {
        List<ProjectComponent> savedComponents = _projectComponentService.loadChildren(component, conn, dbName);
        savedComponents.add(component);
        return _projectComponentService.convertToRest(savedComponents, projectId, conn, dbName);
    }


}
