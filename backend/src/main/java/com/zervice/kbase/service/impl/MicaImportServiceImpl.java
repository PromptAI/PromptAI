package com.zervice.kbase.service.impl;

import cn.hutool.core.lang.Pair;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.zervice.common.i18n.MessageUtils;
import com.zervice.common.utils.Constants;
import com.zervice.common.utils.TimeUtils;
import com.zervice.kbase.database.dao.ProjectComponentDao;
import com.zervice.kbase.database.dao.ProjectDao;
import com.zervice.kbase.database.pojo.Project;
import com.zervice.kbase.database.pojo.ProjectComponent;
import com.zervice.kbase.database.utils.DaoUtils;
import com.zervice.kbase.service.MicaImportService;
import com.zervice.kbase.service.ProjectService;
import com.zervice.kbase.service.impl.mica.ZipProjectConverter;
import com.zervice.kbase.utils.FileUtils;
import com.zervice.kbase.utils.PythonToJsonConverter;
import com.zervice.kbase.utils.YamlToJsonConverter;
import com.zervice.kbase.validator.ComponentValidatorHelper;
import lombok.Cleanup;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

/**
 * @author chenchen
 * @Date 2024/11/28
 */
@Log4j2
@Service
public class MicaImportServiceImpl implements MicaImportService {

    @Autowired
    private ProjectService projectService;
    @Override
    public Project zip(byte[] zipContent, String fileName, String locale, Long userId, String dbName) throws Exception {
        Pair<JSONObject, JSONObject> dataAndPythonFunctions = _readZipFiles(zipContent);
        JSONObject data = dataAndPythonFunctions.getKey();
        JSONObject pythonFunctions = dataAndPythonFunctions.getValue();

        // read Project
        Project project = _parseZipProject(data, fileName, userId, locale);

        ZipProjectConverter converter = new ZipProjectConverter(data, pythonFunctions, locale);
        List<ProjectComponent> components = converter.generate(project);

        @Cleanup Connection conn = DaoUtils.getConnection(false);
        LOG.info("[{}][start save mica zip project:{} with {} component(s)]", dbName, project.getId(), components.size());

        // 先尝试用文件名称作为项目名，如果重复再生成新的
        _resetProjectNameIfNecessary(project, conn, dbName);

        // saveProject
        projectService.save(project, conn, dbName);

        for (ProjectComponent component : components) {
            ProjectComponentDao.add(conn, dbName, component);
        }

        LOG.info("[{}][finish save mica project:{}]", dbName, project.getId());

        // 校验节点是否正确
        ComponentValidatorHelper.validate(project.getId(), conn, dbName);

        conn.commit();

        return project;
    }
    private void _resetProjectNameIfNecessary(Project project, Connection conn, String dbName) throws Exception {
        String projectName = project.getName();
        boolean nameExist = ProjectDao.getByName(conn, dbName, projectName) != null;
        if (nameExist) {
            projectName = _generateProjectName(projectName);
            project.setName(projectName);
        }
    }


    private Pair<JSONObject /*yaml*/, JSONObject /*python functions*/> _readZipFiles(byte[] zipContent) throws Exception {
        Map<String, String /* 文本内容*/> files = FileUtils.extractFilesFromZip(zipContent);

        StringBuilder yamlContent = new StringBuilder();
        StringBuilder pythonContent = new StringBuilder();
        for (String key : files.keySet()) {
            if (key.equals("yaml") || key.endsWith("yml")) {
                yamlContent.append(files.get(key)).append("\n");
                LOG.info("[read yaml file:{}]", key);
                continue;
            }

            if (key.endsWith("py")) {
                pythonContent.append(files.get(key)).append("\n");
                LOG.info("[read python file:{}]", key);
                continue;
            }

            LOG.warn("[unknown file:{} read from mica zip]", key);
        }

        JSONObject yaml = YamlToJsonConverter.convertYamlToJson(yamlContent.toString());
        JSONObject python = PythonToJsonConverter.convertPythonToJson(pythonContent.toString());

        return Pair.of(yaml, python);
    }

    private Project _parseZipProject(JSONObject data, String fileName, Long userId, String locale) {
        String schedule = _readSchedule(data);
        String welcome = _readWelcome(data, locale);
        String fallback = _readFallback(data, locale);

        Project.Prop prop = Project.Prop.builder()
                .createBy(userId).createTime(System.currentTimeMillis())
                .locale(locale)
                .welcome(welcome)
                .fallback(fallback)
                .schedule(schedule)
                .build();

        return Project.builder()
                .name(fileName)
                .properties(prop)
                .id(Project.generateId())
                .build();
    }

    private String _generateProjectName(String baseName) {
        if (baseName == null) {
            baseName = "import";
        }

        return baseName + "-" + TimeUtils.format("yyMMddHHmmss");
    }

    private String _readSchedule(JSONObject data) {
        String defaultSchedule = Project.Prop.SCHEDULE_PRIORITY;
        JSONArray steps = data.getJSONObject("main").getJSONArray("steps");
        if (steps != null && !steps.isEmpty()) {
            for (int i = 0; i < steps.size(); i++) {
                JSONObject step = (JSONObject) steps.get(i);
                String schedule = step.getString("schedule");
                if (StringUtils.isNotBlank(schedule)) {
                    LOG.info("[read schedule:{}]", schedule);
                    return schedule;
                }
            }
        }

        LOG.warn("[no schedule found use default:{}]", defaultSchedule);
        return Project.Prop.SCHEDULE_PRIORITY;
    }

    private String _readFallback(JSONObject data, String locale) {
        Object fallbackSteps = data.getJSONObject("meta").get("fallback");
        String fallbackStr = null;
        if (fallbackSteps instanceof JSONObject) {
            fallbackStr = ((JSONObject) fallbackSteps).getString("policy");
        }

        return StringUtils.isNotBlank(fallbackStr) ? fallbackStr : MessageUtils.get(Constants.I18N_DEFAULT_PROJECT_FALLBACK, locale);
    }

    private String _readWelcome(JSONObject data, String locale) {
        JSONArray steps = data.getJSONObject("meta").getJSONArray("steps");
        String welcome = _readFirstBotFormStep(steps);
        return StringUtils.isNotBlank(welcome) ? welcome : MessageUtils.get(Constants.I18N_DEFAULT_PROJECT_WELCOME, locale);
    }

    private String _readFirstBotFormStep(JSONArray steps) {
        if (steps == null) {
            return null;
        }

        for (Object step : steps) {
            if (step instanceof JSONObject) {
                JSONObject s = (JSONObject) step;
                if (s.containsKey("bot")) {
                    return s.getString("bot");
                }
            }
        }

        return null;
    }
}
