package com.smartcrew.agent.api.decision.domain.vo;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 决策步骤视图对象，表示决策计划中的单个执行阶段。
 *
 * <p>每个步骤由阶段标识和对应动作组成，用于描述决策链路中的关键节点。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DecisionStep {

    /**
     * 决策阶段。
     */
    private String stage;
    /**
     * 阶段动作。
     */
    private String action;
}
