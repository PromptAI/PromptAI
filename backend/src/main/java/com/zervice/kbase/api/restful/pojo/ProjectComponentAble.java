package com.zervice.kbase.api.restful.pojo;

import com.zervice.kbase.database.pojo.ProjectComponent;

/**
 * 'faq-root' | 'user' | 'bot' | 'conversation' | 'webhook';
 * @author Peng Chen
 * @date 2022/6/22
 */
public  interface ProjectComponentAble {

    ProjectComponent toProjectComponent();
}
