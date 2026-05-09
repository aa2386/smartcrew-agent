# 多智能体协作系统（3 Agent 架构）实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将现有单入口对话链路升级为 `InitialAgent + ExecutionAgent + MemoryAgent + MultiAgentOrchestrator` 的 3 Agent 协作体系，并落地协作日志后台页、MySQL 经验池与向量召回链路。

**Architecture:** 保留 `InitialAgent` 作为唯一入口，将调度逻辑下沉到 `MultiAgentOrchestrator`，由 `ExecutionAgent` 承担执行与 Tool 调用，由 `MemoryAgent` 承担经验召回与经验沉淀。协作日志与经验池落在 MySQL，经验摘要复用现有向量链路召回，后台通过 `/api/admin/collaboration-logs` 提供只读分页查询。

**Tech Stack:** Spring Boot, MyBatis-Plus, LangChain4j, H2/MySQL SQL migration, Vue 3, Element Plus, Vite

---

## 文件结构

### 后端新增文件

- `sql/migrations/20260509_multi_agent_collaboration.sql`
- `smartcrew-modules-api/src/main/java/com/smartcrew/agent/api/collaboration/domain/entity/AgentCollaborationLog.java`
- `smartcrew-modules-api/src/main/java/com/smartcrew/agent/api/collaboration/domain/query/AgentCollaborationLogQuery.java`
- `smartcrew-modules-api/src/main/java/com/smartcrew/agent/api/collaboration/domain/vo/AgentCollaborationLogVo.java`
- `smartcrew-modules-api/src/main/java/com/smartcrew/agent/api/collaboration/domain/vo/AgentCollaborationStepVo.java`
- `smartcrew-modules-api/src/main/java/com/smartcrew/agent/api/collaboration/mapper/AgentCollaborationLogMapper.java`
- `smartcrew-modules-api/src/main/java/com/smartcrew/agent/api/collaboration/service/AgentCollaborationLogService.java`
- `smartcrew-modules-api/src/main/java/com/smartcrew/agent/api/experience/domain/entity/AgentExperiencePool.java`
- `smartcrew-modules-api/src/main/java/com/smartcrew/agent/api/experience/domain/entity/AgentExperienceHitLog.java`
- `smartcrew-modules-api/src/main/java/com/smartcrew/agent/api/experience/domain/vo/AgentExperienceRecallVo.java`
- `smartcrew-modules-api/src/main/java/com/smartcrew/agent/api/experience/mapper/AgentExperiencePoolMapper.java`
- `smartcrew-modules-api/src/main/java/com/smartcrew/agent/api/experience/mapper/AgentExperienceHitLogMapper.java`
- `smartcrew-modules-api/src/main/java/com/smartcrew/agent/api/experience/service/AgentExperienceService.java`
- `smartcrew-modules/src/main/java/com/smartcrew/agent/core/collaboration/MultiAgentOrchestrator.java`
- `smartcrew-modules/src/main/java/com/smartcrew/agent/core/collaboration/DefaultMultiAgentOrchestrator.java`
- `smartcrew-modules/src/main/java/com/smartcrew/agent/core/agent/ExecutionAgent.java`
- `smartcrew-modules/src/main/java/com/smartcrew/agent/core/agent/MemoryAgent.java`
- `smartcrew-modules/src/main/java/com/smartcrew/agent/core/collaboration/AgentCollaborationLogServiceImpl.java`
- `smartcrew-modules/src/main/java/com/smartcrew/agent/core/experience/AgentExperienceServiceImpl.java`
- `smartcrew-admin/src/main/java/com/smartcrew/agent/controller/admin/AdminCollaborationLogController.java`
- `smartcrew-ui/src/views/admin/AdminCollaborationLogsView.vue`

### 后端修改文件

- `smartcrew-admin/src/test/resources/sql/test-schema.sql`
- `sql/init-smartcrew-agent.sql`
- `smartcrew-modules-api/src/main/java/com/smartcrew/agent/api/agent/domain/vo/AgentDispatchResponse.java`
- `smartcrew-modules/src/main/java/com/smartcrew/agent/core/agent/InitialAgent.java`
- `smartcrew-modules/src/main/java/com/smartcrew/agent/core/agent/service/InMemoryAgentRegistry.java`
- `smartcrew-modules/src/main/java/com/smartcrew/agent/core/agent/service/AgentDefinitionServiceImpl.java`
- `smartcrew-ui/src/router/index.ts`
- `smartcrew-ui/src/api/portal.ts`
- `smartcrew-ui/src/types/index.ts`
- `smartcrew-ui/src/layouts/AdminLayout.vue`

### 测试文件

- `smartcrew-admin/src/test/java/com/smartcrew/agent/MultiAgentCollaborationIntegrationTests.java`
- `smartcrew-admin/src/test/java/com/smartcrew/agent/AdminCollaborationLogControllerTests.java`
- `smartcrew-admin/src/test/java/com/smartcrew/agent/AgentExperienceServiceTests.java`

## Task 1: 建立数据库结构与 API 领域模型

