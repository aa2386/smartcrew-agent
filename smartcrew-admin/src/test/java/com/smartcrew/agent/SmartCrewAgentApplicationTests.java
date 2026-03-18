package com.smartcrew.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcrew.agent.api.tool.service.ToolExecutor;
import com.smartcrew.agent.common.exception.ServiceException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class SmartCrewAgentApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ToolExecutor toolExecutor;

    @Test
    void shouldRegisterListAndDispatchAgent() throws Exception {
        String registerBody = """
                {
                  "agentCode": "custom-agent",
                  "agentName": "Custom Agent",
                  "agentType": "STUB",
                  "description": "custom stub",
                  "strategyType": "REACT",
                  "enabled": true
                }
                """;

        mockMvc.perform(post("/api/v1/agents/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.agentCode").value("custom-agent"));

        mockMvc.perform(get("/api/v1/agents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows[?(@.agentCode=='custom-agent')]").exists());

        String dispatchBody = """
                {
                  "userId": 1001,
                  "sessionId": "sess-001",
                  "message": "help me",
                  "context": {
                    "channel": "web"
                  }
                }
                """;

        mockMvc.perform(post("/api/v1/agents/custom-agent/dispatch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dispatchBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accepted").value(true))
                .andExpect(jsonPath("$.data.traceId").isNotEmpty());
    }

    @Test
    void shouldReturnDecisionPlan() throws Exception {
        String request = """
                {
                  "agentCode": "planner-agent",
                  "userId": 7,
                  "input": "draft a response",
                  "context": {
                    "priority": "high"
                  }
                }
                """;

        mockMvc.perform(post("/api/v1/decision/plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.thought").isNotEmpty())
                .andExpect(jsonPath("$.data.steps.length()").value(4))
                .andExpect(jsonPath("$.data.finalAction").isNotEmpty());
    }

    @Test
    void shouldExposeToolsAndRejectDisabledTerminalExecution() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/tools"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode terminalNode = null;
        for (JsonNode node : root.path("data")) {
            if ("terminal".equals(node.path("toolCode").asText())) {
                terminalNode = node;
                break;
            }
        }
        assertThat(terminalNode).isNotNull();
        assertThat(terminalNode.path("enabled").asBoolean()).isFalse();

        assertThatThrownBy(() -> toolExecutor.execute("terminal", Map.of("command", "echo hi")))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("disabled");
    }

    @Test
    void shouldUpsertUserPreference() throws Exception {
        String request = """
                {
                  "prefKey": "language",
                  "prefValue": "zh-CN",
                  "prefType": "TEXT",
                  "source": "MANUAL"
                }
                """;

        mockMvc.perform(put("/api/v1/memory/preferences/88")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.prefKey").value("language"));

        mockMvc.perform(get("/api/v1/memory/preferences/88"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].prefKey").value("language"))
                .andExpect(jsonPath("$.data[0].prefValue").value("zh-CN"));
    }

    @Test
    void shouldCreateAndQueryPromptTemplate() throws Exception {
        String request = """
                {
                  "templateName": "chat-default",
                  "templateContent": "You are helpful",
                  "category": "chat",
                  "remark": "default"
                }
                """;

        mockMvc.perform(post("/api/v1/prompts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.category").value("chat"));

        mockMvc.perform(get("/api/v1/prompts/category/chat"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.templateName").value("chat-default"));
    }

    @Test
    void shouldRoutePlatformEvent() throws Exception {
        String request = """
                {
                  "platformUserId": "u-100",
                  "eventType": "message",
                  "content": "hello",
                  "metadata": {
                    "tenant": "demo"
                  }
                }
                """;

        mockMvc.perform(post("/api/v1/platform/wecom/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.platform").value("wecom"))
                .andExpect(jsonPath("$.data.handled").value(true));
    }

    @Test
    void shouldRejectUnknownPlatform() throws Exception {
        String request = """
                {
                  "platformUserId": "u-100",
                  "eventType": "message",
                  "content": "hello"
                }
                """;

        mockMvc.perform(post("/api/v1/platform/unknown/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false));
    }
}
