package com.smartcrew.agent.core.tool;

import com.smartcrew.agent.api.agent.domain.request.AgentDispatchRequest;
import com.smartcrew.agent.api.agent.domain.vo.AgentDispatchResponse;
import com.smartcrew.agent.api.agent.service.AgentCoordinator;
import com.smartcrew.agent.api.agent.service.AgentRegistry;
import com.smartcrew.agent.api.agentlog.entity.AgentBehaviorLog;
import com.smartcrew.agent.api.agentlog.service.AgentBehaviorLogService;
import com.smartcrew.agent.api.tool.service.SmartCrewTool;
import com.smartcrew.agent.common.util.JsonUtils;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Component("agentDelegationTools")
public class AgentDelegationTools implements SmartCrewTool {

    private static final Set<String> ALLOWED_TARGETS = Set.of(
            "life-tool-agent",
            "life-memory-agent"
    );

    private static final int MAX_DEPTH = 2;

    private final AgentCoordinator agentCoordinator;
    private final AgentRegistry agentRegistry;
    private final AgentBehaviorLogService agentBehaviorLogService;

    public AgentDelegationTools(AgentCoordinator agentCoordinator,
                                AgentRegistry agentRegistry,
                                AgentBehaviorLogService agentBehaviorLogService) {
        this.agentCoordinator = agentCoordinator;
        this.agentRegistry = agentRegistry;
        this.agentBehaviorLogService = agentBehaviorLogService;
    }

    @Override
    public String toolCode() {
        return "agent-delegation";
    }

    @Override
    public String toolName() {
        return "Agent delegation tool";
    }

    @Override
    public String description() {
        return "Allows the main agent to delegate work to tool and memory agents.";
    }

    @Override
    public String riskLevel() {
        return "HIGH";
    }

    @Tool("Delegate a task to the life tool agent.")
    public String delegateToToolAgent(@P("Instruction for the tool agent") String instruction) {
        return doDelegate("life-tool-agent", instruction);
    }

    @Tool("Delegate a task to the life memory agent.")
    public String delegateToMemoryAgent(@P("Instruction for the memory agent") String instruction) {
        return doDelegate("life-memory-agent", instruction);
    }

    private String doDelegate(String targetAgentCode, String instruction) {
        ToolCallContextHolder.ToolCallContext ctx = ToolCallContextHolder.get();
        if (ctx == null || ctx.context() == null) {
            return errorResult("Missing tool call context");
        }

        Map<String, Object> context = ctx.context();
        String sourceAgent = asString(context.get("agentCode"));
        Long userId = asLong(context.get("userId"));
        String sessionId = asString(context.get("sessionId"));
        String traceId = ctx.traceId();

        if (!"initial-agent".equals(sourceAgent)) {
            return errorResult("Only initial-agent can delegate");
        }
        if (!ALLOWED_TARGETS.contains(targetAgentCode)) {
            return errorResult("Target agent is not allowed");
        }

        agentRegistry.getDefinition(targetAgentCode).ifPresentOrElse(def -> {
            if (Boolean.FALSE.equals(def.getEnabled())) {
                throw new RuntimeException("Target agent is disabled");
            }
        }, () -> {
            throw new RuntimeException("Target agent does not exist");
        });

        int currentDepth = asInt(context.get("delegationDepth"));
        if (currentDepth >= MAX_DEPTH) {
            return errorResult("Delegation depth limit reached");
        }

        AgentDispatchRequest request = new AgentDispatchRequest();
        request.setUserId(userId != null ? userId : 0L);
        request.setSessionId(sessionId != null ? sessionId : "unknown");
        request.setMessage(instruction);
        request.getContext().put("sourceAgent", sourceAgent);
        request.getContext().put("delegationDepth", currentDepth + 1);
        request.getContext().put("traceId", traceId);

        long startedAt = System.currentTimeMillis();
        writeDelegationLog(traceId, userId, sessionId, sourceAgent, targetAgentCode,
                "DELEGATION_STARTED", "SUCCESS", "Delegation started", currentDepth + 1, null, null);
        try {
            AgentDispatchResponse response = agentCoordinator.dispatch(targetAgentCode, request);
            boolean accepted = response != null && response.isAccepted();
            writeDelegationLog(traceId, userId, sessionId, sourceAgent, targetAgentCode,
                    "DELEGATION_FINISHED", accepted ? "SUCCESS" : "FAILED",
                    accepted ? "Delegation finished" : "Delegation failed",
                    currentDepth + 1, System.currentTimeMillis() - startedAt,
                    accepted ? null : response == null ? "Target agent returned no response" : response.getMessage());
            if (accepted) {
                return response.getMessage() != null ? response.getMessage() : "Delegation completed with empty response";
            }
            return errorResult(response == null ? "Target agent returned no response" : response.getMessage());
        } catch (Exception exception) {
            writeDelegationLog(traceId, userId, sessionId, sourceAgent, targetAgentCode,
                    "DELEGATION_FINISHED", "FAILED", "Delegation threw exception",
                    currentDepth + 1, System.currentTimeMillis() - startedAt, exception.getMessage());
            return errorResult(exception.getMessage());
        }
    }

    private void writeDelegationLog(String traceId, Long userId, String sessionId, String sourceAgent,
                                    String targetAgent, String eventType, String eventStatus,
                                    String eventSummary, int delegationDepth, Long durationMs,
                                    String errorMessage) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("delegationDepth", delegationDepth);
        AgentBehaviorLog logEntry = agentBehaviorLogService.buildLog(
                traceId,
                userId,
                sessionId,
                sourceAgent,
                eventType,
                eventStatus,
                eventSummary,
                metadata
        );
        logEntry.setSourceAgent(sourceAgent);
        logEntry.setTargetAgent(targetAgent);
        logEntry.setDurationMs(durationMs);
        logEntry.setErrorMessage(errorMessage);
        agentBehaviorLogService.write(logEntry);
    }

    private String errorResult(String message) {
        return JsonUtils.toJson(Map.of(
                "success", false,
                "errorMessage", safeMessage(message)
        ));
    }

    private String safeMessage(String message) {
        return message == null || message.isBlank() ? "Unknown error" : message;
    }

    private String asString(Object value) {
        return value == null ? null : value.toString();
    }

    private Long asLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private int asInt(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException exception) {
            return 0;
        }
    }
}