**Files:**
- Create: `sql/migrations/20260509_multi_agent_collaboration.sql`
- Create: `smartcrew-modules-api/src/main/java/com/smartcrew/agent/api/collaboration/domain/entity/AgentCollaborationLog.java`
- Create: `smartcrew-modules-api/src/main/java/com/smartcrew/agent/api/collaboration/domain/query/AgentCollaborationLogQuery.java`
- Create: `smartcrew-modules-api/src/main/java/com/smartcrew/agent/api/collaboration/domain/vo/AgentCollaborationLogVo.java`
- Create: `smartcrew-modules-api/src/main/java/com/smartcrew/agent/api/collaboration/domain/vo/AgentCollaborationStepVo.java`
- Create: `smartcrew-modules-api/src/main/java/com/smartcrew/agent/api/collaboration/mapper/AgentCollaborationLogMapper.java`
- Create: `smartcrew-modules-api/src/main/java/com/smartcrew/agent/api/collaboration/service/AgentCollaborationLogService.java`
- Create: `smartcrew-modules-api/src/main/java/com/smartcrew/agent/api/experience/domain/entity/AgentExperiencePool.java`
- Create: `smartcrew-modules-api/src/main/java/com/smartcrew/agent/api/experience/domain/entity/AgentExperienceHitLog.java`
- Create: `smartcrew-modules-api/src/main/java/com/smartcrew/agent/api/experience/domain/vo/AgentExperienceRecallVo.java`
- Create: `smartcrew-modules-api/src/main/java/com/smartcrew/agent/api/experience/mapper/AgentExperiencePoolMapper.java`
- Create: `smartcrew-modules-api/src/main/java/com/smartcrew/agent/api/experience/mapper/AgentExperienceHitLogMapper.java`
- Create: `smartcrew-modules-api/src/main/java/com/smartcrew/agent/api/experience/service/AgentExperienceService.java`
- Modify: `smartcrew-admin/src/test/resources/sql/test-schema.sql`
- Modify: `sql/init-smartcrew-agent.sql`
- Test: `smartcrew-admin/src/test/java/com/smartcrew/agent/AgentExperienceServiceTests.java`

- [ ] **Step 1: 写数据库迁移的失败测试**

```java
@Test
void shouldExposeCollaborationAndExperienceTables() throws Exception {
    try (Connection connection = dataSource.getConnection()) {
        DatabaseMetaData metaData = connection.getMetaData();
        assertThat(hasTable(metaData, "AGENT_COLLABORATION_LOG")).isTrue();
        assertThat(hasTable(metaData, "AGENT_EXPERIENCE_POOL")).isTrue();
        assertThat(hasTable(metaData, "AGENT_EXPERIENCE_HIT_LOG")).isTrue();
    }
}
```

- [ ] **Step 2: 运行测试确认当前失败**

Run: `mvn -pl smartcrew-admin -am -Dtest=AgentExperienceServiceTests test`

Expected: FAIL with missing table assertions or missing test class errors.

- [ ] **Step 3: 编写 SQL 迁移与初始化脚本**

```sql
CREATE TABLE agent_collaboration_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    trace_id VARCHAR(64) NOT NULL,
    root_session_id VARCHAR(128) NOT NULL,
    user_id BIGINT NOT NULL,
    source VARCHAR(32) NOT NULL,
    agent_code VARCHAR(64) NOT NULL,
    step_type VARCHAR(32) NOT NULL,
    step_name VARCHAR(128) NOT NULL,
    parent_step_id BIGINT NULL,
    status VARCHAR(32) NOT NULL,
    input_snapshot CLOB NULL,
    output_snapshot CLOB NULL,
    decision_snapshot CLOB NULL,
    error_message VARCHAR(512) NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NULL,
    duration_ms BIGINT NULL,
    create_dept BIGINT NULL,
    create_by BIGINT NULL,
    create_time TIMESTAMP NULL,
    update_by BIGINT NULL,
    update_time TIMESTAMP NULL,
    remark VARCHAR(255) NULL
);

CREATE INDEX idx_collaboration_trace ON agent_collaboration_log (trace_id);
CREATE INDEX idx_collaboration_session_time ON agent_collaboration_log (root_session_id, start_time);

CREATE TABLE agent_experience_pool (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    experience_code VARCHAR(64) NOT NULL,
    scope_type VARCHAR(32) NOT NULL,
    experience_type VARCHAR(32) NOT NULL,
    title VARCHAR(128) NOT NULL,
    trigger_pattern VARCHAR(255) NOT NULL,
    strategy_summary CLOB NOT NULL,
    recommended_agent_code VARCHAR(64) NULL,
    recommended_tool_codes CLOB NULL,
    success_sample CLOB NULL,
    failure_avoidance CLOB NULL,
    quality_score DECIMAL(6,2) NOT NULL DEFAULT 0,
    hit_count BIGINT NOT NULL DEFAULT 0,
    success_count BIGINT NOT NULL DEFAULT 0,
    last_used_at TIMESTAMP NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    source_trace_id VARCHAR(64) NULL,
    create_dept BIGINT NULL,
    create_by BIGINT NULL,
    create_time TIMESTAMP NULL,
    update_by BIGINT NULL,
    update_time TIMESTAMP NULL,
    remark VARCHAR(255) NULL,
    CONSTRAINT uk_experience_code UNIQUE (experience_code)
);

CREATE TABLE agent_experience_hit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    trace_id VARCHAR(64) NOT NULL,
    experience_code VARCHAR(64) NOT NULL,
    agent_code VARCHAR(64) NOT NULL,
    applied_stage VARCHAR(32) NOT NULL,
    applied_snapshot CLOB NULL,
    success_flag BOOLEAN NOT NULL DEFAULT FALSE,
    create_dept BIGINT NULL,
    create_by BIGINT NULL,
    create_time TIMESTAMP NULL,
    update_by BIGINT NULL,
    update_time TIMESTAMP NULL,
    remark VARCHAR(255) NULL
);
```

