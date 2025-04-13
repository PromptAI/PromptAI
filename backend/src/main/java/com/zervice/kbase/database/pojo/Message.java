package com.zervice.kbase.database.pojo;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.zervice.common.model.MessageRes;
import com.zervice.common.pojo.chat.TimeRecord;
import com.zervice.common.utils.Constants;
import com.zervice.common.utils.JSONUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * @author xh
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document("messages")
public class Message {

    public static final String INIT_MESSAGE = "/init";
    public static final String OUTPUT_TYPE_HUMAN = "human";
    public static final String OUTPUT_TYPE_BOT = "bot";
    public static final String OUTPUT_TYPE_BACKEND = "backend";
    /** 大语言模型 **/
    public static final String OUTPUT_TYPE_LLM = "llm";
    public static final String OUTPUT_TYPE_FALLBACK = "fallback";
    public static final String OUTPUT_TYPE_WELCOME = "welcome";
    public static final String OUTPUT_TYPE_KBQA = "kbqa";
    public static final String OUTPUT_TYPE_ERROR = "error";

    public static final String INPUT_TYPE_DEFAULT = "default";
    public static final String INPUT_TYPE_QUICK_CLICK = "quick_click";
    public static final String INPUT_TYPE_FILE = "file";
    private String _id;
    private String _chatId;
    private Dialog _dialog;
    private Long _time;
    private Prop _properties;

    public static Message fromMessageModel(String id, String chatId,
                                           String dialog, String properties, long time) {
        Dialog msgDialog = JSONObject.parseObject(dialog, Dialog.class);
        Prop prop = JSONObject.parseObject(properties, Prop.class);
        return Message.builder()
                .id(id).chatId(chatId)
                .dialog(msgDialog)
                .time(time).properties(prop)
                .build();
    }

    @Builder
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Dialog {
        private Input _input;
        private Output _output;
    }

    @Builder
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Input {
        /**
         * 实际发送给模型的,可能与send一样
         */
        private String _query;

        /**
         * 输入框中呈现给用户的
         */
        private String _send;
        /**
         * default: 表示用户输入;quick_click:表示用户点击
         */
        private String _type;
        /**
         * 消息发送时间
         */
        private Long _sendTime;
    }

    @Builder
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Output {
        /**
         * JsonArray, 模型给到的信息
         */
        private String _answers;

        /**
         * JsonArray，相似问
         */
        private String _similarQuestions;
        /**
         * json array，链接
         */
        private String _links;
        private Long _answerTime;
        /**
         * bot:机器回复；human:人工回复（为后期人工接入做预留）
         */
        private String _type;
    }

    public Set<String> parseRoots() {
        Set<String> roots = new HashSet<>();
        if (_dialog.getOutput() != null && StringUtils.isNotBlank(_dialog.getOutput()._answers)) {
            JSONArray answer = JSONArray.parseArray(_dialog._output._answers);
            for (int i = 0; i < answer.size(); i++) {
                JSONObject res = answer.getJSONObject(i);

                /**
                 *  {
                 * 			"rootType": "conversation",
                 * 			"componentId": "cp_by74710mzp4w",
                 * 			"delay": "500",
                 * 			"rootId": "c_by6rs59bz7k0"
                 *  }
                 * {
                 *     "rootType": "faq",
                 *     "componentId": "cp_byrqycfoe77l",
                 *     "delay": "500",
                 *     "rootId": "f_bxhzxqg1wqo0"
                 * }
                 */
                JSONObject custom = res.getJSONObject("custom");
                String rootType = custom == null ? null : custom.getString("rootType");
                if (rootType != null) {
                    roots.add(rootType);
                }
            }
        }

        return roots;
    }

    public static String parseComponentId(JSONArray answer) {
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < answer.size(); i++) {
            JSONObject res = answer.getJSONObject(i);
            /**
             *  {
             * 			"rootType": "conversation",
             * 			"componentId": "cp_by74710mzp4w",
             * 			"delay": "500",
             * 			"rootId": "c_by6rs59bz7k0"
             *  }
             */
            JSONObject custom = res.getJSONObject("custom");
            String componentId = custom == null ? null : custom.getString("componentId");
            if (StringUtils.isNotBlank(componentId)) {
                ids.add(custom.getString("componentId"));
            }
        }
        //这里取最后一个id
        if (CollectionUtils.isNotEmpty(ids)) {
            return ids.get(ids.size() - 1);
        }

