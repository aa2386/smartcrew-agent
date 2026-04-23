package com.smartcrew.agent.api.tool.domain.vo;

import com.smartcrew.agent.api.tool.domain.model.ToolActionMetadata;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 工具定义视图对象，用于接口返回工具的完整信息。
 *
 * <p>在基础工具属性之上，额外包含来源状态、Bean 存在性、可执行性等运行时判定结果，
 * 以及可调用的动作列表，供前端展示和管理使用。</p>
 *
 * @see com.smartcrew.agent.api.tool.domain.model.ResolvedToolDefinition
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
     * 数据来源状态：CODE_ONLY / DB_ONLY / LINKED。
     */
    private String sourceStatus;

    /**
     * 是否存在代码层实现。
     */
    private Boolean hasCodeBean;

    /**
     * 是否存在数据库配置。
     */
    private Boolean hasDatabaseConfig;

    /**
     * 当前是否可执行。
     */
    private Boolean executable;

    /**
     * 解析失败或不可执行原因。
     */
    private String resolveError;

    /**
     * 可调用动作列表。
     */
    @Builder.Default
    private List<ToolActionMetadata> actions = new ArrayList<>();
}