- [ ] **Step 4: 编写实体、Query、VO 与 Mapper 接口**

```java
@Data
@TableName("agent_collaboration_log")
@EqualsAndHashCode(callSuper = true)
public class AgentCollaborationLog extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String traceId;
    private String rootSessionId;
    private Long userId;
    private String source;
    private String agentCode;
    private String stepType;
    private String stepName;
    private Long parentStepId;
    private String status;
    private String inputSnapshot;
    private String outputSnapshot;
    private String decisionSnapshot;
    private String errorMessage;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long durationMs;
}
```

```java
public interface AgentCollaborationLogMapper extends BaseMapper<AgentCollaborationLog> {
}
```

```java
public interface AgentExperienceService {
    List<AgentExperienceRecallVo> recallGlobalExperiences(String traceId, String message, String taskType);
    void recordExperienceHit(String traceId, String experienceCode, String agentCode, String appliedStage, String appliedSnapshot);
    void recordSuccessfulExperience(String traceId, String message, String finalAnswer, String strategySummary);
}
```

- [ ] **Step 5: 运行测试确认 schema 与模型通过编译**

Run: `mvn -pl smartcrew-admin -am -Dtest=AgentExperienceServiceTests test`

Expected: PASS for table existence test and compile of new domain objects.

- [ ] **Step 6: 提交当前数据层骨架**

```bash
git add sql/migrations/20260509_multi_agent_collaboration.sql sql/init-smartcrew-agent.sql smartcrew-admin/src/test/resources/sql/test-schema.sql smartcrew-modules-api/src/main/java/com/smartcrew/agent/api/collaboration smartcrew-modules-api/src/main/java/com/smartcrew/agent/api/experience smartcrew-admin/src/test/java/com/smartcrew/agent/AgentExperienceServiceTests.java
git commit -m "feat: add collaboration and experience data contracts"
```

## Task 2: 落地协作日志服务与后台查询接口

**Files:**
- Create: `smartcrew-modules/src/main/java/com/smartcrew/agent/core/collaboration/AgentCollaborationLogServiceImpl.java`
- Create: `smartcrew-admin/src/main/java/com/smartcrew/agent/controller/admin/AdminCollaborationLogController.java`
- Modify: `smartcrew-modules-api/src/main/java/com/smartcrew/agent/api/collaboration/service/AgentCollaborationLogService.java`
- Test: `smartcrew-admin/src/test/java/com/smartcrew/agent/AdminCollaborationLogControllerTests.java`

- [ ] **Step 1: 先写后台分页与详情接口的失败测试**

```java
@Test
void shouldPageCollaborationLogsAndLoadSteps() throws Exception {
    mockMvc.perform(get("/api/admin/collaboration-logs")
                    .param("pageNum", "1")
                    .param("pageSize", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.rows").isArray());

    mockMvc.perform(get("/api/admin/collaboration-logs/trace-demo/steps"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray());
}
```

- [ ] **Step 2: 运行测试确认控制器不存在**

Run: `mvn -pl smartcrew-admin -am -Dtest=AdminCollaborationLogControllerTests test`

Expected: FAIL with 404 or missing bean errors.

- [ ] **Step 3: 实现日志服务分页查询与按 traceId 查询详情**

```java
@Service
@RequiredArgsConstructor
public class AgentCollaborationLogServiceImpl implements AgentCollaborationLogService {

    private final AgentCollaborationLogMapper agentCollaborationLogMapper;

    @Override
    public IPage<AgentCollaborationLogVo> pageLogs(PageQuery pageQuery, AgentCollaborationLogQuery query) {
        LambdaQueryWrapper<AgentCollaborationLog> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(StringUtils.isNotBlank(query.getTraceId()), AgentCollaborationLog::getTraceId, query.getTraceId());
        wrapper.eq(StringUtils.isNotBlank(query.getAgentCode()), AgentCollaborationLog::getAgentCode, query.getAgentCode());
        wrapper.eq(StringUtils.isNotBlank(query.getStatus()), AgentCollaborationLog::getStatus, query.getStatus());
        wrapper.orderByDesc(AgentCollaborationLog::getStartTime);
        return agentCollaborationLogMapper.selectPage(pageQuery.build(), wrapper)
                .convert(this::toLogVo);
    }

    @Override
    public List<AgentCollaborationStepVo> listSteps(String traceId) {
        return agentCollaborationLogMapper.selectList(Wrappers.<AgentCollaborationLog>lambdaQuery()
                        .eq(AgentCollaborationLog::getTraceId, traceId)
                        .orderByAsc(AgentCollaborationLog::getStartTime))
                .stream()
                .map(this::toStepVo)
                .toList();
    }
}
```

