package com.smartcrew.agent.controller.decision;

import com.smartcrew.agent.api.decision.domain.request.DecisionPlanRequest;
import com.smartcrew.agent.api.decision.domain.vo.DecisionPlanResponse;
import com.smartcrew.agent.api.decision.service.DecisionEngine;
import com.smartcrew.agent.common.domain.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 决策控制器，提供生成决策计划的 REST 接口。
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/decision")
public class DecisionController {

    /**
     * 决策引擎。
     */
    private final DecisionEngine decisionEngine;

    /**
     * 生成决策计划。
     */
    @PostMapping("/plan")
    public R<DecisionPlanResponse> plan(@Valid @RequestBody DecisionPlanRequest request) {
        return R.ok(decisionEngine.plan(request));
    }
}
