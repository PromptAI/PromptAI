package com.zervice.common.pojo.chat;

import lombok.*;
import lombok.extern.log4j.Log4j2;

import java.util.List;

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
public class ProjectComponentGotoPojo extends BaseComponentPojo{


    public static final String NAME = "goto";

    private String _id;

    private String _type;
    private String _projectId;
    private String _parentId;
    private String _rootComponentId;

    private Data _data;

    @Override
    public String getName() {
        return _data.getName();
    }


    @Builder
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Data {
        private String _linkId;
        private String _name;
        private Integer _errorCode;

        private String _description;

    }
}
