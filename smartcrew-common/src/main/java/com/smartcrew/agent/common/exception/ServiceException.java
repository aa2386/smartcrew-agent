package com.smartcrew.agent.common.exception;

/**
 * 业务异常类型，用于携带业务状态码和错误信息。
 */
public class ServiceException extends RuntimeException {

    /**
     * 响应状态码。
     */
    private final int code;

    /**
     * 使用默认业务状态码构造异常。
     */
    public ServiceException(String message) {
        this(400, message);
    }

    /**
     * 使用指定业务状态码和错误信息构造异常。
     */
    public ServiceException(int code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 返回业务异常状态码。
     */
    public int getCode() {
        return code;
    }
}
