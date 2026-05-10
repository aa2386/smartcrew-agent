package com.smartcrew.agent.core.collaboration;

import com.smartcrew.agent.api.agent.domain.model.AgentDispatchCommand;
import com.smartcrew.agent.api.agent.domain.vo.AgentDispatchResponse;
import com.smartcrew.agent.api.agent.service.Agent;
import com.smartcrew.agent.api.agent.service.AgentRegistry;
import com.smartcrew.agent.api.collaboration.domain.entity.AgentCollaborationLog;
import com.smartcrew.agent.api.collaboration.domain.model.AgentCollaborationSources;
import com.smartcrew.agent.api.collaboration.domain.model.AgentCollaborationStatuses;
import com.smartcrew.agent.api.collaboration.domain.model.AgentCollaborationStepTypes;
import com.smartcrew.agent.api.collaboration.service.AgentCollaborationLogService;
import com.smartcrew.agent.common.exception.ServiceException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
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
    private static final String DISPATCH_STEP_NAME = "调度编排";
    private static final String FINAL_RESPONSE_STEP_NAME = "最终响应";

    private final AgentRegistry agentRegistry;
    private final ObjectProvider<AgentCollaborationLogService> collaborationLogServiceProvider;

    public DefaultMultiAgentOrchestrator(AgentRegistry agentRegistry,
                                         ObjectProvider<AgentCollaborationLogService> collaborationLogServiceProvider) {
        this.agentRegistry = agentRegistry;
        this.collaborationLogServiceProvider = collaborationLogServiceProvider;
    }

    /* 按照固定流水线编排：记忆召回 → 执行 → 记忆回写 → 最终响应。 */
    @Override
    public AgentDispatchResponse orchestrate(AgentDispatchCommand command) {
        LocalDateTime requestStart = LocalDateTime.now();
        Agent memoryAgent = requireAgent(MEMORY_AGENT_CODE);
        Agent executionAgent = requireAgent(EXECUTION_AGENT_CODE);

        recordCollaborationStep(command, INITIAL_AGENT_CODE, AgentCollaborationStepTypes.DISPATCH, DISPATCH_STEP_NAME,
                AgentCollaborationStatuses.SUCCESS, buildRequestSnapshot(command),
                buildDispatchOutputSnapshot(), buildDispatchDecisionSnapshot(), null,
                requestStart, LocalDateTime.now());

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

        AgentDispatchResponse finalResponse = AgentDispatchResponse.builder()
                .traceId(command.getTraceId())
                .agentCode(INITIAL_AGENT_CODE)
                .accepted(executionResponse.isAccepted())
                .message(executionResponse.getMessage())
                .metadata(buildFinalMetadata(executionResponse, experienceCount))
                .build();

        LocalDateTime responseStart = LocalDateTime.now();
        recordCollaborationStep(command, INITIAL_AGENT_CODE, AgentCollaborationStepTypes.FINAL_RESPONSE,
                FINAL_RESPONSE_STEP_NAME, AgentCollaborationStatuses.SUCCESS,
                buildFinalInputSnapshot(command, recallResponse, executionResponse, selectedExperienceCode),
                buildFinalOutputSnapshot(finalResponse, selectedExperienceCode),
                buildFinalDecisionSnapshot(selectedExperienceCode), null,
                responseStart, LocalDateTime.now());

        return finalResponse;
    }

    /* 从注册中心获取指定编码的 Agent 实例，不存在则抛异常。 */
    private Agent requireAgent(String agentCode) {
        return agentRegistry.get(agentCode)
                .orElseThrow(() -> new ServiceException("Unknown agent: " + agentCode));
    }

    /* 合并原始命令和额外上下文，构造新的派发命令。 */
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

    /* 安全地将 Object 转为 int，转换失败返回 0。 */
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
    /* 从集合或字符串中提取第一个经验编码。 */
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

    /* 构建最终响应的元数据。 */
    private Map<String, Object> buildFinalMetadata(AgentDispatchResponse executionResponse, int experienceCount) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("orchestrator", ORCHESTRATOR_NAME);
        metadata.put("executionAgent", EXECUTION_AGENT_CODE);
        metadata.put("experienceCount", experienceCount);
        if (executionResponse.getMetadata() != null) {
            metadata.putAll(executionResponse.getMetadata());
        }
        return metadata;
    }

    /* 构建请求快照用于协作日志。 */
    private String buildRequestSnapshot(AgentDispatchCommand command) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("traceId", command.getTraceId());
        snapshot.put("sessionId", command.getSessionId());
        snapshot.put("userId", command.getUserId());
        snapshot.put("agentCode", command.getAgentCode());
        snapshot.put("message", command.getMessage());
        snapshot.put("context", safeContext(command));
        return snapshotString(snapshot);
    }

    /* 构建调度分发输出快照。 */
    private String buildDispatchOutputSnapshot() {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("nextSteps", List.of(
                AgentCollaborationStepTypes.MEMORY_READ,
                AgentCollaborationStepTypes.EXECUTION,
                AgentCollaborationStepTypes.MEMORY_WRITE,
                AgentCollaborationStepTypes.FINAL_RESPONSE
        ));
        snapshot.put("memoryAgent", MEMORY_AGENT_CODE);
        snapshot.put("executionAgent", EXECUTION_AGENT_CODE);
        return snapshotString(snapshot);
    }

    /* 构建调度分发决策快照。 */
    private String buildDispatchDecisionSnapshot() {
        Map<String, Object> decision = new LinkedHashMap<>();
        decision.put("orchestrator", ORCHESTRATOR_NAME);
        decision.put("entryAgent", INITIAL_AGENT_CODE);
        decision.put("route", List.of(MEMORY_AGENT_CODE, EXECUTION_AGENT_CODE, MEMORY_AGENT_CODE));
        decision.put("reason", "先召回记忆，再执行，再写回经验");
        return snapshotString(decision);
    }

    /* 构建最终响应的输入快照。 */
    private String buildFinalInputSnapshot(AgentDispatchCommand command,
                                           AgentDispatchResponse recallResponse,
                                           AgentDispatchResponse executionResponse,
                                           String selectedExperienceCode) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("traceId", command.getTraceId());
        snapshot.put("selectedExperienceCode", selectedExperienceCode);
        snapshot.put("experienceCount", asInt(recallResponse.getMetadata().get("experienceCount")));
        snapshot.put("executionAccepted", executionResponse.isAccepted());
        snapshot.put("executionMessage", executionResponse.getMessage());
        snapshot.put("context", safeContext(command));
        return snapshotString(snapshot);
    }

    /* 构建最终响应的输出快照。 */
    private String buildFinalOutputSnapshot(AgentDispatchResponse response, String selectedExperienceCode) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("accepted", response.isAccepted());
        snapshot.put("message", response.getMessage());
        snapshot.put("selectedExperienceCode", selectedExperienceCode);
        snapshot.put("metadata", response.getMetadata());
        return snapshotString(snapshot);
    }

    /* 构建最终响应的决策快照。 */
    private String buildFinalDecisionSnapshot(String selectedExperienceCode) {
        Map<String, Object> decision = new LinkedHashMap<>();
        decision.put("orchestrator", ORCHESTRATOR_NAME);
        decision.put("selectedExperienceCode", selectedExperienceCode);
        decision.put("flow", List.of(
                AgentCollaborationStepTypes.DISPATCH,
                AgentCollaborationStepTypes.MEMORY_READ,
                AgentCollaborationStepTypes.EXECUTION,
                AgentCollaborationStepTypes.MEMORY_WRITE,
                AgentCollaborationStepTypes.FINAL_RESPONSE
        ));
        return snapshotString(decision);
    }

    /* 安全获取指令上下文，避免空指针。 */
    private Map<String, Object> safeContext(AgentDispatchCommand command) {
        if (command.getContext() == null) {
            return Map.of();
        }
        return command.getContext();
    }

    /* 记录协作步骤到日志服务。 */
    private void recordCollaborationStep(AgentDispatchCommand command,
                                         String agentCode,
                                         String stepType,
                                         String stepName,
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
            log.setAgentCode(agentCode);
            log.setStepType(stepType);
            log.setStepName(stepName);
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

    /* 计算两个时间点之间的毫秒差值。 */
    private Long durationMs(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            return 0L;
        }
        long duration = Duration.between(startTime, endTime).toMillis();
        return Math.max(duration, 0L);
    }

    /* 将快照 Map 转为字符串并截断。 */
    private String snapshotString(Map<String, Object> snapshot) {
        return truncate(String.valueOf(snapshot));
    }

    /* 截断字符串到指定长度上限。 */
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
