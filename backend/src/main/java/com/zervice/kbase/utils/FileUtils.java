package com.zervice.kbase.utils;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.zervice.common.restful.exception.RestException;
import com.zervice.common.restful.exception.StatusCodes;
import com.zervice.kbase.database.pojo.CommonBlob;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.util.StringUtils;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Log4j2
@UtilityClass
public class FileUtils {

    /**
     * 从zip file中递归读取非隐藏文件为utf-8文本
     * @param zipBytes
     * @return
     * @throws IOException
     */
    public Map<String /* file name */, String /* content */> extractFilesFromZip(byte[] zipBytes) throws IOException {
        // 写入临时 ZIP 文件
        File zipfile = FileUtil.writeBytes(zipBytes, FileUtil.createTempFile());

        try {
            // 解压 ZIP 文件到临时目录
            File unzip = ZipUtil.unzip(zipfile, StandardCharsets.UTF_8);
            Map<String, String> result = new HashMap<>();

            // 递归处理解压后的内容
            if (unzip != null) {
                _traverseAndReadFiles(unzip, result);
            }

            return result;
        } finally {
            // 删除临时 ZIP 文件
            FileUtil.del(zipfile);
        }
    }

    /**
     * 递归遍历文件夹并读取文件内容。
     *
     * @param dir    当前目录或文件
     * @param result 存储结果的 Map
     */
    private void _traverseAndReadFiles(File dir, Map<String, String> result) {
        if (dir.isFile()) {
            // 如果是隐藏文件（以"."开头），跳过读取
            if (dir.getName().startsWith(".")) {
                return;
            }

            // 如果是文件，读取内容并存储到 Map
            String fileName = dir.getName();
            String content = FileUtil.readString(dir, StandardCharsets.UTF_8);
            result.put(fileName, content);
        } else if (dir.isDirectory()) {
            // 如果是目录，递归遍历子文件和子目录
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    _traverseAndReadFiles(file, result);
                }
            }
        }

        // 删除处理完的文件或目录
        FileUtil.del(dir);
    }


    public String removeExtension(String filename) {
        if (StringUtils.isEmpty(filename)) {
            return filename;
        }

        int index = filename.lastIndexOf(".");
        if (index == -1) {
            return filename;
        }

        return filename.substring(0, index);
    }

    public boolean isPdf(String filename) {
        String ext = cn.hutool.core.io.FileUtil.extName(filename);
        if (ext == null) {
            return filename.contains("pdf");
        }

        return ext.contains("pdf");
    }

    /**
     * 用原始扩展名 - 不实用原始名称，在langchain中，提取中文pdf的py会出现故障
     */
    public File createTmpFile(CommonBlob blob) {
        String suffix = "." + FileUtil.extName(blob.getFileName());

        File tmpFile = FileUtil.createTempFile(suffix, true);
        return FileUtil.writeBytes(blob.getContent(), tmpFile);
    }

    public String readString(CommonBlob commonBlob, Charset charset) {
        return readString(commonBlob.getContent(), charset);
    }

    public String readUTf8String(byte[] data) {
        return readString(data, StandardCharsets.UTF_8);
    }
    public String readString(byte[] data, Charset charset) {

        File file = FileUtil.createTempFile();
        try {
            FileUtil.writeBytes(data, file);
            return FileUtil.readString(file, charset);
        } finally {
            FileUtil.del(file);
        }
    }

    public Pair<byte[] /*content*/, String /*filename suffix*/> base64ToBytes(@NonNull String base64Str) {
        String dataPrefix = ""; //base64格式前头
        String data = "";//实体部分数据

        String[] d = base64Str.split("base64,");
        if (d.length == 2) {
            dataPrefix = d[0];
            data = d[1];
        } else {
            throw new RestException(StatusCodes.BadRequest, "Illegal data");
        }

        String suffix = "";//图片后缀，用以识别哪种格式数据
        //data:image/jpeg;base64,base64编码的jpeg图片数据

        if ("data:image/jpeg;".equalsIgnoreCase(dataPrefix)) {
            suffix = ".jpg";
        } else if ("data:image/png;".equalsIgnoreCase(dataPrefix)) {
            //data:image/png;base64,base64编码的png图片数据
            suffix = ".png";
        } else {
            throw new RestException(StatusCodes.BadRequest, "Illegal image format");
        }

        try {
            // Base64解码
            byte[] b = Base64.decode(data);
            for (int i = 0; i < b.length; ++i) {
                if (b[i] < 0) {// 调整异常数据
                    b[i] += 256;
                }
            }
            return ImmutablePair.of(b, suffix);
        } catch (Exception e) {
            LOG.error("Fail parse data", e);
            throw new RestException(StatusCodes.BadRequest);
        }
    }

    /**
     * 将图片base64的字符串转换成File写入磁盘并返回图片名(如： 4d0bea42a5db493e9c4a57cfc634fdd7.png)
     *
     * @param base64Str     图片文件的base64字符串
     * @param fileStorePath 文件保存路径(不含文件名 如:/usr/local/kb/app/data)
     * @param dbName        数据库名
     * @return 文件名 如： 4d0bea42a5db493e9c4a57cfc634fdd7.png.png
     */
    public static String base64ToFile(@NonNull String base64Str, String fileStorePath, String dbName) {
        String dataPrefix = ""; //base64格式前头
        String data = "";//实体部分数据

        String[] d = base64Str.split("base64,");
        if (d.length == 2) {
            dataPrefix = d[0];
            data = d[1];
        } else {
            throw new RestException(StatusCodes.BadRequest, "Illegal data");
        }

        String suffix = "";//图片后缀，用以识别哪种格式数据
        //data:image/jpeg;base64,base64编码的jpeg图片数据

        if ("data:image/jpeg;".equalsIgnoreCase(dataPrefix)) {
            suffix = ".jpg";
        } else if ("data:image/png;".equalsIgnoreCase(dataPrefix)) {
            //data:image/png;base64,base64编码的png图片数据
            suffix = ".png";
        } else {
            throw new RestException(StatusCodes.BadRequest, "Illegal image format");
        }

        String fileName = UUID.randomUUID().toString().replaceAll("-", "") + suffix;
        String path = fileStorePath + File.separator + dbName + File.separator + fileName;
        try {
            // Base64解码
            byte[] b = Base64.decode(data);
            for (int i = 0; i < b.length; ++i) {
                if (b[i] < 0) {// 调整异常数据
                    b[i] += 256;
                }
            }

            OutputStream out = new FileOutputStream(path);
            out.write(b);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RestException(StatusCodes.BadRequest, "图片上传失败");
        }
        return fileName;
    }

    public static double base64FileSize(String base64Str) {
        if (StringUtils.isEmpty(base64Str)) {
            return 0;
        }

        //1.获取base64字符串长度(不含data:audio/wav;base64,文件头)
        int size0 = base64Str.length();

        //2.获取字符串的尾巴的最后10个字符，用于判断尾巴是否有等号，正常生成的base64文件'等号'不会超过4个
        String tail = base64Str.substring(size0 - 10);

        //3.找到等号，把等号也去掉,(等号其实是空的意思,不能算在文件大小里面)
        int equalIndex = tail.indexOf("=");
        if (equalIndex > 0) {
            size0 = size0 - (10 - equalIndex);
        }

        //4.计算后得到的文件流大小，单位为字节
        return size0 - ((double) size0 / 8) * 2;
    }

    public static String md5(InputStream inputStream, boolean closeStream) {
        byte[] data = IoUtil.readBytes(inputStream, closeStream);
        String content = StrUtil.str(data, StandardCharsets.UTF_8);
        return DigestUtil.md5Hex16(content);
    }

    public static String md5(InputStream inputStream) {
        return md5(inputStream, false);
    }

    public static String md5(File file) {
        return DigestUtil.md5Hex16(file);
    }

}
