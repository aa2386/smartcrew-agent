package com.smartcrew.agent;

import com.smartcrew.agent.api.experience.domain.entity.AgentExperienceHitLog;
import com.smartcrew.agent.api.experience.domain.entity.AgentExperiencePool;
import com.smartcrew.agent.api.experience.domain.model.AgentExperienceHitStages;
import com.smartcrew.agent.api.experience.domain.model.AgentExperienceScopes;
import com.smartcrew.agent.api.experience.domain.model.AgentExperienceToolCodes;
import com.smartcrew.agent.api.experience.domain.model.AgentExperienceTypes;
import com.smartcrew.agent.api.experience.domain.query.AgentExperiencePoolQuery;
import com.smartcrew.agent.api.experience.domain.vo.AgentExperienceRecallVo;
import com.smartcrew.agent.api.experience.mapper.AgentExperienceHitLogMapper;
import com.smartcrew.agent.api.experience.mapper.AgentExperiencePoolMapper;
import com.smartcrew.agent.api.experience.service.AgentExperienceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 经验服务集成测试。
 */
@ActiveProfiles("test")
@SpringBootTest
class AgentExperienceServiceTests {

    @Autowired
    private AgentExperienceService agentExperienceService;

    @Autowired
    private AgentExperiencePoolMapper experiencePoolMapper;

    @Autowired
    private AgentExperienceHitLogMapper experienceHitLogMapper;

    @BeforeEach
    void setUp() {
        experienceHitLogMapper.delete(null);
        experiencePoolMapper.delete(null);
    }

    @Test
    void shouldRecallGlobalExperiencesInQualityOrder() {
        insertExperience("exp-low", "低优先级经验", "请整理任务", new BigDecimal("0.40"), 1, 1);
        insertExperience("exp-high", "高优先级经验", "请整理任务", new BigDecimal("0.95"), 3, 2);
        insertExperience("exp-other-scope", "其他作用域", "请整理任务", new BigDecimal("0.99"), 10, 10, "PRIVATE");

        AgentExperiencePoolQuery query = new AgentExperiencePoolQuery();
        query.setKeyword("整理任务");
        query.setScopeType(AgentExperienceScopes.GLOBAL);
        query.setExperienceType(AgentExperienceTypes.COLLABORATION_STRATEGY);
        query.setEnabled(Boolean.TRUE);

        var result = agentExperienceService.recallGlobalExperiences(query);

        assertThat(result.getTotal()).isEqualTo(2);
        assertThat(result.getRows())
                .extracting(AgentExperienceRecallVo::getExperienceCode)
                .containsExactly("exp-high", "exp-low");
        assertThat(result.getRows().get(0).getRecommendedToolCodes())
                .containsExactly("tool-a", "tool-b");
    }

