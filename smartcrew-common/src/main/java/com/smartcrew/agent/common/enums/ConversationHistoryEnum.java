package com.smartcrew.agent.common.enums;

/**
 * 会话历史枚举
 */
public enum ConversationHistoryEnum {
    /**
     * 系统消息
     */
    SYSTEM("system", "系统消息"),
    
    /**
     * 用户消息
     */
    USER("user", "用户消息"),
    
    /**
     * AI 消息
     */
    AI("ai", "AI 消息"),
    
    /**
     * 助手消息
     */
    ASSISTANT("assistant", "助手消息"),
    
    /**
     * 工具消息
     */
    TOOL("tool", "工具消息");
    
    private final String code;
    private final String name;
    
    ConversationHistoryEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getName() {
        return name;
    }
    
    public static ConversationHistoryEnum getByCode(String code) {
        for (ConversationHistoryEnum type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}
