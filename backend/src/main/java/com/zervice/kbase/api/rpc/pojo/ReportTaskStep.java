package com.zervice.kbase.api.rpc.pojo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author Peng Chen
 * @date 2022/6/23
 */
@Setter@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReportTaskStep {

    private Long _taskId;

    private Boolean _ok;

    private String _step;

    private Integer _index;

    private String _message;

    private Long _elapsed;
}
