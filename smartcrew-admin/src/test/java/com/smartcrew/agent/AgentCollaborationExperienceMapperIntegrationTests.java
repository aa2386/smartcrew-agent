package com.smartcrew.agent;

import com.smartcrew.agent.api.collaboration.domain.entity.AgentCollaborationLog;
import com.smartcrew.agent.api.collaboration.domain.model.AgentCollaborationSources;
import com.smartcrew.agent.api.collaboration.domain.model.AgentCollaborationStatuses;
import com.smartcrew.agent.api.collaboration.domain.model.AgentCollaborationStepTypes;
import com.smartcrew.agent.api.collaboration.mapper.AgentCollaborationLogMapper;
import com.smartcrew.agent.api.experience.domain.entity.AgentExperienceHitLog;
import com.smartcrew.agent.api.experience.domain.entity.AgentExperiencePool;
import com.smartcrew.agent.api.experience.domain.model.AgentExperienceHitStages;
import com.smartcrew.agent.api.experience.domain.model.AgentExperienceScopes;
import com.smartcrew.agent.api.experience.domain.model.AgentExperienceToolCodes;
import com.smartcrew.agent.api.experience.domain.model.AgentExperienceTypes;
import com.smartcrew.agent.api.experience.mapper.AgentExperienceHitLogMapper;
import com.smartcrew.agent.api.experience.mapper.AgentExperiencePoolMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 协作日志与经验池 mapper 集成测试。
 */
@ActiveProfiles("test")
@SpringBootTest
class AgentCollaborationExperienceMapperIntegrationTests {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AgentCollaborationLogMapper collaborationLogMapper;

    @Autowired
    private AgentExperiencePoolMapper experiencePoolMapper;

    @Autowired
    private AgentExperienceHitLogMapper experienceHitLogMapper;

