package com.zervice.agent.utils;

import lombok.extern.log4j.Log4j2;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Peng Chen
 * @date 2022/6/16
 */
@Log4j2
public class ConfConstant {

    /**
     * update this version if changed
     */
    public static final String VERSION = "1.1.0";
    public static final String HOSTNAME = EnvUtil.getEnvOrProp("HOSTNAME", System.getProperty("user.name"));

    public static String PUBLIC_URL;

    public static final String AI_BASE_URL = EnvUtil.getEnvOrProp("AI_URL", "http://127.0.0.1:6666");

    public static Boolean AI_CAN_USE_GPU = false;

    /**
     * agent related dir
     */
    public static final Path AGENT_BASE_DIR = Paths.get(EnvUtil.getEnv("AI_BASE_DIR", System.getProperty("user.home")), "agent");
    public static final Path DOWNLOAD_DIR = Paths.get(AGENT_BASE_DIR.toString(), "download");

    public static final String AGENT_ID = EnvUtil.getEnvOrProp("AGENT_ID", null);
    public static final String AGENT_AK = EnvUtil.getEnvOrProp("AGENT_AK", null);

    static {
        try {
            Files.createDirectories(ConfConstant.DOWNLOAD_DIR);
        } catch (Exception e) {
            // report event...
            LOG.error("init download path fail:{}", e.getMessage(), e);
        }

        try {
            Files.createDirectories(ConfConstant.AGENT_BASE_DIR);
        } catch (Exception e) {
            LOG.error("init task conf path fail:{}", e.getMessage(), e);
        }
    }
}