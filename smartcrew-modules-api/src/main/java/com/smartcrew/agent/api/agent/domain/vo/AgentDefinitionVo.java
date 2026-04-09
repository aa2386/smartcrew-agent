package com.smartcrew.agent.api.agent.domain.vo;

import lombok.Data;

/**
 * Agent 定义视图对象。
 */
@Data
public class AgentDefinitionVo {

    /**
     * 主键 ID。
     */
    private Long id;

    /**
     * Agent 编码。
     */
    private String agentCode;

    /**
     * Agent 名称。
     */
    private String agentName;

    /**
     * Agent 类型。
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
     * JSON 扩展配置。
     */
    private String configJson;

    /**
     * 运行时模式。
     */
    private String runtimeMode;

    /**
     * 运行时 Bean 类名。
     */
    private String beanClassName;

    /**
     * 数据来源状态：CODE_ONLY / DB_ONLY / LINKED。
     */
    private String sourceStatus;

    /**
     * 是否存在代码实现。
     */
    private Boolean hasCodeBean;

    /**
     * 是否存在数据库配置。
     */
    private Boolean hasDatabaseConfig;
}
