package com.smartcrew.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcrew.agent.api.agent.domain.entity.AgentDefinition;
import com.smartcrew.agent.api.agent.service.AgentRegistry;
import com.smartcrew.agent.api.prompt.mapper.AgentPromptBindingMapper;
import com.smartcrew.agent.api.prompt.mapper.PromptTemplateMapper;
import com.smartcrew.agent.core.agent.service.InitialAgentPromptService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Agent Prompt 分层与缓存一致性集成测试。
 */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class AgentPromptIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AgentRegistry agentRegistry;

    @Autowired
    private InitialAgentPromptService initialAgentPromptService;

    @Autowired
    private PromptTemplateMapper promptTemplateMapper;

    @Autowired
    private AgentPromptBindingMapper agentPromptBindingMapper;

    @Test
    void shouldSyncSystemPromptIntoRegistryCache() throws Exception {
        String agentCode = "cache-agent";
        String createBody = objectMapper.writeValueAsString(Map.of(
                "agentCode", agentCode,
                "agentName", "Cache Agent",
                "agentType", "STUB",
                "description", "cache test",
                "strategyType", "REACT",
                "systemPrompt", "base-persona-v1",
                "enabled", true,
                "configJson", "{}"
        ));

        mockMvc.perform(post("/api/admin/agents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.agentCode").value(agentCode));

        AgentDefinition createdDefinition = agentRegistry.getDefinition(agentCode).orElseThrow();
        assertThat(createdDefinition.getSystemPrompt()).isEqualTo("base-persona-v1");

        String updateBody = objectMapper.writeValueAsString(Map.of(
                "agentCode", agentCode,
                "agentName", "Cache Agent",
                "agentType", "STUB",
                "description", "cache test",
                "strategyType", "REACT",
                "systemPrompt", "base-persona-v2",
                "enabled", true,
                "configJson", "{}"
        ));

        mockMvc.perform(put("/api/admin/agents/" + agentCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.systemPrompt").value("base-persona-v2"));

        AgentDefinition updatedDefinition = agentRegistry.getDefinition(agentCode).orElseThrow();
        assertThat(updatedDefinition.getSystemPrompt()).isEqualTo("base-persona-v2");
    }

    @Test
    void shouldComposePromptByPriorityAndOrder() throws Exception {
        String agentCode = "prompt-agent";
        createAgent(agentCode, "agent-persona-layer");

        Long templateOneId = createPrompt("流程模板A", "workflow-step-one", "workflow-a");
        Long templateTwoId = createPrompt("流程模板B", "workflow-step-two", "workflow-b");

        mockMvc.perform(put("/api/admin/agents/" + agentCode + "/prompt-bindings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "bindings", List.of(
                                        Map.of("promptTemplateId", templateOneId),
                                        Map.of("promptTemplateId", templateTwoId)
                                )
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].promptTemplateId").value(templateOneId))
                .andExpect(jsonPath("$.data[1].promptTemplateId").value(templateTwoId));

        upsertPreference(88L, "language", "zh-CN");
        upsertPreference(88L, "nickname", "小智");
        upsertPreference(88L, "tone", "简洁专业");

        String systemPrompt = initialAgentPromptService.buildSystemPrompt(agentCode, 88L);
        assertOrder(systemPrompt, "agent-persona-layer", "workflow-step-one", "workflow-step-two", "用户偏好语言：zh-CN");
        assertThat(systemPrompt).contains("用户偏好称呼：小智");
        assertThat(systemPrompt).contains("用户偏好风格：简洁专业");

        Long templateThreeId = createPrompt("流程模板C", "workflow-step-three", "workflow-c");
        mockMvc.perform(put("/api/admin/agents/" + agentCode + "/prompt-bindings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "bindings", List.of(
                                        Map.of("promptTemplateId", templateThreeId),
                                        Map.of("promptTemplateId", templateOneId)
                                )
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].promptTemplateId").value(templateThreeId))
                .andExpect(jsonPath("$.data[1].promptTemplateId").value(templateOneId));

        String refreshedPrompt = initialAgentPromptService.buildSystemPrompt(agentCode, 88L);
        assertOrder(refreshedPrompt, "agent-persona-layer", "workflow-step-three", "workflow-step-one", "用户偏好语言：zh-CN");
        assertThat(refreshedPrompt).doesNotContain("workflow-step-two");
    }

    @Test
    void shouldSkipMissingPromptTemplateWhenBuildingPrompt() throws Exception {
        String agentCode = "missing-template-agent";
        createAgent(agentCode, "safe-persona-layer");

        Long templateId = createPrompt("待删除模板", "deleted-workflow-step", "deleted-category");
        mockMvc.perform(put("/api/admin/agents/" + agentCode + "/prompt-bindings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "bindings", List.of(Map.of("promptTemplateId", templateId))
                        ))))
                .andExpect(status().isOk());

        promptTemplateMapper.deleteById(templateId);
        assertThat(agentPromptBindingMapper.selectByAgentCode(agentCode)).hasSize(1);

        String systemPrompt = initialAgentPromptService.buildSystemPrompt(agentCode, 99L);
        assertThat(systemPrompt).contains("safe-persona-layer");
        assertThat(systemPrompt).doesNotContain("deleted-workflow-step");
    }

    /**
     * 创建测试 Agent。
     */
    private void createAgent(String agentCode, String systemPrompt) throws Exception {
        String requestBody = objectMapper.writeValueAsString(Map.of(
                "agentCode", agentCode,
                "agentName", agentCode,
                "agentType", "STUB",
                "description", "test agent",
                "strategyType", "REACT",
                "systemPrompt", systemPrompt,
                "enabled", true,
                "configJson", "{}"
        ));
        mockMvc.perform(post("/api/admin/agents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.agentCode").value(agentCode));
    }

    /**
     * 创建测试 Prompt，并返回主键 ID。
     */
    private Long createPrompt(String templateName, String templateContent, String category) throws Exception {
        String requestBody = objectMapper.writeValueAsString(Map.of(
                "templateName", templateName,
                "templateContent", templateContent,
                "category", category,
                "remark", "test"
        ));
        MvcResult result = mockMvc.perform(post("/api/admin/prompts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.path("data").path("id").asLong();
    }

    /**
     * 写入测试用户偏好。
     */
    private void upsertPreference(Long userId, String prefKey, String prefValue) throws Exception {
        String requestBody = objectMapper.writeValueAsString(Map.of(
                "prefKey", prefKey,
                "prefValue", prefValue,
                "prefType", "TEXT",
                "source", "MANUAL"
        ));
        mockMvc.perform(put("/api/v1/memory/preferences/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());
    }

    /**
     * 断言多个片段在字符串中的出现顺序。
     */
    private void assertOrder(String text, String first, String second, String third, String fourth) {
        assertThat(text.indexOf(first)).isGreaterThanOrEqualTo(0);
        assertThat(text.indexOf(second)).isGreaterThan(text.indexOf(first));
        assertThat(text.indexOf(third)).isGreaterThan(text.indexOf(second));
        assertThat(text.indexOf(fourth)).isGreaterThan(text.indexOf(third));
    }
}
