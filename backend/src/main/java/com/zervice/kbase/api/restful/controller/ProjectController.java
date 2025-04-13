package com.zervice.kbase.api.restful.controller;

import cn.hutool.core.collection.ConcurrentHashSet;
import com.alibaba.fastjson.JSONObject;
import com.zervice.common.pojo.common.EmptyResponse;
import com.zervice.common.restful.exception.RestException;
import com.zervice.common.restful.exception.StatusCodes;
import com.zervice.kbase.AccountCatalog;
import com.zervice.kbase.api.BaseController;
import com.zervice.kbase.api.restful.AuthFilter;
import com.zervice.kbase.api.restful.pojo.*;
import com.zervice.kbase.cache.AccountPublishedProjects;
import com.zervice.kbase.database.criteria.ProjectCriteria;
import com.zervice.kbase.database.dao.ProjectComponentDao;
import com.zervice.kbase.database.dao.ProjectDao;
import com.zervice.kbase.database.pojo.*;
import com.zervice.kbase.database.utils.DaoUtils;
import com.zervice.kbase.database.utils.PageResult;
import com.zervice.kbase.service.*;
import com.zervice.kbase.utils.FileUtils;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Cleanup;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * @author Peng Chen
 * @date 2022/6/8
 */
@Log4j2
@RequestMapping(value = "/api/project", headers = AuthFilter.TOKEN)
@RestController
public class ProjectController extends BaseController {

    /**
     * 为防止同时多次点击发布，这里把同一个componentIds缓存起来，
     * 发现多次点击的，直接拒绝
     */
    private static final Set<String> _publishAndTrainCache = new ConcurrentHashSet<>();

    @Autowired
    private PublishedProjectService publishedProjectService;
    @Autowired
    private ProjectService projectService;

    @Autowired
    private MicaImportService micaImportService;

    private BlobService blobService = BlobService.getInstance();

    private ProjectComponentService componentService = ProjectComponentService.getInstance();

    @GetMapping("/public")
    public Object getTemplate(ProjectCriteria criteria,
                              PageRequest pageRequest,
                              @RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName) throws Exception {
        return projectService.getPublicList(dbName, criteria, pageRequest);
    }

    @PostMapping("/public/clone")
    public Object clone(@RequestBody @Validated RestCloneProject project,
                        @RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName,
                        @RequestHeader(AuthFilter.X_EXTERNAL_USER_ID_HEADER) String uid) throws Exception {
        Long userId = User.fromExternalId(uid);
        return projectService.cloneFromPublic(dbName, userId, project);
    }


    @GetMapping
    public Object get(ProjectCriteria criteria,
                      PageRequest pageRequest,
                      @RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName,
                      @RequestHeader(AuthFilter.X_EXTERNAL_USER_ID_HEADER) String uid) throws Exception {
        Long userId = User.fromExternalId(uid);

        @Cleanup Connection conn = DaoUtils.getConnection(true);
        PageResult<Project> pageResult = ProjectDao.get(conn, dbName, criteria, pageRequest);

        List<RestProject> rests = pageResult.getData().stream()
                .map(RestProject::new).collect(Collectors.toList());

        _setRest(rests, dbName);

        return PageResponse.of(pageResult.getTotalCount(), rests);
    }

    @PostMapping("/mica/zip")
    public Object saveFromMicaZip(@RequestBody JSONObject data,
                               @RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName,
                               @RequestHeader(AuthFilter.X_EXTERNAL_USER_ID_HEADER) String uid) throws Exception {
        String fileId = data.getString("id");
        String locale = data.getString("locale");
        Long userId = User.fromExternalId(uid);
        if (StringUtils.isBlank(fileId)) {
            LOG.error("[{}][import project from mica zip failed, fileId:{} not exist]", dbName, fileId);
            throw new RestException(StatusCodes.BadRequest);
        }

        CommonBlob blob = BlobService.getInstance().getBlob(fileId, dbName);
        if (blob == null) {
            LOG.error("[{}][import project from mica zip failed, file:{} not exist]", dbName, fileId);
            throw new RestException(StatusCodes.BadRequest);
        }

        Project project = micaImportService.zip(blob.getContent(), FileUtils.removeExtension(blob.getFileName()), locale, userId, dbName);
        return new RestProject(project);
    }

