package com.smartcrew.agent.common.enums;

/**
 * 工具状态枚举
 */
public enum ToolStatusEnum {
    /**
     * 启用
     */
    ENABLED(1, "启用"),
    
    /**
     * 禁用
     */
    DISABLED(0, "禁用");
    
    private final Integer code;
    private final String name;
    
    ToolStatusEnum(Integer code, String name) {
        this.code = code;
        this.name = name;
    }
    
    public Integer getCode() {
        return code;
    }
    
    public String getName() {
        return name;
    }
    
    public static ToolStatusEnum getByCode(Integer code) {
        for (ToolStatusEnum status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }
}