package com.smartcrew.agent.common.enums;

/**
 * 提示词模板分类枚举
 */
public enum PromptCategoryEnum {
    /**
     * 系统提示词
     */
    SYSTEM("system", "系统提示词"),
    
    /**
     * 角色扮演
     */
    ROLE_PLAY("role_play", "角色扮演"),
    
    /**
     * 任务特定
     */
    TASK_SPECIFIC("task_specific", "任务特定"),
    
    /**
     * 工具使用
     */
    TOOL_USAGE("tool_usage", "工具使用"),
    
    /**
     * 对话管理
     */
    CONVERSATION("conversation", "对话管理"),
    
    /**
     * 代码生成
     */
    CODE_GENERATION("code_generation", "代码生成"),
    
    /**
     * 内容创作
     */
    CONTENT_CREATION("content_creation", "内容创作");
    
    private final String code;
    private final String name;
    
    PromptCategoryEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getName() {
        return name;
    }
    
    public static PromptCategoryEnum getByCode(String code) {
        for (PromptCategoryEnum category : values()) {
            if (category.code.equals(code)) {
                return category;
            }
        }
        return null;
    }
}