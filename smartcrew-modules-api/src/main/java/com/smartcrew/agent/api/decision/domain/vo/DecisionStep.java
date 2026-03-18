package com.smartcrew.agent.api.decision.domain.vo;

import lombok.Builder;
import lombok.Data;

/**
 * DecisionStep 视图对象，封装接口返回给调用方的数据。
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
