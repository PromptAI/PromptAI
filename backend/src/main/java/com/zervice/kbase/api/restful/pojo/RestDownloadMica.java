package com.zervice.kbase.api.restful.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 *
 * @author admin
 * @date 2022/11/23
 */
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RestDownloadMica {

    private String _id;
    /**
     * 流图名称
     */
    private String _name;
    /**
     * root-faq/conversation
     */
    private String _type;
    /**
     * 是否可以下载
     */
    private Boolean _isReady;
    /**
     * 节点错误信息
     */
    private List<String> _error;

}
