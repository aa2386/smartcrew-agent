package com.smartcrew.agent.api.tool.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.smartcrew.agent.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * ???????????????????
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
     * 描述信息。
     */
    private String description;
    /**
     * Spring Bean 名称。
     */
    private String beanName;
    /**
     * 风险等级。
     */
    private String riskLevel;
    /**
     * 是否启用。
     */
    private Boolean enabled;
    /**
     * JSON 格式的扩展配置。
     */
    private String configJson;
}
