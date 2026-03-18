package com.smartcrew.agent.api.agent.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.smartcrew.agent.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * ?????????????????????
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
