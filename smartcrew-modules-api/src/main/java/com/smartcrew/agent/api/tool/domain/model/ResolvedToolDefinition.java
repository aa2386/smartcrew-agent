package com.smartcrew.agent.api.tool.domain.model;

import com.smartcrew.agent.api.tool.domain.vo.ToolDefinitionVo;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 运行时解析后的 Tool 定义。
 */
@Data
@Builder
public class ResolvedToolDefinition {

    private Long id;

    private String toolCode;

    private String toolName;

    private String description;

    private String beanName;

    private String riskLevel;

    private Boolean enabled;

    private String configJson;

    private String sourceStatus;

    private Boolean hasCodeBean;

    private Boolean hasDatabaseConfig;

    private Boolean executable;

    private String resolveError;

    @Builder.Default
    private List<ToolActionMetadata> actions = new ArrayList<>();

    public ToolDefinitionVo toVo() {
        return ToolDefinitionVo.builder()
                .id(id)
                .toolCode(toolCode)
                .toolName(toolName)
                .description(description)
                .beanName(beanName)
                .riskLevel(riskLevel)
                .enabled(enabled)
                .configJson(configJson)
                .sourceStatus(sourceStatus)
                .hasCodeBean(hasCodeBean)
                .hasDatabaseConfig(hasDatabaseConfig)
                .executable(executable)
                .resolveError(resolveError)
                .actions(actions)
                .build();
    }
}
