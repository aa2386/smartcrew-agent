package com.smartcrew.agent.api.decision.domain.vo;

import lombok.Builder;
import lombok.Data;

/**
 * ??????????????????????
 */
@Data
@Builder
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
