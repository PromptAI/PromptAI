package com.zervice.broker.service;

import com.zervice.common.model.MessageRes;
import com.zervice.common.pojo.chat.TimeRecord;
import com.zervice.common.utils.TimeRecordHelper;

import java.util.List;

/**
 * @author chenchen
 * @Date 2023/8/30
 */
public interface BaseMessageService {
    /**
     * 对话之后的处理逻辑
     */
    default void postMessage(MessageRes messageRes) {
        if (messageRes == null) {
            return;
        }

        List<TimeRecord> timeRecords = TimeRecordHelper.get();
        messageRes.mergeTimeRecord(timeRecords);
    }
}
