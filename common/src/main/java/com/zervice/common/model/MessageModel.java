package com.zervice.common.model;

import com.alibaba.fastjson.JSONArray;
import com.zervice.common.pojo.chat.TimeRecord;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author xh
 */
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MessageModel {

    public static final String OUTPUT_TYPE_HUMAN = "human";
    public static final String OUTPUT_TYPE_BOT = "bot";
    public static final String INPUT_TYPE_DEFAULT = "default";
    public static final String INPUT_TYPE_QUICK_CLICK = "quick_click";
    private String _id;
    @NotBlank(message = "chatId required")
    private String _chatId;
    private Dialog _dialog;
    private Long _time;
    private Prop _properties;

    public static MessageModel toMessage(String agentId, long startTime, SendMessageModel msg, MessageRes res) {
        long now = System.currentTimeMillis();
        Output output = Output.builder()
                .answers(res.getAnswers().toJSONString())
                .similarQuestions(res.getSimilarQuestions().toJSONString())
                .links(res.getLinks().toJSONString())
                .type(res.getType())
                .answerTime(now)
                .build();

        String type = msg.getMessage().startsWith("/") ?
                INPUT_TYPE_QUICK_CLICK : INPUT_TYPE_DEFAULT;

        Input input = Input.builder()
                .query(msg.getMessage())
                .send(msg.getContent())
                .sendTime(startTime).type(type).build();

        Dialog dialog = Dialog.builder()
                .output(output)
                .input(input)
                .build();

        Prop prop = Prop.builder()
                .agentId(agentId)
                .ip(msg.getIp()).projectId(msg.getProjectId())
                .embedding(res.getProperties().getEmbedding())
                .timeRecords(res.getProperties().getTimeRecords())
                .build();
        return MessageModel.builder()
                .dialog(dialog)
                .chatId(msg.getChatId())
                .properties(prop)
                .time(now)
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
         * 输入框中呈现给用户的
         */
        private String _query;
        /**
         * 实际发送给模型的,可能与query一样
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
         * json array links
         * - 目前用于 kb-url的来源
         */
        private String _links;

        private Long _answerTime;
        /**
         * bot:机器回复；human:人工回复（为后期人工接入做预留）
         */
        private String _type;
    }

    @Builder
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Prop {
        private String _projectId;
        private String _agentId;
        private String _ip;
        private JSONArray _embedding;

        private List<TimeRecord> _timeRecords;
    }
}
