package com.zervice.kbase.api.rpc.pojo;

import cn.hutool.core.net.URLDecoder;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.zervice.kbase.ServerInfo;
import com.zervice.kbase.api.restful.pojo.RestEvaluation;
import com.zervice.kbase.api.restful.pojo.RestMessage;
import com.zervice.kbase.database.pojo.Message;
import lombok.*;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author chenchen
 * @Date 2023/9/8
 */
@Log4j2
@Setter@Getter
@AllArgsConstructor
@NoArgsConstructor
public class RpcMessage {
    private static final List<AnswerDecoder> decoders = new ArrayList<>();
    static  {
        // AttachmentDecoder必须在TextDecoder前面,因为attachment借用了text的结构，text作为兜底使用
        decoders.add(new AttachmentDecoder());
        decoders.add(new ButtonDecoder());
        decoders.add(new TextDecoder());
        decoders.add(new ImageDecoder());
    }

    private Dialog _dialog;

    private String _id;
    private String _chatId;
    private Evaluation _evaluation;
    private Long _time;


    /**
     * [
     *    {
     *       "id": "xxxxxxxx",
     *       "avatarUrl": "https://xxxxxx",
     *       "username": "bot",
     *       "timestamp": 16xxxxxxxxx,
     *       "text": "message",
     *       "images": [{
     *           "url": "https://xxxxxxx"
     *       }],
     *       "buttons": [
     *           {
     *              "id": "xxxxxx",
     *              "title": "xxxxx"
     *           }
     *       ],
     * 		 "attachments":[
     *                                  {
     *         	       "url": "https://xxxxxx"
     *         	       "filename": "xxxxxxx.png"
     * 	       }
     *       ],
     *       "type": "bot/user"
     *    },
     *
     * ]
     */
    public JSONArray toRes(String username, String avatar, String botName, String botAvatar) {
        JSONArray res = new JSONArray();
        JSONObject inputRes = _input2Res(username, avatar);
        if (inputRes != null) {
            res.add(inputRes);
        }
        JSONArray outputRes = _output2Res(botName, botAvatar);
        if (CollectionUtils.isNotEmpty(outputRes)) {
            res.addAll(outputRes);
        }

        return res;
    }

    private JSONObject baseRes(String username, String avatar,
                               String type, Long time) {
        JSONObject baseRes = new JSONObject();
        baseRes.put("id", _id + "_" + UUID.randomUUID());
        baseRes.put("username", username);
        baseRes.put("avatarUrl", avatar);
        baseRes.put("type", type);
        baseRes.put("timestamp", time);
        baseRes.put("text", "");
        baseRes.put("images", Collections.emptyList());
        baseRes.put("buttons", Collections.emptyList());
        baseRes.put("attachments", Collections.emptyList());
        return baseRes;
    }

    private JSONObject _input2Res(String username, String avatar) {
        Input input = _dialog._input;
        if (input == null || Message.INIT_MESSAGE.equals(input.getSend())) {
            return null;
        }

        JSONObject baseRes = baseRes(username, avatar, "user", input.getSendTime());

        if (input.getAttachment() != null) {
            JSONArray attachments = _buildAttachments(input.getAttachment());
            baseRes.put("attachments", attachments);
            return baseRes;
        }

        baseRes.put("text", input.getSend());
        return baseRes;
    }


