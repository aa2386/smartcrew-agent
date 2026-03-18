package com.smartcrew.agent.api.decision.service;

import com.smartcrew.agent.api.decision.domain.request.DecisionPlanRequest;
import com.smartcrew.agent.api.decision.domain.vo.DecisionPlanResponse;

/**
 * DecisionEngine 接口，负责执行核心决策与推理流程。
 */
public interface DecisionEngine {

    DecisionPlanResponse plan(DecisionPlanRequest request);
}
