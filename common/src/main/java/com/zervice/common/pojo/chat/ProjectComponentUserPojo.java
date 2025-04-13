package com.zervice.common.pojo.chat;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.zervice.common.utils.Constants;
import lombok.*;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Peng Chen
 * @date 2022/6/23
 */
@Getter
@Setter
@Builder
@Log4j2
@NoArgsConstructor
@AllArgsConstructor
public class ProjectComponentUserPojo extends BaseComponentPojo {


    public static final String NAME = "user";

    private String _id;
    private String _type;
    private String _projectId;

    private String _afterRhetorical;

    private JSONObject _linkedFrom;

    private String _parentId;

    private List<String> _relations;

    private Data _data;

    @JsonIgnore
    @JSONField(serialize = false, deserialize = false)
    public boolean hasSlots() {
        return CollectionUtils.isNotEmpty(_data._mappings);
    }

    public static boolean fuzzyMatch(String text1, String text2) {
        if (StringUtils.isBlank(text1) || StringUtils.isBlank(text2)) {
            return false;
        }

        if (text1.contains(text2) || text2.contains(text1)) {
            return true;
        }

        return false;
    }

    @Override
    public JSONArray toRes() {
        JSONArray res = new JSONArray();
        JSONObject text = new JSONObject();
        text.put("text", _getFirstExampleRes());
        text.put("recipient_id", _id);
        res.add(text);
        return res;
    }

    public JSONArray toButtonRes() {
        JSONArray res = new JSONArray();
        JSONObject text = new JSONObject();
        text.put("title", _getFirstExampleRes());
        text.put("payload", getIntentName());
        res.add(text);
        return res;
    }

    /**
     * 是否为虚拟节点，即后端在转换文件时创建的
     * true 则是虚拟节点
     */
    public boolean virtual() {
        return Boolean.TRUE.equals(_data._virtual);
    }

    /**
     * 获取intent name
     */
    private String getIntentName() {
        String intentName = getIntentId();

        // 如果来自意图模板，优先使用模板的id，保证多个引用，intent是一样的;
        if (_linkedFrom != null) {
            String linkedGlobalUserId = _linkedFrom.getString("id");
            if (StringUtils.isNotBlank(linkedGlobalUserId)) {
                intentName = Constants.BUTTON_PREFIX + linkedGlobalUserId;
            }
        }
        return intentName;
    }

    /**
     * 新版本使用 name 代替 intent_id以提高识别率
     * - 原：/component_id
     * - 现：/你是谁？
     * @return
     */
    private String getIntentId() {
        String intentPrefix = Constants.BUTTON_PREFIX;
        String name = _data._name;
        name = StringUtils.isBlank(name) ? _id : name;
        // intent 加前缀
        return intentPrefix + name;
    }

    private String _getFirstExampleRes() {
        List<String> userSay = _data._examples.stream()
                .map(Example::getText)
                .collect(Collectors.toList());

        // 意料之外的情况
        if (CollectionUtils.isEmpty(userSay)) {
            String defaultRes = "hi";
            LOG.warn("unexpected user bot:{}, example is empty，return default:{} to user", _id, defaultRes);
            return defaultRes;
        }
        //取第一个example
        return userSay.get(0);
    }

    public static JSONObject buildButtonRes(String title, String id) {
        // 这可能已有prefix
        String click = id.startsWith(Constants.BUTTON_PREFIX) ? id : Constants.BUTTON_PREFIX + id;
        JSONObject button = new JSONObject();
        button.put("title", title);
        button.put("payload", click);
        return button;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Builder
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Data {
        private String _name;

        private String _description;

        private Integer _errorCode;

        private List<Example> _examples;

        private Boolean _mappingsEnable;

        private List<Mapping> _mappings;

        private Boolean _rhetoricalEnable;

        private String _rhetorical;
        /**
         * 前端按照display展示为user_click/user_input
         */
        private String _display;

        @Builder.Default
        private Boolean _enable = true;

        /**
         * 是否虚拟的user节点，即后端转换文件时动态生成的
         */
        private Boolean _virtual;

        /**
         * 训练状态
         */
        private String _trainStatus;

        /**
         * 训练完成chunks
         */
        private Integer _finishChunks;
        /**
         * 总chunk
         */
        private Integer _totalChunks;

        /**
         * 记录text chunk 的 md5, 便于判断chunk是否需要更新
         */
        private String _chunkMd5;
    }

    @Builder
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Mapping {
        private String _id;
        private String _slotId;
        private String _slotName;
        private String _type;
        private String _value;
        private Boolean _enable;
    }

    @Builder
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Example {
        private String _text;
        private List<Mark> _marks;
    }

    @Builder
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Mark {
        private Integer _start;
        private Integer _end;
        private String _name;
    }
}
