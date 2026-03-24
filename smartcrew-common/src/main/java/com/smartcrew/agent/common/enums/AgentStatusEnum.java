package com.smartcrew.agent.common.enums;

/**
 * 代理状态枚举
 */
public enum AgentStatusEnum {
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
    
    AgentStatusEnum(Integer code, String name) {
        this.code = code;
        this.name = name;
    }
    
    public Integer getCode() {
        return code;
    }
    
    public String getName() {
        return name;
    }
    
    public static AgentStatusEnum getByCode(Integer code) {
        for (AgentStatusEnum status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }
}