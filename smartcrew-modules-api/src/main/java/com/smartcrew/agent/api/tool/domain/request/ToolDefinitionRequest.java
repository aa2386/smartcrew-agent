package com.smartcrew.agent.api.tool.domain.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 工具定义创建/更新请求对象，用于接收前端提交的工具配置参数。
 *
 * <p>包含工具的基本属性、执行模式、风险等级及扩展配置，
 * 通过 JSR-303 注解进行入参校验。</p>
 *
 * @see com.smartcrew.agent.api.tool.domain.entity.ToolDefinition
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

}
