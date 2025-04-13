package com.zervice.kbase.api.rpc.pojo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author Peng Chen
 * @date 2022/6/24
 */
@Setter@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ReportTaskResult {
    private Long _taskId;
    private Boolean _ok;
    private String _message;
}