- [ ] **Step 4: 实现后台 Controller**

```java
@RestController
@RequestMapping("/api/admin/collaboration-logs")
@ConditionalOnProperty(prefix = "smartcrew.api.admin", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AdminCollaborationLogController {

    private final AgentCollaborationLogService agentCollaborationLogService;

    @GetMapping
    public TableDataInfo<AgentCollaborationLogVo> list(PageQuery pageQuery, AgentCollaborationLogQuery query) {
        return TableDataInfo.build(agentCollaborationLogService.pageLogs(pageQuery, query));
    }

    @GetMapping("/{traceId}/steps")
    public R<List<AgentCollaborationStepVo>> listSteps(@PathVariable String traceId) {
        return R.ok(agentCollaborationLogService.listSteps(traceId));
    }
}
```

- [ ] **Step 5: 运行测试确认后台接口通过**

Run: `mvn -pl smartcrew-admin -am -Dtest=AdminCollaborationLogControllerTests test`

Expected: PASS with paged rows and step detail array.

- [ ] **Step 6: 提交协作日志查询能力**

```bash
git add smartcrew-modules/src/main/java/com/smartcrew/agent/core/collaboration/AgentCollaborationLogServiceImpl.java smartcrew-admin/src/main/java/com/smartcrew/agent/controller/admin/AdminCollaborationLogController.java smartcrew-admin/src/test/java/com/smartcrew/agent/AdminCollaborationLogControllerTests.java
git commit -m "feat: add collaboration log admin query APIs"
```

## Task 3: 改造 Agent 返回结构并接入编排内核

**Files:**
- Create: `smartcrew-modules/src/main/java/com/smartcrew/agent/core/collaboration/MultiAgentOrchestrator.java`
- Create: `smartcrew-modules/src/main/java/com/smartcrew/agent/core/collaboration/DefaultMultiAgentOrchestrator.java`
- Create: `smartcrew-modules/src/main/java/com/smartcrew/agent/core/agent/ExecutionAgent.java`
- Create: `smartcrew-modules/src/main/java/com/smartcrew/agent/core/agent/MemoryAgent.java`
- Modify: `smartcrew-modules-api/src/main/java/com/smartcrew/agent/api/agent/domain/vo/AgentDispatchResponse.java`
- Modify: `smartcrew-modules/src/main/java/com/smartcrew/agent/core/agent/InitialAgent.java`
- Modify: `smartcrew-modules/src/main/java/com/smartcrew/agent/core/agent/service/InMemoryAgentRegistry.java`
- Modify: `smartcrew-modules/src/main/java/com/smartcrew/agent/core/agent/service/AgentDefinitionServiceImpl.java`
- Test: `smartcrew-admin/src/test/java/com/smartcrew/agent/MultiAgentCollaborationIntegrationTests.java`

- [ ] **Step 1: 先写 3 Agent 编排链路的失败集成测试**

```java
@Test
void shouldCollaborateThroughInitialExecutionAndMemoryAgents() {
    AgentDispatchResponse response = conversationGatewayService.chatFromWeb(1001L, "plan-session", "请帮我整理任务");

    assertThat(response.isAccepted()).isTrue();
    assertThat(response.getAgentCode()).isEqualTo("initial-agent");
    assertThat(response.getMetadata()).containsKeys("orchestrator", "experienceCount", "executionAgent");
}
```

- [ ] **Step 2: 运行测试确认当前只有单 Agent 结果**

Run: `mvn -pl smartcrew-admin -am -Dtest=MultiAgentCollaborationIntegrationTests test`

Expected: FAIL because `metadata` is absent or orchestrator path not invoked.

- [ ] **Step 3: 给 `AgentDispatchResponse` 增加可选 metadata 字段**

```java
@Data
@Builder
public class AgentDispatchResponse {
    private String traceId;
    private String agentCode;
    private boolean accepted;
    private String message;
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
}
```

- [ ] **Step 4: 实现编排接口与默认编排器**

```java
public interface MultiAgentOrchestrator {
    AgentDispatchResponse orchestrate(AgentDispatchCommand command);
}
```

```java
@Service
@RequiredArgsConstructor
public class DefaultMultiAgentOrchestrator implements MultiAgentOrchestrator {

    private final AgentRegistry agentRegistry;
    private final AgentCollaborationLogService agentCollaborationLogService;

    @Override
    public AgentDispatchResponse orchestrate(AgentDispatchCommand command) {
        agentCollaborationLogService.startStep(command.getTraceId(), command.getSessionId(), command.getUserId(), "initial-agent", "DISPATCH", "入口调度", command.getMessage());
        AgentDispatchResponse memoryResponse = agentRegistry.get("memory-agent").orElseThrow().handle(command);
        AgentDispatchResponse executionResponse = agentRegistry.get("execution-agent").orElseThrow().handle(enrich(command, memoryResponse));
        agentRegistry.get("memory-agent").orElseThrow().handle(buildWriteBackCommand(command, executionResponse));
        return AgentDispatchResponse.builder()
                .traceId(command.getTraceId())
                .agentCode("initial-agent")
                .accepted(executionResponse.isAccepted())
                .message(executionResponse.getMessage())
                .metadata(Map.of(
                        "orchestrator", "default-multi-agent",
                        "executionAgent", "execution-agent",
                        "experienceCount", memoryResponse.getMetadata().getOrDefault("experienceCount", 0)
                ))
                .build();
    }
}
```

