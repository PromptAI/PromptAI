package com.zervice.kbase.utils;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class CommandRunner {
    public static void exec(final String cmd) {
        String maskedCmd=cmd.replaceAll("password=[^ ]*", "password=******");
        LOG.info("Start executing command - {}", maskedCmd);

        String[] cmds = {"sh", "-c", cmd};
        ProcessBuilder pb = new ProcessBuilder(cmds);
        pb.redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.INHERIT);

        int exitValue = 0;
        try {
            Process p = pb.start();
            exitValue = p.waitFor();
        } catch (Exception e) {
            throw new RuntimeException(String.format("Cannot execute command='%s'", maskedCmd), e);
        }

        if(exitValue != 0) {
            throw new RuntimeException(String.format("Command quitted abnormally with code=%d (command='%s')\n" , exitValue, maskedCmd));
        }
    }
}

