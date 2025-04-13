package com.zervice.common.ding;


import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.zervice.common.utils.HttpClientUtils;
import com.zervice.common.utils.LayeredConf;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author admin
 * @date 2022/11/7
 */
@Log4j2
public class DingTalkSender {

    private static final String WEBHOOK_URL = LayeredConf.getString("ding.talk.webhook.url", "");
    private static final String SECRET = LayeredConf.getString("ding.talk.webhook.token", "");

    private static Boolean _send(WebhookRequest req) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        req.getHeaders().forEach((key, val) -> headers.set(key, val));
        JSONObject resp = HttpClientUtils.postJson(req.getUrl(), req.getPayload(), headers);

        Boolean ok = resp.getInteger("errcode") == 0 && "ok".equals(resp.getString("errmsg"));
        if (!ok) {
            LOG.warn("[core db] [ding push failed，url:{},payload:{}]", req.getUrl(), req.getPayload());
        }
        return ok;
    }

    public static boolean sendQuietly(String msg) {
        try {
           return _send(msg);
        } catch (Exception e) {
            LOG.error("[send ding talk fail:{}]", e.getMessage(), e);
            return false;
        }
    }

    private static Boolean _send(String msg) throws Exception {
        if (!_hasDingConf()) {
            return false;
        }
        DingTalkSender.WebhookRequest request = compose(WEBHOOK_URL, SECRET, null, msg);
        return _send(request);
    }

    /**
     *
     * @param callback
     * @param secret
     * @param receivers  if empty will send to all.
     * @param msg
     * @return
     */
    public static WebhookRequest compose(String callback, String secret, List receivers, String msg) throws Exception {
        Long timestamp = System.currentTimeMillis();

        String stringToSign = timestamp + "\n" + secret;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes("UTF-8"), "HmacSHA256"));
        byte[] signData = mac.doFinal(stringToSign.getBytes("UTF-8"));
        String signature = URLEncoder.encode(new String(Base64.encodeBase64(signData)), "UTF-8");
        JSONObject atList = new JSONObject();
        if (receivers != null) {
            atList.put("atMobiles", new JSONArray(receivers));
        }
        atList.put("isAtAll", CollectionUtils.isEmpty(receivers));

        JSONObject text = new JSONObject();
        text.put("content", msg);

        JSONObject jo = new JSONObject();
        jo.put("msgtype", "text");
        jo.put("at", atList);
        jo.put("text", text);

        // https://oapi.dingtalk.com/robot/send?access_token=XXXXXX&timestamp=XXX&sign=XXX
        String url = callback.indexOf('?') > 0 ? callback + "&" : callback + "?";
        String endpoint = String.format(url + "timestamp=%d&sign=%s", timestamp, signature);
        return WebhookRequest.builder()
                .method("POST")
                .payload(jo.toJSONString())
                .url(endpoint)
                .headers(Map.of("Content-Type", "application/json; charset=UTF-8"))
                .build();
    }

    private static boolean _hasDingConf() {
        if (StringUtils.isBlank(WEBHOOK_URL) || StringUtils.isBlank(SECRET)) {
            return false;
        }
        return true;
    }

    @Builder
    @Getter
    public static class WebhookRequest {
        private final String _method;
        private final String _url;
        private final String _encoding;
        private final String _payload;

        @Builder.Default
        private final Map<String, String> _headers = new HashMap<>();
    }

}
