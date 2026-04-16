package com.smartcrew.agent.api.decision.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 结构化 Tool 调用计划项。
 */
@Data
@Builder
public class PlannedToolCall {

    /**
     * 工具编码。
     */
    private String toolCode;

    /**
     * 动作名称。
     */
    private String actionName;

    /**
     * 调用参数。
     */
    @Builder.Default
    private Map<String, Object> arguments = new LinkedHashMap<>();

    /**
     * 规划原因说明。
     */
    private String reason;
}
