package com.smartcrew.agent.core.agent;

import com.smartcrew.agent.api.agent.domain.model.AgentDispatchCommand;
import com.smartcrew.agent.api.agent.domain.vo.AgentDispatchResponse;
import com.smartcrew.agent.api.agent.service.Agent;
import com.smartcrew.agent.api.llm.service.LlmConversationStore;
import com.smartcrew.agent.api.rag.domain.vo.RagAugmentationResult;
import com.smartcrew.agent.api.rag.service.RagAugmentationService;
import com.smartcrew.agent.core.agent.service.InitialAgentChatService;
import com.smartcrew.agent.core.agent.service.InitialAgentMemoryId;
import com.smartcrew.agent.core.agent.service.InitialAgentPromptService;
import com.smartcrew.agent.core.tool.ToolCallContextHolder;
import dev.langchain4j.service.Result;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 执行智能体，负责实际对话处理与执行结果汇总。
 */
@Component
public class ExecutionAgent implements Agent {

    private final InitialAgentPromptService promptService;
    private final Optional<RagAugmentationService> ragAugmentationService;
    private final ObjectProvider<InitialAgentChatService> chatServiceProvider;
    private final LlmConversationStore conversationStore;
    private final ConcurrentHashMap<String, ReentrantLock> conversationLocks = new ConcurrentHashMap<>();

    public ExecutionAgent(InitialAgentPromptService promptService,
                          Optional<RagAugmentationService> ragAugmentationService,
                          ObjectProvider<InitialAgentChatService> chatServiceProvider,
                          LlmConversationStore conversationStore) {
        this.promptService = promptService;
        this.ragAugmentationService = ragAugmentationService;
        this.chatServiceProvider = chatServiceProvider;
        this.conversationStore = conversationStore;
    }

    @Override
    public String code() {
        return "execution-agent";
    }

    @Override
    public String name() {
        return "执行智能体";
    }

    @Override
    public boolean supports(String capability) {
        return "execute".equalsIgnoreCase(capability)
                || "chat".equalsIgnoreCase(capability)
                || "orchestrate".equalsIgnoreCase(capability);
    }

    @Override
    public AgentDispatchResponse handle(AgentDispatchCommand command) {
        InitialAgentChatService chatService = chatServiceProvider.getIfAvailable();
        if (chatService == null) {
            persistFallbackConversation(command, "当前未启用大模型服务");
            return AgentDispatchResponse.builder()
                    .traceId(command.getTraceId())
                    .agentCode(code())
                    .accepted(false)
                    .message("当前未启用大模型服务")
                    .metadata(buildMetadata(command, 0))
                    .build();
        }

        String commandAgentCode = resolveCommandAgentCode(command);
        RagAugmentationResult augmentationResult = resolveRagAugmentation(commandAgentCode, command);
        String memoryId = InitialAgentMemoryId.encode(commandAgentCode, command.getUserId(), command.getSessionId());
        String systemPrompt = promptService.buildSystemPrompt(
                commandAgentCode,
                command.getUserId(),
                augmentationResult.getPromptBlock()
        );
        String conversationKey = command.getUserId() + "::" + command.getSessionId();
        ReentrantLock lock = conversationLocks.computeIfAbsent(conversationKey, key -> new ReentrantLock());

        lock.lock();
        try {
            ToolCallContextHolder.set(command.getTraceId(), command.getContext());
            Result<String> result = chatService.chat(memoryId, command.getMessage(), systemPrompt);
            return AgentDispatchResponse.builder()
                    .traceId(command.getTraceId())
                    .agentCode(code())
                    .accepted(true)
                    .message(result == null ? "" : result.content())
                    .metadata(buildMetadata(command, augmentationResult.getHitCount()))
                    .build();
        } catch (Exception exception) {
            String message = exception.getMessage() == null ? "当前无法处理请求，请稍后再试" : exception.getMessage();
            persistFallbackConversation(command, message);
            return AgentDispatchResponse.builder()
                    .traceId(command.getTraceId())
                    .agentCode(code())
                    .accepted(false)
                    .message(message)
                    .metadata(buildMetadata(command, augmentationResult.getHitCount()))
                    .build();
        } finally {
            ToolCallContextHolder.clear();
            lock.unlock();
        }
    }

    private RagAugmentationResult resolveRagAugmentation(String agentCode, AgentDispatchCommand command) {
        return ragAugmentationService
                .map(service -> service.augment(agentCode, command.getMessage(), command.getTraceId()))
                .orElseGet(() -> RagAugmentationResult.builder()
                        .enabled(false)
                        .promptBlock("")
                        .hitCount(0)
                        .build());
    }

    private Map<String, Object> buildMetadata(AgentDispatchCommand command, int ragHitCount) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("executionAgent", code());
        metadata.put("phase", command.getContext().getOrDefault("orchestratorPhase", "EXECUTION"));
        metadata.put("experienceCount", readExperienceCount(command));
        metadata.put("ragHitCount", ragHitCount);
        return metadata;
    }

    private int readExperienceCount(AgentDispatchCommand command) {
        Object value = command.getContext().get("experienceCount");
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private String resolveCommandAgentCode(AgentDispatchCommand command) {
        if (command.getAgentCode() == null || command.getAgentCode().isBlank()) {
            return "initial-agent";
        }
        return command.getAgentCode().trim();
    }

    private void persistFallbackConversation(AgentDispatchCommand command, String assistantMessage) {
        String agentCode = resolveCommandAgentCode(command);
        String sessionId = agentCode + "::" + command.getSessionId();
        conversationStore.ensureSession(command.getUserId(), sessionId);
        long userMessageSeq = conversationStore.nextMessageSeq(command.getUserId(), sessionId);
        conversationStore.saveUserMessage(
                command.getUserId(),
                sessionId,
                userMessageSeq,
                command.getMessage(),
                command.getTraceId()
        );
        conversationStore.saveAssistantMessage(
                command.getUserId(),
                sessionId,
                userMessageSeq + 1,
                assistantMessage,
                command.getTraceId(),
                null,
                null,
                null,
                null
        );
    }
}
