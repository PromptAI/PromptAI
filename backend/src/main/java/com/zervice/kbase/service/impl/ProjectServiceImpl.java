package com.zervice.kbase.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.zervice.common.i18n.MessageUtils;
import com.zervice.common.pojo.common.Account;
import com.zervice.common.restful.exception.RestException;
import com.zervice.common.restful.exception.StatusCodes;
import com.zervice.common.utils.AccountService;
import com.zervice.common.utils.Constants;
import com.zervice.common.utils.ServletUtils;
import com.zervice.kbase.AccountCatalog;
import com.zervice.kbase.ZBotRuntime;
import com.zervice.kbase.ai.convert.PublishConvertor;
import com.zervice.kbase.ai.output.zip.MicaZipConverter;
import com.zervice.kbase.api.restful.pojo.*;
import com.zervice.kbase.api.restful.pojo.mica.Button;
import com.zervice.kbase.api.restful.util.PageStream;
import com.zervice.kbase.database.criteria.ProjectCriteria;
import com.zervice.kbase.database.dao.*;
import com.zervice.kbase.database.pojo.*;
import com.zervice.kbase.database.utils.DaoUtils;
import com.zervice.kbase.database.utils.PageResult;
import com.zervice.kbase.service.*;
import lombok.Cleanup;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.sql.Connection;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Log4j2
public class ProjectServiceImpl implements ProjectService {

    @Autowired
    private PublishedProjectService publishedProjectService;
    @Autowired
    private PublishSnapshotService snapshotService;

    private ProjectComponentService projectComponentService = ProjectComponentService.getInstance();

    @Override
    public Object getPublicList(String dbName, ProjectCriteria criteria, PageRequest page) throws Exception {
        @Cleanup Connection conn = DaoUtils.getConnection(true);
        Account coreAccount = getCoreAccount(conn);
        //内置项目不能模板显示 &&  不展示的项目
        List<Project> projects = ProjectDao.getList(conn, coreAccount.getDbName(), criteria)
                .stream()
                .filter(item -> item.getProperties().getViewable())
                .collect(Collectors.toList());

        List<Project> pageData = PageStream.of(Project.class, page, projects.stream());
        long count = projects.size();
        return PageResult.of(pageData, count);
    }

    @Override
    public Project cloneFromPublic(String dbName, long userId, RestCloneProject cloneProject) throws Exception {
        // 1.add project
        @Cleanup Connection conn = DaoUtils.getConnection(false);

        String welcome = StringUtils.isBlank(cloneProject.getWelcome()) ?
                MessageUtils.get(Constants.I18N_DEFAULT_PROJECT_WELCOME, cloneProject.getLocale()) : cloneProject.getWelcome();
        String fallback = StringUtils.isBlank(cloneProject.getFallback()) ?
                MessageUtils.get(Constants.I18N_DEFAULT_PROJECT_FALLBACK, cloneProject.getLocale()) : cloneProject.getFallback();

        cloneProject.setWelcome(welcome);
        cloneProject.setFallback(fallback);

        // check name
        Project dbProject = ProjectDao.getByName(conn, dbName, cloneProject.getName());
        if (dbProject != null) {
            throw new RestException(StatusCodes.AiProjectNameExists, cloneProject.getName());
        }

        // 被克隆的project id
        String cloneProjectId = cloneProject.getTemplateProjectId();

        Project originProject = ProjectDao.get(conn, ZBotRuntime.DEFAULT_EXTERNAL_ACCOUNT_ID, cloneProjectId);
        if (originProject == null) {
            throw new RestException(StatusCodes.AiProjectNotFound);
        }

        // 筛选可以拷贝的components
        List<ProjectComponent> components = _filterCloneProjectComponents(cloneProjectId, conn, ZBotRuntime.DEFAULT_EXTERNAL_ACCOUNT_ID);


        // clone
        Project newProject = _clone(cloneProject, originProject, userId, components, conn, dbName);

        conn.commit();
        LOG.info("[{}][success cloned project:{} from:{}]", dbName, newProject.getId(), cloneProjectId);
        return newProject;
    }

