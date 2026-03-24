package com.smartcrew.agent.common.enums;

/**
 * 平台类型枚举
 */
public enum PlatformTypeEnum {
    /**
     * 企业微信
     */
    WECOM("wecom", "企业微信"),
    
    /**
     * 飞书
     */
    FEISHU("feishu", "飞书"),
    
    /**
     * 网页
     */
    WEB("web", "网页"),
    
    /**
     * 移动端
     */
    MOBILE("mobile", "移动端");
    
    private final String code;
    private final String name;
    
    PlatformTypeEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getName() {
        return name;
    }
    
    public static PlatformTypeEnum getByCode(String code) {
        for (PlatformTypeEnum type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}