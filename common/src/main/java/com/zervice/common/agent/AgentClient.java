package com.zervice.common.agent;

import com.google.common.collect.Maps;
import com.zervice.common.agent.pojo.SimplePublishedProject;
import com.zervice.common.pojo.chat.TimeRecord;
import com.zervice.common.utils.Constants;
import com.zervice.common.utils.HttpClientUtils;
import com.zervice.common.utils.RestClient;
import com.zervice.common.utils.TimeRecordHelper;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpHeaders;

import java.util.*;

@Log4j2
public class AgentClient {
    public static final String MSG_URL = "/api/chat/message";

    protected RestClient normalClient;

    @Setter
    @Getter
    private volatile long _lastCheckTimeMills;
    @Setter
    @Getter
    private volatile long _activeTimeMills;


    /**
     * agent base url
     */
    @Getter
    @Setter
    protected String _id;

    /**
     * agent base url
     */
    @Getter
    @Setter
    protected String _url;
    /**
     * default is ACTIVE
     */
    @Setter
    @Getter
    protected volatile AgentClient.STATUS _status;

    @Setter
    @Getter
    private Set<SimplePublishedProject> _projects;

    public boolean supportProject(String projectId) {
        return getProject(projectId) != null;
    }

    public SimplePublishedProject getProject(String projectId) {
        for (SimplePublishedProject project : _projects) {
            if (projectId.equals(project.getId())) {
                return project;
            }
        }

        return null;
    }

    public void addProjects(List<SimplePublishedProject> projects) {
        if (_projects == null) {
            _projects = new HashSet<>(16);
        }

        _projects.addAll(projects);
    }

    /**
     * kb端调用的
     */
    public void clearProjects() {
        if (_projects == null) {
            _projects = new HashSet<>(16);
            return;
        }

        _projects.clear();

    }

    public enum STATUS {
        /**
         * we don't know
         */
        UNKNOWN,
        /**
         * not assign to account
         */
        FREE,
        /**
         * active
         */
        ACTIVE,
        /**
         * executing tasks...
         */
        EXECUTING,
        /**
         * dead
         */
        DEAD;

        public static STATUS fromName(String str) {
            for (STATUS s : STATUS.values()) {
                if (s.name().equalsIgnoreCase(str)) {
                    return s;
                }
            }
            throw new IllegalArgumentException("Unknown status string " + str);
        }

        public static STATUS valueOf(int ordinal) {
            for (STATUS status : STATUS.values()) {
                if (status.ordinal() == ordinal) {
                    return status;
                }
            }

            throw new IllegalArgumentException("Unknown status ordinal:" + ordinal);
        }
    }

    public AgentClient() {
        this.normalClient = HttpClientUtils.getDefaultRestClient();
    }

    public AgentClient(String id, String url, STATUS status) {
        this();
        this._id = id;
        this._url = url;
        this._status = status;
    }

    public String sendMsg(String chatId, String message, String publishedProjectId) {
        Map<String, String> param = Maps.newHashMapWithExpectedSize(2);
        param.put("sender", chatId);
        param.put("message", message);
        return httpPost(normalClient, MSG_URL, param, publishedProjectId);
    }

    protected String httpPost(RestClient client, String path, Object body, String publishedProjectId) {
        TimeRecordHelper.append(TimeRecord.start(TimeRecord.TYPE_REQUEST_MICA));
      try {
          HttpHeaders httpHeaders = _defaultHeader(publishedProjectId);
          return client.postJson(_url + path, Optional.ofNullable(body).orElse(new Object()), httpHeaders);
      } finally {
          TimeRecordHelper.append(TimeRecord.end(TimeRecord.TYPE_REQUEST_MICA));
      }
    }

    protected String httpDelete(RestClient client, String path, Object body, String publishedProjectId) {
        HttpHeaders httpHeaders = _defaultHeader(publishedProjectId);
        return client.deleteJson(_url + path, Optional.ofNullable(body).orElse(new Object()), httpHeaders);
    }


    private HttpHeaders _defaultHeader(String publishedProjectId) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(Constants.AGENT_ID_HEADER, _id);
        httpHeaders.add(Constants.AGENT_PUBLISHED_PROJECT_HEADER, publishedProjectId);
        return httpHeaders;
    }

    private String _sendHealthCheckMsg() {
        String sender = UUID.randomUUID().toString();
        return sendMsg(sender, "hi", "");
    }

    /**
     * @return null or new status
     */
    public STATUS checkActive() {
//        if (_status != STATUS.ACTIVE && _status != STATUS.DEAD) {
//            return null;
//        }
//
//        _lastCheckTimeMills = System.currentTimeMillis();
//        // check whether current client is active or not
//        int maxCheckTimes = 3;
//        for (int i = 1; i <= maxCheckTimes; i++) {
//            try {
//                String resp = _sendHealthCheckMsg();
//                if (_activeTimeMills <= 0) {
//                    _activeTimeMills = System.currentTimeMillis();
//                }
//                if (_status != STATUS.ACTIVE) {
//                    _status = STATUS.ACTIVE;
//
//                    return _status;
//                }
//                break;
//            }
//            catch (Exception e) {
//                e.printStackTrace();
//                if (i == maxCheckTimes) {
//                    if (_status != STATUS.DEAD) {
//                        _status = STATUS.DEAD;
//                        _activeTimeMills = 0;
//                        LOG.error("Fail to connect Agent client {} is down with error {}", _url, e.getMessage());
//                        return _status;
//                    }
//                }
//                ThreadUtil.sleep(3000);
//            }
//        }

        return null;
    }

    @Override
    public String toString() {
        return "AgentClient{" +
                "normalClient=" + normalClient +
                ", _lastCheckTimeMills=" + _lastCheckTimeMills +
                ", _activeTimeMills=" + _activeTimeMills +
                ", _url='" + _url + '\'' +
                ", _status=" + _status +
                '}';
    }

    public boolean validPublishedProjectAndToken(String projectId, String token) {
        SimplePublishedProject project = this.getProject(projectId);
        if (project == null) {
            LOG.error("publishedProject not exists.id:{}", projectId);
            return false;
        }

        if (!project.validToken(token)) {
            LOG.error("valid token failed. token:{}", token);
            return false;
        }
        return true;
    }
}
