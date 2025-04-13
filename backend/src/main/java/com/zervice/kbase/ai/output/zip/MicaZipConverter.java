package com.zervice.kbase.ai.output.zip;

import com.alibaba.fastjson.JSONObject;
import com.zervice.kbase.ai.output.zip.pojo.ZipContainer;

import java.io.File;

/**
 * 将训练文件转换成mica的zip文件,文件中包含：
 * <p>
 * - config.yml: bot相关的配置信息
 * - agents.yml: flow agents & llm Agents  & meta
 * - xx.py     : function callings  & pythons code
 *
 * @author chenchen
 * @Date 2025/1/3
 */
public class MicaZipConverter {

    public static File convert(String json) {
        ZipContainer container = JSONObject.parseObject(json, ZipContainer.class);
        return container.zip();
    }
}
