package com.zervice.common.restful.exception;

import com.zervice.common.ding.DingTalkSender;
import com.zervice.common.filter.FilterConfigName;
import com.zervice.common.i18n.MessageUtils;
import com.zervice.common.utils.NetworkUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.util.Locale;
import java.util.Objects;

@Log4j2
@RestControllerAdvice
public class CustomGlobalExceptionHandler {


    /**
     * 处理所有接口数据验证异常
     *
     * @param e 接口参数校验异常
     * @return 响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public CustomErrorResponse handleMethodArgumentNotValidException(Locale locale,
                                                                     MethodArgumentNotValidException e,
                                                                     HttpServletResponse response) {
        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            LOG.error("[请求参数校验失败:][field:{}, msg:{}]",
                    fieldError.getField(), fieldError.getDefaultMessage(), e);
            return CustomErrorResponse.of(locale, fieldError, HttpStatus.BAD_REQUEST.value(), response, "");
        }
        String msg = Objects.requireNonNull(e.getBindingResult().getFieldError()).getDefaultMessage();
        return CustomErrorResponse.of(getMsgByStatus(locale, StatusCodes.RequestParamValidateFail, msg), HttpStatus.BAD_REQUEST.value(), response);
    }

    /**
     * 处理所有接口数据验证异常
     *
     * @param e 接口参数校验异常
     * @return 响应
     */
    @ExceptionHandler(BindException.class)
    public CustomErrorResponse handleBindException(Locale locale,
                                                   BindException e,
                                                   HttpServletResponse response) {
        LOG.error("[参数校验异常]", e);
        return CustomErrorResponse.of(getMsgByStatus(locale, StatusCodes.BadRequest), HttpStatus.BAD_REQUEST.value(), response);
    }

    /**
     * 处理所有接口数据绑定异常
     *
     * @param e 接口参数绑定异常
     * @return 响应
     */
    @ExceptionHandler({HttpMessageNotReadableException.class})
    public CustomErrorResponse handleHttpMessageNotReadableException(Locale locale,
                                                                     HttpMessageNotReadableException e,
                                                                     HttpServletResponse response) {
        LOG.error("[参数绑定异常]", e);
        return CustomErrorResponse.of(getMsgByStatus(locale, StatusCodes.BadRequest), HttpStatus.BAD_REQUEST.value(), response);
    }

    /**
     * 自定义业务异常
     *
     * @param ex 自定义异常
     * @return 响应
     */
    @ExceptionHandler(RestException.class)
    public CustomErrorResponse customHandleNotFound(Locale locale,
                                                    RestException ex,
                                                    WebRequest request, HttpServletResponse response) {
        LOG.warn("Found rest exception code={},args={}", ex.getStatus(), ex.getArgs());
        StatusCodes status = ex.getStatus();
        if (request instanceof ServletWebRequest) {
            ServletWebRequest webRequest = (ServletWebRequest) request;
            if (status.getHttpStatusCode() == HttpServletResponse.SC_UNAUTHORIZED) {
                LOG.warn("unauthorized request - " + webRequest.getRequest().getServletPath());
            } else {
                LOG.error("[Unexpected Rest Exception got for request:{} error:{}]", webRequest.getRequest().getServletPath(), ex.getMessage(), ex);
            }
        } else {
            LOG.error("Unexpected Rest Exception got - " + request.toString(), ex);
        }

        return CustomErrorResponse.of(getMsgByStatus(locale, status, ex.getArgs()), ex.getStatus().getHttpStatusCode(), response);
    }

    private String getMsgByStatus(Locale locale,
                                  StatusCodes statusCodes,
                                  Object... optionalArgs) {
        return MessageUtils.getExceptionMessage("error." + statusCodes.getInternalStatusCode(), locale, optionalArgs);
    }

    /**
     * 自定义权限控制异常
     *
     * @param e 权限控制异常
     * @return 相应
     */
    @ExceptionHandler(AccessControlException.class)
    public CustomErrorResponse handleAccessControlException(Locale locale,
                                                            AccessControlException e,
                                                            HttpServletResponse response) {
        LOG.error("[forbidden]- error:{}", e.getMessage(), e);
        return CustomErrorResponse.of(getMsgByStatus(locale, StatusCodes.Forbidden), HttpStatus.FORBIDDEN.value(), response);
    }

    /**
     * 未拦截异常
     *
     * @param e 异常
     * @return 相应
     */
    @ExceptionHandler(Exception.class)
    public CustomErrorResponse handleException(Locale locale,
                                               Exception e, HttpServletRequest request,
                                               HttpServletResponse response) {
        String dbName = request.getHeader(FilterConfigName.X_EXTERNAL_ACCOUNT_ID_HEADER);
        String uri = request.getRequestURI();
        String host = NetworkUtils.getRemoteHost(request);
        DingTalkSender.sendQuietly(String.format("[%s:%s][unknown error:%s from:%s ]", host, dbName, e.getMessage(), uri));
        LOG.error("[unknown error from uri:{}]- error", request.getRequestURI(), e);
        return CustomErrorResponse.of(getMsgByStatus(locale, StatusCodes.InternalError), HttpStatus.INTERNAL_SERVER_ERROR.value(), response);
    }

    /**
     * 参数异常
     *
     * @param e 异常
     * @return 相应
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public CustomErrorResponse handleIllegalArgumentException(Locale locale,
                                                              IllegalArgumentException e,
                                                              HttpServletRequest request,
                                                              HttpServletResponse response) {
        LOG.error("[illegal arg from uri:{},param:{}]- error",
                request.getRequestURI(), request.getParameterMap(), e);
        return CustomErrorResponse.of(getMsgByStatus(locale, StatusCodes.BadRequest), HttpStatus.BAD_REQUEST.value(), response);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public CustomErrorResponse handleHttpRequestMethodNotSupportedException(
            Locale locale,
            HttpRequestMethodNotSupportedException e,
            HttpServletRequest request,
            HttpServletResponse response) {
        LOG.error("[method not support error with uri:{}]- error:{}", request.getRequestURI(), e.getMessage(), e);
        return CustomErrorResponse.of(getMsgByStatus(locale, StatusCodes.BadRequest), HttpStatus.METHOD_NOT_ALLOWED.value(), response);
    }
}
