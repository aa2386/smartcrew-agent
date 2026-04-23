package com.smartcrew.agent.api.tool.domain.model;

import com.smartcrew.agent.api.tool.domain.vo.ToolDefinitionVo;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 运行时解析后的工具定义。
 */
@Data
@Builder
public class ResolvedToolDefinition {

    /**
     * 工具主键 ID。
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
     * 扩展配置 JSON。
     */
    private String configJson;

    /**
     * 流程定义 JSON。
     */
    private String flowDefinitionJson;

    /**
     * 工具来源状态。
     */
    private String sourceStatus;

    /**
     * 是否存在代码 Bean 实现。
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
     * 解析失败时的错误信息。
     */
    private String resolveError;

    /**
     * 工具动作元数据列表。
     */
    @Builder.Default
    private List<ToolActionMetadata> actions = new ArrayList<>();

    /**
     * 转换为接口输出视图。
     */
    public ToolDefinitionVo toVo() {
        return ToolDefinitionVo.builder()
                .id(id)
                .toolCode(toolCode)
                .toolName(toolName)
                .description(description)
                .beanName(beanName)
                .executionMode(executionMode)
                .riskLevel(riskLevel)
                .enabled(enabled)
                .configJson(configJson)
                .flowDefinitionJson(flowDefinitionJson)
                .sourceStatus(sourceStatus)
                .hasCodeBean(hasCodeBean)
                .hasDatabaseConfig(hasDatabaseConfig)
                .executable(executable)
                .resolveError(resolveError)
                .actions(actions)
                .build();
    }
}
