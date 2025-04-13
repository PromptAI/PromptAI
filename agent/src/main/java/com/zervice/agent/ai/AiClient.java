package com.zervice.agent.ai;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.zervice.agent.published.project.PublishedProjectPojo;
import com.zervice.agent.utils.ConfConstant;
import com.zervice.common.restful.exception.RestException;
import com.zervice.common.restful.exception.StatusCodes;
import com.zervice.common.utils.HttpClientUtils;
import lombok.*;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClientException;

import java.io.File;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

/**
 * @author Peng Chen
 * @date 2022/6/17
 */
@Log4j2
public class AiClient {

    private final String _url = ConfConstant.AI_BASE_URL;

    /**
     * publish a project
     */
    private static final String _URI_PUBLISH = "/v1/deploy";

    /**
     * chat
     */
    private static final String _URI_CHAT = "/v1/chat";

    private final PublishedProjectPojo _project;


    public AiClient(PublishedProjectPojo project) {
        this._project = project;
    }

    public CommandRes publish(File file) {
        LOG.info("[{}][ request publish wth data:{}]", this._project.getId(), file.getName());

        String url = _url + _URI_PUBLISH;
        JSONObject result;
        try {
            result = HttpClientUtils.postFile(url, file, "file", Map.of(), _defaultHeader());
            LOG.info("[{}][success request deploy wth result:{}]", this._project.getId(), result.toJSONString());
            return CommandRes.ok(result.toJSONString());
        } catch (RestClientException e) {
            LOG.error("[{}][send deploy command to:{} fail:{}]",  this._project.getId(), url, e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            LOG.error("[{}][send deploy command to:{} fail:{}]",  this._project.getId(), url, e.getMessage(), e);
            throw new RestException(StatusCodes.InternalError);
        }
    }


    public JSONArray chat(JSONObject param) {
        LOG.info("[{}][start chat:{}]", _project.getId(), param.toJSONString());

        String url = _url + _URI_CHAT;
        JSONArray result = HttpClientUtils.postJsonForArray(url, param, _defaultHeader());

        LOG.info("[{}][with result:{}]],", _project.getId(), result.toJSONString());
        return result;
    }


    @Setter
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommandRes {

        public static CommandRes error(String message) {
            return factory(Boolean.FALSE, message);
        }

        public static CommandRes ok(String message) {
            return factory(Boolean.TRUE, message);
        }


        public static CommandRes factory(boolean ok, String message) {
            return CommandRes.builder()
                    .ok(ok).message(message)
                    .build();
        }

        private Boolean _ok;

        private String _message;
    }

    @Setter
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModelRes {

        public static ModelRes parse(String res) {
            JSONObject data = JSONObject.parseObject(res);
            if (data.containsKey("state") && data.getIntValue("state") == HttpStatus.OK.value()) {
                String model = data.getJSONObject("data").getString("model");
                return ok(model);
            }

            return error(res);
        }

        private static ModelRes error(String res) {
            return ModelRes.builder()
                    .ok(Boolean.FALSE).error(res)
                    .build();
        }

        public static ModelRes ok(String modelPath) {
            return ModelRes.builder()
                    .ok(Boolean.TRUE).modelPath(modelPath)
                    .possibleModelName(_parseModelName(modelPath))
                    .build();
        }


        public static String _parseModelName(String modelPath) {
            // no model
            if (StringUtils.isBlank(modelPath)) {
                return null;
            }


            try {
                ///app/tmp/1542445650073882624/
                // 1542445650073882624 is train task id
                String[] paths = modelPath.split("/");
                // 20220630-095140.tar.gz
                Optional<String> modelFilename = Arrays.stream(paths).filter(p -> p.contains(".tar.gz")).findAny();
                if (!modelFilename.isPresent()) {
                    LOG.error("parse model file name fail from:{}", modelPath);
                    return null;
                }

                // 20220630-095140
                return modelFilename.get().split("\\.")[0];
            } catch (Exception e) {
                LOG.error("parse taskId fail:{} from modelPath:{}", e.getMessage(), modelPath, e);
                return null;
            }
        }

        private Boolean _ok;

        private String _modelPath;

        private String _error;

        private String _possibleModelName;
    }

    private HttpHeaders _defaultHeader() {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("accountName", _project.getAccountName());
        httpHeaders.add("accountId", _project.getAccountName());
        httpHeaders.add("bot_name", _project.getId());
        return httpHeaders;
    }
}
