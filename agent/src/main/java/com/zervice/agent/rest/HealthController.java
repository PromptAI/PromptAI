package com.zervice.agent.rest;

import com.zervice.agent.utils.ConfConstant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * indicate agent status of serviceable
 *
 * @author Peng Chen
 * @date 2022/6/27
 */
@RestController
@RequestMapping("api")
public class HealthController {

    @GetMapping("health")
    public Object health() {
        return "ok";
    }

    @GetMapping("version")
    public Object version() {
        return ConfConstant.VERSION;
    }
}
