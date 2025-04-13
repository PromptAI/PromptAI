package com.zervice.kbase.ai.convert;

import com.zervice.common.restful.exception.RestException;
import com.zervice.common.restful.exception.StatusCodes;
import com.zervice.kbase.AccountCatalog;
import com.zervice.kbase.ai.convert.pojo.Publish;
import com.zervice.kbase.api.restful.pojo.RestBaseProjectComponent;
import com.zervice.kbase.database.pojo.Project;
import com.zervice.kbase.database.pojo.ProjectComponent;
import com.zervice.kbase.database.pojo.PublishedProject;
import com.zervice.kbase.service.ProjectComponentService;
import lombok.extern.log4j.Log4j2;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 新版 chat 服务发布
 *
 * @author chenchen
 * @Date 2024/8/19
 */
@Log4j2
public class PublishConvertor {

    public static String convert(Project project, PublishedProject publishedProject, List<String> publishedRoots,
                                 List<ProjectComponent> components, Connection conn, String dbName) {

        AccountCatalog account = AccountCatalog.ensure(dbName);

        // build root tree
        List<RestBaseProjectComponent> roots = buildTree(project.getId(), components, publishedRoots, conn, dbName);

        // build context
        PublishContext context = PublishContext.init(account, project, publishedProject, publishedRoots, roots, components);

        // publish
        return Publish.factory(context).toString();
    }

    public static List<RestBaseProjectComponent> buildTree(String projectId,
                                                           List<ProjectComponent> components,
                                                           List<String> publishedRoots,
                                                           Connection conn, String dbName) {
        List<RestBaseProjectComponent> rests =  ProjectComponentService.getInstance().convertToRest(components, projectId, conn, dbName);

        // convert to map
        Map<String /* component id */ , RestBaseProjectComponent> restComponentMap = rests.stream()
                .collect(Collectors.toMap(RestBaseProjectComponent::getId, c -> c));

        List<RestBaseProjectComponent> roots = new ArrayList<>(publishedRoots.size());

        // build each root tree
        for (String rootComponentId : publishedRoots) {
            RestBaseProjectComponent rootComponent = restComponentMap.get(rootComponentId);
            if (rootComponent == null) {
                LOG.error("[{}:{}][build train file error, root component not found:{}]", dbName, projectId, rootComponentId);

                throw new RestException(StatusCodes.BadRequest);
            }

            RestBaseProjectComponent.filterChild(rootComponentId, rootComponent, rests);

            roots.add(rootComponent);
        }

        return roots;
    }
}
