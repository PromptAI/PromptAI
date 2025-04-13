package com.zervice.common.utils;

import java.util.HashSet;
import java.util.Set;

public final class Constants {
    public static String COREDB = "kbcoredb";
    /**
     * Deploying mode. By default we deploy in standalone mode, so we will not check the account name on the urls, etc.
     */
    public static String MODE_STANDALONE = "standalone";
    public static String MODE_MULTITENANT = "multitenant";

    /**
     *  time zone id of GMT+8
     */
    public static String ZONE_ID_GMT_8 = "GMT+8";

    /**
     * agent related
     */
    public static final String AGENT_ID_HEADER = "X-agent-id";
    public static final String AGENT_AK_HEADER = "X-agent-ak";
    public static final String AGENT_PUBLISHED_PROJECT_HEADER = "X-published-project-id";
    public static final String AGENT_PUBLISHED_PROJECT_TOKEN_HEADER = "X-published-project-token";

    public static final String AGENT_ID = "ai.agentId";
    public static final String AGENT_HOSTNAME = "ai.hostname";
    public static final String AGENT_VERSION = "ai.agentVersion";
    public static final String AGENT_PUBLIC_URL = "ai.publicUrl";
    public static final String AGENT_CURRENT_MODEL = "ai.currentModel";
    public static final String AGENT_HAS_GPU = "ai.hasGpu";
    public static final String AGENT_CAN_USE_GPU = "ai.canUseGpu";
    public static final String AGENT_IMAGE_ID = "ai.imageId";

    /**
     * frp config
     */
    public static final String AGENT_FRP_SERVE_ADDRESS = "agent.frp.server.address";
    public static final String AGENT_FRP_SERVE_PORT = "agent.frp.server.port";
    public static final String AGENT_FRP_SERVE_SECURE = "agent.frp.server.secure";
    public static final String AGENT_FRP_AUTHENTICATION_METHOD = "agent.frp.Authentication.Method";
    public static final String AGENT_FRP_AUTHENTICATION_TOKEN = "agent.frp.Authentication.token";
    public static final String AGENT_FRP_DOWNLOAD_URL = "agent.frp.client.download.url";

    /**
     *  task step
     */
    public static final String TYPE_DOWNLOAD = "DOWNLOAD";
    public static final String TYPE_MOVE_FILE_TO_DIR = "MOVE_FILE_TO_DIR";
    public static final String TYPE_REPLACE_MODEL = "REPLACE_MODEL";
    public static final String TYPE_CHECK_MODEL = "CHECK_MODEL";
    public static final String TYPE_DEPLOY = "DEPLOY";
    public static final String TYPE_UPLOAD = "UPLOAD";
    public static final String TYPE_UNLOAD_MODEL = "UNLOAD_MODEL";
    public static final String TYPE_REPORT_MODEL = "REPORT_MODEL";


    public static final String BUTTON_PREFIX = "/";
    public static final String FALLBACK_BOT_ID = "fallback";
    public static final String ROOT_TYPE_PROJECT = "project";
    public static final String REPLY_TYPE_TEXT = "text";

    /** 主动终止任务时给的message **/
    public static final String TASK_TERMINATED_MESSAGE = "Remote terminate...";

    /**
     * project component
     */
    public static final String ROOT_COMPONENT_ID_UNKNOWN = "unknown";

    /**
     * TODO: in the future we won't require this. But for now, we need this to do test!!!
     * Give information about the server so entities like agent could be able to now how to access service
     */
    public final static String CONFIG_OBJECT_SERVER = "zbot.server";
    public final static String SERVER_NAME = "name";
    public final static String SERVER_NAME_DEFAULT = "Prompt AI";
    public final static String SERVER_HOST = "host";
    public final static String SERVER_HOST_DEFAULT = "www.promptai.cn";
    public final static String SERVER_LOGIN_ADDR_DEFAULT = "https://app.promptai.us";
    public final static String SERVER_PORT = "port";
    public final static int SERVER_PORT_DEFAULT = 0;
    public final static String SERVER_SECURE = "secure";
    public final static boolean SERVER_SECURE_DEFAULT = true;
    public final static String SERVER_BASE = "baseUrl";
    public final static String SERVER_LOGIN_ADDR = "serverLoginAddr";
    public final static String SERVER_ADDR = "serverAddr";
    public final static String SERVER_ADDR_DEFAULT = "https://app.promptai.cn";
    /*// in format of /foo, empty if no base */
    public final static String SERVER_BASE_DEFAULT = "";
    public final static String SERVER_WEBSITE = "website";
    public final static String SERVER_WEBSITE_DEFAULT = "";
    public final static String SERVER_MAILTO = "mailTo";
    public final static String SERVER_MAILTO_DEFAULT = "info@promptai.us";


    /**
     * system
     */
    public static final String SYSTEM_RUN_MODEL_KEY = "SYSTEM_RUN_MODEL";
    /* docker model*/
    public static final String SYSTEM_RUN_MODEL_DOCKER = "DOCKER";

    /**
     * bot 回复的默认delay
     * 单位：毫秒
     */
    public static final Long BOT_REPLY_DELAY_DEFAULT = 500L;

    /**
     * bot 回复的最大delay
     * 单位：毫秒
     */
    public static final Long BOT_REPLY_DELAY_MAX = 5000L;
    /** 默认的项目欢迎语 **/
    public static final String I18N_DEFAULT_PROJECT_WELCOME = "project.default.welcome";
    /**  默认的项目失败回答 **/
    public static final String I18N_DEFAULT_PROJECT_FALLBACK = "project.default.fallback";
    /**  默认回复名称 **/
    public static final String I18N_FALLBACK_NAME = "fallback.name";
    /** 大语言模型名称 **/
    public static final String I18N_LLM_NAME = "llm.name";
    /** 默认的会话欢迎语后缀 **/
    public static final String I18N_DEFAULT_CONVERSATION_WELCOME_SUFFIX = "conversation.default.welcome.suffix";
    /** 已删除 **/
    public static final String I18N_DELETED = "deleted";

    public static final String I18N_YAML_USER_TEMPLATE = "yaml.user.template";

    /**
     * 删除Agent的命令
     */
    public static final String I18N_DELETE_AGENT_CMD = "agent.delete.cmd";
    public static final Set<String> LANGUAGE = new HashSet<>();
    public static final Set<String> TIME_ZONE = new HashSet<>();

    static {
        LANGUAGE.add("ZH");
        LANGUAGE.add("ZH_CN");
        LANGUAGE.add("EN");
        LANGUAGE.add("EN_US");
        TIME_ZONE.add("UTC");
        TIME_ZONE.add("UTC+1");
        TIME_ZONE.add("UTC+2");
        TIME_ZONE.add("UTC+3");
        TIME_ZONE.add("UTC+4");
        TIME_ZONE.add("UTC+5");
        TIME_ZONE.add("UTC+6");
        TIME_ZONE.add("UTC+7");
        TIME_ZONE.add("UTC+8");
        TIME_ZONE.add("UTC+9");
        TIME_ZONE.add("UTC+10");
        TIME_ZONE.add("UTC+11");
        TIME_ZONE.add("UTC+12");
    }

}