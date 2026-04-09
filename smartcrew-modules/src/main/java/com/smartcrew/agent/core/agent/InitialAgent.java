package com.smartcrew.agent.core.agent;

import com.smartcrew.agent.api.agent.domain.model.AgentDispatchCommand;
import com.smartcrew.agent.api.agent.domain.vo.AgentDispatchResponse;
import com.smartcrew.agent.api.agent.service.Agent;
import com.smartcrew.agent.api.llm.domain.request.LlmChatRequest;
import com.smartcrew.agent.api.llm.domain.vo.LlmChatResponse;
import com.smartcrew.agent.api.llm.service.LlmClient;
import com.smartcrew.agent.core.agent.service.InitialAgentPromptService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * SmartCrew 的初始智能体。
 */
@Component
@RequiredArgsConstructor
public class InitialAgent implements Agent {

    /**
     * 大模型客户端。
     */
    private final LlmClient llmClient;

    /**
     * 提示词构建服务。
     */
    private final InitialAgentPromptService promptService;

    @Override
    public String code() {
        return "initial-agent";
    }

    @Override
    public String name() {
        return "初始智能体";
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
                .systemPrompt(promptService.buildSystemPrompt(code(), command.getUserId()))
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
}
