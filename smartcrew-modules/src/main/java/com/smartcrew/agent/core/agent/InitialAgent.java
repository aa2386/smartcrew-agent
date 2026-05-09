package com.smartcrew.agent.core.agent;

import com.smartcrew.agent.api.agent.domain.model.AgentDispatchCommand;
import com.smartcrew.agent.api.agent.domain.vo.AgentDispatchResponse;
import com.smartcrew.agent.api.agent.service.Agent;
import com.smartcrew.agent.core.agent.service.InitialAgentChatService;
import com.smartcrew.agent.core.agent.service.InitialAgentMemoryId;
import com.smartcrew.agent.core.agent.service.InitialAgentPromptService;
import com.smartcrew.agent.core.collaboration.MultiAgentOrchestrator;
import dev.langchain4j.service.Result;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 初始智能体，作为统一入口负责委派多智能体编排。
 */
@Component
public class InitialAgent implements Agent {

    private final InitialAgentPromptService promptService;
    private final ObjectProvider<MultiAgentOrchestrator> multiAgentOrchestratorProvider;
    private final ObjectProvider<InitialAgentChatService> chatServiceProvider;

    public InitialAgent(InitialAgentPromptService promptService,
                        ObjectProvider<MultiAgentOrchestrator> multiAgentOrchestratorProvider,
                        ObjectProvider<InitialAgentChatService> chatServiceProvider) {
        this.promptService = promptService;
        this.multiAgentOrchestratorProvider = multiAgentOrchestratorProvider;
        this.chatServiceProvider = chatServiceProvider;
    }

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
                || "orchestrate".equalsIgnoreCase(capability)
                || "rag".equalsIgnoreCase(capability);
    }

    @Override
    public AgentDispatchResponse handle(AgentDispatchCommand command) {
        MultiAgentOrchestrator orchestrator = multiAgentOrchestratorProvider.getIfAvailable();
        if (orchestrator != null) {
            return orchestrator.orchestrate(command);
        }

        InitialAgentChatService chatService = chatServiceProvider.getIfAvailable();
        if (chatService == null) {
            return AgentDispatchResponse.builder()
                    .traceId(command.getTraceId())
                    .agentCode(code())
                    .accepted(false)
                    .message("当前未启用对话服务")
                    .metadata(Map.of())
                    .build();
        }

        String commandAgentCode = resolveCommandAgentCode(command);
        String memoryId = InitialAgentMemoryId.encode(commandAgentCode, command.getUserId(), command.getSessionId());
        String systemPrompt = promptService.buildSystemPrompt(commandAgentCode, command.getUserId(), "");
        Result<String> result = chatService.chat(memoryId, command.getMessage(), systemPrompt);
        return AgentDispatchResponse.builder()
                .traceId(command.getTraceId())
                .agentCode(code())
                .accepted(true)
                .message(result == null ? "" : result.content())
                .metadata(Map.of())
                .build();
    }

    private String resolveCommandAgentCode(AgentDispatchCommand command) {
        if (command.getAgentCode() == null || command.getAgentCode().isBlank()) {
            return code();
        }
        return command.getAgentCode().trim();
    }
}
