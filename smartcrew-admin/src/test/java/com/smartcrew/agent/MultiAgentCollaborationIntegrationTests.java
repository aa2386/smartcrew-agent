package com.smartcrew.agent;

import com.smartcrew.agent.api.agent.domain.vo.AgentDispatchResponse;
import com.smartcrew.agent.api.agent.service.AgentRegistry;
import com.smartcrew.agent.api.chat.service.ConversationGatewayService;
import com.smartcrew.agent.api.collaboration.domain.entity.AgentCollaborationLog;
import com.smartcrew.agent.api.collaboration.domain.model.AgentCollaborationSources;
import com.smartcrew.agent.api.collaboration.domain.model.AgentCollaborationStatuses;
import com.smartcrew.agent.api.collaboration.domain.model.AgentCollaborationStepTypes;
import com.smartcrew.agent.api.collaboration.domain.vo.AgentCollaborationStepVo;
import com.smartcrew.agent.api.collaboration.mapper.AgentCollaborationLogMapper;
import com.smartcrew.agent.api.collaboration.service.AgentCollaborationLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 多智能体协作链路集成测试。
 */
@ActiveProfiles("test")
@SpringBootTest
class MultiAgentCollaborationIntegrationTests {

    @Autowired
    private ConversationGatewayService conversationGatewayService;

    @Autowired
    private AgentRegistry agentRegistry;

    @Autowired
    private AgentCollaborationLogMapper agentCollaborationLogMapper;

    @Autowired
    private AgentCollaborationLogService agentCollaborationLogService;

    @BeforeEach
    void setUp() {
        agentCollaborationLogMapper.delete(null);
    }

    @Test
    void shouldCollaborateThroughInitialExecutionAndMemoryAgents() {
        assertThat(agentRegistry.contains("execution-agent")).isTrue();
        assertThat(agentRegistry.contains("memory-agent")).isTrue();

        AgentDispatchResponse response = conversationGatewayService.chatFromWeb(1001L, "plan-session", "请帮我整理任务");

        assertThat(response.getAgentCode()).isEqualTo("initial-agent");
        assertThat(response.getMessage()).isNotBlank();
        assertThat(response.getMetadata()).containsKeys("orchestrator", "experienceCount", "executionAgent");
        assertThat(response.getMetadata().get("orchestrator")).isEqualTo("default-multi-agent");

        List<AgentCollaborationStepVo> steps = agentCollaborationLogService.listTraceSteps(response.getTraceId());
        assertThat(steps)
                .extracting(AgentCollaborationStepVo::getStepType)
                .containsSubsequence(
                        AgentCollaborationStepTypes.DISPATCH,
                        AgentCollaborationStepTypes.MEMORY_READ,
                        AgentCollaborationStepTypes.EXECUTION,
                        AgentCollaborationStepTypes.MEMORY_WRITE,
                        AgentCollaborationStepTypes.FINAL_RESPONSE
                );
    }

    private void insertLog(String traceId,
                           String rootSessionId,
                           String agentCode,
                           String stepType,
                           String stepName,
                           Long durationMs,
                           LocalDateTime startTime,
                           String inputSnapshot,
                           String outputSnapshot,
                           String decisionSnapshot) {
        AgentCollaborationLog log = new AgentCollaborationLog();
        log.setTraceId(traceId);
        log.setRootSessionId(rootSessionId);
        log.setUserId(1001L);
        log.setSource(AgentCollaborationSources.WEB);
        log.setAgentCode(agentCode);
        log.setStepType(stepType);
        log.setStepName(stepName);
        log.setStatus(AgentCollaborationStatuses.SUCCESS);
        log.setInputSnapshot(inputSnapshot);
        log.setOutputSnapshot(outputSnapshot);
        log.setDecisionSnapshot(decisionSnapshot);
        log.setStartTime(startTime);
        log.setDurationMs(durationMs);
        agentCollaborationLogMapper.insert(log);
    }
}