    @PostMapping
    public Object save(@RequestBody @Validated RestProject project,
                       @RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName,
                       @RequestHeader(AuthFilter.X_EXTERNAL_USER_ID_HEADER) String uid) throws Exception {

        long userId = User.fromExternalId(uid);

        @Cleanup Connection conn = DaoUtils.getConnection(false);

        Project dbProject = projectService.save(project, userId, conn, dbName);

        conn.commit();
        return new RestProject(dbProject);
    }


    @DeleteMapping("{projectId}")
    public Object delete(@PathVariable String projectId,
                         @RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName,
                         @RequestHeader(AuthFilter.X_EXTERNAL_USER_ID_HEADER) String uid) throws Exception {
        long userId = User.fromExternalId(uid);

        @Cleanup Connection conn = DaoUtils.getConnection(false);

        Project project = ProjectDao.get(conn, dbName, projectId);
        if (project == null) {
            LOG.warn("[{}][delete project:{} fail, not found]", dbName, projectId);
            throw new RestException(StatusCodes.AiProjectNotFound, "Not found");
        }
        ProjectDao.delete(conn, dbName, project.getId());

        _onDelete(project, userId, conn, dbName);

        conn.commit();

        return EmptyResponse.empty();
    }



    @PutMapping
    public Object update(@RequestBody @Validated RestProject project,
                         @RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName,
                         @RequestHeader(AuthFilter.X_EXTERNAL_USER_ID_HEADER) String uid) throws Exception {
        long userId = User.fromExternalId(uid);

        if (project.getId() == null) {
            throw new RestException(StatusCodes.AiProjectBadRequest, "id required");
        }

        Project dbProject = projectService.update(project, userId, dbName);

        return new RestProject(dbProject);
    }


    @GetMapping("pre/download")
    public Object preDownLoad(@RequestParam String projectId,
                              @RequestHeader(AuthFilter.X_EXTERNAL_USER_ID_HEADER) String uid,
                              @RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName) throws Exception {
        return DownloadMicaService.getInstance().preDownLoad(projectId, dbName);
    }

    @GetMapping("download")
    public void export(@RequestParam String projectId,
                       @RequestParam(required = false) List<String> componentIds,
                       HttpServletResponse response,
                       @RequestHeader(AuthFilter.X_EXTERNAL_USER_ID_HEADER) String uid,
                       @RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName) throws Exception {
        if (CollectionUtils.isEmpty(componentIds)) {
            throw new RestException(StatusCodes.PublishedProjectTransferProjectRootRequired);
        }

        Long userId = User.fromExternalId(uid);
        DownloadMicaService.getInstance().download(userId, projectId, componentIds, response, dbName, componentService, publishedProjectService);
    }

    @PostMapping("publish")
    public Object publish(@RequestParam String projectId,
                          @RequestParam(required = false) List<String> componentIds,
                          @RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName,
                          @RequestHeader(AuthFilter.X_EXTERNAL_USER_ID_HEADER) String uid) throws Exception {
        long userId = User.fromExternalId(uid);
        try {
            _addPublishCache("publish", componentIds, userId, dbName);

            try {
                String publishedProjectId = PublishedProject.generateId(dbName, projectId);
                return projectService.publish(projectId, publishedProjectId, PublishedProject.PUBLISH_MODEL_SNAPSHOT, componentIds, userId, dbName);
            } catch (Exception e) {
                LOG.error("[{}][start publish project filed. projectId:{}, components:{}, e:{}]",
                        dbName, projectId, componentIds, e.getMessage(), e);
                throw e;
            }
        } finally {
            _removePublishCache("publish", componentIds, dbName);
        }
    }

    @PostMapping("train")
    public Object train(@RequestParam String projectId,
                        @RequestParam List<String> componentIds,
                        @RequestParam(required = false, defaultValue = "0") Long schedule,
                        @RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName,
                        @RequestHeader(AuthFilter.X_EXTERNAL_USER_ID_HEADER) String uid) throws Exception {
        long userId = User.fromExternalId(uid);
        try {

            _addPublishCache("train", componentIds, userId, dbName);

            try {
                String publishedProjectId = PublishedProject.generateId(dbName, PublishedProject.TEST_PROJECT_ID_SUFFIX);
                return projectService.publish(projectId, publishedProjectId, PublishedProject.PUBLISH_MODEL_DEFAULT, componentIds, userId, dbName);
            } catch (RestException restException) {
                // 忽略已知问题的训练失败
                throw restException;
            } catch (Exception e) {
                LOG.error("[{}][start train filed. projectId:{}, components:{}, e:{}]",
                        dbName, projectId, componentIds, e.getMessage(), e);
                throw e;
            }
        } finally {
            _removePublishCache("train", componentIds, dbName);
        }
    }

