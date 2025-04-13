package com.zervice.agent.report.tool;

public class ReportData {
    public Object data;
    public String publishedProjectId;
    public long generatedTime;

    public ReportData(String publishedProjectId, Object data, long generatedTime) {
        this.publishedProjectId = publishedProjectId;
        this.data = data;
        this.generatedTime = generatedTime;
    }
}
