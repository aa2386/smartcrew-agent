package com.smartcrew.agent.api.decision.domain.vo;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 结构化工具调用计划项，描述单次工具调用的完整信息。
 *
 * <p>包含目标工具编码、动作名称、调用参数及规划原因，
 * 用于在决策计划中精确描述每个工具调用的细节。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
