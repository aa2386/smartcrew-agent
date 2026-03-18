package com.smartcrew.agent.api.agent.domain.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * ?????????????????????
 */
@Data
public class AgentRegisterRequest {

    /**
     * 代理编码。
     */
    @NotBlank
    private String agentCode;

    /**
     * 代理名称。
     */
    @NotBlank
    private String agentName;

    /**
     * 代理类型。
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
     * JSON 格式的扩展配置。
     */
    private String configJson;
}
