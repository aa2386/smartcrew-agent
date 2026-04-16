package com.smartcrew.agent.api.decision.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * DecisionPlanResponse 视图对象，封装结构化决策结果。
 */
@Data
@Builder
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
