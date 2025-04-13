package com.zervice.common.utils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;

@Log4j2
public class HttpClientUtils {

    private static final RestClient defaultRestClient = new RestClient(10, LayeredConf.getInt("default.request.timeout.insec", 20));
    private static final RestClient longReadRestClient = new RestClient(10, 180);


    public static RestClient getDefaultRestClient() {
        return defaultRestClient;
    }

    public static JSONObject getJson(String url, HttpHeaders httpHeaders) {
        return defaultRestClient.getJson(url, httpHeaders);
    }

    public static byte[] getBytes(String url, HttpHeaders httpHeaders) {
        return defaultRestClient.getBytes(url, httpHeaders);
    }

    public static String getString(String url, HttpHeaders httpHeaders) {
        return defaultRestClient.getString(url, httpHeaders);
    }

    public static ResponseEntity<String> getStringResponse(String url, HttpHeaders httpHeaders) {
        return defaultRestClient.getStringResponse(url, httpHeaders);
    }

    public static JSONObject postJson(String url, Object requestObject, HttpHeaders httpHeaders) throws RestClientException {
        String resBody = defaultRestClient.postJson(url, requestObject, httpHeaders);
        return JSONObject.parseObject(resBody);
    }

    public static JSONObject postYaml(String url, String  yamlText, HttpHeaders httpHeaders) throws RestClientException {
        String resBody = defaultRestClient.postYaml(url, yamlText, httpHeaders);
        return JSONObject.parseObject(resBody);
    }

    public static JSONObject postText(String url, String text, HttpHeaders httpHeaders) throws RestClientException {
        String resBody = defaultRestClient.postText(url, text, httpHeaders);
        return JSONObject.parseObject(resBody);
    }

    public static JSONObject putJson(String url, Object requestObject, HttpHeaders httpHeaders) throws RestClientException {
        String resBody = defaultRestClient.putJson(url, requestObject, httpHeaders);
        return JSONObject.parseObject(resBody);
    }

    public static JSONObject deleteJSON(String url, Object requestObject, HttpHeaders httpHeaders) throws RestClientException {
        String resBody = defaultRestClient.deleteJson(url, requestObject, httpHeaders);
        return JSONObject.parseObject(resBody);
    }

    public static JSONArray postJsonForArray(String url, Object requestObject, HttpHeaders httpHeaders) throws RestClientException {
        String resBody = defaultRestClient.postJson(url, requestObject, httpHeaders);
        return JSONObject.parseArray(resBody);
    }

    public static JSONObject postFile(String url, File file, String fileParamName, Map<String, Object> param, HttpHeaders h) {
        String resBody = defaultRestClient.postFile(url, file, fileParamName, param, h);
        return JSONObject.parseObject(resBody);
    }

    public static String postJson(String url, Object requestObject) throws RestClientException {
        return defaultRestClient.postJson(url, requestObject);
    }

    public static String postJson(String url, Object requestObject, RestClient restClient, HttpHeaders httpHeaders) throws RestClientException {
        return restClient.postJson(url, requestObject, httpHeaders);
    }

    public static void downloadFile(String url, File localTargeFile, HttpHeaders httpHeaders) throws IOException {
        localTargeFile.getParentFile().mkdirs();
        localTargeFile.createNewFile();
        defaultRestClient.downloadLargeFile(url, localTargeFile, httpHeaders);
    }

    /**
     * 这里通过post form 传输一个文件，然后获取一个文件
     *
     * @param url             url
     * @param file            需要发送的文件
     * @param localTargetFile url返回的文件
     * @param httpHeaders     http headers
     * @throws IOException e
     */
    public static void exchangeFile(String url, File file, String fileParamName, Map<String, Object> param,
                                    HttpHeaders httpHeaders, File localTargetFile) throws IOException {
        localTargetFile.getParentFile().mkdirs();
        localTargetFile.createNewFile();
        defaultRestClient.exchangeFile(url, file, fileParamName, param, httpHeaders, localTargetFile);
    }

    public static InputStream postJsonWithInputStream(String url, HttpHeaders httpHeaders, JSONObject data) throws Exception {
        RestTemplate restTemplate = longReadRestClient.defaultRestTemplate;
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(url)
                .queryParams(null);
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Object> requestEntity =
                new HttpEntity<>(data, httpHeaders);
        ResponseEntity<Resource> responseEntity = restTemplate.exchange(builder.build().toUriString(),
                HttpMethod.POST, requestEntity, Resource.class);

        Resource body = responseEntity.getBody();
        if (body == null) {
            return null;
        }

        return body.getInputStream();
    }


    public static boolean reachable(String url) {
        try {
            URL u = new URL(url);
            u.openStream();
            return true;
        } catch (Exception e) {
            LOG.error("reachable url:{} with error:{} ", url, e.getMessage(), e);
            return false;
        }
    }
}
