package com.zervice.common.utils;

import lombok.Cleanup;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.exec.CommandLine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * @author Peng Chen
 * @date 2022/8/17
 */
@Log4j2
public class CmdHelper {

    public static String exec(String cmd)throws Exception {
        LOG.info("[try run cmd:{}]", cmd);
        CommandLine commandLine = CommandLine.parse(cmd);
        return exec(commandLine.toStrings());
    }

    public static String exec(String [] argues) throws Exception{
        Process process = Runtime.getRuntime().exec(argues);
        @Cleanup BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        return _captureOutput(reader);
    }

    private static String _captureOutput(final BufferedReader reader) throws IOException {
        final StringBuilder builder = new StringBuilder();

        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line).append("\n");
        }

        return builder.toString().trim();
    }

}
