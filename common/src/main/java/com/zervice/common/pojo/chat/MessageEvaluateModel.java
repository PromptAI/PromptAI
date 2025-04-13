package com.zervice.common.pojo.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 *
 * @author admin
 * @date 2022/10/27
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MessageEvaluateModel {
    /**
     * 会话id
     */
    @NotBlank
    private String _chatId;
    @NotBlank
    private String _messageId;
    @NotNull
    private Integer _helpful;
}
