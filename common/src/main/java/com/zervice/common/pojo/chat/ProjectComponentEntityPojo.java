package com.zervice.common.pojo.chat;

import lombok.*;

/**
 * @author chen
 * @date 2023/6/6 10:05
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectComponentEntityPojo extends BaseComponentPojo{
    public static final String TYPE_NAME = "any";

    private String _id;
    private String _type;
    private String _projectId;

    private String _name;

    private String _display;

    @Override
    public String getParentId() {
        return null;
    }
}
