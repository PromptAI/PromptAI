package com.zervice.kbase.api.restful.controller;

import com.zervice.common.restful.exception.RestException;
import com.zervice.common.restful.exception.StatusCodes;
import com.zervice.kbase.Application;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Log4j2
@RestController
public class VersionController {


    @GetMapping("/api/version")
    public String get() {
        if (!Application.isReady) {
            LOG.warn("[system not ready]");
            throw new RestException(StatusCodes.Forbidden);
        }

        try {
            Resource resource = new ClassPathResource("version.txt");
            return IOUtils.toString(resource.getInputStream());
        } catch (Exception e) {
            LOG.warn("Fail to read version", e);
            return "Fail to read version";
        }
    }
}
