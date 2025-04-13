package com.zervice.common.pojo.chat;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.*;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * @author Peng Chen
 * @date 2022/6/22
 */
@Getter
@Setter
@NoArgsConstructor
public class ProjectComponentConversationPojo extends BaseComponentPojo{

    public static final String NAME = "conversation";

    private String _id;

    private String _type;

    private String _projectId;

    private Data _data;

    private List<String> _relations;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getParentId() {
        return null;
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

        /**
         * 分支引导语，如果有就在命中flow节点就fake一个bot res
         */
        private String _welcome;
    }

}