    /**
     * 克隆数据，生成新的id
     */
    private Project _clone(RestCloneProject cloneProject, Project originProject,
                           long userId, List<ProjectComponent> components,
                           Connection conn, String dbName) throws Exception {
        // 1、clone project
        String projectId = Project.generateId();
        Project project = Project.clone(cloneProject, originProject, ServletUtils.getCurrentIp(), userId);
        project.setId(projectId);
        ProjectDao.add(conn, dbName, project);

        // record old-new id
        Map<String, String> relationMap = new HashMap<>();

        // 2、clone project component
        if (CollectionUtils.isNotEmpty(components)) {
            relationMap.put(cloneProject.getTemplateProjectId(), project.getId());
            List<ProjectComponent> newComponents = new ArrayList<>(components.size());
            for (ProjectComponent component : components) {
                ProjectComponent newComponent = component.clone(project.getId(), userId);
                newComponents.add(newComponent);
                relationMap.put(component.getId(), newComponent.getId());
            }

            // update relation
            for (ProjectComponent newComponent : newComponents) {
                newComponent.updateRelations(relationMap);
            }
            // save  components;
            ProjectComponentDao.add(conn, dbName, newComponents);
        }

        return project;
    }


    private List<ProjectComponent> _filterCloneProjectComponents(String projectId,
                                                                 Connection conn, String dbName) throws Exception {
        return ProjectComponentDao.getByProjectId(conn, dbName, projectId);
    }

    @Override
    public List<RestProjectComponentConversation> getDeployedConversations(String publishedProjectId, Connection conn, String dbName) throws Exception {
        return _getDeployedRoots(publishedProjectId, conn, dbName).stream()
                .filter(c -> ProjectComponent.TYPE_CONVERSATION.equals(c.getType()))
                .map(RestProjectComponentConversation::new)
                .collect(Collectors.toList());
    }

    private List<ProjectComponent> _getDeployedRoots(String publishedProjectId, Connection conn, String dbName) throws Exception {
        List<String> deployedComponentIds = _getDeployedComponentIds(publishedProjectId, conn, dbName);
        if (CollectionUtils.isEmpty(deployedComponentIds)) {
            return Collections.emptyList();
        }

        return ProjectComponentDao.getByIds(conn, dbName, deployedComponentIds);
    }


    private List<String> _getDeployedComponentIds(String publishedProjectId, Connection conn, String dbName) throws Exception {
        PublishedProject publishedProject = PublishedProjectDao.get(conn, dbName, publishedProjectId);
        if (publishedProject == null) {
            return Collections.emptyList();
        }
        return publishedProject.getProperties().getComponentIds();
    }

    @Override
    public List<ProjectComponent> getDeployedComponents(String publishedProjectId, Connection conn, String dbName) throws Exception {
        List<ProjectComponent> deployedRoots = _getDeployedRoots(publishedProjectId, conn, dbName);
        if (CollectionUtils.isEmpty(deployedRoots)) {
            return Collections.emptyList();
        }

        List<ProjectComponent> children = new ArrayList<>();
        for (ProjectComponent root : deployedRoots) {
            List<ProjectComponent> components = projectComponentService.loadChildren(root.getId(), dbName);
            if (CollectionUtils.isNotEmpty(components)) {
                children.addAll(components);
            }
        }

        return children;
    }


    private Account getCoreAccount(Connection conn) throws Exception {
        Account account = AccountDao.get(conn, ZBotRuntime.DEFAULT_INTERNAL_ACCOUNT_ID);
        if (account == null) {
            LOG.error("core account not created");
            throw new RestException(StatusCodes.InternalError, "core account not created");
        }
        return account;
    }

    /**
     * <p>
     * 如果 published project 不存在则创建一个
     * 如果存在, 重新训练模型并发送指令到agent 更新published project
     *       TODO:此时，有可能存在已经训练好的模型（最新的project数据已有一个最新的model）则无需训练，直接load模型。
     *       目前，我们直接基于最新的数据训练一个模型并加载
     * </p>
     * 并发问题:这里会更新PublishedProject，在 {@link com.zervice.kbase.api.rpc.RpcPublishedProjectController#started(JSONObject, String, String)}也会更新
     * <p>
     * <p>
     * 目前存在以下情况：
     *   <ul> - 模型Image更新了，但是这里更新会丢掉启动时上报的数据. 【这个数据必须要】</ul>
     *
     * </p>
     * <p>
     *    解决方案：在发布/处理start时，同步内存中的project，让其中一个请求等待起来.
     * <p/>
     */
    @Autowired
    private AgentService agentService;

