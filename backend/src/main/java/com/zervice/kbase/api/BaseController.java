package com.zervice.kbase.api;

import com.zervice.common.restful.exception.RestException;
import com.zervice.common.restful.exception.StatusCodes;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.autoconfigure.web.ErrorProperties;
import org.springframework.boot.autoconfigure.web.servlet.error.BasicErrorController;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 所有controller需要继承这个class
 * 解决filter中 异常不能能被@ExceptionnHandler 处理
 */
@Log4j2
@RestController
public class BaseController extends BasicErrorController {
    public BaseController() {
        super(new DefaultErrorAttributes(), new ErrorProperties());
    }
    @Override
    public ResponseEntity<Map<String, Object>> error(HttpServletRequest request) {
        Map<String, Object> body = getErrorAttributes(request, ErrorAttributeOptions.defaults());
        LOG.error("Found error page req {}", body);
        throw new RestException(StatusCodes.parse(String.valueOf(body.get("status"))));
    }
}
