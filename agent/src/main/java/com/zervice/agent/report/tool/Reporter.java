package com.zervice.agent.report.tool;

/**
 * 持久化的上报队列
 *
 * @author chen
 * @date 2022/9/1
 */
public interface Reporter {

    /**
     * report的数据
     *
     * @param data               d
     * @param publishedProjectId publishedProjectId
     */
    void report(String publishedProjectId, Object data);

    /**
     * 不用上报数据了
     *   - 目前当Agent注册三次失败后，就进入沉默模式，直到下次重启
     */
    void stop();
}
