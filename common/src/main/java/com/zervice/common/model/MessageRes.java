package com.zervice.common.model;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.zervice.common.pojo.chat.TimeRecord;
import lombok.*;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;

@Builder
@Setter
@Getter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class MessageRes {
    public static final String OUTPUT_TYPE_HUMAN = "human";
    public static final String OUTPUT_TYPE_BOT = "bot";
    public static final String OUTPUT_TYPE_BACKEND = "backend";
    /** 大语言模型 **/
    public static final String OUTPUT_TYPE_LLM = "llm";
    public static final String OUTPUT_TYPE_FALLBACK = "fallback";
    public static final String OUTPUT_TYPE_WELCOME = "welcome";
    public static final String OUTPUT_TYPE_KBQA = "kbqa";
    public static final String OUTPUT_TYPE_ERROR = "error";
    public static final String EMPTY_RES = "[]";

    /**
     * 链接
     * [
     *  {
     *      "url":"xxx"
     *  }
     * ]
     */
    private JSONArray _links;

    public static MessageRes factory(JSONArray res, JSONArray similarQuestions, JSONArray links, String type) {
        similarQuestions = similarQuestions == null ? new JSONArray() : similarQuestions;
        links = links == null ? new JSONArray() : links;
        return MessageRes.builder()
                .answers(res)
                .similarQuestions(similarQuestions)
                .type(type).links(links)
                .properties(new Prop())
                .build();
    }

    public static MessageRes factory(JSONArray res, String type) {
        return factory(res, null, type);
    }

    private JSONArray _answers;

    /**
     * 相似问：
     * [
     *  {
     *      "id":"/cp_xxx",
     *      "query":"name..."
     *  }
     * ]
     */
    private JSONArray _similarQuestions;

    public static MessageRes factory(JSONArray res, JSONArray similarQuestions, String type) {
        return factory(res, similarQuestions, null, type);
    }

    /**
     * output type
     */
    private String _type;

    /**
     * message id
     */
    private String _id;

    private Prop _properties;

    /**
     * 合并 time record
     */
    public void mergeTimeRecord(List<TimeRecord> timeRecords) {
        if (CollectionUtils.isEmpty(timeRecords)) {
            return;
        }

        if (_properties._timeRecords == null) {
            _properties._timeRecords = timeRecords;
            return;
        }

        _properties._timeRecords.addAll(timeRecords);
    }
    @Builder
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Prop {

        private Long _queryTime;

        private Long _answerTime;

        /**
         * 标记是否可以评价
         */
        private Boolean _canEvaluate;

        /**
         * Kb qa 的附带产物
         */
        @JsonIgnore
        @JSONField(serialize = false, deserialize = false)
        private JSONArray _embedding;

        private List<TimeRecord> _timeRecords;

        @Override
        public String toString() {
            return "Prop{" +
                    "_canEvaluate=" + _canEvaluate +
                    ", _embedding=" + (_embedding == null ? "null" : _embedding.size()) +
                    '}';
        }
    }


}
