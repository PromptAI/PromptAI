package com.zervice.agent.rest.interceptor;

import com.zervice.common.restful.exception.RestException;
import com.zervice.common.restful.exception.StatusCodes;
import com.zervice.common.utils.Constants;
import com.zervice.common.utils.NetworkUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * @author Peng Chen
 * @date 2022/6/29
 */
@Log4j2
@Component
public class AuthInterceptor implements HandlerInterceptor {
    @Value("${AGENT_ID}")
    private String agentId;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String auth = request.getHeader(Constants.AGENT_ID_HEADER);
        String ip = NetworkUtils.getRemoteIP(request);
        if (!agentId.equals(auth)) {
            LOG.warn("Unauthorized request:{} from:{}", request.getRequestURI(), ip);
            throw new RestException(StatusCodes.Unauthorized);
        }

        return true;
    }
}