- [ ] **Step 5: 实现 `ExecutionAgent` 与 `MemoryAgent` 骨架**

```java
@Component
public class ExecutionAgent implements Agent {
    @Override
    public String code() { return "execution-agent"; }

    @Override
    public AgentDispatchResponse handle(AgentDispatchCommand command) {
        return AgentDispatchResponse.builder()
                .traceId(command.getTraceId())
                .agentCode(code())
                .accepted(true)
                .message(chatService.chat(memoryId(command), command.getMessage(), promptService.buildSystemPrompt(code(), command.getUserId(), "" )).content())
                .metadata(Map.of("executionSummary", "执行智能体已完成回复生成"))
                .build();
    }
}
```

```java
@Component
public class MemoryAgent implements Agent {
    @Override
    public String code() { return "memory-agent"; }

    @Override
    public AgentDispatchResponse handle(AgentDispatchCommand command) {
        List<AgentExperienceRecallVo> experiences = agentExperienceService.recallGlobalExperiences(command.getTraceId(), command.getMessage(), "GENERAL");
        return AgentDispatchResponse.builder()
                .traceId(command.getTraceId())
                .agentCode(code())
                .accepted(true)
                .message("经验召回完成")
                .metadata(Map.of(
                        "experienceCount", experiences.size(),
                        "experienceSummary", experiences.stream().map(AgentExperienceRecallVo::getStrategySummary).toList()
                ))
                .build();
    }
}
```

- [ ] **Step 6: 改造 `InitialAgent` 只做入口与汇总**

```java
@Override
public AgentDispatchResponse handle(AgentDispatchCommand command) {
    return multiAgentOrchestrator.orchestrate(command);
}
```

- [ ] **Step 7: 在注册表中确保 `execution-agent` 与 `memory-agent` 可被识别**

```java
agentRegistry.register(executionAgent, new AgentDefinition());
agentRegistry.register(memoryAgent, new AgentDefinition());
```

- [ ] **Step 8: 运行集成测试确认编排链路通过**

Run: `mvn -pl smartcrew-admin -am -Dtest=MultiAgentCollaborationIntegrationTests test`

Expected: PASS with `metadata.orchestrator=default-multi-agent`.

- [ ] **Step 9: 提交编排内核与三类 Agent**

```bash
git add smartcrew-modules-api/src/main/java/com/smartcrew/agent/api/agent/domain/vo/AgentDispatchResponse.java smartcrew-modules/src/main/java/com/smartcrew/agent/core/collaboration smartcrew-modules/src/main/java/com/smartcrew/agent/core/agent/ExecutionAgent.java smartcrew-modules/src/main/java/com/smartcrew/agent/core/agent/MemoryAgent.java smartcrew-modules/src/main/java/com/smartcrew/agent/core/agent/InitialAgent.java smartcrew-admin/src/test/java/com/smartcrew/agent/MultiAgentCollaborationIntegrationTests.java
git commit -m "feat: add multi-agent orchestrator runtime"
```

## Task 4: 接入经验召回与经验沉淀服务

**Files:**
- Create: `smartcrew-modules/src/main/java/com/smartcrew/agent/core/experience/AgentExperienceServiceImpl.java`
- Modify: `smartcrew-modules/src/main/java/com/smartcrew/agent/core/agent/MemoryAgent.java`
- Modify: `smartcrew-modules/src/main/java/com/smartcrew/agent/core/collaboration/DefaultMultiAgentOrchestrator.java`
- Test: `smartcrew-admin/src/test/java/com/smartcrew/agent/AgentExperienceServiceTests.java`

- [ ] **Step 1: 先写经验召回和成功统计更新的失败测试**

```java
@Test
void shouldRecallGlobalExperiencesAndUpdateHitStatistics() {
    List<AgentExperienceRecallVo> result = agentExperienceService.recallGlobalExperiences("trace-1", "请总结上周任务", "GENERAL");

    assertThat(result).isNotEmpty();
    agentExperienceService.recordExperienceHit("trace-1", result.get(0).getExperienceCode(), "memory-agent", "MEMORY_READ", "命中经验");
    agentExperienceService.recordSuccessfulExperience("trace-1", "请总结上周任务", "已整理完成", "先召回历史模板再输出总结");
}
```

- [ ] **Step 2: 运行测试确认服务未实现**

Run: `mvn -pl smartcrew-admin -am -Dtest=AgentExperienceServiceTests test`

Expected: FAIL with missing bean or missing mapper behavior.

- [ ] **Step 3: 实现基于 MySQL 的经验粗筛**

