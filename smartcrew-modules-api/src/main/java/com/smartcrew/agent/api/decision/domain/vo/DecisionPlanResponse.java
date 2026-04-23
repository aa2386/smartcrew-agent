package com.smartcrew.agent.api.decision.domain.vo;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 决策计划响应视图对象，封装智能体结构化决策结果。
 *
 * <p>包含决策思考过程、分步执行计划、工具调用安排及最终行动建议，
 * 用于将决策引擎的输出以结构化方式返回给调用方。</p>
 *
 * @see DecisionStep
 * @see PlannedToolCall
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DecisionPlanResponse {

    /**
     * 决策思考摘要。
     */
    private String thought;

    /**
     * 决策步骤列表。
     */
    private List<DecisionStep> steps;

    /**
     * 兼容旧字段：建议使用的工具编码列表。
     */
    private List<String> selectedTools;

    /**
     * 结构化 Tool 调用计划。
     */
    @Builder.Default
    private List<PlannedToolCall> plannedToolCalls = new ArrayList<>();

    /**
     * 最终行动建议。
     */
    private String finalAction;
}
