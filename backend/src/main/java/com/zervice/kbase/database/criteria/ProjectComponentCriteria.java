package com.zervice.kbase.database.criteria;

import com.zervice.kbase.api.restful.pojo.RestProjectComponentWebhook;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author Peng Chen
 * @date 2022/6/24
 */
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProjectComponentCriteria {

    /**
     * type 为 ProjectComponent.TYPE_USER时生效
     * 筛选：keyword
     * true: keyword需有值
     * false: keyword需无值
     * null: 不做处理
     */
    private Boolean _haveKeyword;

    /**
     * type 为 ProjectComponent.TYPE_USER时生效
     * 筛选：linked
     * true: linked is true
     * false: linked is false
     * null: 不做处理
     */
    private Boolean _haveLinked;

    /**
     * list user component api 接口增加一个参数hasName
     * 1.当hasName = null 时候，查询所有
     * 2.当hasName = false 时候，查询所有不带name的“user component” （name情况：null， 空字符串 为“不带name”）
     * 3.当hasName = true 时候，查询所有带name的“user component”
     */
    private Boolean _hasName;
    /**
     * 同义词标签查询
     */
    private String _label;
    /**
     * 同义词原始词
     */
    private String _original;
    /**
     * 同义词是否启用
     */
    private Boolean _enable;

    /**
     *  webhook的子类
     *   - 目前存在talk2bits的webhook， 其中{@link com.zervice.kbase.api.restful.pojo.RestProjectComponentWebhook.Data#_talk2bitsInput}
     */
    private String _webhookType = RestProjectComponentWebhook.NORMAL_WEBHOOK;

    /**
     * entity使用，是否获取内置变量
     */
    private Boolean _blnInternal;
}
