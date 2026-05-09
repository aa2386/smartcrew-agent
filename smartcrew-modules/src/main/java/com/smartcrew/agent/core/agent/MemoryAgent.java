package com.smartcrew.agent.core.agent;

import com.smartcrew.agent.api.agent.domain.model.AgentDispatchCommand;
import com.smartcrew.agent.api.agent.domain.vo.AgentDispatchResponse;
import com.smartcrew.agent.api.agent.service.Agent;
import com.smartcrew.agent.api.experience.domain.entity.AgentExperiencePool;
import com.smartcrew.agent.api.experience.domain.entity.AgentExperienceHitLog;
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

import java.util.ArrayList;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 记忆智能体，负责经验召回与命中回写。
 */
@Component
public class MemoryAgent implements Agent {

    private static final String CODE = "memory-agent";

    private final ObjectProvider<AgentExperienceService> agentExperienceServiceProvider;

    public MemoryAgent(ObjectProvider<AgentExperienceService> agentExperienceServiceProvider) {
        this.agentExperienceServiceProvider = agentExperienceServiceProvider;
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
        String phase = String.valueOf(command.getContext().getOrDefault("orchestratorPhase", "RECALL"));
        AgentExperienceService experienceService = agentExperienceServiceProvider.getIfAvailable();
        if (experienceService == null) {
            return AgentDispatchResponse.builder()
                    .traceId(command.getTraceId())
                    .agentCode(code())
                    .accepted(true)
                    .message("经验服务未启用")
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

        return AgentDispatchResponse.builder()
                .traceId(command.getTraceId())
                .agentCode(code())
                .accepted(true)
                .message("记忆智能体已召回 " + experiences.size() + " 条经验")
                .metadata(buildMetadata(phase, experiences.size(), experienceCodes))
                .build();
    }

    private AgentDispatchResponse writeBack(AgentDispatchCommand command, AgentExperienceService experienceService) {
        String selectedExperienceCode = stringValue(command.getContext().get("selectedExperienceCode"));
        String executionSummary = stringValue(command.getContext().get("executionSummary"));
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
            experiencePool.setTitle(stringValue(command.getContext().get("experienceTitle")));
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
            experiencePool.setFailureAvoidance(stringValue(command.getContext().get("failureAvoidance")));
            experiencePool.setQualityScore(new BigDecimal("1.0"));
            experiencePool.setHitCount(1);
            experiencePool.setSuccessCount(1);
            experiencePool.setEnabled(Boolean.TRUE);
            experiencePool.setSourceTraceId(command.getTraceId());
            experienceService.recordSuccessfulExperience(experiencePool);
        }

        List<String> experienceCodes = selectedExperienceCode.isBlank() ? List.of() : List.of(selectedExperienceCode);
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
        Object value = command.getContext().get("experienceType");
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

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
