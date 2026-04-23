package com.smartcrew.agent.api.tool.domain.vo;

import com.smartcrew.agent.api.tool.domain.model.ToolActionMetadata;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Tool 定义视图对象。
 */
@Data
@Builder
public class ToolDefinitionVo {

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
}
