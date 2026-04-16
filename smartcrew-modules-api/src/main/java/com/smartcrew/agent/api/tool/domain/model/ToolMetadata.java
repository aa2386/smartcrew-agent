package com.smartcrew.agent.api.tool.domain.model;

import lombok.Builder;
import lombok.Data;

/**
 * ToolMetadata 兼容旧接口的简化元数据视图。
 */
@Data
@Builder
public class ToolMetadata {

    /**
     * 工具编码。
     */
    private String toolCode;

    /**
     * 工具名称。
     */
    private String toolName;

    /**
     * 描述信息。
     */
    private String description;

    /**
     * Spring Bean 名称。
     */
    private String beanName;

    /**
     * 风险等级。
     */
    private String riskLevel;

    /**
     * 是否启用。
     */
    private boolean enabled;

    /**
     * 从解析后的 Tool 定义转换兼容视图。
     */
    public static ToolMetadata fromResolved(ResolvedToolDefinition definition) {
        return ToolMetadata.builder()
                .toolCode(definition.getToolCode())
                .toolName(definition.getToolName())
                .description(definition.getDescription())
                .beanName(definition.getBeanName())
                .riskLevel(definition.getRiskLevel())
                .enabled(Boolean.TRUE.equals(definition.getEnabled()))
                .build();
    }
}
