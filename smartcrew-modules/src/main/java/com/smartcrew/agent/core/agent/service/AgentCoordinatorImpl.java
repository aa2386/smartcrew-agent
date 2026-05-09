package com.smartcrew.agent.core.agent.service;

import com.smartcrew.agent.api.agent.domain.model.AgentDispatchCommand;
import com.smartcrew.agent.api.agent.domain.model.MessageEnvelope;
import com.smartcrew.agent.api.agent.domain.request.AgentDispatchRequest;
import com.smartcrew.agent.api.agent.domain.vo.AgentDispatchResponse;
import com.smartcrew.agent.api.agent.service.Agent;
import com.smartcrew.agent.api.agent.service.AgentCoordinator;
import com.smartcrew.agent.api.agent.service.AgentMessageBus;
import com.smartcrew.agent.api.agent.service.AgentRegistry;
import com.smartcrew.agent.api.agentlog.entity.AgentBehaviorLog;
import com.smartcrew.agent.api.agentlog.service.AgentBehaviorLogService;
import com.smartcrew.agent.common.exception.ServiceException;
import com.smartcrew.agent.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class AgentCoordinatorImpl implements AgentCoordinator {

    private final AgentRegistry agentRegistry;
    private final AgentMessageBus agentMessageBus;
    private final AgentBehaviorLogService agentBehaviorLogService;

    @Override
    public AgentDispatchResponse dispatch(String agentCode, AgentDispatchRequest request) {
        Agent agent = agentRegistry.get(agentCode)
                .orElseThrow(() -> new ServiceException("Unknown agent: " + agentCode));

        Map<String, Object> requestContext = request.getContext() == null
                ? new HashMap<>()
                : new HashMap<>(request.getContext());
        String traceId = resolveTraceId(requestContext);
        requestContext.put("traceId", traceId);
        String sourceAgent = resolveSourceAgent(requestContext);
        long startedAt = System.currentTimeMillis();

        AgentDispatchCommand command = AgentDispatchCommand.builder()
                .traceId(traceId)
                .agentCode(agentCode)
                .userId(request.getUserId())
                .sessionId(request.getSessionId())
                .message(request.getMessage())
                .context(requestContext)
                .build();

        MessageEnvelope envelope = MessageEnvelope.builder()
                .traceId(traceId)
                .sourceAgent(sourceAgent)
                .targetAgent(agentCode)
                .userId(request.getUserId())
                .sessionId(request.getSessionId())
                .payload(request.getMessage())
                .metadata(requestContext)
                .createdAt(LocalDateTime.now())
                .build();

        writeBehaviorLog(traceId, request.getUserId(), request.getSessionId(), agentCode, sourceAgent,
                "AGENT_STARTED", "SUCCESS", "Agent started processing request",
                buildLogMetadata(requestContext), null, null);

        try {
            agentMessageBus.publish(agentCode, envelope);
            AgentDispatchResponse response = agent.handle(command);
            if (response == null) {
                response = AgentDispatchResponse.builder()
                        .traceId(traceId)
                        .agentCode(agentCode)
                        .accepted(false)
                        .message("Agent returned no response")
                        .build();
            }
            response.setTraceId(traceId);
            response.setAgentCode(agentCode);
            writeBehaviorLog(traceId, request.getUserId(), request.getSessionId(), agentCode, sourceAgent,
                    "AGENT_FINISHED", response.isAccepted() ? "SUCCESS" : "FAILED",
                    response.isAccepted() ? "Agent finished successfully" : "Agent finished with failure",
                    buildLogMetadata(requestContext), System.currentTimeMillis() - startedAt,
                    response.isAccepted() ? null : response.getMessage());
            return response;
        } catch (Exception exception) {
            writeBehaviorLog(traceId, request.getUserId(), request.getSessionId(), agentCode, sourceAgent,
                    "ERROR", "FAILED", "Agent dispatch failed",
                    buildLogMetadata(requestContext), System.currentTimeMillis() - startedAt,
                    exception.getMessage());
            if (exception instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("Agent dispatch failed", exception);
        }
    }

    private String resolveTraceId(Map<String, Object> context) {
        Object traceId = context.get("traceId");
        if (traceId instanceof String text && StringUtils.isNotBlank(text)) {
            return text.trim();
        }
        return UUID.randomUUID().toString();
    }

    private String resolveSourceAgent(Map<String, Object> context) {
        Object sourceAgent = context.get("sourceAgent");
        if (sourceAgent instanceof String text && StringUtils.isNotBlank(text)) {
            return text.trim();
        }
        Object source = context.get("source");
        if (source instanceof String text && StringUtils.isNotBlank(text)) {
            return text.trim().toLowerCase() + "-gateway";
        }
        return "api-gateway";
    }

    private Map<String, Object> buildLogMetadata(Map<String, Object> context) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        copyIfPresent(context, metadata, "source");
        copyIfPresent(context, metadata, "provider");
        copyIfPresent(context, metadata, "delegationDepth");
        return metadata;
    }

    private void copyIfPresent(Map<String, Object> source, Map<String, Object> target, String key) {
        Object value = source.get(key);
        if (value != null) {
            target.put(key, value);
        }
    }

    private void writeBehaviorLog(String traceId, Long userId, String sessionId, String agentCode, String sourceAgent,
                                  String eventType, String eventStatus, String eventSummary,
                                  Map<String, Object> metadata, Long durationMs, String errorMessage) {
        AgentBehaviorLog logEntry = agentBehaviorLogService.buildLog(
                traceId,
                userId,
                sessionId,
                agentCode,
                eventType,
                eventStatus,
                eventSummary,
                metadata
        );
        logEntry.setSourceAgent(sourceAgent);
        logEntry.setTargetAgent(agentCode);
        logEntry.setDurationMs(durationMs);
        logEntry.setErrorMessage(errorMessage);
        agentBehaviorLogService.write(logEntry);
    }
}
