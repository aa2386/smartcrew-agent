package com.smartcrew.agent.core.agent;

import com.smartcrew.agent.api.agent.domain.model.AgentDispatchCommand;
import com.smartcrew.agent.api.agent.domain.vo.AgentDispatchResponse;
import com.smartcrew.agent.api.agent.service.Agent;
import com.smartcrew.agent.api.collaboration.domain.entity.AgentCollaborationLog;
import com.smartcrew.agent.api.collaboration.domain.model.AgentCollaborationSources;
import com.smartcrew.agent.api.collaboration.domain.model.AgentCollaborationStatuses;
import com.smartcrew.agent.api.collaboration.domain.model.AgentCollaborationStepTypes;
import com.smartcrew.agent.api.collaboration.service.AgentCollaborationLogService;
import com.smartcrew.agent.api.experience.domain.entity.AgentExperienceHitLog;
import com.smartcrew.agent.api.experience.domain.entity.AgentExperiencePool;
import com.smartcrew.agent.api.experience.domain.model.AgentExperienceHitStages;
import com.smartcrew.agent.api.experience.domain.model.AgentExperienceScopes;
import com.smartcrew.agent.api.experience.domain.model.AgentExperienceToolCodes;
import com.smartcrew.agent.api.experience.domain.model.AgentExperienceTypes;
import com.smartcrew.agent.api.experience.domain.query.AgentExperiencePoolQuery;
import com.smartcrew.agent.api.experience.domain.vo.AgentExperienceRecallVo;
import com.smartcrew.agent.api.experience.service.AgentExperienceService;
import com.smartcrew.agent.core.page.TableDataInfo;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 记忆智能体，负责经验召回与命中回写。
 */
@Component
public class MemoryAgent implements Agent {

    private static final String CODE = "memory-agent";

    private final ObjectProvider<AgentExperienceService> agentExperienceServiceProvider;
    private final ObjectProvider<AgentCollaborationLogService> collaborationLogServiceProvider;

    public MemoryAgent(ObjectProvider<AgentExperienceService> agentExperienceServiceProvider,
                       ObjectProvider<AgentCollaborationLogService> collaborationLogServiceProvider) {
        this.agentExperienceServiceProvider = agentExperienceServiceProvider;
        this.collaborationLogServiceProvider = collaborationLogServiceProvider;
    }

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public String name() {
        return "记忆智能体";
    }

    @Override
    public boolean supports(String capability) {
        return "memory".equalsIgnoreCase(capability)
                || "chat".equalsIgnoreCase(capability)
                || "orchestrate".equalsIgnoreCase(capability);
    }

    @Override
    public AgentDispatchResponse handle(AgentDispatchCommand command) {
        String phase = String.valueOf(safeContext(command).getOrDefault("orchestratorPhase", "RECALL"));
        AgentExperienceService experienceService = agentExperienceServiceProvider.getIfAvailable();
        if (experienceService == null) {
            String message = "经验服务未启用";
            recordMemoryStep(command,
                    "WRITE_BACK".equalsIgnoreCase(phase) ? AgentCollaborationStepTypes.MEMORY_WRITE : AgentCollaborationStepTypes.MEMORY_READ,
                    "WRITE_BACK".equalsIgnoreCase(phase) ? "经验回写" : "经验召回",
                    AgentCollaborationStatuses.SKIPPED,
                    buildNoServiceInputSnapshot(command, phase),
                    buildNoServiceOutputSnapshot(message),
                    buildNoServiceDecisionSnapshot(phase),
                    null,
                    LocalDateTime.now(),
                    LocalDateTime.now());
            return AgentDispatchResponse.builder()
                    .traceId(command.getTraceId())
                    .agentCode(code())
                    .accepted(true)
                    .message(message)
                    .metadata(buildMetadata(phase, 0, List.of()))
                    .build();
        }
        if ("WRITE_BACK".equalsIgnoreCase(phase)) {
            return writeBack(command, experienceService);
        }
        return recall(command, experienceService, phase);
    }