    @Autowired
    private AccountService accountService;

    @Override
    public RestPublishedProject publish(String projectId, String publishedProjectId,
                                        String publishModel, List<String> componentIds,
                                        long userId, String dbName) throws Exception {
        // check
        if (CollectionUtils.isEmpty(componentIds)) {
            LOG.error("[{}][publish project:{} fail, componentIds can't be null or empty]", dbName, projectId);
            throw new RestException(StatusCodes.BadRequest);
        }


        LOG.info("[{}][start publish project:{} with component:{}]", dbName, projectId, componentIds);
        componentIds = componentIds.stream().distinct().collect(Collectors.toList());

        Project project = _getProject(projectId, dbName);

        // preparation
        publishedProjectService.preparePublishedProject(userId, projectId, publishedProjectId, publishModel, dbName);

        @Cleanup Connection conn = DaoUtils.getConnection(false);

        // check llm server config
        accountService.enoughToken(conn, dbName);

        _checkIfCanTrain(componentIds, conn, dbName);

        PublishedProject publishedProject = AccountCatalog.ensure(dbName).getPublishedProjects().get(publishedProjectId);
        synchronized (publishedProject) {

            PublishRecord publishRecord = PublishRecord.factory(projectId, publishedProjectId, componentIds, userId);
            PublishRecord.Prop prop = publishRecord.getProperties();

            // 2、snapshot model : create a snapshot
            PublishSnapshot publishSnapshot = null;
            if (PublishedProject.PUBLISH_MODEL_SNAPSHOT.equals(publishModel)) {
                publishSnapshot = snapshotService.generate(publishRecord, userId, dbName, conn);
                long snapshotId = publishSnapshot.getId();

                // add stage
                PublishRecord.Stage stage = PublishRecord.Stage.factory(PublishRecord.Stage.NAME_SNAPSHOT, snapshotId);
                prop.getStages().add(stage);

                // text model 需要将snapshot准备好后再发布
                if (Objects.equals(publishedProject.getStatus(), PublishedProject.STATUS_RUNNING)) {
                    publishedProject.setStatus(PublishedProject.STATUS_DEPLOYING);
                }
            }

            // 请求 Agent 发布
            File publishData = _preparePublishDate(project, publishedProject, componentIds, conn, dbName);
            Agent agent = agentService.getByAgentId(publishedProject.getAgentId(), conn);
            agentService.publish(publishData, agent, publishedProject, dbName);

            // 清理临时文件
            if (publishData.exists()) {
                publishData.delete();
            }

            publishedProject.getProperties().setComponentIds(componentIds);
            publishedProject.getProperties().setUpdateTime(System.currentTimeMillis());
            publishedProject.getProperties().setUpdateBy(userId);

            // check finish - if no stages exists, mark finish
            publishRecord.checkSuccess();

            // save record
            PublishRecordDao.add(conn, dbName, publishRecord);

            // update to db & cache
            PublishedProjectDao.updateStatusAndProp(conn, dbName, publishedProject);
            AccountCatalog.ensure(dbName).getPublishedProjects().onUpdate(publishedProject);
            conn.commit();

            if (publishSnapshot != null) {
                snapshotService.run(publishSnapshot, dbName);
            }

            List<RestPublishRecord> recentRecords = publishedProjectService.loadRecentRecords(publishedProjectId, conn, dbName);
            LOG.info("[{}][finish release project:{}]", dbName, publishedProjectId);
            return new RestPublishedProject(publishedProject, new RestPublishRecord(publishRecord, dbName), recentRecords);
        }
    }

