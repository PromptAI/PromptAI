package com.zervice.kbase.validator.error;

import com.zervice.common.i18n.MessageUtils;
import com.zervice.common.utils.ServletUtils;
import lombok.*;

import java.util.Set;

/**
 * @author chenchen
 * @Date 2023/10/7
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ValidatorError {

    private ErrorCode _errorCode;
    private Object[] _args;
    private Set<String> _conflictComponents;
    private String _errorMessage;

    public static ValidatorError factory(ErrorCode errorCode) {
        return factory(errorCode, null, null);
    }

    public static ValidatorError factory(ErrorCode errorCode, Object[] args) {
        return factory(errorCode, args, null);
    }

    public static ValidatorError factory(ErrorCode errorCode, Set<String> conflictComponents) {
        return factory(errorCode, null, conflictComponents);
    }

    public static ValidatorError factory(ErrorCode errorCode, Object[] args,
                                         Set<String> conflictComponents) {
        return ValidatorError.builder()
                .errorCode(errorCode)
                .args(args).conflictComponents(conflictComponents)
                .build();
    }

    public String buildError() {
        _errorMessage = MessageUtils.getComponentValidatorMessage(_errorCode.getCode() + "", ServletUtils.getLocale(), _args);
        return _errorMessage;
    }
}