    @Test
    void shouldRecordHitAndSuccessfulExperience() {
        AgentExperienceHitLog hitLog = new AgentExperienceHitLog();
        hitLog.setTraceId("trace-001");
        hitLog.setExperienceCode("exp-001");
        hitLog.setAgentCode("memory-agent");
        hitLog.setAppliedStage(AgentExperienceHitStages.RECALL);
        hitLog.setAppliedSnapshot("命中经验");
        hitLog.setSuccessFlag(Boolean.TRUE);

        agentExperienceService.recordExperienceHit(hitLog);

        assertThat(experienceHitLogMapper.selectCount(null)).isEqualTo(1L);
        AgentExperienceHitLog storedHitLog = experienceHitLogMapper.selectList(null).get(0);
        assertThat(storedHitLog.getTraceId()).isEqualTo("trace-001");
        assertThat(storedHitLog.getSuccessFlag()).isTrue();

        AgentExperiencePool experiencePool = new AgentExperiencePool();
        experiencePool.setExperienceCode("exp-001");
        experiencePool.setScopeType(AgentExperienceScopes.GLOBAL);
        experiencePool.setExperienceType(AgentExperienceTypes.COLLABORATION_STRATEGY);
        experiencePool.setTitle("多 Agent 协作经验");
        experiencePool.setTriggerPattern("请整理任务");
        experiencePool.setStrategySummary("先召回历史经验，再交给执行智能体");
        experiencePool.setRecommendedAgentCode("execution-agent");
        experiencePool.setRecommendedToolCodesJson(AgentExperienceToolCodes.toJson(List.of("tool-a")));
        experiencePool.setSuccessSample("已完成整理");
        experiencePool.setFailureAvoidance("避免重复调度");
        experiencePool.setQualityScore(new BigDecimal("0.80"));
        experiencePool.setHitCount(1);
        experiencePool.setSuccessCount(1);
        experiencePool.setEnabled(Boolean.TRUE);
        experiencePool.setSourceTraceId("trace-001");

        AgentExperiencePool created = agentExperienceService.recordSuccessfulExperience(experiencePool);
        assertThat(created.getId()).isNotNull();
        assertThat(created.getHitCount()).isEqualTo(1);
        assertThat(created.getSuccessCount()).isEqualTo(1);

        AgentExperiencePool update = new AgentExperiencePool();
        update.setExperienceCode("exp-001");
        update.setTitle("多 Agent 协作经验 - 更新");
        update.setStrategySummary("更新后的策略");
        update.setSuccessSample("新的成功样例");
        update.setFailureAvoidance("新的失败规避");
        update.setQualityScore(new BigDecimal("0.90"));
        update.setHitCount(1);
        update.setSuccessCount(1);
        update.setEnabled(Boolean.TRUE);
        update.setSourceTraceId("trace-002");

        AgentExperiencePool updated = agentExperienceService.recordSuccessfulExperience(update);
        assertThat(updated.getId()).isEqualTo(created.getId());
        assertThat(updated.getHitCount()).isEqualTo(2);
        assertThat(updated.getSuccessCount()).isEqualTo(2);
        assertThat(updated.getSourceTraceId()).isEqualTo("trace-002");
        assertThat(updated.getLastUsedAt()).isNotNull();

        AgentExperiencePool stored = experiencePoolMapper.selectById(created.getId());
        assertThat(stored.getTitle()).isEqualTo("多 Agent 协作经验 - 更新");
        assertThat(stored.getStrategySummary()).isEqualTo("更新后的策略");
        assertThat(stored.getHitCount()).isEqualTo(2);
        assertThat(stored.getSuccessCount()).isEqualTo(2);
    }

    private void insertExperience(String experienceCode,
                                  String title,
                                  String keyword,
                                  BigDecimal qualityScore,
                                  int hitCount,
                                  int successCount) {
        insertExperience(experienceCode, title, keyword, qualityScore, hitCount, successCount, AgentExperienceScopes.GLOBAL);
    }

    private void insertExperience(String experienceCode,
                                  String title,
                                  String keyword,
                                  BigDecimal qualityScore,
                                  int hitCount,
                                  int successCount,
                                  String scopeType) {
        AgentExperiencePool experiencePool = new AgentExperiencePool();
        experiencePool.setExperienceCode(experienceCode);
        experiencePool.setScopeType(scopeType);
        experiencePool.setExperienceType(AgentExperienceTypes.COLLABORATION_STRATEGY);
        experiencePool.setTitle(title);
        experiencePool.setTriggerPattern(keyword);
        experiencePool.setStrategySummary(title + " 策略");
        experiencePool.setRecommendedAgentCode("execution-agent");
        experiencePool.setRecommendedToolCodesJson(AgentExperienceToolCodes.toJson(List.of("tool-a", "tool-b")));
        experiencePool.setSuccessSample(title + " 成功样例");
        experiencePool.setFailureAvoidance(title + " 失败规避");
        experiencePool.setQualityScore(qualityScore);
        experiencePool.setHitCount(hitCount);
        experiencePool.setSuccessCount(successCount);
        experiencePool.setLastUsedAt(LocalDateTime.now().minusHours(hitCount));
        experiencePool.setEnabled(Boolean.TRUE);
        experiencePoolMapper.insert(experiencePool);
    }
}
