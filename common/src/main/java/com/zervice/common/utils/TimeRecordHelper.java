package com.zervice.common.utils;

import com.zervice.common.pojo.chat.TimeRecord;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;

/**
 * 保存时间记录
 *
 * @author chenchen
 * @Date 2023/8/30
 */
@UtilityClass
public class TimeRecordHelper {

    private final ThreadLocal<List<TimeRecord>> threadLocal = new InheritableThreadLocal<>();

    /**
     * 在处理对话之前调用一次即可
     */
    public synchronized void init() {
        List<TimeRecord> timeRecords = threadLocal.get();
        if (timeRecords == null) {
            timeRecords = new ArrayList<>(8);
            threadLocal.set(timeRecords);
        } else {
            // 防止积累之前的对话数据，
            timeRecords.clear();
        }
    }

    public void append(TimeRecord timeRecord) {

        threadLocal.get().add(timeRecord);
    }

    public List<TimeRecord> get() {
        return threadLocal.get();
    }
}
