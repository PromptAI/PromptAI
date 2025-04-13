package com.zervice.kbase.validator;

import com.zervice.kbase.validator.component.ComponentValidatorContext;
import com.zervice.kbase.validator.error.ValidatorError;

/**
 * 验证数据库节点是否正确
 *
 * @author chen
 * @date 2022/10/25
 */
public interface ComponentValidateAble {

    /**
     * 验证节点是否正确
     *
     * @param context 上下文
     * @return ValidateError 不为null时则表示有问题
     */
    ValidatorError validate(ComponentValidatorContext context);
}
