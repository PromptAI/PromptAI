package com.zervice.broker.backend;

import cn.hutool.core.thread.ThreadUtil;
import com.alibaba.fastjson.JSONObject;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.zervice.broker.restful.filter.BrokerFilter;
import com.zervice.common.utils.HttpClientUtils;
import com.zervice.common.utils.LayeredConf;
import com.zervice.common.utils.ServletUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpHeaders;

import java.io.File;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

@Log4j2
@Setter
@Getter
public class BackendClient {

    private final String _url;

    private final String _authCode;

    /**
     * default is active
     */
    private volatile STATUS _status = STATUS.ACTIVE;

    private volatile long _lastCheckTimeMills;

    private volatile long _activeTimeMills;

    /** max check time */
    private final int _MAX_CHECK_TIMES = 2;


    public enum STATUS {
        /** active */
        ACTIVE,
        /** dead */
        DEAD
    }

    public BackendClient(String url, String authCode) {
        this._url = url;
        this._authCode = authCode;

        int checkIntervalInSec = LayeredConf.getInt("flow.service.check.intervalInSec", 60);
        boolean checkEnable = LayeredConf.getBoolean("flow.service.check.enable", true);

        if (checkEnable) {
            ThreadFactory factory = new ThreadFactoryBuilder().setNameFormat("check-flow-active-" + this._url)
                    .setDaemon(true)
                    .build();
            ScheduledExecutorService checkExecutors = Executors.newSingleThreadScheduledExecutor(factory);

            checkExecutors.scheduleAtFixedRate(this::_checkActive,
                    0, checkIntervalInSec, TimeUnit.SECONDS);
            LOG.info("flow connectivity check is enable for client {}", this._url);
            return;
        }

        LOG.info("Wont check flow connectivity for client {}", this._url);
    }

    private void _checkActive() {
        _lastCheckTimeMills = System.currentTimeMillis();
        // check whether current client is active or not
        for (int i = 1; i <= _MAX_CHECK_TIMES; i++) {
            try {
                String resp = getString("/api/version");
                LOG.debug("flow check active response for url={}, resp={}", _url, resp);
                _status = STATUS.ACTIVE;
                if (_activeTimeMills == 0) {
                    _activeTimeMills = System.currentTimeMillis();
                }
                LOG.debug("flow client {} is okay", _url);
                break;
            } catch (Exception e) {
                if (i == _MAX_CHECK_TIMES) {
                    _status = STATUS.DEAD;
                    _activeTimeMills = 0;
                    LOG.error("Fail to connect flow client {} is DOWN, error={}", _url, e.getMessage());
                    continue;
                }

                LOG.warn("Fail to connect to flow client {} with err {}. will retry ", _url, e.getMessage());
                ThreadUtil.sleep(1000);
            }
        }
    }


    public String getString(String uri) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(BrokerFilter.TOKEN, _authCode);
//        LOG.info("Request flow {} with uri {}", this._url, uri);
        return HttpClientUtils.getString(this._url + uri, httpHeaders);
    }

    public String getString(String account, String uri) {
        HttpHeaders httpHeaders = createHeader(account);
        LOG.info("Request flow {} with uri {}", this._url, uri);
        return HttpClientUtils.getString(this._url + uri, httpHeaders);
    }

    public JSONObject getJson(String account, String uri) {
        HttpHeaders h = createHeader(account);
        LOG.info("Request flow {} with uri {}", this._url, uri);
        return HttpClientUtils.getJson(this._url + uri, h);
    }

    public JSONObject postFile(String account, String uri, File file, Map<String, Object> param) {
        HttpHeaders h = createHeader(account);
        return HttpClientUtils.postFile(this._url + uri, file, "file", param, h);
    }

    public JSONObject postJson(String account, String uri, Object body) {
        HttpHeaders h = createHeader(account);
        LOG.info("Request flow {} with uri {}", this._url, uri);
        return HttpClientUtils.postJson(this._url + uri, body, h);
    }

    public JSONObject putJson(String account, String uri, Object body) {
        HttpHeaders h = createHeader(account);
        LOG.info("Request flow {} with uri {}", this._url, uri);
        return HttpClientUtils.putJson(this._url + uri, body, h);
    }

    public HttpHeaders createHeader(String dbName) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(BrokerFilter.X_EXTERNAL_ACCOUNT_ID_HEADER, dbName);
        httpHeaders.add(BrokerFilter.TOKEN, _authCode);
        httpHeaders.add("Accept-Language", ServletUtils.getLocale().toString());
        return httpHeaders;
    }

    public String getUrl() {
        return _url;
    }

}
