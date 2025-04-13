package com.zervice.kbase.api.restful.controller;

import com.zervice.kbase.api.restful.AuthFilter;
import com.zervice.kbase.api.restful.pojo.RestFeedback;
import com.zervice.kbase.service.FeedbackService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Log4j2
@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {

    @Autowired
    private FeedbackService feedbackService;
    @PostMapping
    public Object save(@RequestBody @Validated RestFeedback feedback,
                       @RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName)throws Exception {
        return feedbackService.add(feedback, dbName);
    }

}
