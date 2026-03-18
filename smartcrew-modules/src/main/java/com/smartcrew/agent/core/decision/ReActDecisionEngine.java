package com.smartcrew.agent.core.decision;

import com.smartcrew.agent.api.decision.domain.request.DecisionPlanRequest;
import com.smartcrew.agent.api.decision.domain.vo.DecisionPlanResponse;
import com.smartcrew.agent.api.decision.domain.vo.DecisionStep;
import com.smartcrew.agent.api.decision.service.DecisionEngine;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 基于占位逻辑的 ReAct 决策引擎实现。
 */
@Service
public class ReActDecisionEngine implements DecisionEngine {

    /**
     * 生成决策计划。
     */
    @Override
    public DecisionPlanResponse plan(DecisionPlanRequest request) {
        return DecisionPlanResponse.builder()
                .thought("Analyze input and prepare a safe placeholder ReAct plan.")
                .steps(List.of(
                        DecisionStep.builder().stage("observe").action("Inspect the incoming user input").build(),
                        DecisionStep.builder().stage("think").action("Break the intent into workable sub-problems").build(),
                        DecisionStep.builder().stage("act").action("Select candidate tools and agents for execution").build(),
                        DecisionStep.builder().stage("summarize").action("Return a structured next action").build()
                ))
                .selectedTools(List.of("basic", "web-page"))
                .finalAction("Await downstream execution wiring for agent " + request.getAgentCode())
                .build();
    }
}
