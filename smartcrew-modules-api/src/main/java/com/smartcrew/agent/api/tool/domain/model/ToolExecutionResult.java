package com.smartcrew.agent.api.tool.domain.model;

import lombok.Builder;
import lombok.Data;

/**
 * Tool 执行结果。
 */
@Data
@Builder
public class ToolExecutionResult {

    private String toolCode;

    private String actionName;

    private Boolean success;

    private Object output;

    private String errorMessage;

    private Long durationMs;
}
