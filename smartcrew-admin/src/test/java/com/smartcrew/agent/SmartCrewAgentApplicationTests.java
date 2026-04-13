package com.smartcrew.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcrew.agent.api.tool.service.ToolExecutor;
import com.smartcrew.agent.common.exception.ServiceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SmartCrew Agent 应用集成测试类。
 *
 * <p>测试覆盖以下核心能力：
 * <ul>
 *   <li>Agent 注册、查询与指令派发</li>
 *   <li>决策规划器能力</li>
 *   <li>工具管理与执行控制</li>
 *   <li>用户偏好记忆管理</li>
 *   <li>提示词模板管理</li>
 *   <li>平台事件路由</li>
 * </ul>
 *
 * @author SmartCrew
 */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SmartCrewAgentApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ToolExecutor toolExecutor;

    /**
     * 测试 Agent 的完整生命周期：注册、查询与派发。
     * 1. 注册一个新的自定义 Agent。
     * 2. 查询 Agent 列表并校验注册结果。
     * 3. 向该 Agent 发送指令并校验响应。
     */
    @Test
    void shouldRegisterListAndDispatchAgent() throws Exception {
        // 测试步骤 1：注册 Agent
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

        // 测试步骤 2：查询 Agent 列表
        mockMvc.perform(get("/api/v1/agents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows[?(@.agentCode=='custom-agent')]").exists());

        // 测试步骤 3：向 Agent 派发指令
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

    /**
     * 测试决策规划器（Planner Agent）能力。
     * 验证系统可根据用户输入生成完整决策计划。
     * - thought：思考过程
     * - steps：执行步骤
     * - finalAction：最终动作
     */
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

    /**
     * 测试工具管理与执行控制能力。
     * 1. 验证 terminal 工具默认存在且处于禁用状态。
     * 2. 验证禁用工具无法被执行。
     */
    @Test
    void shouldExposeToolsAndRejectDisabledTerminalExecution() throws Exception {
        // 测试步骤 1：查询工具列表并验证 terminal 状态
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

        // 测试步骤 2：验证禁用工具无法执行
        assertThatThrownBy(() -> toolExecutor.execute("terminal", Map.of("command", "echo hi")))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("disabled");
    }

    /**
     * 测试用户偏好记忆管理能力。
     * 1. 为用户设置偏好（语言为简体中文）。
     * 2. 查询用户偏好并验证写入结果。
     */
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

        // 测试步骤 1：写入用户偏好
        mockMvc.perform(put("/api/v1/memory/preferences/88")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.prefKey").value("language"));

        // 测试步骤 2：查询用户偏好
        mockMvc.perform(get("/api/v1/memory/preferences/88"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].prefKey").value("language"))
                .andExpect(jsonPath("$.data[0].prefValue").value("zh-CN"));
    }

    /**
     * 测试提示词模板管理能力。
     * 1. 创建新的提示词模板。
     * 2. 按分类查询并验证返回结果。
     */
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

        // 测试步骤 1：创建提示词模板
        mockMvc.perform(post("/api/v1/prompts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.category").value("chat"));

        // 测试步骤 2：按分类查询提示词模板
        mockMvc.perform(get("/api/v1/prompts/category/chat"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.templateName").value("chat-default"));
    }

    /**
     * 验证后台 Prompt 支持按 ID 修改。
    @Test
    void shouldUpdatePromptByIdInAdmin() throws Exception {
        String createRequest = """
                {
                  "templateName": "admin-prompt-origin",
                  "templateContent": "origin content",
                  "category": "admin-edit-case",
                  "remark": "origin"
                }
                """;

        MvcResult createResult = mockMvc.perform(post("/api/admin/prompts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        Long promptId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        String updateRequest = """
                {
                  "templateName": "admin-prompt-updated",
                  "templateContent": "updated content",
                  "category": "admin-edit-case",
                  "remark": "updated"
                }
                """;

        mockMvc.perform(put("/api/admin/prompts/" + promptId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(promptId))
                .andExpect(jsonPath("$.data.templateName").value("admin-prompt-updated"))
                .andExpect(jsonPath("$.data.templateContent").value("updated content"))
                .andExpect(jsonPath("$.data.remark").value("updated"));
    }

    /**
     * 验证 Prompt 删除前会校验绑定关系。
    @Test
    void shouldBlockDeletingPromptWhenBindingsExist() throws Exception {
        String createAgentRequest = objectMapper.writeValueAsString(Map.of(
                "agentCode", "prompt-delete-check-agent",
                "agentName", "Prompt 删除校验 Agent",
                "agentType", "STUB",
                "description", "用于测试 Prompt 删除关联校验",
                "strategyType", "REACT",
                "systemPrompt", "test",
                "enabled", true,
                "configJson", "{}"
        ));

        mockMvc.perform(post("/api/admin/agents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createAgentRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        String createPromptRequest = """
                {
                  "templateName": "prompt-delete-case",
                  "templateContent": "delete me",
                  "category": "admin-delete-case",
                  "remark": "for binding delete check"
                }
                """;

        MvcResult createPromptResult = mockMvc.perform(post("/api/admin/prompts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPromptRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        Long promptId = objectMapper.readTree(createPromptResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        String bindRequest = """
                {
                  "bindings": [
                    {
                      "promptTemplateId": %d
                    }
                  ]
                }
                """.formatted(promptId);

        mockMvc.perform(put("/api/admin/agents/prompt-delete-check-agent/prompt-bindings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bindRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(delete("/api/admin/prompts/" + promptId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(409))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("prompt-delete-check-agent")));

        mockMvc.perform(put("/api/admin/agents/prompt-delete-check-agent/prompt-bindings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bindings\":[]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(delete("/api/admin/prompts/" + promptId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    /**
     * 测试平台事件路由能力。
     * 1. 发送企业微信事件消息。
     * 2. 验证事件被正确路由并处理。
     */
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

    /**
     * 测试未知平台事件处理能力。
     * 验证发送到不支持平台时会返回失败。
     */
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

    /**
     * 验证后台 Agent 列表可识别代码 Agent 并返回统一来源视图。
     */
    @Test
    @Order(1)
    void shouldExposeCodeOnlyAgentInAdminView() throws Exception {
        MvcResult listResult = mockMvc.perform(get("/api/admin/agents"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode listRoot = objectMapper.readTree(listResult.getResponse().getContentAsString());
        JsonNode initialAgentNode = null;
        for (JsonNode node : listRoot.path("rows")) {
            if ("initial-agent".equals(node.path("agentCode").asText())) {
                initialAgentNode = node;
                break;
            }
        }

        assertThat(initialAgentNode).isNotNull();
        assertThat(initialAgentNode.path("sourceStatus").asText()).isEqualTo("CODE_ONLY");
        assertThat(initialAgentNode.path("hasCodeBean").asBoolean()).isTrue();
        assertThat(initialAgentNode.path("hasDatabaseConfig").asBoolean()).isFalse();

        mockMvc.perform(get("/api/admin/agents/initial-agent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.agentCode").value("initial-agent"))
                .andExpect(jsonPath("$.data.sourceStatus").value("CODE_ONLY"))
                .andExpect(jsonPath("$.data.hasCodeBean").value(true))
                .andExpect(jsonPath("$.data.hasDatabaseConfig").value(false));
    }

    /**
     * 验证可直接新增数据库 Agent，或为代码 Agent 创建数据库信息。
     */
    @Test
    @Order(2)
    void shouldCreateDatabaseAgentAndLinkCodeAgent() throws Exception {
        String createDatabaseOnlyBody = objectMapper.writeValueAsString(Map.of(
                "agentCode", "ops-agent",
                "agentName", "运营占位 Agent",
                "agentType", "STUB",
                "description", "仅数据库存在的运营 Agent",
                "strategyType", "REACT",
                "systemPrompt", "请协助处理运营问题",
                "enabled", true,
                "configJson", "{\"channel\":\"admin\"}"
        ));

        mockMvc.perform(post("/api/admin/agents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createDatabaseOnlyBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.agentCode").value("ops-agent"))
                .andExpect(jsonPath("$.data.sourceStatus").value("DB_ONLY"))
                .andExpect(jsonPath("$.data.hasCodeBean").value(false))
                .andExpect(jsonPath("$.data.hasDatabaseConfig").value(true));

        String createLinkedBody = objectMapper.writeValueAsString(Map.of(
                "agentCode", "initial-agent",
                "agentName", "初始智能体数据库配置",
                "agentType", "BUILTIN",
                "description", "为代码 Agent 创建数据库配置",
                "strategyType", "REACT",
                "systemPrompt", "请优先使用数据库配置",
                "enabled", true,
                "configJson", "{\"scene\":\"web\"}"
        ));

        mockMvc.perform(post("/api/admin/agents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createLinkedBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.agentCode").value("initial-agent"))
                .andExpect(jsonPath("$.data.sourceStatus").value("LINKED"))
                .andExpect(jsonPath("$.data.hasCodeBean").value(true))
                .andExpect(jsonPath("$.data.hasDatabaseConfig").value(true));

        mockMvc.perform(get("/api/admin/agents/initial-agent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sourceStatus").value("LINKED"))
                .andExpect(jsonPath("$.data.hasCodeBean").value(true))
                .andExpect(jsonPath("$.data.hasDatabaseConfig").value(true));
    }
    /**
     * 验证后台用户列表支持分页参数。
     */
    @Test
    void shouldPageAdminUsersWhenRequested() throws Exception {
        String suffix = String.valueOf(System.currentTimeMillis());
        String firstUser = """
                {
                  "username": "page_user_%s_a",
                  "password": "123456",
                  "displayName": "分页用户A"
                }
                """.formatted(suffix);
        String secondUser = """
                {
                  "username": "page_user_%s_b",
                  "password": "123456",
                  "displayName": "分页用户B"
                }
                """.formatted(suffix);

        mockMvc.perform(post("/api/web/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(firstUser))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(post("/api/web/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(secondUser))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/admin/users")
                        .param("keyword", "page_user_" + suffix)
                        .param("pageNum", "1")
                        .param("pageSize", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.rows.length()").value(1));
    }

    /**
     * 验证后台 Prompt 分类列表支持分页参数。
     */
    @Test
    void shouldPageAdminPromptCategoriesWhenRequested() throws Exception {
        String suffix = String.valueOf(System.currentTimeMillis());
        String firstPrompt = """
                {
                  "templateName": "prompt-category-a",
                  "templateContent": "content-a",
                  "category": "aa-page-category-%s",
                  "remark": "page-test-a"
                }
                """.formatted(suffix);
        String secondPrompt = """
                {
                  "templateName": "prompt-category-b",
                  "templateContent": "content-b",
                  "category": "ab-page-category-%s",
                  "remark": "page-test-b"
                }
                """.formatted(suffix);

        mockMvc.perform(post("/api/admin/prompts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(firstPrompt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(post("/api/admin/prompts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(secondPrompt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/admin/prompts/categories")
                        .param("pageNum", "1")
                        .param("pageSize", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.rows.length()").value(1));
    }

    /**
     * 验证后台会话列表支持分页参数。
     */
    @Test
    void shouldPageAdminConversationSessionsWhenRequested() throws Exception {
        String suffix = String.valueOf(System.currentTimeMillis());
        String firstEvent = """
                {
                  "platformUserId": "page-session-%s-a",
                  "eventType": "message",
                  "content": "hello a",
                  "metadata": {
                    "tenant": "demo"
                  }
                }
                """.formatted(suffix);
        String secondEvent = """
                {
                  "platformUserId": "page-session-%s-b",
                  "eventType": "message",
                  "content": "hello b",
                  "metadata": {
                    "tenant": "demo"
                  }
                }
                """.formatted(suffix);

        mockMvc.perform(post("/api/v1/platform/wecom/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(firstEvent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.handled").value(true));

        mockMvc.perform(post("/api/v1/platform/wecom/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(secondEvent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.handled").value(true));

        mockMvc.perform(get("/api/admin/conversations/sessions")
                        .param("provider", "WECOM")
                        .param("keyword", "page-session-" + suffix)
                        .param("pageNum", "1")
                        .param("pageSize", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.rows.length()").value(1));
    }
}
