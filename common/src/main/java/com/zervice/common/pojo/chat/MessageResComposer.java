package com.zervice.common.pojo.chat;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.List;

/**
 * @author chen
 * @date 2022/8/25
 */
@Setter
@Getter
@Log4j2
public class MessageResComposer {

    public static List<Object> composeText(String text) {
        List<Object> result = new ArrayList<>();

        JSONObject textRes = ProjectComponentBotPojo.buildTextRes(text);
        result.add(textRes);
        return result;
    }



    public static class BaseRes {
    }

    @Setter
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Text extends BaseRes {
        private String _text;

        @JsonProperty("recipient_id")
        @JSONField(name = "recipient_id")
        private String _recipientId;
    }

    @Setter
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Button extends BaseRes {
        private String _payload;

        private String _title;
    }

    @Setter
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Image extends BaseRes {
        private String _image;

        @JsonProperty("recipient_id")
        @JSONField(name = "recipient_id")
        private String _recipientId;
    }

}
