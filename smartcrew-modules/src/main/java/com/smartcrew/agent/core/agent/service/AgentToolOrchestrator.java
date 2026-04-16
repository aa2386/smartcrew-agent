package com.smartcrew.agent.core.agent.service;

import com.smartcrew.agent.api.decision.domain.vo.PlannedToolCall;
import com.smartcrew.agent.api.tool.domain.model.ToolExecutionResult;
import com.smartcrew.agent.api.tool.service.ToolExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Agent Tool 编排器，负责顺序执行规划出的 Tool 调用。
 */
@Service
@RequiredArgsConstructor
public class AgentToolOrchestrator {

    private final ToolExecutor toolExecutor;

    /**
     * 顺序执行 Tool 调用计划，遇到失败后终止后续调用。
     */
    public List<ToolExecutionResult> execute(String agentCode,
                                             List<PlannedToolCall> plannedToolCalls,
                                             Map<String, Object> baseExecutionContext) {
        if (plannedToolCalls == null || plannedToolCalls.isEmpty()) {
            return List.of();
        }
        List<ToolExecutionResult> results = new ArrayList<>();
        for (PlannedToolCall plannedToolCall : plannedToolCalls) {
            Map<String, Object> executionContext = new LinkedHashMap<>();
            if (baseExecutionContext != null) {
                executionContext.putAll(baseExecutionContext);
            }
            executionContext.put("agentCode", agentCode);
            executionContext.put("toolReason", plannedToolCall.getReason());
            try {
                ToolExecutionResult result = toolExecutor.execute(
                        plannedToolCall.getToolCode(),
                        plannedToolCall.getActionName(),
                        plannedToolCall.getArguments(),
                        executionContext
                );
                results.add(result);
            } catch (Exception exception) {
                results.add(ToolExecutionResult.builder()
                        .toolCode(plannedToolCall.getToolCode())
                        .actionName(plannedToolCall.getActionName())
                        .success(Boolean.FALSE)
                        .errorMessage(exception.getMessage())
                        .build());
                break;
            }
        }
        return results;
    }
}
