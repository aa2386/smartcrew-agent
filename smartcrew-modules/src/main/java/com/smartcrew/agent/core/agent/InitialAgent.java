package com.smartcrew.agent.core.agent;

import com.smartcrew.agent.api.agent.domain.model.AgentDispatchCommand;
import com.smartcrew.agent.api.agent.domain.vo.AgentDispatchResponse;
import com.smartcrew.agent.api.agent.service.Agent;
import com.smartcrew.agent.api.agent.service.AgentToolBindingService;
import com.smartcrew.agent.api.decision.domain.request.DecisionPlanRequest;
import com.smartcrew.agent.api.decision.domain.vo.DecisionPlanResponse;
import com.smartcrew.agent.api.llm.domain.request.LlmChatRequest;
import com.smartcrew.agent.api.llm.domain.vo.LlmChatResponse;
import com.smartcrew.agent.api.llm.service.LlmClient;
import com.smartcrew.agent.api.rag.domain.vo.RagAugmentationResult;
import com.smartcrew.agent.api.rag.service.RagAugmentationService;
import com.smartcrew.agent.api.tool.domain.model.ResolvedToolDefinition;
import com.smartcrew.agent.api.tool.domain.model.ToolExecutionResult;
import com.smartcrew.agent.api.decision.service.DecisionEngine;
import com.smartcrew.agent.common.util.JsonUtils;
import com.smartcrew.agent.core.agent.service.AgentToolOrchestrator;
import com.smartcrew.agent.core.agent.service.InitialAgentPromptService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * SmartCrew 的初始智能体。
 */
@Component
@RequiredArgsConstructor
public class InitialAgent implements Agent {

    private final LlmClient llmClient;
    private final InitialAgentPromptService promptService;
    private final Optional<RagAugmentationService> ragAugmentationService;// 当项目配置为不启用RAG时可能为null，所以使用Optional
    private final AgentToolBindingService agentToolBindingService;
    private final DecisionEngine decisionEngine;
    private final AgentToolOrchestrator agentToolOrchestrator;

    /**
     * 返回 Agent 唯一编码。
     */
    @Override
    public String code() {
        return "initial-agent";
    }

    /**
     * 返回 Agent 显示名称。
     */
    @Override
    public String name() {
        return "初始智能体";
    }

    /**
     * 判断是否支持指定能力。
     *
     * @param capability 能力标识
     * @return 是否支持
     */
    @Override
    public boolean supports(String capability) {
        return "chat".equalsIgnoreCase(capability)
                || "orchestrate".equalsIgnoreCase(capability)
                || "rag".equalsIgnoreCase(capability);
    }

    /**
     * 处理用户指令，执行 RAG 检索、工具决策与调用、LLM 对话。
     *
     * @param command Agent 派发指令
     * @return 处理响应
     */
    @Override
    public AgentDispatchResponse handle(AgentDispatchCommand command) {
        String llmSessionId = code() + "::" + command.getSessionId();
        // 构建RAG检索结果
        RagAugmentationResult augmentationResult = resolveRagAugmentation(command);
        // 构建工具决策计划
        List<ResolvedToolDefinition> availableTools = agentToolBindingService.listEnabledResolvedToolsByAgentCode(code());
        DecisionPlanResponse decisionPlan = buildDecisionPlan(command, availableTools);
        List<ToolExecutionResult> toolResults = agentToolOrchestrator.execute(
                code(),
                decisionPlan.getPlannedToolCalls(),
                buildExecutionContext(command, augmentationResult, availableTools)
        );

        LlmChatRequest request = LlmChatRequest.builder()
                .userId(command.getUserId())
                .sessionId(llmSessionId)
                .userMessage(buildUserMessage(command.getMessage(), toolResults))
                .systemPrompt(promptService.buildSystemPrompt(code(), command.getUserId(), augmentationResult.getPromptBlock()))
                .traceId(command.getTraceId())
                .build();

        LlmChatResponse response = llmClient.chat(request);
        if (!Boolean.TRUE.equals(response.getSuccess())) {
            return AgentDispatchResponse.builder()
                    .traceId(command.getTraceId())
                    .agentCode(code())
                    .accepted(false)
                    .message(response.getErrorMessage() == null ? "当前无法处理请求，请稍后再试" : response.getErrorMessage())
                    .build();
        }

        return AgentDispatchResponse.builder()
                .traceId(command.getTraceId())
                .agentCode(code())
                .accepted(true)
                .message(response.getContent())
                .build();
    }

    /* 构建工具决策计划。 */
    private DecisionPlanResponse buildDecisionPlan(AgentDispatchCommand command, List<ResolvedToolDefinition> availableTools) {
        DecisionPlanRequest request = new DecisionPlanRequest();
        request.setAgentCode(code());
        request.setUserId(command.getUserId());
        request.setInput(command.getMessage());
        Map<String, Object> context = new LinkedHashMap<>();
        if (command.getContext() != null) {
            context.putAll(command.getContext());
        }
        context.put("availableTools", availableTools);
        request.setContext(context);
        return decisionEngine.plan(request);
    }

    /* 构建工具执行上下文。 */
    private Map<String, Object> buildExecutionContext(AgentDispatchCommand command,
                                                      RagAugmentationResult augmentationResult,
                                                      List<ResolvedToolDefinition> availableTools) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("traceId", command.getTraceId());
        context.put("sessionId", command.getSessionId());
        context.put("userId", command.getUserId());
        context.put("input", command.getMessage());
        context.put("ragHitCount", augmentationResult.getHitCount());
        context.put("availableTools", availableTools.stream().map(ResolvedToolDefinition::toVo).toList());
        if (command.getContext() != null) {
            context.putAll(command.getContext());
        }
        return context;
    }

    /* 构建包含工具执行结果的用户消息。 */
    private String buildUserMessage(String originalMessage, List<ToolExecutionResult> toolResults) {
        if (toolResults == null || toolResults.isEmpty()) {
            return originalMessage;
        }
        StringBuilder builder = new StringBuilder();
        builder.append("用户原始问题：").append(originalMessage).append(System.lineSeparator()).append(System.lineSeparator());
        builder.append("已执行的工具结果如下，请优先基于这些结果回答：").append(System.lineSeparator());
        int index = 1;
        for (ToolExecutionResult result : toolResults) {
            builder.append(index++)
                    .append(". ")
                    .append(result.getToolCode())
                    .append("#")
                    .append(result.getActionName())
                    .append(System.lineSeparator());
            if (Boolean.TRUE.equals(result.getSuccess())) {
                builder.append("输出：").append(formatOutput(result.getOutput())).append(System.lineSeparator());
            } else {
                builder.append("错误：").append(result.getErrorMessage()).append(System.lineSeparator());
            }
        }
        builder.append(System.lineSeparator())
                .append("请结合系统提示、知识库上下文和以上工具结果进行回答；若工具结果不足，请明确说明。");
        return builder.toString();
    }

    /* 格式化工具输出对象。 */
    private String formatOutput(Object output) {
        if (output == null) {
            return "";
        }
        if (output instanceof String text) {
            return text;
        }
        return JsonUtils.toJson(output);
    }

    /* 解析当前请求的检索增强结果。 */
    private RagAugmentationResult resolveRagAugmentation(AgentDispatchCommand command) {
        return ragAugmentationService
                .map(service -> service.augment(code(), command.getMessage(), command.getTraceId()))
                .orElseGet(() -> RagAugmentationResult.builder()
                        .enabled(false)
                        .promptBlock("")
                        .hitCount(0)
                        .build());
    }
}
