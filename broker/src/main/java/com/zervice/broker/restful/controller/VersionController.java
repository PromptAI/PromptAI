package com.zervice.broker.restful.controller;

import com.google.common.collect.Maps;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Log4j2
@RestController
public class VersionController {


    @GetMapping("/api/version")
    public Object get() {
        try {
            Resource resource = new ClassPathResource("version.txt");
            Map<String, Object> result = Maps.newHashMapWithExpectedSize(2);
            result.put("version", IOUtils.toString(resource.getInputStream(), StandardCharsets.UTF_8));
            return result;
        }
        catch (Exception e) {
            LOG.warn("Fail to read version", e);
            return "Fail to read version";
        }
    }
}
