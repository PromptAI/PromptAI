package com.zervice.kbase.validator.component;

import com.zervice.kbase.api.restful.pojo.RestBaseProjectComponent;
import com.zervice.kbase.validator.error.ValidatorError;

/**
 * todo
 */
public class ComponentGotoValidator extends BaseComponentValidator {

    public ComponentGotoValidator(RestBaseProjectComponent component) {
        super(component);
    }

    @Override
    public ValidatorError validate(ComponentValidatorContext context) {
        return null;
    }
}