    /**
     * [
     *    {
     *       "id": "xxxxxxxx",
     *       "avatarUrl": "https://xxxxxx",
     *       "username": "bot",
     *       "timestamp": 16xxxxxxxxx,
     *       "text": "message",
     *       "images": [{
     *           "url": "https://xxxxxxx"
     *       }],
     *       "buttons": [
     *           {
     *              "id": "xxxxxx",
     *              "title": "xxxxx"
     *           }
     *       ],
     * 		 "attachments":[
     *                                  {
     *         	       "url": "https://xxxxxx"
     *         	       "filename": "xxxxxxx.png"
     * 	       }
     *       ],
     *       "type": "bot/user"
     *    },
     * ]
     */
    private JSONArray _output2Res(String username, String avatar) {
        Output output = _dialog._output;
        if (CollectionUtils.isEmpty(output._answers)) {
            return null;
        }

        JSONObject baseRes = baseRes(username, avatar, "bot", output.getAnswerTime());
        String bassResStr = baseRes.toJSONString();
        JSONArray res = new JSONArray();

        for (int i = 0; i < output._answers.size(); i++) {
            JSONObject answer = output._answers.getJSONObject(i);
            String type = answer.getString("type");
            JSONObject payload = answer.getJSONObject("payload");

            switch (type) {
                /**
                 * {
                 *    "type":"text",
                 *    "payload":{
                 *        "text":"hello"
                 *    }
                 * }
                 */
                case AnswerGenerator.TYPE_TEXT:
                    String text = payload.getString("text");
                    JSONObject textRes = JSONObject.parseObject(bassResStr);
                    textRes.put("text", text);
                    res.add(textRes);
                    continue;

                    /**
                     * {
                     *     "type":"button",
                     *     "payload":{
                     *        "text":"text",
                     *        "buttons":[{
                     *            "title":"button1"
                     *        },{
                     *            "title":"button2"
                     *        }
                     *        ]
                     *     }
                     * }
                     */
                case AnswerGenerator.TYPE_BUTTON:
                    text = payload.getString("text");
                    JSONArray buttons = payload.getJSONArray("buttons");
                    buttons.forEach( b -> {
                        ((JSONObject) b).put("id", UUID.randomUUID());
                    });
                    JSONObject buttonRes = JSONObject.parseObject(bassResStr);
                    buttonRes.put("text", text);
                    buttonRes.put("buttons", buttons);
                    res.add(buttonRes);
                    continue;
                    /**
                     * {
                     *     "type":"image",
                     *     "payload":{
                     *         "urls":["url1","url2"]
                     *     }
                     * }
                     */
                case AnswerGenerator.TYPE_IMAGE:
                    List<String> urls = payload.getJSONArray("urls").toJavaList(String.class);
                    JSONArray images = new JSONArray();
                    for (String url : urls) {
                        JSONObject image = new JSONObject();
                        image.put("url", url);
                        images.add(image);
                    }
                    JSONObject imageRes = JSONObject.parseObject(bassResStr);
                    imageRes.put("images", images);
                    res.add(imageRes);
                    continue;
                    /**
                     * {
                     *     "type":"attachment",
                     *     "payload":{
                     *          "name":"test.txt.json",
                     *         "ext":"ext",
                     *         "url":"url"
                     *     }
                     * }
                     */
                case AnswerGenerator.TYPE_ATTACHMENT:
                    JSONArray attachments = _buildAttachments(answer);
                    JSONObject attachmentsRes = JSONObject.parseObject(bassResStr);
                    attachmentsRes.put("attachments", attachments);
                    res.add(attachmentsRes);
                    continue;
                default:
                    LOG.info("[unknown rpc message type:{}]", type);
            }
        }

        return res;
    }

    /**
     * from
     * {
     *     "type":"attachment",
     *     "payload":{
     *          "name":"test.txt.json",
     *         "ext":"ext",
     *         "url":"url"
     *     }
     * }
     * to:
     *
     *  [{
     *     "url": "https://xxxxxx"
     *     "filename": "xxxxxxx.png"
     *  }]
     */
    private JSONArray _buildAttachments(JSONObject attachment) {
        JSONObject payload = attachment.getJSONObject("payload");
        String name = payload.getString("name");
        String url = payload.getString("url");
        JSONObject item = new JSONObject();
        item.put("url", url);
        item.put("filename", name);

        JSONArray res = new JSONArray();
        res.add(item);
        return res;
    }

    public RpcMessage(RestMessage message) {
        this._id = message.getId();
        this._chatId = message.getChatId();
        this._time = message.getTime();
        this._dialog = new Dialog(message.getDialog());

        RestEvaluation restEvaluation = message.getEvaluation();
        if (restEvaluation != null) {
            this._evaluation = new Evaluation(restEvaluation);
        }
    }

