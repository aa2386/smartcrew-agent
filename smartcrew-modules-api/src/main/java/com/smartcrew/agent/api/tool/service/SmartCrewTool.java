package com.smartcrew.agent.api.tool.service;

/**
 * SmartCrewTool 接口，定义可插拔工具的元信息与执行入口。
 */
public interface SmartCrewTool {

    String toolCode();

    String toolName();

    String description();

    /**
     * 返回风险等级。
     */
    default String riskLevel() {
        return "MEDIUM";
    }

    /**
     * 返回是否默认启用。
     */
    default boolean enabledByDefault() {
        return true;
    }
}
