package com.smartcrew.agent.core.agent;

import com.smartcrew.agent.api.agent.domain.model.AgentDispatchCommand;
import com.smartcrew.agent.api.agent.domain.vo.AgentDispatchResponse;
import com.smartcrew.agent.api.agent.service.Agent;
import com.smartcrew.agent.api.llm.domain.request.LlmChatRequest;
import com.smartcrew.agent.api.llm.domain.vo.LlmChatResponse;
import com.smartcrew.agent.api.llm.service.LlmClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * - 它是用户唯一入口 Agent
 * - 它是会话主控 Agent
 * - 它是长期偏好统一读写者
 * - 它是 RAG 检索增强的决策者
 * - 它是工具调用协调者
 * - 它未来还是多 Agent 协作的总控 Agent
 * - 改多Agent时再将该Agent逐步拆解分权
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class InitialAgent implements Agent {
    private final LlmClient llmClient;

    @Override
    public String code() {
        return "initial-agent";
    }

    @Override
    public String name() {
        return "Initial Agent";
    }

    @Override
    public boolean supports(String capability) {
        return "chat".equalsIgnoreCase(capability)
                || "orchestrate".equalsIgnoreCase(capability);
    }

    @Override
    public AgentDispatchResponse handle(AgentDispatchCommand command) {
        String llmSessionId = code() + "::" + command.getSessionId();

        LlmChatRequest request = LlmChatRequest.builder()
                .userId(command.getUserId())
                .sessionId(llmSessionId)
                .userMessage(command.getMessage())
                .systemPrompt("你是 SmartCrew 的初始 Agent，负责直接与用户交流。")
                .traceId(command.getTraceId())
                .build();

        LlmChatResponse response = llmClient.chat(request);
        if (!Boolean.TRUE.equals(response.getSuccess())) {
            return AgentDispatchResponse.builder()
                    .traceId(command.getTraceId())
                    .agentCode(code())
                    .accepted(false)
                    .message("暂时无法处理你的请求，请稍后再试。")
                    .build();
        }

        return AgentDispatchResponse.builder()
                .traceId(command.getTraceId())
                .agentCode(code())
                .accepted(true)
                .message(response.getContent())
                .build();
    }
}
