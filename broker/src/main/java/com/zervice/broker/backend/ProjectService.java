package com.zervice.broker.backend;

import com.alibaba.fastjson.JSONObject;
import com.zervice.common.i18n.MessageUtils;
import com.zervice.common.utils.Constants;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class ProjectService {

    private BackendClient _backendClient = null;


    private static final String _URI_WELCOME = "/rpc/project/welcome";
    private static final String _URI_FALLBACK = "/rpc/project/fallback";

    private static ProjectService _instance = new ProjectService();


    private ProjectService() {
    }

    public static ProjectService getInstance() {
        return _instance;
    }

    public void init(BackendClient backendClient) {
        this._backendClient = backendClient;
    }

    public String welcome(String projectId, String chatId, String publishedProjectId, String accountName) {
        String uri = _URI_WELCOME
                + "?publishedProjectId=" + publishedProjectId
                + "&projectId=" + projectId
                + "&chatId=" + chatId;

        try {
            JSONObject res = _backendClient.getJson(accountName, uri);
            if (res.containsKey("welcome")) {
                return res.getString("welcome");
            }
        } catch (Exception e) {
            LOG.error("[{}:{}]get welcome from backend .publishedProjectId:{} error:{}",
                    accountName, projectId, publishedProjectId, e.getMessage(), e);

        }

        return MessageUtils.get(Constants.I18N_DEFAULT_PROJECT_WELCOME);
    }

    public JSONObject fallback(String projectId, String chatId, String publishedProjectId, String accountName) {
        String uri = _URI_FALLBACK + "?publishedProjectId=" + publishedProjectId +
                "&projectId=" + projectId + "&chatId=" + chatId;

        JSONObject result = new JSONObject();
        try {
            JSONObject res = _backendClient.getJson(accountName, uri);
            if (res.containsKey("fallback")) {
                result.put("fallback", res.getString("fallback"));
                result.put("fallbackButtons", res.getJSONArray("fallbackButtons"));
                return result;
            }

        } catch (Exception e) {
            LOG.error("[{}:{}:{}]get fallback from backend .publishedProjectId:{} error:{}",
                    accountName, projectId, chatId, publishedProjectId, e.getMessage(), e);
        }

        result.put("fallback", MessageUtils.get(Constants.I18N_DEFAULT_PROJECT_FALLBACK));
        return result;
    }

}
