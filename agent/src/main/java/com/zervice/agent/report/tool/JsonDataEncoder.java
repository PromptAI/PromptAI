package com.zervice.agent.report.tool;

import com.alibaba.fastjson.JSON;

/**
 * Helper class to encode data
 *
 * The input would be our internal JSON format, the output might be
 *    JSONformat - i.e. without change
 *    GRPC       - turned into google GRPC protocol
 *    ...
 */
public class JsonDataEncoder implements Encoder<JSON> {
    public static final JsonDataEncoder instance = new JsonDataEncoder();

    @Override
    public JSON encode(Object input) {
        if (input instanceof String) {
            return JSON.parseObject((String)input);
        }
        return (JSON)JSON.toJSON(input);
    }
}