    @Test
    void shouldPersistAndLoadContractsThroughMappers() {
        assertThat(countRows("agent_collaboration_log")).isZero();
        assertThat(countRows("agent_experience_pool")).isZero();
        assertThat(countRows("agent_experience_hit_log")).isZero();

        List<String> recommendedToolCodes = List.of("search_web", "write_memory");
        String recommendedToolCodesJson = AgentExperienceToolCodes.toJson(recommendedToolCodes);
        assertThat(AgentExperienceToolCodes.fromJson(recommendedToolCodesJson)).containsExactlyElementsOf(recommendedToolCodes);

        AgentCollaborationLog collaborationLog = new AgentCollaborationLog();
        collaborationLog.setTraceId("trace-001");
        collaborationLog.setRootSessionId("root-session-001");
        collaborationLog.setUserId(1001L);
        collaborationLog.setSource(AgentCollaborationSources.WEB);
        collaborationLog.setAgentCode("initial-agent");
        collaborationLog.setStepType(AgentCollaborationStepTypes.DECISION);
        collaborationLog.setStepName("选择执行策略");
        collaborationLog.setStatus(AgentCollaborationStatuses.SUCCESS);
        collaborationLog.setInputSnapshot("输入摘要");
        collaborationLog.setOutputSnapshot("输出摘要");
        collaborationLog.setDecisionSnapshot("决策摘要");
        collaborationLog.setStartTime(LocalDateTime.of(2026, 5, 9, 18, 0));
        collaborationLog.setEndTime(LocalDateTime.of(2026, 5, 9, 18, 0, 1));
        collaborationLog.setDurationMs(1000L);

        AgentExperiencePool experiencePool = new AgentExperiencePool();
        experiencePool.setExperienceCode("exp-001");
        experiencePool.setScopeType(AgentExperienceScopes.GLOBAL);
        experiencePool.setExperienceType(AgentExperienceTypes.COLLABORATION_STRATEGY);
        experiencePool.setTitle("多 Agent 调度策略");
        experiencePool.setTriggerPattern("需要多角色拆解任务");
        experiencePool.setStrategySummary("先拆解，再并发执行，最后归并结果。");
        experiencePool.setRecommendedAgentCode("memory-agent");
        experiencePool.setRecommendedToolCodesJson(recommendedToolCodesJson);
        experiencePool.setSuccessSample("成功完成跨角色协作。");
        experiencePool.setFailureAvoidance("避免重复调度同一 Agent。");
        experiencePool.setQualityScore(new BigDecimal("0.9500"));
        experiencePool.setHitCount(2);
        experiencePool.setSuccessCount(1);
        experiencePool.setLastUsedAt(LocalDateTime.of(2026, 5, 9, 18, 5));
        experiencePool.setEnabled(Boolean.TRUE);
        experiencePool.setSourceTraceId("trace-001");

        AgentExperienceHitLog experienceHitLog = new AgentExperienceHitLog();
        experienceHitLog.setTraceId("trace-001");
        experienceHitLog.setExperienceCode("exp-001");
        experienceHitLog.setAgentCode("memory-agent");
        experienceHitLog.setAppliedStage(AgentExperienceHitStages.RECALL);
        experienceHitLog.setAppliedSnapshot("在经验召回阶段命中该经验。");
        experienceHitLog.setSuccessFlag(Boolean.TRUE);

        assertThat(collaborationLogMapper.insert(collaborationLog)).isEqualTo(1);
        assertThat(experiencePoolMapper.insert(experiencePool)).isEqualTo(1);
        assertThat(experienceHitLogMapper.insert(experienceHitLog)).isEqualTo(1);
        assertThat(countRows("agent_collaboration_log")).isEqualTo(1L);
        assertThat(countRows("agent_experience_pool")).isEqualTo(1L);
        assertThat(countRows("agent_experience_hit_log")).isEqualTo(1L);

        AgentCollaborationLog storedCollaborationLog = collaborationLogMapper.selectById(collaborationLog.getId());
        AgentExperiencePool storedExperiencePool = experiencePoolMapper.selectById(experiencePool.getId());
        AgentExperienceHitLog storedExperienceHitLog = experienceHitLogMapper.selectById(experienceHitLog.getId());

        assertThat(storedCollaborationLog.getTraceId()).isEqualTo("trace-001");
        assertThat(storedCollaborationLog.getSource()).isEqualTo(AgentCollaborationSources.WEB);
        assertThat(storedCollaborationLog.getStepType()).isEqualTo(AgentCollaborationStepTypes.DECISION);
        assertThat(storedCollaborationLog.getStatus()).isEqualTo(AgentCollaborationStatuses.SUCCESS);
        assertThat(storedCollaborationLog.getDurationMs()).isEqualTo(1000L);

        assertThat(storedExperiencePool.getExperienceCode()).isEqualTo("exp-001");
        assertThat(storedExperiencePool.getScopeType()).isEqualTo(AgentExperienceScopes.GLOBAL);
        assertThat(storedExperiencePool.getExperienceType()).isEqualTo(AgentExperienceTypes.COLLABORATION_STRATEGY);
        assertThat(storedExperiencePool.getRecommendedToolCodesJson()).isEqualTo(recommendedToolCodesJson);
        assertThat(AgentExperienceToolCodes.fromJson(storedExperiencePool.getRecommendedToolCodesJson()))
                .containsExactlyElementsOf(recommendedToolCodes);
        assertThat(storedExperiencePool.getSourceTraceId()).isEqualTo("trace-001");

        assertThat(storedExperienceHitLog.getTraceId()).isEqualTo("trace-001");
        assertThat(storedExperienceHitLog.getAppliedStage()).isEqualTo(AgentExperienceHitStages.RECALL);
        assertThat(storedExperienceHitLog.getSuccessFlag()).isTrue();

        String storedRecommendedToolCodesJson = jdbcTemplate.queryForObject(
                "select recommended_tool_codes from agent_experience_pool where id = ?",
                String.class,
                experiencePool.getId()
        );
        assertThat(storedRecommendedToolCodesJson).isEqualTo(recommendedToolCodesJson);
    }

    private long countRows(String tableName) {
        Long count = jdbcTemplate.queryForObject("select count(*) from " + tableName, Long.class);
        return count == null ? 0L : count;
    }
}
