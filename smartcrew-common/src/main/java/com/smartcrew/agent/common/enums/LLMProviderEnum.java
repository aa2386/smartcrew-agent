package com.smartcrew.agent.common.enums;

/**
 * 大模型供应商枚举
 */
public enum LLMProviderEnum {
    /**
     * 阿里云千问
     */
    DASHSCOPE("dashscope", "阿里云千问"),
    
    /**
     * 智谱 GLM
     */
    ZHIPU("zhipu", "智谱 GLM"),
    
    /**
     * OpenAI
     */
    OPENAI("openai", "OpenAI"),
    
    /**
     * Azure OpenAI
     */
    AZURE("azure", "Azure OpenAI"),
    
    /**
     * 百度文心一言
     */
    ERNIE("ernie", "百度文心一言"),
    
    /**
     * 讯飞星火
     */
    SPARK("spark", "讯飞星火");
    
    private final String code;
    private final String name;
    
    LLMProviderEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getName() {
        return name;
    }
    
    public static LLMProviderEnum getByCode(String code) {
        for (LLMProviderEnum provider : values()) {
            if (provider.code.equals(code)) {
                return provider;
            }
        }
        return null;
    }
}