package com.smartcrew.agent.common.enums;

/**
 * 工具风险等级枚举
 */
public enum ToolRiskLevelEnum {
    /**
     * 低风险
     */
    LOW(1, "低风险"),
    
    /**
     * 中风险
     */
    MEDIUM(2, "中风险"),
    
    /**
     * 高风险
     */
    HIGH(3, "高风险");
    
    private final Integer code;
    private final String name;
    
    ToolRiskLevelEnum(Integer code, String name) {
        this.code = code;
        this.name = name;
    }
    
    public Integer getCode() {
        return code;
    }
    
    public String getName() {
        return name;
    }
    
    public static ToolRiskLevelEnum getByCode(Integer code) {
        for (ToolRiskLevelEnum level : values()) {
            if (level.code.equals(code)) {
                return level;
            }
        }
        return null;
    }
}