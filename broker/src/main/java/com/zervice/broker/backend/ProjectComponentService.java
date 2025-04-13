package com.zervice.broker.backend;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.zervice.common.pojo.chat.*;
import com.zervice.common.utils.TimeRecordHelper;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author chen
 * @date 2022/8/30
 */
@Log4j2
public class ProjectComponentService {

    private ProjectComponentService() {
    }

    private BackendClient _backendClient;
    private static ProjectComponentService _instance = new ProjectComponentService();

    public static ProjectComponentService getInstance() {
        return _instance;
    }

    public void init(BackendClient backendClient) {
        _backendClient = backendClient;
    }


    private final String URI_COMPONENT = "/rpc/project/component/";
    private final String URI_COMPONENT_ENTITY = "/rpc/project/component/entity/";

    public List<ProjectComponentEntityPojo> entities(String chatId, String projectId, String accountName) {
        String uri = URI_COMPONENT_ENTITY + projectId + "?chatId=" + chatId;

        TimeRecordHelper.append(TimeRecord.start(TimeRecord.TYPE_REQUEST_KB_ENTITY));
        try {
            String res = _backendClient.getString(accountName, uri);
            List<BaseComponentPojo> componentPojos = parseFromRes(res, chatId);
            return componentPojos.stream()
                    .filter(e -> e instanceof ProjectComponentEntityPojo)
                    .map(e -> (ProjectComponentEntityPojo) e)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            LOG.error("[{}][get project:{} entities fail:{}]", chatId, projectId, e.getMessage(), e);
            return null;
        } finally {
            TimeRecordHelper.append(TimeRecord.end(TimeRecord.TYPE_REQUEST_KB_ENTITY));
        }
    }


    private List<BaseComponentPojo> parseFromRes(String res, String chatId) {
        JSONArray results = JSONArray.parseArray(res);
        if (results.isEmpty()) {
            return Collections.emptyList();
        }

        List<BaseComponentPojo> r = new ArrayList<>(results.size());
        for (int i = 0; i < results.size(); i++) {
            JSONObject item = results.getJSONObject(i);
            r.add(BaseComponentPojo.convert(item, chatId));
        }

        return r;
    }

    public BaseComponentPojo get(String chatId, String componentId,
                                 String projectId, String accountName) {
        String uri = URI_COMPONENT + componentId + "?projectId=" + projectId
                + "&chatId=" + chatId;
        TimeRecordHelper.append(TimeRecord.start(TimeRecord.TYPE_REQUEST_KB_COMPONENT));
        try {
            String res = _backendClient.getString(accountName, uri);
            JSONObject result = JSONObject.parseObject(res);
            // not found
            if (result.isEmpty()) {
                return null;
            }
            return BaseComponentPojo.convert(result, chatId);
        } catch (Exception e) {
            LOG.error("[{}][load component:{} fail:{}]", chatId, componentId, e.getMessage(), e);
            return null;
        } finally {
            TimeRecordHelper.append(TimeRecord.end(TimeRecord.TYPE_REQUEST_KB_COMPONENT));
        }
    }


}
