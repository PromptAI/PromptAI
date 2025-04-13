package com.zervice.kbase.service;

import com.zervice.kbase.api.restful.pojo.PageRequest;
import com.zervice.kbase.api.restful.pojo.RestFeedback;
import com.zervice.kbase.database.criteria.FeedbackCriteria;
import com.zervice.kbase.database.pojo.Feedback;
import com.zervice.kbase.database.utils.PageResult;

public interface FeedbackService {

    RestFeedback add(RestFeedback feedback, String dbName) throws Exception;

    PageResult<Feedback> get(FeedbackCriteria criteria, PageRequest pageRequest) throws Exception;
}
