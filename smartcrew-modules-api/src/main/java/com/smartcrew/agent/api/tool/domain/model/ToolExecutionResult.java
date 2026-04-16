package com.smartcrew.agent.api.tool.domain.model;

import lombok.Builder;
import lombok.Data;

/**
 * Tool 执行结果。
 */
@Data
@Builder
public class ToolExecutionResult {

    /**
     * 工具编码。
     */
    private String toolCode;

    /**
     * 动作名称。
     */
    private String actionName;

    /**
     * 执行模式。
     */
    private String executionMode;

    /**
     * 是否执行成功。
     */
    private Boolean success;

    /**
     * 执行输出。
     */
    private Object output;

    /**
     * 错误信息。
     */
    private String errorMessage;

    /**
     * 耗时毫秒。
     */
    private Long durationMs;
}
