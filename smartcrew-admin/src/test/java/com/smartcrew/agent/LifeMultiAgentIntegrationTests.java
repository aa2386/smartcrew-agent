package com.smartcrew.agent;

import com.smartcrew.agent.api.agent.domain.model.AgentDispatchCommand;
import com.smartcrew.agent.api.agent.domain.request.AgentDispatchRequest;
import com.smartcrew.agent.api.agent.domain.vo.AgentDispatchResponse;
import com.smartcrew.agent.api.agent.service.AgentCoordinator;
import com.smartcrew.agent.api.agent.service.AgentRegistry;
import com.smartcrew.agent.api.agent.service.AgentToolBindingService;
import com.smartcrew.agent.api.agentlog.entity.AgentBehaviorLog;
import com.smartcrew.agent.api.agentlog.service.AgentBehaviorLogService;
import com.smartcrew.agent.api.memory.domain.request.UserPreferenceUpsertRequest;
import com.smartcrew.agent.api.memory.domain.vo.UserPreferenceVo;
import com.smartcrew.agent.api.memory.service.UserPreferenceService;
import com.smartcrew.agent.api.schedule.entity.LifeTaskRecord;
import com.smartcrew.agent.api.schedule.service.LifeTaskRecordService;
import com.smartcrew.agent.api.tool.service.ToolRegistry;
import com.smartcrew.agent.common.config.SmartCrewProperties;
import com.smartcrew.agent.core.agent.InitialAgent;
import com.smartcrew.agent.core.agent.LifeMemoryAgent;
import com.smartcrew.agent.core.agent.LifeToolAgent;
import com.smartcrew.agent.core.agent.StubAgent;
import com.smartcrew.agent.core.agent.service.InitialAgentChatService;
import com.smartcrew.agent.core.chat.ConversationGatewayServiceImpl;
import dev.langchain4j.service.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 生活日程方向多 Agent 协作一期 集成测试。
 *
 * <p>覆盖 Agent 注册、入口调度、工具绑定、委托、记忆隔离、任务记录、调用关系和行为日志。</p>
 */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class LifeMultiAgentIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AgentRegistry agentRegistry;

    @Autowired
    private InitialAgent initialAgent;

    @Autowired
    private LifeToolAgent lifeToolAgent;

    @Autowired
    private LifeMemoryAgent lifeMemoryAgent;

    @Autowired
    private AgentCoordinator agentCoordinator;

    @Autowired
    private AgentToolBindingService agentToolBindingService;

    @Autowired
    private ToolRegistry toolRegistry;

    @Autowired
    private UserPreferenceService userPreferenceService;

    @Autowired
    private LifeTaskRecordService lifeTaskRecordService;

    @Autowired
    private AgentBehaviorLogService agentBehaviorLogService;

    @Autowired
    private SmartCrewProperties properties;

    @Autowired
    private ConversationGatewayServiceImpl conversationGatewayService;

    @MockBean
    private InitialAgentChatService initialAgentChatService;

    @BeforeEach
    void setUp() {
        Mockito.reset(initialAgentChatService);
        // 默认 mock LLM 返回成功
        when(initialAgentChatService.chat(anyString(), anyString(), anyString()))
                .thenReturn(new Result<>("Mocked LLM response", null, null, null, List.of()));
    }

    // ======================== 步骤 14.1: Agent 注册测试 ========================

    @Test
    void shouldRegisterAllThreeAgents() {
        assertThat(agentRegistry.contains("initial-agent")).isTrue();
        assertThat(agentRegistry.contains("life-tool-agent")).isTrue();
        assertThat(agentRegistry.contains("life-memory-agent")).isTrue();
    }

    @Test
    void shouldNotRegisterLifeMainAgent() {
        assertThat(agentRegistry.contains("life-main-agent")).isFalse();
    }

    @Test
    void newAgentsShouldNotBeStubAgent() {
        var toolAgent = agentRegistry.get("life-tool-agent");
        var memoryAgent = agentRegistry.get("life-memory-agent");

        assertThat(toolAgent).isPresent();
        assertThat(memoryAgent).isPresent();

        assertThat(toolAgent.get()).isNotInstanceOf(StubAgent.class);
        assertThat(memoryAgent.get()).isNotInstanceOf(StubAgent.class);
        assertThat(toolAgent.get()).isInstanceOf(LifeToolAgent.class);
        assertThat(memoryAgent.get()).isInstanceOf(LifeMemoryAgent.class);
    }

    @Test
    void initialAgentShouldStillBeInitialAgentBean() {
        assertThat(initialAgent).isInstanceOf(InitialAgent.class);
        assertThat(initialAgent.code()).isEqualTo("initial-agent");
    }

    // ======================== 步骤 14.2: 默认入口测试 ========================

    @Test
    void shouldDefaultToInitialAgentWhenNoConfig() {
        String defaultAgent = properties.getAgent().getDefaultChatAgent();
        assertThat(defaultAgent).isEqualTo("initial-agent");
    }

    @Test
    void shouldDispatchToInitialAgentByDefault() {
        var response = conversationGatewayService.chatFromWeb(1001L, "test-session", "你好");
        assertThat(response.getAgentCode()).isEqualTo("initial-agent");
    }

    // ======================== 步骤 14.3: 工具绑定测试 ========================

    @Test
    void agentToolBindingServiceShouldNotThrowForRegisteredAgent() {
        // 对于已注册的 Agent，绑定服务应正常返回（即使绑定为空）
        Set<String> boundTools = agentToolBindingService.listBoundToolCodes("initial-agent");
        assertThat(boundTools).isNotNull();
    }

    // ======================== 步骤 14.4: 委托工具测试 ========================

    @Test
    void initialAgentShouldHandleCorrectly() {
        var response = initialAgent.handle(AgentDispatchCommand.builder()
                .agentCode("initial-agent")
                .userId(1001L)
                .sessionId("delegation-test")
                .message("帮我创建一个明天上午九点的会议任务")
                .traceId("trace-deleg-01")
                .build());

        assertThat(response).isNotNull();
        assertThat(response.getAgentCode()).isEqualTo("initial-agent");
        assertThat(response.isAccepted()).isTrue();
    }

    @Test
    void lifeToolAgentShouldHandleCorrectly() {
        var response = lifeToolAgent.handle(AgentDispatchCommand.builder()
                .agentCode("life-tool-agent")
                .userId(1001L)
                .sessionId("tool-test")
                .message("查询当前时间")
                .traceId("trace-tool-01")
                .build());

        assertThat(response).isNotNull();
        assertThat(response.getAgentCode()).isEqualTo("life-tool-agent");
    }

    @Test
    void lifeMemoryAgentShouldHandleCorrectly() {
        var response = lifeMemoryAgent.handle(AgentDispatchCommand.builder()
                .agentCode("life-memory-agent")
                .userId(1001L)
                .sessionId("memory-test")
                .message("查询用户偏好语言")
                .traceId("trace-mem-01")
                .build());

        assertThat(response).isNotNull();
        assertThat(response.getAgentCode()).isEqualTo("life-memory-agent");
    }

    @Test
    void shouldDispatchToToolAgentViaCoordinator() {
        AgentDispatchRequest request = new AgentDispatchRequest();
        request.setUserId(1001L);
        request.setSessionId("coord-test");
        request.setMessage("查询当前时间");

        var response = agentCoordinator.dispatch("life-tool-agent", request);
        assertThat(response).isNotNull();
        assertThat(response.getAgentCode()).isEqualTo("life-tool-agent");
    }

    @Test
    void shouldPreserveInboundTraceIdWhenDispatching() {
        AgentDispatchRequest request = new AgentDispatchRequest();
        request.setUserId(1001L);
        request.setSessionId("coord-trace-test");
        request.setMessage("查询当前时间");
        request.setContext(new java.util.HashMap<>(Map.of("traceId", "trace-preserved-01")));

        var response = agentCoordinator.dispatch("life-tool-agent", request);
        assertThat(response).isNotNull();
        assertThat(response.getTraceId()).isEqualTo("trace-preserved-01");
    }

    // ======================== 步骤 14.5: 记忆隔离测试 ========================

    @Test
    void shouldIsolatePreferencesByUserId() {
        // 用户 2001 写入偏好
        UserPreferenceUpsertRequest req = new UserPreferenceUpsertRequest();
        req.setPrefKey("language");
        req.setPrefValue("中文");
        req.setPrefType("TEXT");
        req.setSource("TEST");
        userPreferenceService.upsert(2001L, req);

        // 用户 2001 可读取
        var vo1 = userPreferenceService.getByUserIdAndKey(2001L, "language");
        assertThat(vo1).isPresent();
        assertThat(vo1.get().getPrefValue()).isEqualTo("中文");

        // 用户 2002 不能读取用户 2001 的偏好
        var vo2 = userPreferenceService.getByUserIdAndKey(2002L, "language");
        assertThat(vo2).isEmpty();
    }

    // ======================== 步骤 14.6: 任务记录测试 ========================

    @Test
    void shouldCreateTaskRecord() {
        LifeTaskRecord record = new LifeTaskRecord();
        record.setUserId(1001L);
        record.setTitle("测试会议");
        record.setDescription("测试任务描述");
        record.setSource("TEST");

        LifeTaskRecord created = lifeTaskRecordService.create(record);
        assertThat(created.getId()).isNotNull();
        assertThat(created.getStatus()).isEqualTo("NEEDS_CONFIRMATION"); // 无时间 → 待确认
    }

    @Test
    void shouldCreateTaskWithTimeInPendingStatus() {
        LifeTaskRecord record = new LifeTaskRecord();
        record.setUserId(1001L);
        record.setTitle("有时间的任务");
        record.setDueTime(java.time.LocalDateTime.now().plusDays(1));
        record.setTimeText("明天上午九点");
        record.setSource("TEST");

        LifeTaskRecord created = lifeTaskRecordService.create(record);
        assertThat(created.getId()).isNotNull();
        // 有时间信息且未显式设 status → 默认 PENDING
        assertThat(created.getStatus()).isEqualTo("PENDING");
    }

    @Test
    void shouldQueryTasksByUserId() {
        List<LifeTaskRecord> records = lifeTaskRecordService.listByUserId(1001L);
        assertThat(records).isNotNull();
    }

    @Test
    void shouldUpdateTaskStatus() {
        LifeTaskRecord record = new LifeTaskRecord();
        record.setUserId(1001L);
        record.setTitle("待更新任务");
        record.setSource("TEST");
        LifeTaskRecord created = lifeTaskRecordService.create(record);

        LifeTaskRecord updated = lifeTaskRecordService.updateStatus(created.getId(), "DONE");
        assertThat(updated).isNotNull();
        assertThat(updated.getStatus()).isEqualTo("DONE");
    }

    // ======================== 步骤 14.7: 调用关系测试 ========================

    @Test
    void shouldQueryCallableAgentsForInitialAgent() throws Exception {
        mockMvc.perform(get("/api/admin/agents/initial-agent/callable-agents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").isNumber());
    }

    @Test
    void shouldReturnErrorForNonexistentAgent() throws Exception {
        mockMvc.perform(get("/api/admin/agents/nonexistent-agent/callable-agents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    // ======================== 步骤 14.8: 行为日志测试 ========================

    @Test
    void shouldWriteAndQueryBehaviorLog() {
        AgentBehaviorLog log = agentBehaviorLogService.buildLog(
                "trace-test-01", 1001L, "session-01", "initial-agent",
                "AGENT_STARTED", "SUCCESS", "Agent 开始处理用户请求", null);

        agentBehaviorLogService.write(log);

        var logs = agentBehaviorLogService.queryByTraceId("trace-test-01");
        assertThat(logs).isNotEmpty();
        assertThat(logs.get(0).getAgentCode()).isEqualTo("initial-agent");
        assertThat(logs.get(0).getEventType()).isEqualTo("AGENT_STARTED");
    }

    @Test
    void conversationGatewayShouldWriteLifecycleLogsWithSharedTraceId() {
        var response = conversationGatewayService.chatFromWeb(1001L, "log-session-01", "你好");
        assertThat(response).isNotNull();
        assertThat(response.getTraceId()).isNotBlank();

        var logs = agentBehaviorLogService.queryByTraceId(response.getTraceId());
        assertThat(logs).extracting(AgentBehaviorLog::getEventType)
                .contains("SESSION_RECEIVED", "AGENT_STARTED", "AGENT_FINISHED");
    }

    @Test
    void shouldQueryLogsByTraceId() throws Exception {
        // 先写入一条日志
        AgentBehaviorLog log = agentBehaviorLogService.buildLog(
                "trace-query-01", 1001L, "session-q", "initial-agent",
                "AGENT_FINISHED", "SUCCESS", "处理完成", null);
        agentBehaviorLogService.write(log);

        // 通过 API 查询时间线
        mockMvc.perform(get("/api/admin/agent-logs/traces/trace-query-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.traceId").value("trace-query-01"))
                .andExpect(jsonPath("$.data.logs[0].eventType").value("AGENT_FINISHED"));
    }

    @Test
    void shouldQueryLogsByFilters() throws Exception {
        // 分页查询
        mockMvc.perform(get("/api/admin/agent-logs")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").isNumber());
    }

    // ======================== 步骤 14.9: 日志脱敏测试 ========================

    @Test
    void shouldSanitizeSensitiveMetadataInLogs() {
        AgentBehaviorLog log = agentBehaviorLogService.buildLog(
                "trace-sanitize", 1001L, "session-s", "initial-agent",
                "ERROR", "FAILED", "处理异常", java.util.Map.of("password", "s3cr3t", "input", "normal"));

        agentBehaviorLogService.write(log);

        var logs = agentBehaviorLogService.queryByTraceId("trace-sanitize");
        assertThat(logs).isNotEmpty();

        // metadata 不应包含明文密码
        String metadataJson = logs.get(0).getMetadataJson();
        assertThat(metadataJson).isNotNull();
        assertThat(metadataJson).doesNotContain("s3cr3t");
    }

    @Test
    void shouldTruncateLongEventSummary() {
        String longMsg = "A".repeat(500);
        AgentBehaviorLog log = agentBehaviorLogService.buildLog(
                "trace-trunc", 1001L, "session-t", "initial-agent",
                "ERROR", "FAILED", longMsg, null);

        agentBehaviorLogService.write(log);

        var logs = agentBehaviorLogService.queryByTraceId("trace-trunc");
        assertThat(logs).isNotEmpty();
        // eventSummary 应被截断
        assertThat(logs.get(0).getEventSummary()).isNotNull();
        assertThat(logs.get(0).getEventSummary().length()).isLessThanOrEqualTo(203); // 200 + "..."
    }

    // ======================== Agent 编码与能力测试 ========================

    @Test
    void agentsShouldHaveCorrectCodes() {
        assertThat(initialAgent.code()).isEqualTo("initial-agent");
        assertThat(lifeToolAgent.code()).isEqualTo("life-tool-agent");
        assertThat(lifeMemoryAgent.code()).isEqualTo("life-memory-agent");
    }

    @Test
    void agentsShouldSupportCorrectCapabilities() {
        assertThat(initialAgent.supports("chat")).isTrue();
        assertThat(initialAgent.supports("orchestrate")).isTrue();
        assertThat(initialAgent.supports("rag")).isTrue();
        assertThat(initialAgent.supports("schedule")).isTrue();

        assertThat(lifeToolAgent.supports("tool")).isTrue();
        assertThat(lifeToolAgent.supports("schedule")).isTrue();
        assertThat(lifeToolAgent.supports("chat")).isFalse();

        assertThat(lifeMemoryAgent.supports("memory")).isTrue();
        assertThat(lifeMemoryAgent.supports("preference")).isTrue();
        assertThat(lifeMemoryAgent.supports("history")).isTrue();
        assertThat(lifeMemoryAgent.supports("task-record")).isTrue();
        assertThat(lifeMemoryAgent.supports("tool")).isFalse();
    }

    // ======================== 默认配置测试 ========================

    @Test
    void defaultChatAgentPropertyShouldBeInitialAgent() {
        SmartCrewProperties.Agent agentConfig = properties.getAgent();
        assertThat(agentConfig).isNotNull();
        assertThat(agentConfig.getDefaultChatAgent()).isEqualTo("initial-agent");
    }
}
