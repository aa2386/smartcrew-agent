package com.smartcrew.agent.api.tool.domain.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Tool 元数据创建/更新请求。
 */
@Data
public class ToolDefinitionRequest {

    @NotBlank
    private String toolCode;

    @NotBlank
    private String toolName;

    @NotBlank
    private String description;

    private String beanName;

    private String riskLevel = "MEDIUM";

    private Boolean enabled = true;

    private String configJson;
}
