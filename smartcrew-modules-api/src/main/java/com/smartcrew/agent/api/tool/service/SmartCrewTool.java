package com.smartcrew.agent.api.tool.service;

/**
 * SmartCrew ???????????????????????
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
