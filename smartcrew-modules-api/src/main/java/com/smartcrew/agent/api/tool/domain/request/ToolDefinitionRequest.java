package com.smartcrew.agent.api.tool.domain.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * ToolDefinitionRequest 请求对象。
 */
@Data
public class ToolDefinitionRequest {

    /**
     * 工具编码。
     */
    @NotBlank
    private String toolCode;

    /**
     * 工具名称。
     */
    @NotBlank
    private String toolName;

    /**
     * 描述信息。
     */
    @NotBlank
    private String description;

    /**
     * Spring Bean 名称，BEAN 模式下使用。
     */
    private String beanName;

    /**
     * 执行模式：BEAN / FLOW。
     */
    @NotBlank
    private String executionMode = "BEAN";

    /**
     * 风险等级。
     */
    private String riskLevel = "MEDIUM";

    /**
     * 是否启用。
     */
    private Boolean enabled = true;

    /**
     * 运行时附加配置 JSON。
     */
    private String configJson;

    /**
     * 顺序流程 DSL 定义 JSON。
     */
    private String flowDefinitionJson;
}
