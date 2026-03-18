package com.smartcrew.agent.common.domain;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 通用响应包装对象，用于统一 REST 接口返回格式。
 */
@Data
public class R<T> implements Serializable {

    /**
     * 序列化版本号。
     */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 请求是否成功。
     */
    private boolean success;
    /**
     * 响应状态码。
     */
    private int code;
    /**
     * 消息内容。
     */
    private String message;
    /**
     * 响应数据。
     */
    private T data;

    /**
     * 构造仅包含数据体的成功响应。
     */
    public static <T> R<T> ok(T data) {
        R<T> result = new R<>();
        result.setSuccess(true);
        result.setCode(200);
        result.setMessage("success");
        result.setData(data);
        return result;
    }

    /**
     * 构造包含提示信息的数据成功响应。
     */
    public static <T> R<T> ok(String message, T data) {
        R<T> result = ok(data);
        result.setMessage(message);
        return result;
    }

    /**
     * 构造默认状态码的失败响应。
     */
    public static <T> R<T> fail(String message) {
        return fail(400, message);
    }

    /**
     * 构造指定状态码的失败响应。
     */
    public static <T> R<T> fail(int code, String message) {
        R<T> result = new R<>();
        result.setSuccess(false);
        result.setCode(code);
        result.setMessage(message);
        return result;
    }
}
