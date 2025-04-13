package com.zervice.kbase.ai.output.zip.pojo;

import cn.hutool.core.io.FileUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.zervice.common.utils.JSONUtils;
import com.zervice.kbase.ai.output.pojo.*;
import com.zervice.kbase.utils.YamlUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


/**
 * 将 json  转成 所需的 files
 * <p>
 * - config.yml: bot相关的配置信息
 * - agents.yml: flow agents & llm Agents  & meta
 * - xx.py     : function callings  & pythons code
 *
 * @author chenchen
 * @author chenchen
 * @Date 2025/1/3
 */
@Setter
@Getter
@Log4j2
@AllArgsConstructor
public class ZipContainer extends Bot {

    public static final String TYPE_LLM_AGENT = "llm agent";
    public static final String TYPE_FLOW_AGENT = "flow agent";
    public static final String TYPE_ENSEMBLE_AGENT ="ensemble agent";
    public static final String TYPE_FUNCTION = "function";

    public List<File> _toFiles() {
        // config.yml
        File config = _config();

        // agents.yml
        File agents = _agents();

        // xxx.py
        File python = _pythons();

        return List.of(config, agents, python);
    }

    /**
     * 使用完成后及时清理文件
     */
    public File zip() {
        String filename = "/tmp/" + _botName + ".zip";

        List<File> files = _toFiles();

        File zipFile = new File(filename);

        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            for (File file : files) {
                if (!file.exists() || !file.isFile()) {
                    System.err.println("Skipping invalid file: " + file.getAbsolutePath());
                    continue;
                }
                try (FileInputStream fis = new FileInputStream(file)) {
                    ZipEntry zipEntry = new ZipEntry(file.getName());
                    zos.putNextEntry(zipEntry);

                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = fis.read(buffer)) >= 0) {
                        zos.write(buffer, 0, length);
                    }
                }
            }
        } catch (IOException e) {
            LOG.error("[convert:{} to zip file with error:{}]", JSONObject.toJSONString(this), e.getMessage(), e);
            throw new RuntimeException(e);
        }

        return zipFile;
    }


    private File _pythons() {
        String filename = "libs.py";

        String fileContent = String.join("\n", _buildPythons());
        return FileUtil.writeString(fileContent, filename, StandardCharsets.UTF_8);
    }

    private List<String> _buildPythons() {
        List<String> results = new ArrayList<>();

        for (Python python : _data.getEnsembleAgents().getPythons()) {
            results.add(python.getBody());

        }

        for (Function function : _data.getEnsembleAgents().getFunctions()) {
            results.add(function.getBody());
        }

        return results;
    }


    /**
     * build Config file
     *
     * <pre>
     *     bot_name:xxx,
     *     gptConfig:{}
     * </pre>
     */
    private File _config() {
        String filename = "config.yml";

        JSONObject config = new JSONObject(true);
        config.put("bot_name", _botName);
        config.put("gptConfig", JSONUtils.toJsonObject(_llmConfig));

        String fileContent = YamlUtil.dump(config);
        return FileUtil.writeString(fileContent, filename, StandardCharsets.UTF_8);
    }


    /**
     * build agents file
     * <pre>
     *  agents: {}
     *  pythons:{}
     *  functions:{}
     *
     * </pre>
     */
    private File _agents() {
        String filename = "agents.yml";

        JSONObject agents = new JSONObject(true);

        // flow agents  & llm agents
        for (JSONObject agent : _buildAgents()) {
            agents.put(agent.remove("name").toString(), agent);
        }

        // meta
        agents.put("meta", _buildMeta());

        // main
        agents.put("main", _buildMain());

        String fileContent = YamlUtil.dump(agents, true);
        return FileUtil.writeString(fileContent, filename, StandardCharsets.UTF_8);
    }

    private List<JSONObject> _buildPyReferences() {
        List<JSONObject> results = new ArrayList<>();

        for (Python python : _data.getEnsembleAgents().getPythons()) {
            results.add(_buildPythonRef(python));
        }

        for (Function function : _data.getEnsembleAgents().getFunctions()) {
            results.add(_buildFunctionRef(function));
        }

        return results;
    }


    private JSONObject _buildFunctionRef(Function function) {
        JSONObject f = new JSONObject(true);
        f.put("name", function.getName());
        f.put("type", TYPE_FUNCTION);
        return f;
    }

    /**
     * webhook 也专成function
     */
    private JSONObject _buildPythonRef(Python python) {
        JSONObject py = new JSONObject(true);
        py.put("name", python.getName());
        py.put("type", TYPE_FUNCTION);
        py.put("args", python.getArgs());

        return py;
    }

    /**
     * flow & llm Agents
     */
    private List<JSONObject> _buildAgents() {
        List<JSONObject> results = new ArrayList<>(16);

        for (FlowAgent flow : _data.getEnsembleAgents().getFlowAgents()) {
            results.add(_buildFlowAgent(flow));
        }

        for (LlmAgent llm : _data.getEnsembleAgents().getLlmAgents()) {
            results.add(_buildLlmAgent(llm));
        }

        return results;
    }

    private JSONObject _buildLlmAgent(LlmAgent llm) {
        JSONObject llmAgent = new JSONObject(true);

        llmAgent.put("name", llm.getName());
        llmAgent.put("type", TYPE_LLM_AGENT);
        llmAgent.put("description", llm.getDescription());
        llmAgent.put("prompt", llm.getPrompt());
        llmAgent.put("args", llm.buildArgs());
        llmAgent.put("uses", llm.getUses());
        return llmAgent;
    }


    private JSONObject _buildFlowAgent(FlowAgent flow) {
        JSONObject flowAgent = new JSONObject(true);

        flowAgent.put("name", flow.getName());
        flowAgent.put("type", TYPE_FLOW_AGENT);
        flowAgent.put("description", flow.getDescription());
        flowAgent.put("args", _buildFlowSates(flow.getStates()));
        flowAgent.put("steps", _buildFlowFromStepsAndSubflows(flow.getSteps(), flow.getSubflows()));
        flowAgent.put("fallback", List.of("policy"));
        return flowAgent;
    }

    private JSONObject _buildMeta() {
        EnsembleAgent.Meta m = _data.getEnsembleAgents().getMeta();

        JSONObject meta = new JSONObject(true);
        meta.put("type", TYPE_ENSEMBLE_AGENT);
        meta.put("description", m.getDescription());
        meta.put("contains", m.getContain());
        meta.put("steps", _buildFlowSteps(m.getSteps(), false));
        meta.put("fallback", m.getFallback());
        return meta;
    }

    private JSONObject _buildMain() {
        EnsembleAgent.Main m = _data.getEnsembleAgents().getMain();
        JSONObject main = new JSONObject(true);
        main.put("steps", _buildMainStapes(m.getCall(),m.getSchedule()));
        return main;
    }

    private JSONArray _buildMainStapes(String call,String  schedule) {
        JSONObject callStep = new JSONObject();
        callStep.put("call",call);
        callStep.put("schedule", schedule);
        JSONArray steps = new JSONArray();
        steps.add(callStep);
        return steps;
    }


}
