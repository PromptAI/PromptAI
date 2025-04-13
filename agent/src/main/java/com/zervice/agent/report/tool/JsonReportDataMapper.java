package com.zervice.agent.report.tool;

import com.alibaba.fastjson.JSON;
import com.zoomphant.common.util.GzipUtil;
import com.zoomphant.common.util.QueueDataMapper;

public class JsonReportDataMapper implements QueueDataMapper<ReportData> {

    @Override
    public ReportData bytes2object(byte[] bytes) {
        try {
            byte[] un = GzipUtil.decompress(bytes);
            ReportData obj = JSON.parseObject(new String(un), ReportData.class);
            return obj;
        }
        catch (Exception e) {
            ReportData obj = JSON.parseObject(new String(bytes), ReportData.class);
            return obj;
        }
    }

    @Override
    public byte[] object2bytes(ReportData reportData) {
        try {
            byte[] uncompressed = JSON.toJSON(reportData).toString().getBytes("UTF-8");
            return GzipUtil.compress(uncompressed);
        }
        catch (Exception e) {
            return JSON.toJSON(reportData).toString().getBytes();
        }
    }
}