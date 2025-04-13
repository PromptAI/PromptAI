package com.zervice.kbase.service;

import com.zervice.common.i18n.MessageUtils;
import com.zervice.common.utils.DownloadUtil;
import com.zervice.common.utils.TimeUtils;
import com.zervice.kbase.ai.convert.PublishConvertor;
import com.zervice.kbase.ai.output.zip.MicaZipConverter;
import com.zervice.kbase.api.restful.pojo.RestDownloadMica;
import com.zervice.kbase.database.dao.ProjectComponentDao;
import com.zervice.kbase.database.dao.ProjectDao;
import com.zervice.kbase.database.dao.PublishedProjectDao;
import com.zervice.kbase.database.pojo.Project;
import com.zervice.kbase.database.pojo.ProjectComponent;
import com.zervice.kbase.database.pojo.PublishedProject;
import com.zervice.kbase.database.utils.DaoUtils;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Cleanup;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.sql.Connection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author chen
 * @date 2022/10/27
 */
@Log4j2
public class DownloadMicaService {

    private final static DownloadMicaService instance = new DownloadMicaService();

    private DownloadMicaService() {

    }

    public static DownloadMicaService getInstance() {
        return instance;
    }


    private File _generateMicaFile(Project project, PublishedProject publishedProject, List<String> componentIds,
                                    ProjectComponentService projectComponentService, Connection conn, String dbName) throws Exception {
        List<ProjectComponent> projectComponents = projectComponentService.getProjectComponentWithInternalEntity(project.getId(), conn, dbName);
        String json = PublishConvertor.convert(project, publishedProject, componentIds, projectComponents, conn, dbName);
        return MicaZipConverter.convert(json);
    }



    public void download(Long userId, String projectId, List<String> componentIds,
                         HttpServletResponse response, String dbName,
                         ProjectComponentService projectComponentService,
                         PublishedProjectService publishedProjectService) throws Exception {

        @Cleanup Connection conn = DaoUtils.getConnection(true);
        Project project = ProjectDao.get(conn, dbName, projectId);

        // 1、准备环境
        String debugPublishProjectId = PublishedProject.generateDebugPublishId(dbName);
        publishedProjectService.preparePublishedProject(userId, projectId, debugPublishProjectId,
                PublishedProject.PUBLISH_MODEL_DEFAULT, dbName);
        PublishedProject debugProject = PublishedProjectDao.get(conn, dbName, debugPublishProjectId);

        File miacZipFile = _generateMicaFile(project, debugProject, componentIds, projectComponentService, conn, dbName);

        String downloadFileName = _generateMicaDownloadName(project, componentIds, conn, dbName);
        downloadFileName += ".zip";

        DownloadUtil.download(response, miacZipFile, downloadFileName);
    }

    public List<RestDownloadMica> preDownLoad(String projectId, String dbName) throws Exception {
        @Cleanup Connection conn = DaoUtils.getConnection(true);

        List<ProjectComponent> roots = ProjectComponentDao.getByProjectIdAndTypes(conn, dbName, projectId, ProjectComponent.TYPE_ROOTS);

        return roots.stream().map(root -> {
                    String name = root.parseName();
                    String error = root.parseError();
                    List<String> errors = error == null ? List.of() : List.of(error);

                    return RestDownloadMica.builder()
                            .id(root.getId()).name(name)
                            .type(root.getType()).isReady(root.parseIsReady())
                            .error(errors)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private String _generateMicaDownloadName(Project project, List<String> componentIds,
                                             Connection conn, String dbName) throws Exception {
        String projectName = StringUtils.isNotBlank(project.getName()) ? StringUtils.trim(project.getName()) : "-";
        if (componentIds.size() == 1) {
            ProjectComponent component = ProjectComponentDao.get(conn, dbName, componentIds.get(0));
            if (component != null && StringUtils.isNotBlank(component.parseName())) {
                return projectName + "-" + TimeUtils.format(System.currentTimeMillis(), TimeUtils.PATTERN_DAY);
            }
        }

        return projectName + "-" + MessageUtils.getMessage("download.mica.multi") + "-" + TimeUtils.format(System.currentTimeMillis(), TimeUtils.PATTERN_DAY);
    }
}
