package com.smartcrew.agent;

import com.smartcrew.agent.api.agent.domain.vo.AgentDispatchResponse;
import com.smartcrew.agent.api.agent.service.AgentRegistry;
import com.smartcrew.agent.api.chat.service.ConversationGatewayService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

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

    @Test
    void shouldCollaborateThroughInitialExecutionAndMemoryAgents() {
        assertThat(agentRegistry.contains("execution-agent")).isTrue();
        assertThat(agentRegistry.contains("memory-agent")).isTrue();

        AgentDispatchResponse response = conversationGatewayService.chatFromWeb(1001L, "plan-session", "请帮我整理任务");

        assertThat(response.getAgentCode()).isEqualTo("initial-agent");
        assertThat(response.getMessage()).isNotBlank();
        assertThat(response.getMetadata()).containsKeys("orchestrator", "experienceCount", "executionAgent");
        assertThat(response.getMetadata().get("orchestrator")).isEqualTo("default-multi-agent");
    }
}
