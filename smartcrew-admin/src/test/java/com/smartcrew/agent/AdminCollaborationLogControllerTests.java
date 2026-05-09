package com.smartcrew.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcrew.agent.api.collaboration.domain.entity.AgentCollaborationLog;
import com.smartcrew.agent.api.collaboration.domain.model.AgentCollaborationSources;
import com.smartcrew.agent.api.collaboration.domain.model.AgentCollaborationStatuses;
import com.smartcrew.agent.api.collaboration.domain.model.AgentCollaborationStepTypes;
import com.smartcrew.agent.api.collaboration.mapper.AgentCollaborationLogMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 协作日志后台查询集成测试。
 */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class AdminCollaborationLogControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AgentCollaborationLogMapper agentCollaborationLogMapper;

    @BeforeEach
    void setUp() {
        agentCollaborationLogMapper.delete(null);
    }

    @Test
    void shouldPageCollaborationLogsAndLoadSteps() throws Exception {
        insertLog("trace-beta", "root-beta", "execution-agent", AgentCollaborationStepTypes.EXECUTION, "执行策略", 2000L,
                LocalDateTime.of(2026, 5, 9, 18, 10), "beta 输入", "beta 输出", "beta 决策");
        insertLog("trace-alpha", "root-alpha", "initial-agent", AgentCollaborationStepTypes.DECISION, "入口调度", 1000L,
                LocalDateTime.of(2026, 5, 9, 18, 0), "alpha 输入", "alpha 输出", "alpha 决策");
        insertLog("trace-demo", "root-demo", "memory-agent", AgentCollaborationStepTypes.MEMORY_READ, "经验召回", 500L,
                LocalDateTime.of(2026, 5, 9, 18, 5), "demo 输入 1", "demo 输出 1", "demo 决策 1");
        insertLog("trace-demo", "root-demo", "memory-agent", AgentCollaborationStepTypes.MEMORY_WRITE, "经验沉淀", 800L,
                LocalDateTime.of(2026, 5, 9, 18, 6), "demo 输入 2", "demo 输出 2", "demo 决策 2");

        mockMvc.perform(get("/api/admin/collaboration-logs")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(4))
                .andExpect(jsonPath("$.rows[0].traceId").value("trace-beta"))
                .andExpect(jsonPath("$.rows[1].traceId").value("trace-demo"))
                .andExpect(jsonPath("$.rows[2].traceId").value("trace-demo"))
                .andExpect(jsonPath("$.rows[3].traceId").value("trace-alpha"));

        mockMvc.perform(get("/api/admin/collaboration-logs")
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .param("keyword", "入口调度"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.rows[0].stepType").value(AgentCollaborationStepTypes.DECISION));

        mockMvc.perform(get("/api/admin/collaboration-logs/trace-demo/steps"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].stepName").value("经验召回"))
                .andExpect(jsonPath("$.data[1].stepName").value("经验沉淀"));

        assertThat(objectMapper.readTree(mockMvc.perform(get("/api/admin/collaboration-logs/trace-demo/steps"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString())
                .path("data")
                .get(0)
                .path("traceId")
                .asText()).isEqualTo("trace-demo");
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
