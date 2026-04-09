package com.smartcrew.agent.api.admin.domain.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Agent 配置更新请求。
 */
@Data
public class AgentConfigUpdateRequest {

    /**
     * Agent 编码。
     */
    @NotBlank
    private String agentCode;

    /**
     * Agent 名称。
     */
    @NotBlank
    private String agentName;

    /**
     * Agent 类型。
     */
    @NotBlank
    private String agentType;

    /**
     * 描述信息。
     */
    private String description;

    /**
     * 策略类型。
     */
    private String strategyType;

    /**
     * 系统提示词。
     */
    private String systemPrompt;

    /**
     * 是否启用。
     */
    private Boolean enabled = true;

    /**
     * 扩展配置 JSON。
     */
    private String configJson;
}
