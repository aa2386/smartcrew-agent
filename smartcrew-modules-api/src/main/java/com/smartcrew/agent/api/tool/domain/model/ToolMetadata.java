package com.smartcrew.agent.api.tool.domain.model;

import lombok.Builder;
import lombok.Data;

/**
 * ToolMetadata 领域模型，描述业务流程中的中间数据结构。
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
}
