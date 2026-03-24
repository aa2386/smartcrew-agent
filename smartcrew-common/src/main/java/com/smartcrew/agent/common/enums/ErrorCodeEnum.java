package com.smartcrew.agent.common.enums;

/**
 * 错误码枚举
 */
public enum ErrorCodeEnum {
    /**
     * 成功
     */
    SUCCESS(200, "成功"),
    
    /**
     * 参数错误
     */
    PARAM_ERROR(400, "参数错误"),
    
    /**
     * 未授权
     */
    UNAUTHORIZED(401, "未授权"),
    
    /**
     * 禁止访问
     */
    FORBIDDEN(403, "禁止访问"),
    
    /**
     * 资源不存在
     */
    NOT_FOUND(404, "资源不存在"),
    
    /**
     * 服务器内部错误
     */
    INTERNAL_ERROR(500, "服务器内部错误"),
    
    /**
     * 服务不可用
     */
    SERVICE_UNAVAILABLE(503, "服务不可用"),
    
    /**
     * LLM 调用失败
     */
    LLM_ERROR(1001, "LLM 调用失败"),
    
    /**
     * 工具调用失败
     */
    TOOL_ERROR(1002, "工具调用失败"),
    
    /**
     * 代理处理失败
     */
    AGENT_ERROR(1003, "代理处理失败"),
    
    /**
     * 平台接入失败
     */
    PLATFORM_ERROR(1004, "平台接入失败"),
    
    /**
     * 记忆系统错误
     */
    MEMORY_ERROR(1005, "记忆系统错误");
    
    private final Integer code;
    private final String message;
    
    ErrorCodeEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
    
    public Integer getCode() {
        return code;
    }
    
    public String getMessage() {
        return message;
    }
    
    public static ErrorCodeEnum getByCode(Integer code) {
        for (ErrorCodeEnum errorCode : values()) {
            if (errorCode.code.equals(code)) {
                return errorCode;
            }
        }
        return null;
    }
}