    public static String buildAttachmentUrl(String uri) {
       return ServerInfo.getServerAddr() + uri;
    }

    public interface AnswerDecoder {
        JSONObject decode(JSONObject answer);


        default String attachmentUrl(String uri) {
            return buildAttachmentUrl(uri);
        }
    }

    @Setter@Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Evaluation {

        /**
         *  1:有用，2:答非所问 3:内容看不懂 4:答案不详细 5:操作后解决  6 :其他
         */
        private Integer _helpful;
        private Long _createTime;

        public Evaluation(RestEvaluation restEvaluation) {
            this._helpful = restEvaluation.getHelpful();
            this._createTime = restEvaluation.getProperties().getCreateTime();
        }
    }

    @Setter@Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Dialog {

        private Input _input;
        private Output _output;
        public Dialog(Message.Dialog dialog) {
            this._input = new Input(dialog.getInput());
            this._output = new Output(dialog.getOutput());
        }
    }

    @Builder
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Input {

        /**
         * 输入框中呈现给用户的
         */
        private String _send;
        private JSONObject _attachment;
        /**
         * default: 表示用户输入;quick_click:表示用户点击
         */
        private String _type;
        /**
         * 消息发送时间
         */
        private Long _sendTime;

        public Input(Message.Input input) {
            this._type = input.getType();

            // build an attachment
            if (Message.INPUT_TYPE_FILE.equals(_type)) {
                try {
                    String file = URLDecoder.decode(input.getSend(), StandardCharsets.UTF_8);
                    JSONObject attachment = JSONObject.parseObject(file);
                    String uri = attachment.getString("href");
                    String name = attachment.getString("name");
                    String type = attachment.getString("type");
                    if (StringUtils.isNotBlank(uri) && StringUtils.isNotBlank(name)) {
                        this._attachment = AnswerGenerator.attachment(name, type, buildAttachmentUrl(uri));
                    }
                } catch (Exception e) {
                    this._send = input.getSend();
                }

            } else {
                this._send = input.getSend();
            }
            this._sendTime = input.getSendTime();
        }
    }

    @Builder
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Output {

        /**
         * JsonArray, 模型给到的信息
         */
        private JSONArray _answers;
        /**
         * JsonArray，相似问
         */
        private JSONArray _similarQuestions;
        private Long _answerTime;
        public Output(Message.Output output) {
            this._answerTime = output.getAnswerTime();
            this._answers = _decodeAnswer(output.getAnswers());

            // optional
            if (output.getSimilarQuestions() != null) {
                this._similarQuestions = JSONArray.parseArray(output.getSimilarQuestions());
            } else {
                this._similarQuestions = new JSONArray();
            }
        }

        private JSONArray _decodeAnswer(String answer) {
            if (StringUtils.isBlank(answer)) {
                return new JSONArray();
            }

            JSONArray decoded = new JSONArray();
            JSONArray origin = JSONArray.parseArray(answer);
            for (int i = 0; i < origin.size(); i++) {
                JSONObject item = origin.getJSONObject(i);
                if (_isCustom(item)) {
                    continue;
                }

                for (int d= 0; d < decoders.size(); d++) {
                    AnswerDecoder decoder = decoders.get(d);
                    JSONObject de = decoder.decode(item);
                    if (de != null) {
                        decoded.add(de);
                        break;
                    }
                }
            }

            return decoded;
        }

        private  boolean _isCustom(JSONObject item) {
            return item.getJSONObject("custom") != null;
        }
    }

    public static class AnswerGenerator {
        public static final String TYPE_TEXT = "text";
        public static final String TYPE_IMAGE = "image";
        public static final String TYPE_BUTTON = "button";
        public static final String TYPE_ATTACHMENT = "attachment";

