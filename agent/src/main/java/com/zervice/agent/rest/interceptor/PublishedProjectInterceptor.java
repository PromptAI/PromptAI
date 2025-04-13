package com.zervice.agent.rest.interceptor;

import com.zervice.agent.published.project.PublishedProjectManager;
import com.zervice.common.restful.exception.RestException;
import com.zervice.common.restful.exception.StatusCodes;
import com.zervice.common.utils.Constants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * @author Peng Chen
 * @date 2022/8/4
 */
@Log4j2
@Component
public class  PublishedProjectInterceptor implements HandlerInterceptor {

    PublishedProjectManager manager = PublishedProjectManager.getInstance();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String publishedProjectId = request.getHeader(Constants.AGENT_PUBLISHED_PROJECT_HEADER);
        if (StringUtils.isBlank(publishedProjectId) || manager.getProject(publishedProjectId) == null) {
            LOG.info("uri:{}, publishedProjectId:{} required", request.getRequestURI(), publishedProjectId);
            throw new RestException(StatusCodes.BadRequest, "publishedProjectId required");
        }

        return true;
    }
}
