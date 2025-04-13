package com.zervice.common.pojo.chat;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.zervice.common.utils.Constants;
import com.zervice.common.utils.RegexUtils;
import lombok.*;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Peng Chen
 * @date 2022/6/22
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Log4j2
public class ProjectComponentBotPojo  extends BaseComponentPojo{


    /**
     * bot中包含变量的正则
     */
    private final static Pattern ENTITY_PATTERN = Pattern.compile("\\{([^}]*)\\}");

    public static final String NAME = "bot";

    private String _id;
    private String _type;
    private String _projectId;

    private String _parentId;

    private Data _data;

    private List<String> _relations;

    private List<String> _keyword;

    private String _rootComponentId;

    public boolean existsEntityReply() {
        return CollectionUtils.isNotEmpty(_data._entityReplies);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public JSONArray toRes() {
        JSONArray res = new JSONArray();
        if (CollectionUtils.isNotEmpty(_data._responses)) {
            for (Response r : _data._responses) {
                String text = r.getContent().getString("text");
                res.addAll(r.toRest(text, null));
            }
        }
        return res;
    }

    @Builder
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Data {
        private String _description;
        private Integer _errorCode;

        private List<Response> _responses;

        private List<EntityReply> _entityReplies;
    }

    /**
     * 多轮中变量回答列表， 目前支持entity的and匹配.
     * - 后期要升级or，可以在EntityReply增加一个属性：operator  ： and /or, entity间仅支持一种操作
     */
    @Builder
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EntityReply {

        /**
         * 包含的回复
         *  for now we support text、image & attachment
         */
        private List<Response> _responses;

        /**
         * 选择的entity
         *  entityId -> value...
         */
        private Map<String, String> _entities;

    }

    @Builder
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {

        public static final String TYPE_TEXT = "text";
        public static final String TYPE_IMAGE = "image";
        public static final String TYPE_WEBHOOK = "webhook";
        public static final String TYPE_ACTION = "action";
        public static final String TYPE_ATTACHMENT = "attachment";

        private String _id;
        private String _type;

        /**
         * 回复的 delay， 单位：毫秒，默认：500
         */
        @Builder.Default
        private Long _delay = 500L;


        private JSONObject _content;

        public JSONObject generateDelay(Long delay, JSONObject slots) {
            // 如果没有设置或小于0，设置为0，不延迟
            if (delay == null || delay <= 0) {
                delay = 0L;
            }

            // 超过了最大值，设置为默认值
            if (delay > Constants.BOT_REPLY_DELAY_MAX) {
                delay = Constants.BOT_REPLY_DELAY_DEFAULT;
            }

            JSONObject custom = new JSONObject();
            JSONObject delayRes = new JSONObject();
            delayRes.put("delay", delay.toString());
            delayRes.put("slots", slots);
            custom.put("custom", delayRes);
            return custom;
        }

        private Set<String> _extractSlotName(String input) {
            if (input == null) {
                return Set.of();
            }

            Set<String> values = new HashSet<>();
            Pattern pattern = Pattern.compile("\\{([^{}]+)}");
            Matcher matcher = pattern.matcher(input);

            while (matcher.find()) {
                String value = matcher.group(1);
                values.add(value);
            }

            return values;
        }

        /**
         * 变量回复列表来生成答案
         */
        public JSONArray toRest(JSONObject /* entityName, value */ slots) {
            String text = _content.getString("text");
            Set<String> slotNamesInText = _extractSlotName(text);
            for (String slotName : slotNamesInText) {
                text = text.replaceAll(RegexUtils.buildReferenceKeyWithout$(slotName), slots.getString(slotName));
            }

            return toRest(text, slots);
        }

        public JSONArray toRest(String t, JSONObject slots) {
            JSONArray res = new JSONArray();
            if (TYPE_TEXT.equals(_type) || TYPE_ATTACHMENT.equals(_type)) {
                JSONObject text = new JSONObject();
                text.put("text", t);
                text.put("recipient_id", _id);

                res.add(text);

                res.add(generateDelay(_delay, slots));
                return res;
            }

            if (TYPE_IMAGE.equals(_type)) {
                // build text
                JSONObject text = new JSONObject();
                text.put("text", t);
                text.put("recipient_id", _id);

                res.add(text);

                // build image

                JSONArray images = _content.getJSONArray("image");
                for (int i = 0; i < images.size(); i++) {
                    JSONObject image = new JSONObject();
                    image.put("image",images.getJSONObject(i).getString("url"));
                    image.put("recipient_id", _id);

                    res.add(image);
                    res.add(generateDelay(_delay, slots));
                }

                return res;
            }

            LOG.warn("unsupported type:{} to res", _type);
            return res;
        }
    }


    public static JSONObject buildTextRes(String text) {
        // build text
        JSONObject textRes = new JSONObject();
        textRes.put("text", text);
        textRes.put("recipient_id", UUID.randomUUID().toString());
        return textRes;
    }

}
