package com.smartcrew.agent.api.tool.domain.vo;

import lombok.Builder;
import lombok.Data;

/**
 * ToolDefinitionVo 视图对象，封装接口返回给调用方的数据。
 */
@Data
@Builder
public class ToolDefinitionVo {

    /**
     * 主键 ID。
     */
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
