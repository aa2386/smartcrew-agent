package com.smartcrew.agent;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartcrew.agent.api.agent.domain.entity.AgentToolBinding;
import com.smartcrew.agent.api.agent.domain.model.AgentDispatchCommand;
import com.smartcrew.agent.api.agent.mapper.AgentToolBindingMapper;
import com.smartcrew.agent.core.agent.InitialAgent;
import com.smartcrew.agent.core.agent.service.InitialAgentChatService;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.service.Result;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderRequest;
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

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tool 基础设施集成测试。
 */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class ToolInfrastructureIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InitialAgent initialAgent;

    @Autowired
    private AgentToolBindingMapper agentToolBindingMapper;

    @Autowired
    private ToolProvider toolProvider;

    @Autowired
    private DataSource dataSource;

    @MockBean
    private InitialAgentChatService initialAgentChatService;

    @BeforeEach
    void setUp() {
        Mockito.reset(initialAgentChatService);
        agentToolBindingMapper.delete(new LambdaQueryWrapper<AgentToolBinding>()
                .eq(AgentToolBinding::getAgentCode, "initial-agent"));
    }

    @Test
    void shouldExposeCodeToolsAndMarkDatabaseOnlyMetadataAsNonExecutable() throws Exception {
        mockMvc.perform(put("/api/admin/tools/basic")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "toolCode": "basic",
                                  "toolName": "基础工具配置",
                                  "description": "基础工具数据库配置",
                                  "beanName": "basicTools",
                                  "riskLevel": "LOW",
                                  "enabled": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.toolCode").value("basic"))
                .andExpect(jsonPath("$.data.sourceStatus").value("LINKED"))
                .andExpect(jsonPath("$.data.hasCodeBean").value(true))
                .andExpect(jsonPath("$.data.hasDatabaseConfig").value(true))
                .andExpect(jsonPath("$.data.executable").value(true));

        mockMvc.perform(post("/api/admin/tools")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "toolCode": "db-only-metadata",
                                  "toolName": "数据库元数据 Tool",
                                  "description": "仅保留治理元数据",
                                  "beanName": "missingBean",
                                  "riskLevel": "LOW",
                                  "enabled": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.toolCode").value("db-only-metadata"))
                .andExpect(jsonPath("$.data.sourceStatus").value("DB_ONLY"))
                .andExpect(jsonPath("$.data.hasCodeBean").value(false))
                .andExpect(jsonPath("$.data.hasDatabaseConfig").value(true))
                .andExpect(jsonPath("$.data.executable").value(false));

        mockMvc.perform(get("/api/admin/tools/db-only-metadata"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resolveError").value("缺少代码实现"));
    }

    @Test
    void shouldExposeFlattenedToolToLangChainProvider() throws Exception {
        mockMvc.perform(put("/api/admin/agents/initial-agent/tool-bindings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "toolCodes": ["basic"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.boundTools[0].toolCode").value("basic"));

        var providerResult = toolProvider.provideTools(new ToolProviderRequest(
                "initial-agent::1001::tool-session",
                UserMessage.from("现在几点")
        ));

        ToolExecutor executor = providerResult.tools().entrySet().stream()
                .filter(entry -> "basic__currentTime".equals(entry.getKey().name()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElseThrow();

        String output = executor.execute(ToolExecutionRequest.builder()
                .id("tool-1")
                .name("basic__currentTime")
                .arguments("{}")
                .build(), "initial-agent::1001::tool-session");

        assertThat(providerResult.tools().keySet()).anySatisfy(spec -> {
            assertThat(spec.name()).isEqualTo("basic__currentTime");
            assertThat(spec.parameters().properties()).isEmpty();
        });
        assertThat(output).isNotBlank();
    }

    @Test
    void shouldDelegateInitialAgentChatToNewService() throws Exception {
        when(initialAgentChatService.chat(anyString(), anyString(), anyString()))
                .thenReturn(new Result<>("已经通过 LangChain4j Tool Calling 回答", null, null, null, List.of()));

        var response = initialAgent.handle(AgentDispatchCommand.builder()
                .agentCode("initial-agent")
                .userId(1001L)
                .sessionId("tool-session")
                .message("现在几点")
                .traceId("trace-tool")
                .build());

        ArgumentCaptor<String> systemPromptCaptor = ArgumentCaptor.forClass(String.class);
        verify(initialAgentChatService).chat(
                org.mockito.ArgumentMatchers.eq("initial-agent::1001::tool-session"),
                org.mockito.ArgumentMatchers.eq("现在几点"),
                systemPromptCaptor.capture()
        );
        assertThat(response.isAccepted()).isTrue();
        assertThat(response.getMessage()).isEqualTo("已经通过 LangChain4j Tool Calling 回答");
        assertThat(systemPromptCaptor.getValue()).isNotBlank();
    }

    @Test
    void shouldUseBeanOnlyToolDefinitionSchema() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();

            assertThat(hasColumn(metaData, "tool_definition", "execution_mode")).isFalse();
            assertThat(hasColumn(metaData, "tool_definition", "flow_definition_json")).isFalse();
            assertThat(isColumnNotNull(metaData, "tool_definition", "bean_name")).isTrue();
        }
    }

    private boolean hasColumn(DatabaseMetaData metaData, String tableName, String columnName) throws Exception {
        try (ResultSet columns = metaData.getColumns(null, null, tableName.toUpperCase(), columnName.toUpperCase())) {
            return columns.next();
        }
    }

    private boolean isColumnNotNull(DatabaseMetaData metaData, String tableName, String columnName) throws Exception {
        try (ResultSet columns = metaData.getColumns(null, null, tableName.toUpperCase(), columnName.toUpperCase())) {
            assertThat(columns.next()).isTrue();
            return columns.getInt("NULLABLE") == DatabaseMetaData.columnNoNulls;
        }
    }
}
