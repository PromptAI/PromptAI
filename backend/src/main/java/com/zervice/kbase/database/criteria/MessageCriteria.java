package com.zervice.kbase.database.criteria;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;



@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MessageCriteria {
    @NotBlank(message = "_chatId required")
    private String _chatId;
}
