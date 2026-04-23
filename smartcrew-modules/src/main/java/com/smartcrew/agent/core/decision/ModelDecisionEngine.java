package com.smartcrew.agent.core.decision;

import com.smartcrew.agent.api.agent.service.AgentToolBindingService;
import com.smartcrew.agent.api.decision.domain.request.DecisionPlanRequest;
import com.smartcrew.agent.api.decision.domain.vo.DecisionPlanResponse;
import com.smartcrew.agent.api.decision.domain.vo.DecisionStep;
import com.smartcrew.agent.api.decision.domain.vo.PlannedToolCall;
import com.smartcrew.agent.api.decision.service.DecisionEngine;
import com.smartcrew.agent.api.tool.domain.model.ResolvedToolDefinition;
import com.smartcrew.agent.api.tool.domain.model.ToolActionMetadata;
import com.smartcrew.agent.common.util.JsonUtils;
import com.smartcrew.agent.common.util.StringUtils;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * 基于大模型的决策引擎实现，通过 AI Service 生成工具调用预览计划。
 *
 * <p>利用 LangChain4j AI Service 构建决策预览助手，根据智能体绑定的工具元数据
 * 和用户输入，生成结构化的决策计划响应。当大模型不可用或调用异常时，
 * 自动降级为兼容模式输出。</p>
 *
 * @see DecisionEngine
 * @see DecisionPlanResponse
 */
@Service
public class ModelDecisionEngine implements DecisionEngine {

    private final AgentToolBindingService agentToolBindingService;
    private final DecisionPreviewAssistant assistant;

    /**
     * 构造模型决策引擎实例。
     *
     * @param agentToolBindingService 智能体工具绑定服务，用于获取可用工具列表
     * @param chatLanguageModelProvider 对话模型提供者，延迟获取以适配未启用大模型的场景
     */
    public ModelDecisionEngine(AgentToolBindingService agentToolBindingService,
                               ObjectProvider<ChatLanguageModel> chatLanguageModelProvider) {
        this.agentToolBindingService = agentToolBindingService;
        ChatLanguageModel chatLanguageModel = chatLanguageModelProvider.getIfAvailable();
        this.assistant = chatLanguageModel == null ? null : AiServices.builder(DecisionPreviewAssistant.class)
                .chatLanguageModel(chatLanguageModel)
                .build();
    }

    /**
     * 生成决策计划，基于大模型预览工具调用安排。
     *
     * <p>先获取智能体绑定的可用工具，若大模型可用则调用 AI Service 生成预览；
     * 否则返回兼容模式的降级响应。</p>
     *
     * @param request 决策计划请求
     * @return 结构化决策计划响应
     */
    @Override
    public DecisionPlanResponse plan(DecisionPlanRequest request) {
        List<ResolvedToolDefinition> tools;
        try {
            tools = agentToolBindingService.listEnabledResolvedToolsByAgentCode(request.getAgentCode());
        } catch (Exception exception) {
            tools = List.of();
        }
        if (assistant == null) {
            return buildFallbackResponse(tools);
        }

        try {
            String toolSummary = buildToolSummary(tools);
            String previewJson = assistant.preview(toolSummary, buildUserMessage(request));
            DecisionPlanResponse response = JsonUtils.parse(previewJson, DecisionPlanResponse.class);
            return normalizeResponse(response, tools);
        } catch (Exception exception) {
            return buildFallbackResponse(tools);
        }
    }

    /**
     * 规范化决策计划响应，补全缺失字段并确保数据完整性。
     *
     * @param response 大模型原始响应
     * @param tools    可用工具列表
     * @return 规范化后的决策计划响应
     */
    private DecisionPlanResponse normalizeResponse(DecisionPlanResponse response, List<ResolvedToolDefinition> tools) {
        if (response == null) {
            return buildFallbackResponse(tools);
        }
        if (response.getSteps() == null || response.getSteps().isEmpty()) {
            response.setSteps(defaultSteps());
        }
        if (response.getPlannedToolCalls() == null) {
            response.setPlannedToolCalls(new ArrayList<>());
        }
        if (response.getSelectedTools() == null || response.getSelectedTools().isEmpty()) {
            LinkedHashSet<String> selectedTools = new LinkedHashSet<>();
            for (PlannedToolCall plannedToolCall : response.getPlannedToolCalls()) {
                if (StringUtils.isNotBlank(plannedToolCall.getToolCode())) {
                    selectedTools.add(plannedToolCall.getToolCode());
                }
            }
            response.setSelectedTools(new ArrayList<>(selectedTools));
        }
        if (StringUtils.isBlank(response.getThought())) {
            response.setThought("已根据可用工具生成预览结果");
        }
        if (StringUtils.isBlank(response.getFinalAction())) {
            response.setFinalAction("预览完成");
        }
        return response;
    }