```java
List<AgentExperiencePool> candidates = agentExperiencePoolMapper.selectList(Wrappers.<AgentExperiencePool>lambdaQuery()
        .eq(AgentExperiencePool::getEnabled, true)
        .eq(AgentExperiencePool::getScopeType, "GLOBAL")
        .eq(StringUtils.isNotBlank(taskType), AgentExperiencePool::getExperienceType, taskType)
        .orderByDesc(AgentExperiencePool::getQualityScore)
        .orderByDesc(AgentExperiencePool::getSuccessCount)
        .last("limit 50"));
```

- [ ] **Step 4: 复用现有向量链路做二次召回**

```java
List<AgentExperienceRecallVo> recalls = candidates.stream()
        .map(item -> AgentExperienceRecallVo.builder()
                .experienceCode(item.getExperienceCode())
                .strategySummary(item.getStrategySummary())
                .recommendedAgentCode(item.getRecommendedAgentCode())
                .qualityScore(item.getQualityScore())
                .build())
        .limit(8)
        .toList();
```

```java
Embedding queryEmbedding = embeddingService.embed(message);
List<EmbeddingMatch<TextSegment>> vectorMatches = vectorStoreService.search("agent_experience_pool", queryEmbedding, 8, 0.65d);
Map<String, AgentExperienceRecallVo> recallMap = recalls.stream()
        .collect(Collectors.toMap(AgentExperienceRecallVo::getExperienceCode, item -> item));
List<AgentExperienceRecallVo> reranked = vectorMatches.stream()
        .map(match -> recallMap.get(match.embedded().metadata().getString("experienceCode")))
        .filter(Objects::nonNull)
        .toList();
```

- [ ] **Step 5: 实现命中记录与成功沉淀**

```java
@Override
public void recordExperienceHit(String traceId, String experienceCode, String agentCode, String appliedStage, String appliedSnapshot) {
    AgentExperienceHitLog hitLog = new AgentExperienceHitLog();
    hitLog.setTraceId(traceId);
    hitLog.setExperienceCode(experienceCode);
    hitLog.setAgentCode(agentCode);
    hitLog.setAppliedStage(appliedStage);
    hitLog.setAppliedSnapshot(appliedSnapshot);
    hitLog.setSuccessFlag(false);
    agentExperienceHitLogMapper.insert(hitLog);

    agentExperiencePoolMapper.incrementHitCount(experienceCode);
}
```

```java
@Override
public void recordSuccessfulExperience(String traceId, String message, String finalAnswer, String strategySummary) {
    AgentExperiencePool pool = new AgentExperiencePool();
    pool.setExperienceCode("exp-" + traceId);
    pool.setScopeType("GLOBAL");
    pool.setExperienceType("GENERAL");
    pool.setTitle(message.length() > 40 ? message.substring(0, 40) : message);
    pool.setTriggerPattern(message);
    pool.setStrategySummary(strategySummary);
    pool.setSuccessSample(finalAnswer);
    pool.setEnabled(true);
    pool.setSourceTraceId(traceId);
    pool.setQualityScore(BigDecimal.valueOf(80));
    agentExperiencePoolMapper.insert(pool);
}
```

- [ ] **Step 6: 在 `MemoryAgent` 和编排器中接入召回/回写**

```java
command.getContext().put("recalledExperiences", experiences);
```

```java
agentExperienceService.recordSuccessfulExperience(command.getTraceId(), command.getMessage(), executionResponse.getMessage(), "入口先召回经验，执行智能体生成最终回复");
```

- [ ] **Step 7: 运行测试确认经验链路通过**

Run: `mvn -pl smartcrew-admin -am -Dtest=AgentExperienceServiceTests,MultiAgentCollaborationIntegrationTests test`

Expected: PASS with persisted hit logs and recall results.

- [ ] **Step 8: 提交经验服务实现**

```bash
git add smartcrew-modules/src/main/java/com/smartcrew/agent/core/experience/AgentExperienceServiceImpl.java smartcrew-modules/src/main/java/com/smartcrew/agent/core/agent/MemoryAgent.java smartcrew-modules/src/main/java/com/smartcrew/agent/core/collaboration/DefaultMultiAgentOrchestrator.java smartcrew-admin/src/test/java/com/smartcrew/agent/AgentExperienceServiceTests.java
git commit -m "feat: add experience recall and persistence flow"
```

## Task 5: 记录编排步骤日志并完成后台菜单接入

**Files:**
- Modify: `smartcrew-modules/src/main/java/com/smartcrew/agent/core/collaboration/AgentCollaborationLogServiceImpl.java`
- Modify: `smartcrew-modules/src/main/java/com/smartcrew/agent/core/collaboration/DefaultMultiAgentOrchestrator.java`
- Modify: `smartcrew-modules/src/main/java/com/smartcrew/agent/core/agent/ExecutionAgent.java`
- Modify: `smartcrew-modules/src/main/java/com/smartcrew/agent/core/agent/MemoryAgent.java`
- Modify: `smartcrew-ui/src/layouts/AdminLayout.vue`
- Test: `smartcrew-admin/src/test/java/com/smartcrew/agent/MultiAgentCollaborationIntegrationTests.java`

