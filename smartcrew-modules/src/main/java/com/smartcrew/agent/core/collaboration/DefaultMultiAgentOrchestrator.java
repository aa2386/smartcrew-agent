package com.smartcrew.agent.core.collaboration;

import com.smartcrew.agent.api.agent.domain.model.AgentDispatchCommand;
import com.smartcrew.agent.api.agent.domain.vo.AgentDispatchResponse;
import com.smartcrew.agent.api.agent.service.Agent;
import com.smartcrew.agent.api.agent.service.AgentRegistry;
import com.smartcrew.agent.common.exception.ServiceException;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 默认多智能体编排器。
 */
@Service
public class DefaultMultiAgentOrchestrator implements MultiAgentOrchestrator {

    private static final String ORCHESTRATOR_NAME = "default-multi-agent";
    private static final String INITIAL_AGENT_CODE = "initial-agent";
    private static final String MEMORY_AGENT_CODE = "memory-agent";
    private static final String EXECUTION_AGENT_CODE = "execution-agent";
    private static final String CONTEXT_PHASE = "orchestratorPhase";

    private final AgentRegistry agentRegistry;

    public DefaultMultiAgentOrchestrator(AgentRegistry agentRegistry) {
        this.agentRegistry = agentRegistry;
    }

    @Override
    public AgentDispatchResponse orchestrate(AgentDispatchCommand command) {
        Agent memoryAgent = requireAgent(MEMORY_AGENT_CODE);
        Agent executionAgent = requireAgent(EXECUTION_AGENT_CODE);

        AgentDispatchResponse recallResponse = memoryAgent.handle(enrich(command, Map.of(
                CONTEXT_PHASE, "RECALL"
        )));
        int experienceCount = asInt(recallResponse.getMetadata().get("experienceCount"));
        String selectedExperienceCode = firstExperienceCode(recallResponse.getMetadata().get("experienceCodes"));

        AgentDispatchResponse executionResponse = executionAgent.handle(enrich(command, Map.of(
                CONTEXT_PHASE, "EXECUTION",
                "experienceCount", experienceCount,
                "selectedExperienceCode", selectedExperienceCode
        )));

        memoryAgent.handle(enrich(command, Map.of(
                CONTEXT_PHASE, "WRITE_BACK",
                "experienceCount", experienceCount,
                "selectedExperienceCode", selectedExperienceCode,
                "executionSummary", executionResponse.getMessage()
        )));

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("orchestrator", ORCHESTRATOR_NAME);
        metadata.put("executionAgent", EXECUTION_AGENT_CODE);
        metadata.put("experienceCount", experienceCount);
        if (executionResponse.getMetadata() != null) {
            metadata.putAll(executionResponse.getMetadata());
        }

        return AgentDispatchResponse.builder()
                .traceId(command.getTraceId())
                .agentCode(INITIAL_AGENT_CODE)
                .accepted(executionResponse.isAccepted())
                .message(executionResponse.getMessage())
                .metadata(metadata)
                .build();
    }

    private Agent requireAgent(String agentCode) {
        return agentRegistry.get(agentCode)
                .orElseThrow(() -> new ServiceException("Unknown agent: " + agentCode));
    }

    private AgentDispatchCommand enrich(AgentDispatchCommand source, Map<String, Object> extraContext) {
        Map<String, Object> context = new HashMap<>();
        if (source.getContext() != null) {
            context.putAll(source.getContext());
        }
        context.putAll(extraContext);
        return AgentDispatchCommand.builder()
                .traceId(source.getTraceId())
                .agentCode(source.getAgentCode())
                .userId(source.getUserId())
                .sessionId(source.getSessionId())
                .message(source.getMessage())
                .context(context)
                .build();
    }

    private int asInt(Object value) {
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

    @SuppressWarnings("unchecked")
    private String firstExperienceCode(Object value) {
        if (value instanceof List<?> list && !list.isEmpty()) {
            Object first = list.get(0);
            return first == null ? "" : String.valueOf(first);
        }
        if (value instanceof String text) {
            return text;
        }
        return "";
    }
}
