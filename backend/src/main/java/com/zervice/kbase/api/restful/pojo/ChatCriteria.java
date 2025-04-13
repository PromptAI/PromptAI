package com.zervice.kbase.api.restful.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatCriteria {
    /**
     * 来自debug的消息
     */
    public static final String SCENE_DEBUG = "debug";
    /**
     * 来自发布的消息
     */
    public static final String SCENE_PUBLISH = "publish";

    private String _publishedProjectId;

    @NotBlank(message = "projectId required")
    private String _projectId;

    @Builder.Default()
    private Boolean _hasMessage = true;

    private Set<String> _rootComponentIds;

    /**
     * see {@link com.zervice.kbase.database.pojo.Evaluation#HELP}
     */
    private List<Integer> _evaluates;

    private String _scene;
}