    private AgentDispatchResponse recall(AgentDispatchCommand command,
                                         AgentExperienceService experienceService,
                                         String phase) {
        LocalDateTime startTime = LocalDateTime.now();
        TableDataInfo<AgentExperienceRecallVo> page = recallExperiences(command, experienceService);
        List<AgentExperienceRecallVo> experiences = page == null || page.getRows() == null ? List.of() : page.getRows();
        List<String> experienceCodes = experiences.stream()
                .map(AgentExperienceRecallVo::getExperienceCode)
                .filter(code -> code != null && !code.isBlank())
                .toList();

        for (AgentExperienceRecallVo experience : experiences) {
            AgentExperienceHitLog hitLog = new AgentExperienceHitLog();
            hitLog.setTraceId(command.getTraceId());
            hitLog.setExperienceCode(experience.getExperienceCode());
            hitLog.setAgentCode(code());
            hitLog.setAppliedStage(AgentExperienceHitStages.RECALL);
            hitLog.setAppliedSnapshot(experience.getStrategySummary());
            hitLog.setSuccessFlag(Boolean.FALSE);
            experienceService.recordExperienceHit(hitLog);
        }

        recordMemoryStep(command,
                AgentCollaborationStepTypes.MEMORY_READ,
                "经验召回",
                AgentCollaborationStatuses.SUCCESS,
                buildRecallInputSnapshot(command, phase),
                buildRecallOutputSnapshot(experiences.size(), experienceCodes),
                buildRecallDecisionSnapshot(command, experienceCodes),
                null,
                startTime,
                LocalDateTime.now());

        return AgentDispatchResponse.builder()
                .traceId(command.getTraceId())
                .agentCode(code())
                .accepted(true)
                .message("记忆智能体已召回 " + experiences.size() + " 条经验")
                .metadata(buildMetadata(phase, experiences.size(), experienceCodes))
                .build();
    }

    private AgentDispatchResponse writeBack(AgentDispatchCommand command, AgentExperienceService experienceService) {
        LocalDateTime startTime = LocalDateTime.now();
        String selectedExperienceCode = stringValue(safeContext(command).get("selectedExperienceCode"));
        String executionSummary = stringValue(safeContext(command).get("executionSummary"));
        String status = AgentCollaborationStatuses.SUCCESS;
        if (!selectedExperienceCode.isBlank()) {
            AgentExperienceHitLog hitLog = new AgentExperienceHitLog();
            hitLog.setTraceId(command.getTraceId());
            hitLog.setExperienceCode(selectedExperienceCode);
            hitLog.setAgentCode(code());
            hitLog.setAppliedStage(AgentExperienceHitStages.FINAL_RESPONSE);
            hitLog.setAppliedSnapshot(executionSummary);
            hitLog.setSuccessFlag(Boolean.TRUE);
            experienceService.recordExperienceHit(hitLog);

            AgentExperiencePool experiencePool = new AgentExperiencePool();
            experiencePool.setExperienceCode(selectedExperienceCode);
            experiencePool.setScopeType(AgentExperienceScopes.GLOBAL);
            experiencePool.setExperienceType(resolveExperienceType(command));
            experiencePool.setTitle(stringValue(safeContext(command).get("experienceTitle")));
            if (experiencePool.getTitle().isBlank()) {
                experiencePool.setTitle(selectedExperienceCode);
            }
            experiencePool.setTriggerPattern(command.getMessage());
            experiencePool.setStrategySummary(executionSummary.isBlank()
                    ? "已通过多智能体协作完成经验沉淀"
                    : executionSummary);
            experiencePool.setRecommendedAgentCode("execution-agent");
            experiencePool.setRecommendedToolCodesJson(AgentExperienceToolCodes.toJson(List.of()));
            experiencePool.setSuccessSample(executionSummary);
            experiencePool.setFailureAvoidance(stringValue(safeContext(command).get("failureAvoidance")));
            experiencePool.setQualityScore(new BigDecimal("1.0"));
            experiencePool.setHitCount(1);
            experiencePool.setSuccessCount(1);
            experiencePool.setEnabled(Boolean.TRUE);
            experiencePool.setSourceTraceId(command.getTraceId());
            experienceService.recordSuccessfulExperience(experiencePool);
        } else {
            status = AgentCollaborationStatuses.SKIPPED;
        }

        List<String> experienceCodes = selectedExperienceCode.isBlank() ? List.of() : List.of(selectedExperienceCode);
        recordMemoryStep(command,
                AgentCollaborationStepTypes.MEMORY_WRITE,
                "经验回写",
                status,
                buildWriteBackInputSnapshot(command, selectedExperienceCode, executionSummary),
                buildWriteBackOutputSnapshot(selectedExperienceCode, executionSummary, status),
                buildWriteBackDecisionSnapshot(command, selectedExperienceCode),
                null,
                startTime,
                LocalDateTime.now());

        return AgentDispatchResponse.builder()
                .traceId(command.getTraceId())
                .agentCode(code())
                .accepted(true)
                .message("记忆智能体已完成回写")
                .metadata(buildMetadata("WRITE_BACK", experienceCodes.size(), experienceCodes))
                .build();
    }

