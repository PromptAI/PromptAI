package com.zervice.common.restful.exception;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
public class RestException extends RuntimeException {
    @Getter
    @Setter
    private StatusCodes _status;

    @Getter
    Object[] _args;

    public RestException(StatusCodes code, Object ... args) {
        super("error " + code);
        this._status = code;
        this._args = args;
    }

}
