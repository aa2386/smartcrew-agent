package com.smartcrew.agent.api.agent.domain.vo;

import lombok.Data;

/**
 * AgentDefinitionVo 视图对象，封装接口返回给调用方的数据。
 */
@Data
public class AgentDefinitionVo {

    /**
     * 主键 ID。
     */
    private Long id;
    /**
     * 代理编码。
     */
    private String agentCode;
    /**
     * 代理名称。
     */
    private String agentName;
    /**
     * 代理类型。
     */
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
    private Boolean enabled;
    /**
     * JSON 格式的扩展配置。
     */
    private String configJson;
}
