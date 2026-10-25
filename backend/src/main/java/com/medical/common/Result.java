package com.medical.common;

import lombok.Data;

/**
 * 统一响应结构：{ code, msg, data }
 */
@Data
public class Result<T> {

    private int code;
    private String msg;
    private T data;

    public static <T> Result<T> ok() {
        return build(200, "success", null);
    }

    public static <T> Result<T> ok(T data) {
        return build(200, "success", data);
    }

    public static <T> Result<T> error(int code, String msg) {
        return build(code, msg, null);
    }

    public static <T> Result<T> error(String msg) {
        return build(400, msg, null);
    }

    public static <T> Result<T> build(int code, String msg, T data) {
        Result<T> r = new Result<>();
        r.setCode(code);
        r.setMsg(msg);
        r.setData(data);
        return r;
    }
}