- [ ] **Step 1: 先写协作日志落盘的失败断言**

```java
@Test
void shouldPersistTraceStepsForEachCollaborationStage() {
    AgentDispatchResponse response = conversationGatewayService.chatFromWeb(1001L, "collab-log-session", "安排一个执行计划");

    List<AgentCollaborationStepVo> steps = agentCollaborationLogService.listSteps(response.getTraceId());
    assertThat(steps).extracting(AgentCollaborationStepVo::getStepType)
            .contains("DISPATCH", "MEMORY_READ", "EXECUTION", "MEMORY_WRITE", "FINAL_RESPONSE");
}
```

- [ ] **Step 2: 运行测试确认只有根步骤或空数据**

Run: `mvn -pl smartcrew-admin -am -Dtest=MultiAgentCollaborationIntegrationTests test`

Expected: FAIL because step logs are missing.

- [ ] **Step 3: 在日志服务中实现开始、成功、失败记录方法**

```java
public Long startStep(String traceId, String rootSessionId, Long userId, String agentCode, String stepType, String stepName, String inputSnapshot) {
    AgentCollaborationLog entity = new AgentCollaborationLog();
    entity.setTraceId(traceId);
    entity.setRootSessionId(rootSessionId);
    entity.setUserId(userId);
    entity.setAgentCode(agentCode);
    entity.setStepType(stepType);
    entity.setStepName(stepName);
    entity.setStatus("RUNNING");
    entity.setInputSnapshot(inputSnapshot);
    entity.setStartTime(LocalDateTime.now());
    agentCollaborationLogMapper.insert(entity);
    return entity.getId();
}
```

```java
public void finishStep(Long id, String status, String outputSnapshot, String decisionSnapshot, String errorMessage) {
    AgentCollaborationLog entity = agentCollaborationLogMapper.selectById(id);
    entity.setStatus(status);
    entity.setOutputSnapshot(outputSnapshot);
    entity.setDecisionSnapshot(decisionSnapshot);
    entity.setErrorMessage(errorMessage);
    entity.setEndTime(LocalDateTime.now());
    entity.setDurationMs(Duration.between(entity.getStartTime(), entity.getEndTime()).toMillis());
    agentCollaborationLogMapper.updateById(entity);
}
```

- [ ] **Step 4: 在编排器中为每个阶段包裹日志**

```java
Long dispatchStepId = agentCollaborationLogService.startStep(command.getTraceId(), command.getSessionId(), command.getUserId(), "initial-agent", "DISPATCH", "入口调度", command.getMessage());
try {
    Long memoryReadStepId = agentCollaborationLogService.startStep(command.getTraceId(), command.getSessionId(), command.getUserId(), "memory-agent", "MEMORY_READ", "经验召回", command.getMessage());
    AgentDispatchResponse memoryResponse = memoryAgent.handle(command);
    agentCollaborationLogService.finishStep(memoryReadStepId, "SUCCESS", memoryResponse.getMessage(), "先查经验池再执行", null);
} catch (Exception ex) {
    agentCollaborationLogService.finishStep(dispatchStepId, "FAILED", null, null, ex.getMessage());
    throw ex;
}
```

- [ ] **Step 5: 在后台导航中接入“协作日志”菜单**

```ts
{ path: '/admin/collaboration-logs', label: '协作日志', icon: Tickets }
```

- [ ] **Step 6: 运行测试确认日志步骤完整**

Run: `mvn -pl smartcrew-admin -am -Dtest=MultiAgentCollaborationIntegrationTests test`

Expected: PASS with step types persisted in order.

- [ ] **Step 7: 提交日志落盘与后台菜单**

```bash
git add smartcrew-modules/src/main/java/com/smartcrew/agent/core/collaboration/AgentCollaborationLogServiceImpl.java smartcrew-modules/src/main/java/com/smartcrew/agent/core/collaboration/DefaultMultiAgentOrchestrator.java smartcrew-modules/src/main/java/com/smartcrew/agent/core/agent/ExecutionAgent.java smartcrew-modules/src/main/java/com/smartcrew/agent/core/agent/MemoryAgent.java smartcrew-ui/src/layouts/AdminLayout.vue
git commit -m "feat: persist collaboration trace steps"
```

## Task 6: 新增后台协作日志页面、路由、API 与类型定义

**Files:**
- Create: `smartcrew-ui/src/views/admin/AdminCollaborationLogsView.vue`
- Modify: `smartcrew-ui/src/router/index.ts`
- Modify: `smartcrew-ui/src/api/portal.ts`
- Modify: `smartcrew-ui/src/types/index.ts`
- Test: `smartcrew-ui` manual verification

- [ ] **Step 1: 先写前端数据契约与 API 封装**

```ts
export interface CollaborationLogRecord {
  id?: number
  traceId: string
  rootSessionId: string
  userId: number
  agentCode: string
  stepType: string
  stepName: string
  status: string
  startTime: string
  durationMs?: number
}

export interface CollaborationLogStepRecord extends CollaborationLogRecord {
  inputSnapshot?: string
  outputSnapshot?: string
  decisionSnapshot?: string
  errorMessage?: string
}
```