    @Override
    public Project save(RestProject project, Long userId, Connection conn, String dbName) throws Exception {
        _checkNameExists(project, conn, dbName);

        Project.Prop prop = Project.Prop.builder()
                .createBy(userId).createTime(System.currentTimeMillis())
                .description(project.getDescription())
                .locale(project.getLocale())
                .welcome(project.getWelcome())
                .fallback(project.getFallback())
                .fallbackType(project.getFallbackType())
                .webhooks(project.getWebhooks())
                .showSubNodesAsOptional(project.getShowSubNodesAsOptional())
                .chatBotSettings(project.getChatBotSettings())
                .introduction(project.getIntroduction())
                .viewable(project.getViewable())
                .schedule(project.getSchedule())
                .image(project.getImage())
                .build();

        String id = Project.generateId();
        Project dbProject = Project.builder()
                .name(project.getName())
                .properties(prop)
                .id(id)
                .build();

        save(dbProject, conn, dbName);

        return dbProject;
    }

    public void save(Project dbProject, Connection conn, String dbName) throws Exception {
        _welcome(dbProject);
        _fallback(dbProject, conn, dbName);

        ProjectDao.add(conn, dbName, dbProject);
    }

    @Override
    public Project update(RestProject project, Long userId, String dbName) throws Exception {
        @Cleanup Connection conn = DaoUtils.getConnection(false);
        Project dbProject = ProjectDao.get(conn, dbName, project.getId());
        if (dbProject == null) {
            LOG.warn("[{}][update project:{} fail, not found]", dbName, project.getId());
            throw new RestException(StatusCodes.AiProjectNotFound, "Not found");
        }

        _checkNameExists(project, conn, dbName);
        dbProject.getProperties().setWelcome(project.getWelcome());
        dbProject.getProperties().setFallback(project.getFallback());
        dbProject.getProperties().setFallbackType(project.getFallbackType());
        dbProject.getProperties().setWebhooks(project.getWebhooks());

        _welcome(dbProject);
        _fallback(dbProject, conn, dbName);

        dbProject.setName(project.getName());

        String image =  project.getImage();

        dbProject.getProperties().setImage(image);
        dbProject.getProperties().setLocale(project.getLocale());
        dbProject.getProperties().setDescription(project.getDescription());
        dbProject.getProperties().setUpdateBy(userId);
        dbProject.getProperties().setShowSubNodesAsOptional(project.getShowSubNodesAsOptional());
        dbProject.getProperties().setUpdateTime(System.currentTimeMillis());
        dbProject.getProperties().setChatBotSettings(project.getChatBotSettings());
        dbProject.getProperties().setFallbackButtons(project.getFallbackButtons());

        // 解决前端部分场景下丢失这两个值
        if (project.getIntroduction() != null) {
            dbProject.getProperties().setIntroduction(project.getIntroduction());
        }
        if (project.getViewable() != null) {
            dbProject.getProperties().setViewable(project.getViewable());
        }

        if (project.getSchedule() != null) {
            dbProject.getProperties().setSchedule(project.getSchedule());
        }

        ProjectDao.update(conn, dbName, dbProject);

        _updateShowFlowAsOption(project, conn, dbName);

        conn.commit();

        return dbProject;
    }

    private void _checkIfCanTrain(List<String> ids, Connection conn, String dbName) throws Exception {
        List<ProjectComponent> components = ProjectComponentDao.getByIds(conn, dbName, ids);
        components.stream().forEach(item -> {
            if (!item.parseIsReady()) {
                throw new RestException(StatusCodes.AiTrainNotReady);
            }
        });
    }


    private File _preparePublishDate(Project project, PublishedProject publishedProject,
                                     List<String> componentIds, Connection conn, String dbName) throws Exception {
        List<ProjectComponent> projectComponents = projectComponentService.getProjectComponentWithInternalEntity(project.getId(), conn, dbName);
        String json = PublishConvertor.convert(project, publishedProject, componentIds, projectComponents, conn, dbName);
        return MicaZipConverter.convert(json);
    }

    private Project _getProject(String projectId, String dbName) throws Exception {
        @Cleanup Connection conn = DaoUtils.getConnection(true);
        return ProjectDao.get(conn, dbName, projectId);
    }

    public void _checkNameExists(RestProject project, Connection conn, String dbName) throws Exception {
        Optional<Project> existOp = ProjectDao.get(conn, dbName).stream()
                .filter(p -> p.getName().equals(project.getName()) && (project.getId() == null || !p.getId().equals(project.getId())))
                .findAny();

        if (existOp.isPresent()) {
            throw new RestException(StatusCodes.AiProjectNameExists, project.getName());
        }
    }

