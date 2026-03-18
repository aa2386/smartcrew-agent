package com.smartcrew.agent.api.tool.service;

/**
 * SmartCrewTool 接口，定义可插拔工具的元信息与执行入口。
 */
public interface SmartCrewTool {

    /**
     * 返回工具编码。
     *
     * @return 对应编码。
     */
    String toolCode();

    /**
     * 返回工具名称。
     *
     * @return 对应名称。
     */
    String toolName();

    /**
     * 返回工具描述信息。
     *
     * @return 描述信息。
     */
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
