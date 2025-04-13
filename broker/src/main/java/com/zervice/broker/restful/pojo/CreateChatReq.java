package com.zervice.broker.restful.pojo;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Map;

/**
 * @author chenchen
 * @Date 2023/12/1
 */
@Builder
@Setter@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreateChatReq {
    private Map<String, Object> _slots;

    private Map<String, Object> _variables;

    @NotNull
    private String _scene;

    private String _chatId;

    /**
     * flow/faq id，
     */
    private String _componentId;

    private String _projectId;

    private String _publishedProjectId;

}
