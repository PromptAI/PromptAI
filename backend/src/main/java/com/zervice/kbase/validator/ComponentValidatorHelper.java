package com.zervice.kbase.validator;

import cn.hutool.core.lang.Pair;
import cn.hutool.core.thread.ThreadUtil;
import com.google.common.base.Stopwatch;
import com.zervice.common.restful.exception.RestException;
import com.zervice.common.restful.exception.StatusCodes;
import com.zervice.kbase.api.restful.pojo.RestBaseProjectComponent;
import com.zervice.kbase.api.restful.pojo.RestProjectComponentBot;
import com.zervice.kbase.api.restful.pojo.RestProjectComponentUser;
import com.zervice.kbase.database.dao.ProjectComponentDao;
import com.zervice.kbase.database.dao.ProjectDao;
import com.zervice.kbase.database.pojo.Project;
import com.zervice.kbase.database.pojo.ProjectComponent;
import com.zervice.kbase.service.ProjectComponentService;
import com.zervice.kbase.validator.component.*;
import com.zervice.kbase.validator.error.ValidatorError;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 校验节点是否完整
 *
 * @author chen
 * @date 2022/10/25
 */
@Log4j2
public class ComponentValidatorHelper {
    private static ProjectComponentService _componentService = ProjectComponentService.getInstance();


    /**
     * 现在不关心节点了，只要更新就扫描整个Project
     */
    public static void validate(String projectId, Connection conn, String dbName) throws Exception {
        List<ProjectComponent> roots = ProjectComponentDao.getByProjectIdAndTypes(conn, dbName, projectId, ProjectComponent.TYPE_ROOTS);
        for (ProjectComponent root : roots) {
            _validate(root.getId(), projectId, conn, dbName);
        }
    }

    /**
     * 校验节点是否完整
     *
     * @param componentId 根节点或某个子节点
     * @param conn        conn
     * @param dbName      dbName
     */
    private static void _validate(String componentId, String projectId, Connection conn, String dbName) throws Exception {
        Stopwatch stopwatch = Stopwatch.createStarted();
        // 1、build context
        ComponentValidatorContext context = _buildContext(componentId, projectId, conn, dbName);
        if (context == null) {
            return;
        }

        LOG.debug("[{}][finish build context, spend:{}]", dbName, stopwatch.elapsed(TimeUnit.MILLISECONDS));

        // 2、 query 当前节点所在的validator
        Pair<BaseComponentValidator, List<BaseComponentValidator>> validatorPair = _validators(context);

        LOG.debug("[{}][finish build validate pair, spend:{}]", dbName, stopwatch.elapsed(TimeUnit.MILLISECONDS));

        // 3、遍历节点，更新错误信息
        List<BaseComponentValidator> validators = validatorPair.getValue();
        if (CollectionUtils.isEmpty(validators)) {
            LOG.debug("[{}][validate used in millis:{}]", dbName, stopwatch.elapsed(TimeUnit.MILLISECONDS));
            return;
        }
        BaseComponentValidator root = validatorPair.getKey();

        LOG.debug("[{}][start validate with length:{}, spend:{}]", dbName, validators.size(), stopwatch.elapsed(TimeUnit.MILLISECONDS));

        int writeDbCount = 0;

        boolean hasError = false;
        for (BaseComponentValidator validator : validators) {
            ValidatorError validatorError = validator.validate(context);
            if (validatorError != null) {
                hasError = true;
            }

            // 节点错误
            ProjectComponent component = validator.getComponent().toProjectComponent();
            boolean updated = component.appendValidatorError(validatorError);
            if (updated) {
                ProjectComponentDao.update(conn, dbName, component);
                writeDbCount++;
            }
        }

        LOG.debug("[{}][finish validate with write 2 db count:{}, spend:{} ]", dbName, writeDbCount, stopwatch.elapsed(TimeUnit.MILLISECONDS));

        // 3、更新root上的标记
        ProjectComponent component = ProjectComponentDao.get(conn, dbName, root.getId());
        component.appendReady(!hasError);
        ProjectComponentDao.update(conn, dbName, component);
        LOG.debug("[{}][validate used in millis:{}]", dbName, stopwatch.elapsed(TimeUnit.MILLISECONDS));
    }


    private static Pair<BaseComponentValidator /* root */, List<BaseComponentValidator>> _validators(ComponentValidatorContext context) throws Exception {
        // 1、rest components
        List<RestBaseProjectComponent> rests = context.getCurrentRootComponents();

        // 2 to validator
        List<BaseComponentValidator> validators = _convert2Validator(rests);
        String rootId = context.getRootComponentId();

        // 转成tree
        BaseComponentValidator root = BaseComponentValidator.buildTree(rootId, validators);

        return Pair.of(root, validators);
    }

    private static List<BaseComponentValidator> _convert2Validator(List<RestBaseProjectComponent> rests) {
        List<BaseComponentValidator> validators = new ArrayList<>(rests.size());
        for (RestBaseProjectComponent component : rests) {
            BaseComponentValidator validator = _convert2Validator(component);
            validators.add(validator);
        }
        return validators;
    }