    /**
     * 构建兼容模式的降级响应，当大模型不可用或调用异常时使用。
     *
     * @param tools 可用工具列表，最多取前3个工具编码
     * @return 降级决策计划响应
     */
    private DecisionPlanResponse buildFallbackResponse(List<ResolvedToolDefinition> tools) {
        List<String> selectedTools = tools.stream()
                .map(ResolvedToolDefinition::getToolCode)
                .limit(3)
                .toList();
        return DecisionPlanResponse.builder()
                .thought("已生成工具调用预览，当前结果为兼容模式输出")
                .steps(defaultSteps())
                .selectedTools(selectedTools)
                .plannedToolCalls(List.of())
                .finalAction("预览接口仅供调试参考")
                .build();
    }

    /**
     * 生成默认决策步骤列表，描述标准的工具调用预览流程。
     *
     * @return 包含收集上下文、整理工具、生成预览、返回结果四个阶段的步骤列表
     */
    private List<DecisionStep> defaultSteps() {
        return List.of(
                DecisionStep.builder().stage("收集上下文").action("读取当前 Agent 与用户输入").build(),
                DecisionStep.builder().stage("整理工具").action("加载该 Agent 已绑定的可用 Tool").build(),
                DecisionStep.builder().stage("生成预览").action("输出可能使用的 Tool 与动作").build(),
                DecisionStep.builder().stage("返回结果").action("构造结构化预览响应").build()
        );
    }

    /**
     * 构建工具摘要文本，将可用工具的元数据格式化为大模型可理解的描述。
     *
     * @param tools 可用工具列表
     * @return 工具摘要文本
     */
    private String buildToolSummary(List<ResolvedToolDefinition> tools) {
        if (tools.isEmpty()) {
            return "当前没有可用 Tool。";
        }
        StringBuilder builder = new StringBuilder();
        for (ResolvedToolDefinition tool : tools) {
            builder.append("- toolCode=").append(tool.getToolCode())
                    .append(", toolName=").append(tool.getToolName())
                    .append(", description=").append(firstNonBlank(tool.getDescription(), ""))
                    .append(System.lineSeparator());
            for (ToolActionMetadata action : tool.getActions()) {
                builder.append("  - action=").append(action.getActionName())
                        .append(", description=").append(firstNonBlank(action.getDescription(), ""))
                        .append(", parameters=").append(JsonUtils.toJson(action.getParameters()))
                        .append(System.lineSeparator());
            }
        }
        return builder.toString();
    }

    /**
     * 构建用户消息文本，将决策请求参数格式化为大模型输入。
     *
     * @param request 决策计划请求
     * @return 格式化后的用户消息文本
     */
    private String buildUserMessage(DecisionPlanRequest request) {
        return """
                agentCode: %s
                userId: %s
                input: %s
                context: %s
                """.formatted(
                request.getAgentCode(),
                request.getUserId(),
                request.getInput(),
                JsonUtils.toJson(request.getContext())
        );
    }

    /**
     * 返回第一个非空白字符串值。
     *
     * @param values 候选字符串数组
     * @return 第一个非空白字符串，全部为空时返回空字符串
     */
    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.isNotBlank(value)) {
                return value.trim();
            }
        }
        return "";
    }

    /**
     * 决策预览助手接口，由 LangChain4j AI Service 自动实现。
     *
     * <p>根据工具摘要和用户消息，生成结构化的工具调用预览 JSON。</p>
     */
    interface DecisionPreviewAssistant {

        @SystemMessage("""
                你是 Tool 调用预览助手。
                你只能基于给定 Tool 元数据生成“可能的调用预览”，不能声称已经执行工具。
                可用 Tool 如下：
                {{toolSummary}}

                只返回 JSON，格式必须为：
                {
                  "thought": "一句中文总结",
                  "steps": [{"stage":"阶段","action":"动作"}],
                  "selectedTools": ["toolCode"],
                  "plannedToolCalls": [{"toolCode":"toolCode","actionName":"action","arguments":{},"reason":"原因"}],
                  "finalAction": "一句中文结论"
                }
                """)
        /**
         * 生成工具调用预览 JSON。
         *
         * @param toolSummary 工具摘要文本，注入到系统提示词模板
         * @param userMessage 用户消息
         * @return 结构化决策计划 JSON 字符串
         */
        String preview(@V("toolSummary") String toolSummary, @UserMessage String userMessage);
    }
}
        String preview(@V("toolSummary") String toolSummary, @UserMessage String userMessage);
