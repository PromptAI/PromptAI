package com.zervice.test;

import cn.hutool.core.io.FileUtil;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.zervice.common.utils.HttpClientUtils;
import org.springframework.http.HttpHeaders;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * @author chen
 * @date 2022/11/30
 */
public class DictionaryTest {

    private static final String HEADER_PROJECT = "p_bxde7flpxzpc";
    private static final String HEADER_TOKEN = "NmRmYzA5NjktNTU3Ni00OGRhLWE0OGQtNmFmNzc0YmYyMjFm";
    private static final String HEADER_PUBLISHED_PROJECT_ID = "a1_debug";
    private static final String CHAT_URL = "http://127.0.0.1/chat/api/message";
    private static final String CITY_FILE = "/Users/chen/Downloads/areaName.txt";
    private static final String CHAT_ID = "c50x5rk7cikg";


    public static void main(String[] args) {
        HttpHeaders httpHeaders = httpHeaders();
        List<String> cities = cities();
        System.out.println("read city size:" + cities.size());

        List<String> failed = new ArrayList<>();

        for (String c : cities) {
            String message = c + "天气怎么样？";
            String res = chat(CHAT_ID, message, httpHeaders);
            System.out.println(res);

            if (!res.contains(c)) {
                System.out.println(c + "查询失败!");
                failed.add(c);
            }
        }

        System.out.println("查询失败:");
        System.out.println(JSONObject.toJSONString(failed, SerializerFeature.PrettyFormat));
    }

    private static HttpHeaders httpHeaders() {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("X-project-id", HEADER_PROJECT);
        httpHeaders.add("X-published-project-id", HEADER_PUBLISHED_PROJECT_ID);
        httpHeaders.add("X-published-project-token", HEADER_TOKEN);
        return httpHeaders;
    }
    private static String  chat(String chatId, String input,HttpHeaders httpHeaders) {
        JSONObject message = new JSONObject();
        message.put("message", input);
        message.put("content", input);
        message.put("chatId", chatId);
        JSONObject res = HttpClientUtils.postJson(CHAT_URL, message, httpHeaders);
       return  res.toJSONString();


    }
    private static List<String> cities() {
        return FileUtil.readLines(new File(CITY_FILE), StandardCharsets.UTF_8);
    }




}
