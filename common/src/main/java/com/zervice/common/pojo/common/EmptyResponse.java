package com.zervice.common.pojo.common;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize
public class EmptyResponse {

    private static final EmptyResponse _empty = new EmptyResponse();
    /**
     * create a new EmptyResponse
     *
     * @return EmptyResponse
     */
    public static EmptyResponse empty() {
        return _empty;
    }
}
