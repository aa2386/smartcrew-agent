package com.smartcrew.agent.api.tool.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.smartcrew.agent.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * ToolDefinition 实体，表示持久化层的 Tool 配置。
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
     * Spring Bean 名称，BEAN 模式下使用。
     */
    private String beanName;

    /**
     * 执行模式：BEAN / FLOW。
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
     * 运行时附加配置 JSON。
     */
    private String configJson;

    /**
     * 顺序流程 DSL 定义 JSON。
     */
    private String flowDefinitionJson;
}
