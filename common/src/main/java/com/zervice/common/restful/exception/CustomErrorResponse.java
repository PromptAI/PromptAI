package com.zervice.common.restful.exception;

import com.zervice.common.i18n.MessageUtils;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.Setter;
import org.springframework.validation.FieldError;

import java.util.Locale;

@Getter @Setter
public class CustomErrorResponse {
    private Long _timestamp;
    private int _status;
    private String _message;

    /**
     * Favor this field if it is valid as backend may set more accurate message in this field
     * TODO: we need to change RestException interface to pass a message in
     */
    private String _description;

    public static CustomErrorResponse of(String message, Integer status, HttpServletResponse response) {
        response.setStatus(status);

        CustomErrorResponse customErrorResponse = new CustomErrorResponse();
        customErrorResponse.setStatus(status);
        customErrorResponse.setMessage(message);
        customErrorResponse.setDescription(null);
        customErrorResponse.setTimestamp(System.currentTimeMillis());
        return customErrorResponse;
    }

    public static CustomErrorResponse of(String message, Integer status, HttpServletResponse response, String description) {
        response.setStatus(status);

        CustomErrorResponse customErrorResponse = new CustomErrorResponse();
        customErrorResponse.setStatus(status);
        customErrorResponse.setMessage(message);
        customErrorResponse.setDescription(description);
        customErrorResponse.setTimestamp(System.currentTimeMillis());
        return customErrorResponse;
    }

    public static CustomErrorResponse of(Locale locale, FieldError fieldError, Integer status, HttpServletResponse response, String description) {
        response.setStatus(status);

        String prop = fieldError.getObjectName() + "." + fieldError.getField();
        String message = MessageUtils.getRestValidatorMessage(prop, locale);
        CustomErrorResponse customErrorResponse = new CustomErrorResponse();
        customErrorResponse.setStatus(status);
        customErrorResponse.setMessage(message);
        customErrorResponse.setDescription(description);
        customErrorResponse.setTimestamp(System.currentTimeMillis());
        return customErrorResponse;
    }
}
