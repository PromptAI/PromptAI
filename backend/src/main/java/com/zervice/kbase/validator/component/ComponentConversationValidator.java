package com.zervice.kbase.validator.component;

import com.zervice.kbase.api.restful.pojo.RestBaseProjectComponent;
import com.zervice.kbase.api.restful.pojo.RestProjectComponentConversation;
import com.zervice.kbase.database.pojo.ProjectComponent;
import com.zervice.kbase.validator.error.ValidatorError;
import org.apache.commons.lang3.StringUtils;

public class ComponentConversationValidator extends BaseComponentValidator {

    public ComponentConversationValidator(RestBaseProjectComponent component) {
        super(component);
    }

    @Override
    public ValidatorError validate(ComponentValidatorContext context) {
//        if (CollectionUtils.isEmpty(_children)) {
//            return ValidatorError.factory(ErrorCode.MissingChild);
//        }

//        if (!_rightWelcome()) {
//            return ValidatorError.factory(ErrorCode.MissingWelcome);
//        }

        return null;
    }

    /**
     * conversation下面是user节点，那么必须要配置welcome
     */
    private boolean _rightWelcome() {
        RestProjectComponentConversation conversation = (RestProjectComponentConversation) _component;
        // 验证下引导语
        String welcome = conversation.getData().getWelcome();
        if (StringUtils.isNotBlank(welcome)) {
            return true;
        }

        // 如果子节点是user，那么必须设置引导语
        for (BaseComponentValidator child : _children) {
            if (ProjectComponent.TYPE_USER.equals(child.getType())) {
                return false;
            }
        }

        return true;
    }
}