    private static ComponentValidatorContext _buildContext(String componentId, String projectId,
                                                           Connection conn, String dbName) throws Exception {
        LOG.debug("[{}][build - context start build validate context]", dbName);

        List<ProjectComponent> allProjectComponents = _componentService.getProjectComponentWithInternalEntity(projectId, conn, dbName);

        Project project = ProjectDao.get(conn, dbName, projectId);
        Stopwatch stopwatch = Stopwatch.createStarted();
        Pair<String, List<RestBaseProjectComponent>> restPair = _queryRests(componentId, project, allProjectComponents, conn, dbName);
        if (restPair == null) {
            return null;
        }

        LOG.debug("[{}][build - context convert2Rest pair spend:{}]", dbName, stopwatch.elapsed(TimeUnit.MILLISECONDS));
        List<RestBaseProjectComponent> projectComponents = _queryProjectComponents(allProjectComponents, project, dbName);

        List<String> allProjectComponentsInTrashIds = _componentService.loadProjectTrashedComponentId(allProjectComponents);
        //项目下所有不在回收站的节点
        List<RestBaseProjectComponent> allProjectComponentsNotInTrash = projectComponents.stream()
                .filter(item -> !allProjectComponentsInTrashIds.contains(item.getId()))
                .collect(Collectors.toList());

        LOG.debug("[{}][build - context spend:{}]", dbName, stopwatch.elapsed(TimeUnit.MILLISECONDS));

        return ComponentValidatorContext.builder()
                .rootComponentId(restPair.getKey())
                .currentRootComponents(restPair.getValue())
                .projectComponentsNotInTrash(allProjectComponentsNotInTrash)
                .dbName(dbName)
                .build();
    }

    private static List<RestBaseProjectComponent> _queryProjectComponents(List<ProjectComponent> projectComponents,
                                                                          Project project, String dbName)  {
        return _componentService.convertToRest(projectComponents, projectComponents, project, dbName);
    }

    private static BaseComponentValidator _convert2Validator(RestBaseProjectComponent component) {
        switch (component.getType()) {
            case ProjectComponent.TYPE_BOT:
                return new ComponentBotValidator(component);
            case ProjectComponent.TYPE_CONVERSATION:
                return new ComponentConversationValidator(component);
            case ProjectComponent.TYPE_ENTITY:
                return new ComponentEntityValidator(component);
            case ProjectComponent.TYPE_GOTO:
                return new ComponentGotoValidator(component);
            case ProjectComponent.TYPE_USER:
                return new ComponentUserValidator(component);
            case ProjectComponent.TYPE_WEBHOOK:
                return new ComponentWebhookValidator(component);
            case ProjectComponent.TYPE_GPT:
                return new ComponentGptValidator(component);
            default:
                LOG.error("[not support component validate, componentId:{},type:{}]", component.getId(), component.getType());
                throw new RestException(StatusCodes.AiTrainFileInvalidNotSupportValidatorType, component.getType());
        }
    }


    private static Pair<String /* root id*/, List<RestBaseProjectComponent>> _queryRests(String componentId,Project project,
                                                                                         List<ProjectComponent> allProjectComponent,
                                                                                         Connection conn, String dbName) throws Exception {
        // 1、当前节点
        ProjectComponent component = ProjectComponentDao.get(conn, dbName, componentId);
        if (component == null) {
            return null;
        }

        LOG.debug("[{}][convert2Rest - start]", dbName);
        Stopwatch stopwatch = Stopwatch.createStarted();
        // 2、查找root节点
        ProjectComponent root = _componentService.findRootComponent(component, allProjectComponent);
        LOG.debug("[{}][convert2Rest - find root & query all projects spend:{}]", dbName, stopwatch.elapsed(TimeUnit.MILLISECONDS));

        // 3、当前root节点下的components
        List<ProjectComponent> components = _componentService.loadChildren(root, allProjectComponent);
        components.add(root);

        // 这里如果更新global相关，那么会重复
        components = components.stream().distinct().collect(Collectors.toList());

        // 5、convert 2 rest
        LOG.debug("[{}][convert2Rest - load children:{}]", dbName, stopwatch.elapsed(TimeUnit.MILLISECONDS));
        List<RestBaseProjectComponent> rests = _componentService.convertToRest(components, allProjectComponent, project, dbName);
        LOG.debug("[{}][convert2Rest - covert children:{}]", dbName, stopwatch.elapsed(TimeUnit.MILLISECONDS));
        return Pair.of(root.getId(), rests);
    }



    public static void main(String[] args) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        ThreadUtil.sleep(1000);
        System.out.println("消耗时间：" + stopwatch.elapsed(TimeUnit.MILLISECONDS));
        ThreadUtil.sleep(2000);
        System.out.println("消耗时间：" + stopwatch.elapsed(TimeUnit.MILLISECONDS));
    }
}