```ts
listCollaborationLogs(token: string, params: { traceId?: string; sessionId?: string; agentCode?: string; stepType?: string; status?: string } & PageParams) {
  const search = new URLSearchParams()
  if (params.traceId) search.set('traceId', params.traceId)
  if (params.sessionId) search.set('sessionId', params.sessionId)
  if (params.agentCode) search.set('agentCode', params.agentCode)
  if (params.stepType) search.set('stepType', params.stepType)
  if (params.status) search.set('status', params.status)
  if (params.pageNum) search.set('pageNum', String(params.pageNum))
  if (params.pageSize) search.set('pageSize', String(params.pageSize))
  return request<TablePayload<CollaborationLogRecord>>(`/api/admin/collaboration-logs?${search.toString()}`, { token })
}
```

- [ ] **Step 2: 新增页面骨架并复用现有会话页布局**

```vue
<template>
  <div class="conversation-grid">
    <GlassPanel panel-class="admin-card session-card">
      <div class="card-head">
        <div>
          <h3>协作日志</h3>
          <p class="muted">查看多智能体协作过程中的调度、执行与记忆读写日志。</p>
        </div>
      </div>

      <div class="filter-row">
        <el-input v-model="filters.traceId" placeholder="Trace ID" clearable />
        <el-input v-model="filters.sessionId" placeholder="Session ID" clearable />
        <el-input v-model="filters.agentCode" placeholder="Agent 编码" clearable />
        <el-select v-model="filters.status" placeholder="状态" clearable>
          <el-option label="成功" value="SUCCESS" />
          <el-option label="失败" value="FAILED" />
          <el-option label="运行中" value="RUNNING" />
        </el-select>
        <el-button type="primary" @click="handleSearch">查询</el-button>
      </div>
    </GlassPanel>
  </div>
</template>
```

- [ ] **Step 3: 实现页面数据加载与明细切换**

```ts
const filters = reactive({ traceId: '', sessionId: '', agentCode: '', stepType: '', status: '' })
const pager = reactive({ pageNum: 1, pageSize: 10, total: 0 })
const logs = ref<CollaborationLogRecord[]>([])
const steps = ref<CollaborationLogStepRecord[]>([])

async function loadLogs() {
  const response = await adminPortalApi.listCollaborationLogs(authStore.adminToken, {
    ...filters,
    pageNum: pager.pageNum,
    pageSize: pager.pageSize
  })
  logs.value = response.rows
  pager.total = response.total
}
```

- [ ] **Step 4: 接入路由**

```ts
{
  path: 'collaboration-logs',
  name: 'admin-collaboration-logs',
  component: () => import('../views/admin/AdminCollaborationLogsView.vue')
}
```

- [ ] **Step 5: 运行前端构建确认页面通过**

Run: `npm run build`

Expected: PASS with new admin route and no TypeScript errors.

- [ ] **Step 6: 手工验证页面交互**

Run: `npm run dev`

Expected:
- 后台侧边栏出现“协作日志”
- 列表分页正常
- 点击某条记录后右侧展示步骤时间线
- `1200px` 和 `768px` 下布局不塌陷

- [ ] **Step 7: 提交后台页面**

```bash
git add smartcrew-ui/src/views/admin/AdminCollaborationLogsView.vue smartcrew-ui/src/router/index.ts smartcrew-ui/src/api/portal.ts smartcrew-ui/src/types/index.ts
git commit -m "feat: add admin collaboration logs page"
```

## Task 7: 全链路回归验证与文档收尾

**Files:**
- Modify: `docs/superpowers/specs/2026-05-09-multi-agent-orchestrator-design.md`
- Modify: `docs/superpowers/plans/2026-05-09-multi-agent-orchestrator.md`
- Test: full backend + frontend verification

- [ ] **Step 1: 运行后端目标测试集**

Run: `mvn -pl smartcrew-admin -am -Dtest=AgentExperienceServiceTests,AdminCollaborationLogControllerTests,MultiAgentCollaborationIntegrationTests test`

Expected: PASS with all new collaboration, experience, and admin log tests green.

- [ ] **Step 2: 运行前端构建**

Run: `npm run build`

Expected: PASS with no route, type, or component errors.

- [ ] **Step 3: 检查 Git 变更范围**

```bash
git status --short
git diff --stat
```

Expected: Only planned backend, SQL, test, and UI files are modified.

- [ ] **Step 4: 更新设计文档中的实现状态说明**

```markdown
## 实施状态

- [x] 数据层结构完成
- [x] 编排内核完成
- [x] 协作日志后台页完成
- [x] 经验召回链路完成
```

- [ ] **Step 5: 进行最终提交**

```bash
git add sql/migrations/20260509_multi_agent_collaboration.sql smartcrew-modules-api smartcrew-modules smartcrew-admin smartcrew-ui docs/superpowers/specs/2026-05-09-multi-agent-orchestrator-design.md docs/superpowers/plans/2026-05-09-multi-agent-orchestrator.md
git commit -m "feat: implement multi-agent collaboration system"
```
