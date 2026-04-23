package com.smartcrew.agent.api.tool.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.smartcrew.agent.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工具定义实体，描述可接入平台的工具配置。
 */
@Data
@TableName("tool_definition")
@EqualsAndHashCode(callSuper = true)
public class ToolDefinition extends BaseEntity {

    /**
     * 主键 ID。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 工具编码。
     */
    private String toolCode;

    /**
     * 工具名称。
     */
    private String toolName;

    /**
     * 工具描述。
     */
    private String description;

    /**
     * Spring Bean 名称。
     */
    private String beanName;

    /**
     * 执行模式。
     */
    private String executionMode;

    /**
     * 风险等级。
     */
    private String riskLevel;

    /**
     * 是否启用。
     */
    private Boolean enabled;

    /**
     * 运行时扩展配置 JSON。
     */
    private String configJson;

    /**
     * 流程定义 JSON。
     */
    private String flowDefinitionJson;
}
