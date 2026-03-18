package com.smartcrew.agent.api.decision.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * DecisionPlanResponse 视图对象，封装接口返回给调用方的数据。
 */
@Data
@Builder
public class DecisionPlanResponse {

    /**
     * 决策思考过程。
     */
    private String thought;
    /**
     * 决策步骤列表。
     */
    private List<DecisionStep> steps;
    /**
     * 建议使用的工具列表。
     */
    private List<String> selectedTools;
    /**
     * 最终行动建议。
     */
    private String finalAction;
}
