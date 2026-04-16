package com.smartcrew.agent;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcrew.agent.api.agent.domain.entity.AgentToolBinding;
import com.smartcrew.agent.api.agent.domain.model.AgentDispatchCommand;
import com.smartcrew.agent.api.agent.mapper.AgentToolBindingMapper;
import com.smartcrew.agent.api.llm.domain.request.LlmChatRequest;
import com.smartcrew.agent.api.llm.domain.vo.LlmChatResponse;
import com.smartcrew.agent.api.llm.service.LlmClient;
import com.smartcrew.agent.core.agent.InitialAgent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tool 双层配置与 Agent 接入链路集成测试。
 */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class ToolInfrastructureIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InitialAgent initialAgent;

    @Autowired
    private AgentToolBindingMapper agentToolBindingMapper;

    @MockBean
    private LlmClient llmClient;

    @BeforeEach
    void setUp() {
        Mockito.reset(llmClient);
        agentToolBindingMapper.delete(new LambdaQueryWrapper<AgentToolBinding>()
                .eq(AgentToolBinding::getAgentCode, "initial-agent"));
    }

    @Test
    void shouldExposeLinkedAndFlowToolsInAdminViewAndExecuteFlowTool() throws Exception {
        String flowToolCode = "time-flow-" + UUID.randomUUID().toString().replace("-", "");
        String flowDefinitionJson = objectMapper.writeValueAsString(Map.of(
                "description", "包装服务器时间",
                "steps", List.of(
                        Map.of(
                                "type", "tool_call",
                                "toolCode", "basic",
                                "actionName", "currentTime",
                                "output", "now"
                        ),
                        Map.of(
                                "type", "return",
                                "template", Map.of(
                                        "label", "server-time",
                                        "now", "{{vars.now}}"
                                )
                        )
                )
        ));

        mockMvc.perform(put("/api/admin/tools/basic")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "toolCode", "basic",
                                "toolName", "基础工具配置",
                                "description", "基础工具数据库配置",
                                "beanName", "basicTools",
                                "executionMode", "BEAN",
                                "riskLevel", "LOW",
                                "enabled", true
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.toolCode").value("basic"))
                .andExpect(jsonPath("$.data.sourceStatus").value("LINKED"))
                .andExpect(jsonPath("$.data.hasCodeBean").value(true))
                .andExpect(jsonPath("$.data.hasDatabaseConfig").value(true));

        mockMvc.perform(post("/api/admin/tools")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "toolCode", flowToolCode,
                                "toolName", "时间包装 Tool",
                                "description", "数据库 Flow Tool",
                                "executionMode", "FLOW",
                                "enabled", true,
                                "riskLevel", "LOW",
                                "flowDefinitionJson", flowDefinitionJson
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.toolCode").value(flowToolCode))
                .andExpect(jsonPath("$.data.sourceStatus").value("DB_ONLY"))
                .andExpect(jsonPath("$.data.hasCodeBean").value(false))
                .andExpect(jsonPath("$.data.hasDatabaseConfig").value(true))
                .andExpect(jsonPath("$.data.executable").value(true));

        mockMvc.perform(get("/api/admin/tools/" + flowToolCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.actions[0].actionName").value("execute"))
                .andExpect(jsonPath("$.data.executionMode").value("FLOW"));

        MvcResult executeResult = mockMvc.perform(post("/api/admin/tools/" + flowToolCode + "/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.success").value(true))
                .andReturn();

        JsonNode root = objectMapper.readTree(executeResult.getResponse().getContentAsString());
        assertThat(root.path("data").path("output").path("label").asText()).isEqualTo("server-time");
        assertThat(root.path("data").path("output").path("now").asText()).isNotBlank();
    }

    @Test
    void shouldBindToolsToInitialAgentAndInjectExecutionResultsIntoPrompt() throws Exception {
        mockMvc.perform(put("/api/admin/agents/initial-agent/tool-bindings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "toolCodes", List.of("basic")
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.boundTools[0].toolCode").value("basic"));

        mockMvc.perform(get("/api/admin/agents/initial-agent/tool-bindings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.boundTools[0].toolCode").value("basic"));

        when(llmClient.chat(any(LlmChatRequest.class))).thenReturn(LlmChatResponse.builder()
                .success(Boolean.TRUE)
                .content("已经结合工具结果回答")
                .build());

        var response = initialAgent.handle(AgentDispatchCommand.builder()
                .agentCode("initial-agent")
                .userId(1001L)
                .sessionId("tool-session")
                .message("现在几点")
                .traceId("trace-tool")
                .build());

        ArgumentCaptor<LlmChatRequest> captor = ArgumentCaptor.forClass(LlmChatRequest.class);
        verify(llmClient).chat(captor.capture());

        assertThat(response.isAccepted()).isTrue();
        assertThat(response.getMessage()).isEqualTo("已经结合工具结果回答");
        assertThat(captor.getValue().getUserMessage()).contains("basic#currentTime");
        assertThat(captor.getValue().getUserMessage()).contains("已执行的工具结果如下");
    }
}
