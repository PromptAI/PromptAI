package com.zervice.common.utils;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.log4j.Log4j2;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

@Log4j2
public class RestClient {
    private final ClientHttpRequestFactory requestFactory;
    public final RestTemplate defaultRestTemplate;

    public RestClient(int connectTimeoutInSec, int readTimeoutInSec) {
        HttpClient httpClient = HttpClients.custom()
                .build();

        requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
        defaultRestTemplate = new RestTemplate(requestFactory);
        defaultRestTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public void handleError(ClientHttpResponse response) throws IOException {
                HttpStatusCode statusCode = response.getStatusCode();
                try {
                    super.handleError(response);
                } catch (HttpClientErrorException ex) {

                    LOG.error("Get error response. code={}, resp={}", statusCode, ex.getResponseBodyAsString(), ex);
                    throw ex;
                } catch (Exception ex) {
                    LOG.error("Get error response. code={}, message={}", statusCode, ex.getMessage(), ex);
                    throw ex;
                }
            }
        });

    }


    public JSONObject getJson(String url, HttpHeaders httpHeaders) {
        return JSONObject.parseObject(getString(url, httpHeaders));
    }

    public byte[] getBytes(String url, HttpHeaders httpHeaders) {
        try {
            HttpEntity<String> requestEntity = new HttpEntity<>(null, httpHeaders);
            ResponseEntity<byte[]> response = defaultRestTemplate.exchange(url, HttpMethod.GET, requestEntity, byte[].class);
            if (response.getStatusCode() != HttpStatus.OK) {
                throw new RestClientException(String.format("Fail to access %s and response code %s , body %s", url, response.getStatusCode(), response.getBody()));
            }
            return response.getBody();
        } catch (RestClientException e) {
            throw e;
        }
    }

    public String getString(String url, HttpHeaders httpHeaders) {

        try {
            HttpEntity<String> requestEntity = new HttpEntity<>(null, httpHeaders);
            ResponseEntity<String> response = defaultRestTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);
            if (response.getStatusCode() != HttpStatus.OK) {
                throw new RestClientException(String.format("Fail to access %s and response code %s , body %s", url, response.getStatusCode(), response.getBody()));
            }
            return response.getBody();
        } catch (RestClientException e) {
            throw e;
        }
    }

    public ResponseEntity<String> getStringResponse(String url, HttpHeaders httpHeaders) {

        HttpEntity<String> requestEntity = new HttpEntity<>(null, httpHeaders);
        ResponseEntity<String> response = defaultRestTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);
        return response;
    }

    public String postJson(String url, Object requestObject, HttpHeaders httpHeaders) throws RestClientException {
        try {
            HttpEntity<Object> requestEntity = new HttpEntity<>(requestObject, httpHeaders);
            ResponseEntity<String> response = defaultRestTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
            return response.getBody();
        } catch (RestClientException e) {
            LOG.error("Fail to post to url={}, error={}", url, e);
            throw e;
        }
    }

    /**
     * 发送 POST 请求并接收文本响应。
     *
     * @param url         请求 URL
     * @param text 请求体对象
     * @param httpHeaders HTTP 头信息
     * @return 响应文本
     * @throws RestClientException 如果请求失败
     */
    public String postText(String url, String text, HttpHeaders httpHeaders) throws RestClientException {
        try {
            if (httpHeaders != null) {
                httpHeaders.add("Content-Type", MediaType.TEXT_PLAIN_VALUE + ";charset=UTF-8");
            }
            HttpEntity<Object> requestEntity = new HttpEntity<>(text, httpHeaders);
            ResponseEntity<String> response = defaultRestTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
            return response.getBody();
        } catch (RestClientException e) {
            LOG.error("Fail to post to url={}, error={}", url, e);
            throw e;
        }
    }

    public String postYaml(String url, String  yamlText, HttpHeaders httpHeaders) throws RestClientException {
        try {
            httpHeaders.setContentType(MediaType.parseMediaType("application/x-yaml"));

            HttpEntity<Object> requestEntity = new HttpEntity<>(yamlText, httpHeaders);
            ResponseEntity<String> response = defaultRestTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
            return response.getBody();
        } catch (RestClientException e) {
            LOG.error("Fail to post to url={}, error={}", url, e);
            throw e;
        }
    }


    public String putJson(String url, Object requestObject, HttpHeaders httpHeaders) throws RestClientException {
        try {
            HttpEntity<Object> requestEntity = new HttpEntity<>(requestObject, httpHeaders);
            ResponseEntity<String> response = defaultRestTemplate.exchange(url, HttpMethod.PUT, requestEntity, String.class);
            return response.getBody();
        } catch (RestClientException e) {
            LOG.error("Fail to put to url={}, error={}", url, e);
            throw e;
        }
    }

    public String deleteJson(String url, Object requestObject, HttpHeaders httpHeaders) throws RestClientException {
        try {
            HttpEntity<Object> requestEntity = new HttpEntity<>(requestObject, httpHeaders);
            ResponseEntity<String> response = defaultRestTemplate.exchange(url, HttpMethod.DELETE, requestEntity, String.class);
            return response.getBody();
        } catch (RestClientException e) {
            LOG.error("Fail to delete to url={}, error={}", url, e);
            throw e;
        }
    }

    public String postJson(String url, Object requestObject) throws RestClientException {
        return defaultRestTemplate.postForObject(url, requestObject, String.class);
    }

    public String postFile(String url, File file, String fileParamName, Map<String, Object> param, HttpHeaders httpHeaders) {
        HttpEntity<MultiValueMap<String, Object>> files = _preparePostFromHttpEntity(file, fileParamName, param, httpHeaders);
        ResponseEntity<String> response = defaultRestTemplate.postForEntity(url, files, String.class);
        return response.getBody();
    }

    /**
     * smaller files
     * @param url
     * @param httpHeaders
     * @return
     * @throws IOException
     */
    public byte[] getFile(String url, HttpHeaders httpHeaders) throws IOException {
        HttpHeaders newHeaders = new HttpHeaders();
        if (httpHeaders != null) {
            newHeaders = new HttpHeaders(httpHeaders);
        }
        newHeaders.setAccept(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM)); // for file
        ResponseEntity<byte[]> response = defaultRestTemplate.exchange(url, HttpMethod.GET, new HttpEntity<byte[]>(newHeaders),
                byte[].class);
        return response.getBody();
    }

    public void downloadLargeFile(String url, File localTargetFile, HttpHeaders httpHeaders) throws IOException {
        defaultRestTemplate.execute(url, HttpMethod.GET, clientHttpRequest -> {
            HttpHeaders newHeaders = new HttpHeaders();
            if (httpHeaders != null) {
                newHeaders = new HttpHeaders(httpHeaders);
            }
            newHeaders.setAccept(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM)); // for file
            clientHttpRequest.getHeaders().addAll(newHeaders);
        }, clientHttpResponse -> {
            StreamUtils.copy(clientHttpResponse.getBody(), new FileOutputStream(localTargetFile));
            return localTargetFile;
        });
    }

    public void exchangeFile(String url, File file, String fileParamName,
                             Map<String, Object> param, HttpHeaders httpHeaders,
                             File downloadFile) {
        HttpEntity<MultiValueMap<String, Object>> files = _preparePostFromHttpEntity(file, fileParamName, param, httpHeaders);
        RequestCallback clientHttpRequest = defaultRestTemplate.httpEntityCallback(files);

        // 从response中提取file
        ResponseExtractor<File> responseExtractor = clientHttpResponse -> {
            StreamUtils.copy(clientHttpResponse.getBody(), new FileOutputStream(downloadFile));
            return downloadFile;
        };

        defaultRestTemplate.execute(url, HttpMethod.POST, clientHttpRequest, responseExtractor);
    }

    private HttpEntity<MultiValueMap<String, Object>> _preparePostFromHttpEntity(File file, String fileParamName,
                                                                                 Map<String, Object> param, HttpHeaders httpHeaders) {
        FileSystemResource fileSystemResource = new FileSystemResource(file);
        MediaType type = MediaType.parseMediaType("multipart/form-data");
        httpHeaders.setContentType(type);

        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add(fileParamName, fileSystemResource);

        for (Map.Entry<String, Object> entry : param.entrySet()) {
            form.add(entry.getKey(), entry.getValue());
        }

        return new HttpEntity<>(form, httpHeaders);
    }

}
