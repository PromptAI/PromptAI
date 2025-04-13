package com.zervice.agent.rest.pojo;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


/**
 * @author Peng Chen
 * @date 2022/6/29
 */
@Setter@Getter
@NoArgsConstructor
public class CancelTaskRequest {
    @NotNull(message = "id required")
    private Long _id;
}