    private void _welcome(Project project) {
        String welcome = project.getProperties().getWelcome();
        if (StringUtils.isBlank(welcome)) {
            welcome = MessageUtils.get(Constants.I18N_DEFAULT_PROJECT_WELCOME, project.getProperties().getLocale());
        }
        project.getProperties().setWelcome(welcome);
    }

    private void _fallback(Project project, Connection conn, String dbName) throws Exception {
        Project.Prop properties = project.getProperties();
        String fallback = properties.getFallback();
        if (StringUtils.isBlank(fallback)) {
            fallback = MessageUtils.get(Constants.I18N_DEFAULT_PROJECT_FALLBACK, project.getProperties().getLocale());
        }

        properties.setFallback(fallback);

        // check type
        String type = properties.getFallbackType();
        Project.Prop.checkFallbackType(type);

        // check fallback type
        List<Button> fallbackButton = project.getProperties().getFallbackButtons();
        if (CollectionUtils.isNotEmpty(fallbackButton)) {
            List<String> componentIds = fallbackButton.stream()
                    .map(Button::getId)
                    .distinct()
                    .collect(Collectors.toList());

            Map<String, ProjectComponent> componentMap = ProjectComponentDao.getByIds(conn, dbName, componentIds).stream()
                    .collect(Collectors.toMap(ProjectComponent::getId, c -> c));
            for (Button button : fallbackButton) {
                String buttonType = button.getType();
                ProjectComponent buttonComponent = componentMap.get(button.getId());

                // conversation
                if (ProjectComponent.TYPE_CONVERSATION.equals(buttonType)) {
                    // invalid config
                    if (buttonComponent == null || !ProjectComponent.TYPE_CONVERSATION.equals(buttonComponent.getType())) {
                        throw new RestException(StatusCodes.InvalidFallbackButton);
                    }
                    continue;
                }

                LOG.info("[{}][unsupported fallback button type:{}]", dbName, buttonType);
                throw new RestException(StatusCodes.InvalidFallbackButton);
            }
        }

        if (Project.Prop.FALLBACK_TYPE_WEBHOOK.equals(type)) {
            //检查webhook
            if (CollectionUtils.isEmpty(properties.getWebhooks())) {
                throw new RestException(StatusCodes.AiTrainFileInvalidWebhookNotExists);
            }

            RestProjectComponentBot.BotResponse first = properties.getWebhooks().get(0);
            String webhookId = first.getContent().getString("id");
            if (!first.isWebhook() || StringUtils.isBlank(webhookId)) {
                throw new RestException(StatusCodes.AiTrainFileInvalidWebhookNotExists);
            }

            ProjectComponent component = ProjectComponentDao.get(conn, dbName, webhookId);
            if (component == null) {
                throw new RestException(StatusCodes.AiTrainFileInvalidWebhookNotExists);
            }
        }
    }

    /**
     * 是否选择展示节点信息（conversations）
     * 如果为custom 同时showShbNodesAsOptionalIds 不为null则更新对应的flow配置
     * 只用在update项目时调用
     */
    private void _updateShowFlowAsOption(RestProject project, Connection conn, String dbName) throws Exception {
        if (!Project.Prop.SHOW_SUB_NODES_AS_OPTIONAL_CUSTOM.equals(project.getShowSubNodesAsOptional())) {
            return;
        }
        Set<String> ids = project.getShowShbNodesAsOptionalIds();
        // 与前端约定，null不处理
        if (ids == null) {
            return;
        }

        List<ProjectComponent> components = ProjectComponentDao.getByProjectIdAndType(conn, dbName, project.getId(), ProjectComponent.TYPE_CONVERSATION);
        for (ProjectComponent component : components) {
            RestProjectComponentConversation c = new RestProjectComponentConversation(component);
            boolean hidden = true;
            if (ids.contains(c.getId())) {
                hidden = false;
            }
            boolean needUpdate = c.getData().getHidden() == null || c.getData().getHidden() != hidden;
            if (needUpdate) {
                c.getData().setHidden(hidden);
                ProjectComponentDao.updateProp(conn, dbName, c.toProjectComponent());
            }
        }
    }


}
