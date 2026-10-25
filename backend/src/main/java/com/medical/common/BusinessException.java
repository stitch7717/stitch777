package com.medical.common;

import lombok.Getter;

/**
 * 业务异常，携带响应 code（默认 400 参数错误）
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(String msg) {
        this(400, msg);
    }

    public BusinessException(int code, String msg) {
        super(msg);
        this.code = code;
    }
}
