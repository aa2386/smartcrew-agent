package com.smartcrew.agent.api.tool.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.smartcrew.agent.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Tool 定义实体，对应数据库中的元数据配置。
 */
@Data
@TableName("tool_definition")
@EqualsAndHashCode(callSuper = true)
public class ToolDefinition extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String toolCode;

    private String toolName;

    private String description;

    private String beanName;

    private String riskLevel;

    private Boolean enabled;

    private String configJson;
}
