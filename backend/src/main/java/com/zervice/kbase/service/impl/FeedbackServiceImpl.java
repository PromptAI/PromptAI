package com.zervice.kbase.service.impl;

import com.zervice.common.pojo.common.Account;
import com.zervice.kbase.ServerInfo;
import com.zervice.kbase.api.restful.pojo.PageRequest;
import com.zervice.kbase.api.restful.pojo.RestFeedback;
import com.zervice.kbase.database.criteria.FeedbackCriteria;
import com.zervice.kbase.database.dao.AccountDao;
import com.zervice.kbase.database.dao.FeedbackDao;
import com.zervice.kbase.database.pojo.Feedback;
import com.zervice.kbase.database.utils.DaoUtils;
import com.zervice.kbase.database.utils.PageResult;
import com.zervice.common.ding.DingTalkSender;
import com.zervice.kbase.service.FeedbackService;
import lombok.Cleanup;
import org.springframework.stereotype.Service;

import java.sql.Connection;

/**
 * @author chen
 * @date 2023/3/24 10:55
 */
@Service
public class FeedbackServiceImpl implements FeedbackService {
    @Override
    public RestFeedback add(RestFeedback feedback, String dbName) throws Exception {
        @Cleanup Connection conn = DaoUtils.getConnection(true);
        long accountId = Account.fromExternalId(dbName);
        Feedback dbFeedback = Feedback.builder()
                .accountId(accountId)
                .contact(feedback.getContact())
                .content(feedback.getContent())
                .time(System.currentTimeMillis())
                .build();

        Account account = AccountDao.getByDbName(conn, dbName);

        long id = FeedbackDao.add(conn, dbFeedback);
        dbFeedback.setId(id);
        String content = ServerInfo.getName() + " account:" + account.getName() + "\n" +
                "contact:" + dbFeedback.getContact() + "\n" +
                "content:" + dbFeedback.getContent() + "\n";
        DingTalkSender.sendQuietly("feedback:\n" + content);
        return new RestFeedback(dbFeedback, account.getName());
    }

    @Override
    public PageResult<Feedback> get(FeedbackCriteria criteria, PageRequest pageRequest) throws Exception {
        @Cleanup Connection conn = DaoUtils.getConnection(true);
        return FeedbackDao.get(conn, criteria, pageRequest);
    }
}