        /**
         * {
         *     "type":"button",
         *     "payload":{
         *        "text":"text",
         *        "buttons":[{
         *            "title":"button1"
         *        },{
         *            "title":"button2"
         *        }
         *        ]
         *     }
         * }
         */
        public static JSONObject button(String text,JSONArray buttons) {
            JSONObject imageRes = new JSONObject();
            imageRes.put("type", TYPE_BUTTON);

            List<JSONObject> b = buttons.stream()
                    .map(bt -> {
                        JSONObject originButton = (JSONObject) bt;
                        JSONObject item = new JSONObject();
                        item.put("title", originButton.getString("title"));
                        return item;
                    })
                    .collect(Collectors.toList());

            JSONObject payload = new JSONObject();
            payload.put("text", text);
            payload.put("buttons", b);

            imageRes.put("payload", payload);

            return imageRes;
        }

        /**
         * {
         *     "type":"image",
         *     "payload":{
         *         "urls":["url1","url2"]
         *     }
         * }
         */
        public static JSONObject image(List<String> urls) {
            JSONObject imageRes = new JSONObject();
            imageRes.put("type", TYPE_IMAGE);

            JSONObject payload = new JSONObject();
            payload.put("urls", urls);

            imageRes.put("payload", payload);

            return imageRes;
        }

        /**
         * {
         *    "type":"text",
         *    "payload":{
         *        "text":"hello"
         *    }
         * }
         */
        public static JSONObject text(String text) {
            JSONObject textRes = new JSONObject();
            textRes.put("type", TYPE_TEXT);

            JSONObject payload = new JSONObject();
            payload.put("text", text);

            textRes.put("payload", payload);

            return textRes;
        }

        /**
         * {
         *     "type":"attachment",
         *     "payload":{
         *          "name":"test.txt.json",
         *         "ext":"ext",
         *         "url":"url"
         *     }
         * }
         */
        public static JSONObject attachment(String name, String ext, String url) {
            JSONObject attachment = new JSONObject();
            attachment.put("type", TYPE_ATTACHMENT);

            JSONObject payload = new JSONObject();
            payload.put("name", name);
            payload.put("ext", ext);
            payload.put("url", url);

            attachment.put("payload", payload);
            return attachment;
        }
    }

    public static class TextDecoder implements AnswerDecoder {
        @Override
        public JSONObject decode(JSONObject answer) {
            String text = answer.getString("text");
            JSONArray buttons = answer.getJSONArray("buttons");
            if (StringUtils.isBlank(text) || buttons != null) {
                return null;
            }
            return AnswerGenerator.text(text);
        }
    }

    public static class ButtonDecoder implements AnswerDecoder {
        @Override
        public JSONObject decode(JSONObject answer) {
            String text = answer.getString("text");
            JSONArray buttons = answer.getJSONArray("buttons");
            if (StringUtils.isBlank(text) ||  buttons == null) {
                return null;
            }

            return AnswerGenerator.button(text, buttons);
        }
    }

    /**
     * {
     * 	"name": "test.txt.json",
     * 	"type": "json",
     * 	"href": "/api/blobs/get/a1_d6du7acgdyio",
     * 	"version": "0.0.1"
     * }
     */
    public static class AttachmentDecoder implements AnswerDecoder {
        @Override
        public JSONObject decode(JSONObject answer) {
            String text = answer.getString("text");
            if (StringUtils.isBlank(text)) {
                return null;
            }
            try {
                // url decode
                String decoded = URLDecoder.decode(text, StandardCharsets.UTF_8);

                // try
                JSONObject attachment = JSONObject.parseObject(decoded);
                String name = attachment.getString("name");
                String ext = attachment.getString("type");
                String uri = attachment.getString("href");

                if (StringUtils.isBlank(name) || StringUtils.isBlank(ext) || StringUtils.isBlank(uri)) {
                    return null;
                }

                return AnswerGenerator.attachment(name, ext, attachmentUrl(uri));
            } catch (Exception e) {
                return null;
            }
        }
    }

    public static class ImageDecoder implements AnswerDecoder {
        @Override
        public JSONObject decode(JSONObject answer) {
            String image = answer.getString("image");
            if (StringUtils.isBlank(image)) {
                return null;
            }

            List<String> uris = Arrays.asList(image.split(","));

            List<String > urls = uris.stream()
                    .map(this::attachmentUrl)
                    .collect(Collectors.toList());

            return AnswerGenerator.image(urls);
        }
    }
}