        return null;
    }

    public String parseLlmOrKbqaType() {
        if (_dialog.getOutput() != null) {
            String type = _dialog._output._type;

            if (MessageRes.OUTPUT_TYPE_LLM.equals(type) || MessageRes.OUTPUT_TYPE_KBQA.equals(type)) {
                return type;
            }
        }

        return null;
    }

    public String parseFallback() {
        if (_dialog.getOutput() != null && StringUtils.isNotBlank(_dialog.getOutput()._answers)) {
            JSONArray answer = JSONArray.parseArray(_dialog._output._answers);
            for (int i = 0; i < answer.size(); i++) {
                JSONObject res = answer.getJSONObject(i);

                /**
                 * {
                 * 		"replyType": "webhook",
                 * 		"rootType": "project",
                 * 		"componentId": "fallback",
                 * 		"delay": "500",
                 * 		"rootId": "p_conpno4nozcw",
                 * 		"fallback": "talk2bits"
                 */
                JSONObject custom = res.getJSONObject("custom");
                String componentId = custom == null ? null : custom.getString("componentId");
                if (custom != null && StringUtils.isNotBlank(componentId) && Constants.FALLBACK_BOT_ID.equals(componentId)) {

                    // fallback 明细
                    String fallback = JSONUtils.getOrDefault(custom, "fallback", Project.Prop.FALLBACK_TYPE_TEXT);
                    return Constants.FALLBACK_BOT_ID + "-" + fallback;
                }
            }

            // 兜底算到text
            if (MessageRes.OUTPUT_TYPE_FALLBACK.equals(_dialog.getOutput()._type)) {
                return Constants.FALLBACK_BOT_ID + "-" + Project.Prop.FALLBACK_TYPE_TEXT;
            }
        }


        return null;
    }

    /**
     * 查询被填充的slot
     */
    public JSONObject parseValidSlots() {
        if (_dialog.getOutput() != null && StringUtils.isNotBlank(_dialog.getOutput()._answers)) {
            JSONArray answer = JSONArray.parseArray(_dialog._output._answers);
            for (int i = 0; i < answer.size(); i++) {
                JSONObject res = answer.getJSONObject(i);

                JSONObject custom = res.getJSONObject("custom");
                /*
                 * {"id_card_no":"None","gender":"None","name":"None"}
                 */
                JSONObject slots = custom == null ? null : custom.getJSONObject("slots");
                if (slots != null && !slots.isEmpty()) {
                    JSONObject validSlots = new JSONObject();

                    for (String entityName : slots.keySet()) {
                        String entityValue = slots.getString(entityName);
                        // filter valid slot value
                        if (StringUtils.isNotBlank(entityValue) && !"None".equals(entityValue)) {
                            validSlots.put(entityName, entityValue);
                        }
                    }

                    return validSlots;
                }
            }
        }

        return null;
    }
    /**
     * 解析命中的root id
     *  这里rootType有可能是project，要过滤掉
     */
    public String parseRootComponentId() {
        // 只要faq/ conversation
        Set<String> rootTypes = Set.of( ProjectComponent.TYPE_CONVERSATION);

        if (_dialog.getOutput() != null && StringUtils.isNotBlank(_dialog.getOutput()._answers)) {
            JSONArray answer = JSONArray.parseArray(_dialog._output._answers);
            for (int i = 0; i < answer.size(); i++) {
                JSONObject res = answer.getJSONObject(i);

                /**
                 *  {
                 * 			"rootType": "conversation",
                 * 			"componentId": "cp_by74710mzp4w",
                 * 			"delay": "500",
                 * 			"rootId": "c_by6rs59bz7k0"
                 *  }
                 * {
                 *     "rootType": "faq",
                 *     "componentId": "cp_byrqycfoe77l",
                 *     "delay": "500",
                 *     "rootId": "f_bxhzxqg1wqo0"
                 * }
                 */
                JSONObject custom = res.getJSONObject("custom");
                String rootType = custom == null ? null : custom.getString("rootType");
                if (custom != null && StringUtils.isNotBlank(rootType) && rootTypes.contains(rootType)) {
                    return custom.getString("rootId");
                }
            }
        }

        return null;

    }

    @Builder
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Prop {
        private String _projectId;
        private String _agentId;

        /**
         * 标记是否可以评价
         */
        private Boolean _canEvaluate;

        /**
         * 对话时的ip
         */
        private String _ip;

        /**
         * kbqa 的input embedding
         */
        private JSONArray _embedding;

        private List<TimeRecord> _timeRecords;

        /**
         *  root types
         */
        private Set<String> _roots;

        /**
         * same with {@link Chat.Prop#getScene()}
         */
        private String _scene;
    }

    public boolean isInitMessage() {
        try {
            return INIT_MESSAGE.equals(_dialog._input._query);
        } catch (Exception e) {
            return false;
        }
    }
}
