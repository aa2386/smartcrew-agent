package com.smartcrew.agent.api.tool.domain.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * ToolDefinitionRequest 请求对象，封装接口调用所需的入参数据。
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
     * Spring Bean 名称。
     */
    @NotBlank
    private String beanName;

    /**
     * 风险等级。
     */
    private String riskLevel = "MEDIUM";
    /**
     * 是否启用。
     */
    private Boolean enabled = true;
    /**
     * JSON 格式的扩展配置。
     */
    private String configJson;
}