    private TableDataInfo<AgentExperienceRecallVo> recallExperiences(AgentDispatchCommand command,
                                                                     AgentExperienceService experienceService) {
        AgentExperiencePoolQuery query = new AgentExperiencePoolQuery();
        query.setKeyword(command.getMessage());
        query.setScopeType(AgentExperienceScopes.GLOBAL);
        query.setExperienceType(resolveExperienceType(command));
        query.setEnabled(Boolean.TRUE);
        return experienceService.recallGlobalExperiences(query);
    }

    private String resolveExperienceType(AgentDispatchCommand command) {
        Object value = safeContext(command).get("experienceType");
        if (value == null || String.valueOf(value).isBlank()) {
            return AgentExperienceTypes.COLLABORATION_STRATEGY;
        }
        return String.valueOf(value).trim();
    }

    private Map<String, Object> buildMetadata(String phase, int experienceCount, List<String> experienceCodes) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("phase", phase);
        metadata.put("experienceCount", experienceCount);
        metadata.put("experienceCodes", new ArrayList<>(experienceCodes));
        return metadata;
    }

    private Map<String, Object> safeContext(AgentDispatchCommand command) {
        if (command.getContext() == null) {
            return Map.of();
        }
        return command.getContext();
    }

    private String buildRecallInputSnapshot(AgentDispatchCommand command, String phase) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("traceId", command.getTraceId());
        snapshot.put("phase", phase);
        snapshot.put("message", command.getMessage());
        snapshot.put("context", safeContext(command));
        return snapshotString(snapshot);
    }

    private String buildRecallOutputSnapshot(int experienceCount, List<String> experienceCodes) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("experienceCount", experienceCount);
        snapshot.put("experienceCodes", experienceCodes);
        return snapshotString(snapshot);
    }

    private String buildRecallDecisionSnapshot(AgentDispatchCommand command, List<String> experienceCodes) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("scope", AgentExperienceScopes.GLOBAL);
        snapshot.put("experienceType", resolveExperienceType(command));
        snapshot.put("selectedExperienceCodes", experienceCodes);
        snapshot.put("reason", "先从全局经验池召回可复用策略");
        return snapshotString(snapshot);
    }

    private String buildWriteBackInputSnapshot(AgentDispatchCommand command,
                                               String selectedExperienceCode,
                                               String executionSummary) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("traceId", command.getTraceId());
        snapshot.put("selectedExperienceCode", selectedExperienceCode);
        snapshot.put("executionSummary", executionSummary);
        snapshot.put("context", safeContext(command));
        return snapshotString(snapshot);
    }

    private String buildWriteBackOutputSnapshot(String selectedExperienceCode,
                                                String executionSummary,
                                                String status) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("selectedExperienceCode", selectedExperienceCode);
        snapshot.put("executionSummary", executionSummary);
        snapshot.put("status", status);
        return snapshotString(snapshot);
    }

    private String buildWriteBackDecisionSnapshot(AgentDispatchCommand command, String selectedExperienceCode) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("experienceType", resolveExperienceType(command));
        snapshot.put("selectedExperienceCode", selectedExperienceCode);
        snapshot.put("reason", selectedExperienceCode.isBlank() ? "没有可写回的经验" : "执行成功后沉淀全局经验");
        return snapshotString(snapshot);
    }

    private String buildNoServiceInputSnapshot(AgentDispatchCommand command, String phase) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("traceId", command.getTraceId());
        snapshot.put("phase", phase);
        snapshot.put("message", command.getMessage());
        return snapshotString(snapshot);
    }

    private String buildNoServiceOutputSnapshot(String message) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("message", message);
        snapshot.put("enabled", Boolean.FALSE);
        return snapshotString(snapshot);
    }

    private String buildNoServiceDecisionSnapshot(String phase) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("phase", phase);
        snapshot.put("reason", "经验服务未启用，跳过记忆步骤");
        return snapshotString(snapshot);
    }

    private void recordMemoryStep(AgentDispatchCommand command,
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
            log.setAgentCode(code());
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

    private Long durationMs(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            return 0L;
        }
        return Math.max(Duration.between(startTime, endTime).toMillis(), 0L);
    }

    private String snapshotString(Map<String, Object> snapshot) {
        return truncate(String.valueOf(snapshot));
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
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
