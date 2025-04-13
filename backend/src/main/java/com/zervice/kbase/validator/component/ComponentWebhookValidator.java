package com.zervice.kbase.validator.component;

import com.zervice.kbase.api.restful.pojo.RestBaseProjectComponent;
import com.zervice.kbase.validator.error.ValidatorError;

/**
 * todo
 */
public class ComponentWebhookValidator extends BaseComponentValidator {

    public ComponentWebhookValidator(RestBaseProjectComponent component) {
        super(component);
    }

    @Override
    public ValidatorError validate(ComponentValidatorContext context) {
        return null;
    }
}
