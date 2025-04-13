package com.zervice.common.utils;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 使用HttpServletResponse进行文件下载
 *
 * @author Peng Chen
 * @date 2022/6/30
 */
public class DownloadUtil {

    public static void download(HttpServletResponse response, File file) throws Exception {
        download(response, file, file.getName());
    }

    public static void download(HttpServletResponse response, File file, String fileName) throws Exception {
        response.reset();
        //解决中文乱码及多的+
        String filename = URLEncoder.encode(fileName,
                StandardCharsets.UTF_8.toString()).replaceAll("\\+", "%20");
        response.setHeader("Content-Disposition",
                "attachment; filename=" + filename);

        response.addHeader("Content-Length", "" + file.length());
        OutputStream toClient = new BufferedOutputStream(response.getOutputStream());
        response.setContentType("application/octet-stream");
        IOUtils.copy(new FileInputStream(file), toClient);
        toClient.flush();
        toClient.close();
    }
}
