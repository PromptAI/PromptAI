package com.zervice.kbase.validator.component;


import com.zervice.kbase.api.restful.pojo.RestBaseProjectComponent;
import com.zervice.kbase.validator.error.ValidatorError;

public class ComponentEntityValidator extends BaseComponentValidator {
    public ComponentEntityValidator(RestBaseProjectComponent component) {
        super(component);
    }

    @Override
    public ValidatorError validate(ComponentValidatorContext context) {
        return null;
    }
}
