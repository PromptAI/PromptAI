package com.zervice.kbase;

import com.zervice.common.utils.Constants;
import com.zervice.common.utils.LayeredConf;
import lombok.Getter;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

/**
 * A class represents the information of the server
 */
@UtilityClass
public class ServerInfo {
    private final int _PORT80 = 80;
    private final int _PORT443 = 443;

    @Getter
    private final String _name;

    @Getter
    private final String _host;

    @Getter
    private final int _port;

    @Getter
    private final boolean _ssl;

    @Getter
    private final String _mailTo;

    @Getter
    private final String _base;

    @Getter
    private final String _website;

    @Getter
    private final String _loginAddr;

    @Getter
    private final String _serverAddr;

    //
    // Server base shall always not ends with a '/', so we shall always add '/' to it when creating URIs
    @Getter
    private final String _serverBase;
    static {
        LayeredConf.Config conf = LayeredConf.getConfig(Constants.CONFIG_OBJECT_SERVER);
        _name = conf.getString(Constants.SERVER_NAME, Constants.SERVER_NAME_DEFAULT);
        _host = conf.getString(Constants.SERVER_HOST, Constants.SERVER_HOST_DEFAULT);
        _ssl = conf.getBoolean(Constants.SERVER_SECURE, Constants.SERVER_SECURE_DEFAULT);
        _mailTo = conf.getString(Constants.SERVER_MAILTO, Constants.SERVER_MAILTO_DEFAULT);
        _base = conf.getString(Constants.SERVER_BASE, Constants.SERVER_BASE_DEFAULT);
        _loginAddr = conf.getString(Constants.SERVER_LOGIN_ADDR, Constants.SERVER_LOGIN_ADDR_DEFAULT);
        // Agent using
        _serverAddr = conf.getString(Constants.SERVER_ADDR, Constants.SERVER_ADDR_DEFAULT);

        int port = conf.getInt(Constants.SERVER_PORT, Constants.SERVER_PORT_DEFAULT);
        if(port == 0) {
            _port = _ssl ? _PORT443 : _PORT80;
        }
        else {
            _port = port;
        }

        String serverBase;
        if(_ssl) {
            if(_port == _PORT443) {
                serverBase = "https://" + _host + _base;
            }
            else {
                serverBase = "https://" + _host + ":" + _port + _base;
            }
        }
        else {
            if(_port == _PORT80) {
                serverBase = "http://" + _host + _base;
            }
            else {
                serverBase = "http://" + _host + ":" + _port + _base;
            }
        }

        // make sure it always send with '/'
        if(serverBase.endsWith("/")) {
            _serverBase = serverBase.substring(0, serverBase.length() - 1);
        }
        else {
            _serverBase = serverBase;
        }

        String website = conf.getString(Constants.SERVER_WEBSITE, Constants.SERVER_WEBSITE_DEFAULT);

        if(StringUtils.isEmpty(website)) {
            _website = serverBase;
        }
        else {
            _website = website;
        }
    }
}