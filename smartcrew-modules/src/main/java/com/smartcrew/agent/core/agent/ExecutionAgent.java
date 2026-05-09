package com.smartcrew.agent.core.agent;

import com.smartcrew.agent.api.agent.domain.model.AgentDispatchCommand;
import com.smartcrew.agent.api.agent.domain.vo.AgentDispatchResponse;
import com.smartcrew.agent.api.agent.service.Agent;
import com.smartcrew.agent.api.collaboration.domain.entity.AgentCollaborationLog;
import com.smartcrew.agent.api.collaboration.domain.model.AgentCollaborationSources;
import com.smartcrew.agent.api.collaboration.domain.model.AgentCollaborationStatuses;
import com.smartcrew.agent.api.collaboration.domain.model.AgentCollaborationStepTypes;
import com.smartcrew.agent.api.collaboration.service.AgentCollaborationLogService;
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

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
    private final ObjectProvider<AgentCollaborationLogService> collaborationLogServiceProvider;
    private final ConcurrentHashMap<String, ReentrantLock> conversationLocks = new ConcurrentHashMap<>();

    public ExecutionAgent(InitialAgentPromptService promptService,
                          Optional<RagAugmentationService> ragAugmentationService,
                          ObjectProvider<InitialAgentChatService> chatServiceProvider,
                          LlmConversationStore conversationStore,
                          ObjectProvider<AgentCollaborationLogService> collaborationLogServiceProvider) {
        this.promptService = promptService;
        this.ragAugmentationService = ragAugmentationService;
        this.chatServiceProvider = chatServiceProvider;
        this.conversationStore = conversationStore;
        this.collaborationLogServiceProvider = collaborationLogServiceProvider;
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
        LocalDateTime startTime = LocalDateTime.now();
        InitialAgentChatService chatService = chatServiceProvider.getIfAvailable();
        if (chatService == null) {
            String fallbackMessage = "当前未启用大模型服务";
            persistFallbackConversation(command, fallbackMessage);
            Map<String, Object> metadata = buildMetadata(command, 0);
            recordExecutionStep(command, AgentCollaborationStatuses.SKIPPED,
                    buildExecutionInputSnapshot(command, "initial-agent", 0, "", ""),
                    buildExecutionOutputSnapshot(fallbackMessage, false, metadata),
                    buildExecutionDecisionSnapshot(command, "initial-agent", 0, "", ""),
                    fallbackMessage, startTime, LocalDateTime.now());
            return AgentDispatchResponse.builder()
                    .traceId(command.getTraceId())
                    .agentCode(code())
                    .accepted(false)
                    .message(fallbackMessage)
                    .metadata(metadata)
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
            ToolCallContextHolder.set(command.getTraceId(), safeContext(command));
            Result<String> result = chatService.chat(memoryId, command.getMessage(), systemPrompt);
            String assistantMessage = result == null ? "" : result.content();
            Map<String, Object> metadata = buildMetadata(command, augmentationResult.getHitCount());
            recordExecutionStep(command, AgentCollaborationStatuses.SUCCESS,
                    buildExecutionInputSnapshot(command, commandAgentCode, augmentationResult.getHitCount(), memoryId, systemPrompt),
                    buildExecutionOutputSnapshot(assistantMessage, true, metadata),
                    buildExecutionDecisionSnapshot(command, commandAgentCode, augmentationResult.getHitCount(), memoryId, systemPrompt),
                    null, startTime, LocalDateTime.now());
            return AgentDispatchResponse.builder()
                    .traceId(command.getTraceId())
                    .agentCode(code())
                    .accepted(true)
                    .message(assistantMessage)
                    .metadata(metadata)
                    .build();
        } catch (Exception exception) {
            String message = exception.getMessage() == null ? "当前无法处理请求，请稍后再试" : exception.getMessage();
            persistFallbackConversation(command, message);
            recordExecutionStep(command, AgentCollaborationStatuses.FAILED,
                    buildExecutionInputSnapshot(command, commandAgentCode, augmentationResult.getHitCount(), memoryId, systemPrompt),
                    buildExecutionOutputSnapshot(message, false, buildMetadata(command, augmentationResult.getHitCount())),
                    buildExecutionDecisionSnapshot(command, commandAgentCode, augmentationResult.getHitCount(), memoryId, systemPrompt),
                    message, startTime, LocalDateTime.now());
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
        metadata.put("phase", safeContext(command).getOrDefault("orchestratorPhase", "EXECUTION"));
        metadata.put("experienceCount", readExperienceCount(command));
        metadata.put("ragHitCount", ragHitCount);
        return metadata;
    }

    private int readExperienceCount(AgentDispatchCommand command) {
        Object value = safeContext(command).get("experienceCount");
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

    private Map<String, Object> safeContext(AgentDispatchCommand command) {
        if (command.getContext() == null) {
            return Map.of();
        }
        return command.getContext();
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

    private String buildExecutionInputSnapshot(AgentDispatchCommand command,
                                               String commandAgentCode,
                                               int ragHitCount,
                                               String memoryId,
                                               String systemPrompt) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("traceId", command.getTraceId());
        snapshot.put("agentCode", commandAgentCode);
        snapshot.put("message", command.getMessage());
        snapshot.put("ragHitCount", ragHitCount);
        snapshot.put("memoryId", memoryId);
        snapshot.put("systemPrompt", clip(systemPrompt));
        snapshot.put("context", safeContext(command));
        return snapshotString(snapshot);
    }

    private String buildExecutionOutputSnapshot(String assistantMessage,
                                                boolean accepted,
                                                Map<String, Object> metadata) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("accepted", accepted);
        snapshot.put("message", assistantMessage);
        snapshot.put("metadata", metadata);
        return snapshotString(snapshot);
    }

    private String buildExecutionDecisionSnapshot(AgentDispatchCommand command,
                                                  String commandAgentCode,
                                                  int ragHitCount,
                                                  String memoryId,
                                                  String systemPrompt) {
        Map<String, Object> decision = new LinkedHashMap<>();
        decision.put("phase", safeContext(command).getOrDefault("orchestratorPhase", "EXECUTION"));
        decision.put("agentCode", commandAgentCode);
        decision.put("memoryId", memoryId);
        decision.put("ragHitCount", ragHitCount);
        decision.put("systemPromptReady", !clip(systemPrompt).isBlank());
        decision.put("conversationKey", command.getUserId() + "::" + command.getSessionId());
        return snapshotString(decision);
    }

    private void recordExecutionStep(AgentDispatchCommand command,
                                     String status,
                                     String inputSnapshot,
                                     String outputSnapshot,
                                     String decisionSnapshot,
                                     String errorMessage,
                                     LocalDateTime startTime,
                                     LocalDateTime endTime) {
        AgentCollaborationLogService collaborationLogService = collaborationLogServiceProvider.getIfAvailable();
        if (collaborationLogService == null) {
            return;
        }
        try {
            AgentCollaborationLog log = new AgentCollaborationLog();
            log.setTraceId(command.getTraceId());
            log.setRootSessionId(command.getSessionId());
            log.setUserId(command.getUserId());
            log.setSource(AgentCollaborationSources.SYSTEM);
            log.setAgentCode(code());
            log.setStepType(AgentCollaborationStepTypes.EXECUTION);
            log.setStepName("执行处理");
            log.setStatus(status);
            log.setInputSnapshot(truncate(inputSnapshot));
            log.setOutputSnapshot(truncate(outputSnapshot));
            log.setDecisionSnapshot(truncate(decisionSnapshot));
            log.setErrorMessage(truncate(errorMessage));
            log.setStartTime(startTime);
            log.setEndTime(endTime);
            log.setDurationMs(durationMs(startTime, endTime));
            collaborationLogService.createCollaborationLog(log);
        } catch (Exception ignored) {
        }
    }

    private Long durationMs(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            return 0L;
        }
        return Math.max(Duration.between(startTime, endTime).toMillis(), 0L);
    }

    private String snapshotString(Map<String, Object> snapshot) {
        return truncate(String.valueOf(snapshot));
    }

    private String clip(String value) {
        return truncate(value);
    }

    private String truncate(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        int limit = 2000;
        if (value.length() <= limit) {
            return value;
        }
        return value.substring(0, limit) + "...";
    }
}
