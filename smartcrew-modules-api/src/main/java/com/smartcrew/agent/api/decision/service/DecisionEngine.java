package com.smartcrew.agent.api.decision.service;

import com.smartcrew.agent.api.decision.domain.request.DecisionPlanRequest;
import com.smartcrew.agent.api.decision.domain.vo.DecisionPlanResponse;

/**
 * ?????????????????????
 */
public interface DecisionEngine {

    DecisionPlanResponse plan(DecisionPlanRequest request);
}