    @DeleteMapping("stop/{projectId}")
    public Object stop(@PathVariable String projectId,
                       @RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName,
                       @RequestHeader(AuthFilter.X_EXTERNAL_USER_ID_HEADER) String uid) throws Exception {
        long userId = User.fromExternalId(uid);
        String publishedProjectId = PublishedProject.generateId(dbName, projectId);
        return publishedProjectService.stop(publishedProjectId, userId, dbName);
    }

    private synchronized void _addPublishCache(String type, List<String> componentIds, long userId, String dbName) {
        String key = _convert2PublishKey(type, componentIds, dbName);
        if (!_publishAndTrainCache.add(key)) {
            LOG.warn("[{}][user:{} click {} too fast, reject!!!]", dbName, userId, type);
            throw new RestException(StatusCodes.CLICK_TOO_FAST);
        }
    }

    private synchronized void _removePublishCache(String type, List<String> componentIds, String dbName) {
        // 这给id排个序，排出顺序的差异
        String key = _convert2PublishKey(type, componentIds, dbName);
        _publishAndTrainCache.remove(key);
    }

    /**
     *  这给id排个序，排出顺序的差异
     */
    private String _convert2PublishKey(String type, List<String> componentIds, String dbName) {
        String component = CollectionUtils.isEmpty(componentIds) ? "" : JSONObject.toJSONString(new TreeSet<>(componentIds));
        return dbName + type + component;
    }



    @GetMapping("{id}")
    public Object get(@PathVariable String id,
                      @RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName,
                      @RequestHeader(AuthFilter.X_EXTERNAL_USER_ID_HEADER) String uid) throws Exception {
        Long userId = User.fromExternalId(uid);

        @Cleanup Connection conn = DaoUtils.getConnection(true);
        Project dbProject = ProjectDao.get(conn, dbName, id);
        if (dbProject == null) {
            LOG.warn("[{}][project:{} not found]", dbName, id);
            throw new RestException(StatusCodes.AiProjectNotFound, "not found");
        }

        String publishedId = PublishedProject.generateId(dbName, dbProject.getId());
        String debugPublishedProjectId = PublishedProject.generateDebugPublishId(dbName);
        RestPublishedProject published = publishedProjectService.get(publishedId, conn, dbName);
        RestPublishedProject debug = publishedProjectService.get(debugPublishedProjectId, conn, dbName);

        RestProject restProject = new RestProject(dbProject, debug, published);

        // set webhook
        Map<String, RestProjectComponentWebhook> webhookMap = ProjectComponentDao.getByProjectIdAndType(conn,
                dbName, id, ProjectComponent.TYPE_WEBHOOK).stream()
                .map(RestProjectComponentWebhook::new)
                .collect(Collectors.toMap(RestProjectComponentWebhook::getId, w -> w));
        componentService.setWebhook(restProject.getWebhooks(), webhookMap);

        return restProject;
    }

    private void _setRest(List<RestProject> rests, String dbName) {
        // set related published project
        AccountPublishedProjects projects = AccountCatalog.ensure(dbName).getPublishedProjects();
        for (RestProject p : rests) {
            // set related publish project
            String publishedProjectId = PublishedProject.generateId(dbName, p.getId());
            PublishedProject publishedProject = projects.get(publishedProjectId);
            if (publishedProject != null) {
                p.setPublishedProject(new RestPublishedProject(publishedProject));
            }
        }
    }


    /**
     * 做资源清理
     */
    private void _onDelete(Project project, long userId, Connection conn, String dbName) throws Exception {
        String projectId = project.getId();
        // 删除对应的组建
        ProjectComponentDao.deleteByProjectId(conn, dbName, projectId);

        // 删除发布的项目及通知agent 删除对应的容器
        publishedProjectService.release(projectId, userId, conn, dbName);

    }


}
