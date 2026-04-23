package com.smartcrew.agent.api.agent.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.smartcrew.agent.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Agent 定义实体，描述智能体的基础配置。
 */
@Data
@TableName("agent_definition")
@EqualsAndHashCode(callSuper = true)
public class AgentDefinition extends BaseEntity {

    /**
     * 主键 ID。
     */
    @TableId(type = IdType.AUTO)
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
     * Agent 描述。
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
     * 扩展配置 JSON。
     */
    private String configJson;
}
