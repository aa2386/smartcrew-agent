# 多智能体协作系统（3 Agent 架构）扩展与智能化升级设计

## 1. 背景与目标

当前 SmartCrew-Agent 已具备以下基础能力：

- `InitialAgent` 作为默认对话入口
- `AgentRegistry / AgentCoordinator` 作为运行时注册与派发骨架
- `agent_definition / agent_prompt_binding / agent_tool_binding` 作为 Agent 配置化管理模型
- `llm_conversation_session / llm_conversation_message` 作为会话持久化模型
- RAG、知识库与向量检索链路
- 后台管理端的 Agent、Prompt、Tool、会话与偏好管理能力

本次需求是在不破坏现有 `/api/v1` 兼容性的前提下，将当前单入口对话链路升级为可审计、可扩展、可持续积累经验的 3 Agent 协作体系。

本次设计目标：

1. 将 `InitialAgent` 改造为唯一用户入口，负责决策与调度。
2. 新增 `ExecutionAgent`，负责任务执行、工具调用与具体落地。
3. 新增 `MemoryAgent`，负责经验召回、经验沉淀与记忆管理。
4. 为多 Agent 协作过程建立完整日志审计链路。
5. 在后台管理模块新增协作日志只读查询页面。
6. 采用 `MySQL` 作为权威存储，采用现有向量链路作为经验召回层，一期不引入 `Redis` 缓存。
7. 通过全局经验池实现“越用越智能”，但本期不做自动 Prompt/策略生成。

## 2. 设计原则

### 2.1 最小侵入

尽量复用现有：

- `AgentRegistry`
- `AgentCoordinator`
- `agent_definition`
- `agent_prompt_binding`
- `agent_tool_binding`
- `llm_conversation_*`
- 后台 `PageQuery + TableDataInfo + MyBatis-Plus` 分页体系
- `smartcrew-ui` 现有液态玻璃后台风格

### 2.2 职责清晰

本次升级不把三类 Agent 的职责混在一个 Bean 里，而是引入单独的编排内核统一调度。

### 2.3 主链路稳定优先

经验召回、经验沉淀、向量检索均不得阻塞主聊天链路；若记忆链路失败，系统必须可退化为无经验协作。

### 2.4 一期边界明确

本期只实现：

- 3 Agent 协作架构
- 协作日志只读查询
- 全局经验池
- MySQL 主存储 + 向量召回

本期不实现：

- 缓存层
- 日志重试/删除/导出
- 协作链路回放
- 自动生成或自动改写 Prompt/策略
- 用户级经验优先策略

## 3. 总体方案选择

本次采用“编排内核式”方案。

对比结论：

- 轻量拼接式：改动小，但会继续把逻辑堆进 `InitialAgent`，后续难以扩展。
- 编排内核式：能够兼顾 3 Agent 架构、全配置化管理、日志审计、经验积累，适合当前仓库。
- 事件驱动式：过重，需要处理异步一致性与失败补偿，不适合作为本期方案。

最终方案：

- `InitialAgent` 保留为唯一入口，但降为门面与调度入口
- 新增 `MultiAgentOrchestrator` 作为运行时编排内核
- 新增 `ExecutionAgent`
- 新增 `MemoryAgent`

## 4. 运行时架构

### 4.1 逻辑结构

```text
Web/平台消息
  -> ConversationGatewayService
  -> InitialAgent（唯一入口）
  -> MultiAgentOrchestrator（编排内核）
       -> MemoryAgent（经验召回 / 经验沉淀）
       -> ExecutionAgent（执行 / 工具调用）
  -> InitialAgent 汇总最终答复
  -> llm_conversation_* 保存最终对话结果
```

### 4.2 Agent 职责

#### InitialAgent

职责：

- 接收用户消息
- 生成根协作上下文与 `traceId`
- 发起协作编排
- 基于历史经验形成调度决策
- 汇总最终答复返回用户

边界：

- 不直接承载大部分执行型工具逻辑
- 不直接扫描日志或经验原始表
- 不直接负责经验落盘

#### ExecutionAgent

职责：

- 负责具体任务执行
- 调用 Tool
- 产出执行结果
- 输出执行摘要供日志与经验沉淀使用

边界：

- 不直接面向最终用户
- 不直接决定是否沉淀经验

#### MemoryAgent

职责：

- 查询全局经验池
- 调用向量检索能力召回经验
- 汇总经验摘要包
- 在执行成功后沉淀新经验或更新已有经验统计

边界：

- 不直接执行工具任务
- 不直接向用户回复最终结果

#### MultiAgentOrchestrator

职责：

- 统一协作编排
- 维护协作上下文
- 记录步骤日志
- 管理调用顺序与失败降级

### 4.3 一次典型请求链路

1. 用户消息进入 `InitialAgent`
2. `InitialAgent` 创建根协作请求并调用 `MultiAgentOrchestrator`
3. 编排层先调用 `MemoryAgent` 执行经验召回
4. `MemoryAgent` 先走 MySQL 元数据过滤，再走向量层精召回
5. 返回经验摘要包
6. `InitialAgent` 基于当前消息 + 经验摘要做调度决策
7. 编排层调用 `ExecutionAgent`
8. `ExecutionAgent` 完成工具调用或任务执行
9. 编排层调用 `MemoryAgent` 进行经验沉淀
10. 编排层写入最终协作日志
11. `InitialAgent` 返回最终回复

## 5. 配置化管理方案

### 5.1 三类 Agent 全配置化

三类 Agent 全部进入现有 `Agent 管理` 体系：

- `initial-agent`
- `execution-agent`
- `memory-agent`

复用现有表：

- `agent_definition`
- `agent_prompt_binding`
- `agent_tool_binding`

### 5.2 可配置项

每个 Agent 支持以下配置：

- `agentCode`
- `agentName`
- `agentType`
- `description`
- `strategyType`
- `systemPrompt`
- `configJson`
- `enabled`
- Prompt 绑定
- Tool 绑定

### 5.3 默认职责约束

虽然三类 Agent 进入同一配置体系，但代码仍应保留职责约束：

- `initial-agent`：唯一用户入口，默认不绑定大批执行型工具
- `execution-agent`：主要执行者，默认承接多数 Tool 绑定
- `memory-agent`：默认仅承接记忆、经验与检索相关能力

该约束用于避免后台误配置导致三个 Agent 职责漂移。

## 6. 数据模型设计

### 6.1 复用现有表

继续复用：

- `llm_conversation_session`
- `llm_conversation_message`
- `user_preference`
- `agent_definition`
- `agent_prompt_binding`
- `agent_tool_binding`

其中：

- `llm_conversation_*` 仅承担用户对话结果存储
- `user_preference` 保留用户偏好存储能力，但本期不作为主经验池

### 6.2 新增表：协作日志表

建议新增：`agent_collaboration_log`

用途：

- 记录一次协作过程中的每一个步骤
- 支撑后台日志查询
- 为经验沉淀提供可追溯来源

建议字段：

- `id`
- `trace_id`
- `root_session_id`
- `user_id`
- `source`
- `agent_code`
- `step_type`
- `step_name`
- `parent_step_id`
- `status`
- `input_snapshot`
- `output_snapshot`
- `decision_snapshot`
- `error_message`
- `start_time`
- `end_time`
- `duration_ms`
- `create_time`
- `update_time`

说明：

- `trace_id`：一条用户请求的全链路标识
- `root_session_id`：根会话 ID，对齐现有聊天会话
- `step_type`：如 `DISPATCH`、`MEMORY_READ`、`DECISION`、`EXECUTION`、`TOOL_CALL`、`MEMORY_WRITE`、`FINAL_RESPONSE`
- `decision_snapshot`：记录调度原因摘要
- `input_snapshot` 与 `output_snapshot`：存摘要，不存无边界原文

建议索引：

- `(trace_id)`
- `(root_session_id, start_time)`
- `(agent_code, start_time)`
- `(status, start_time)`
- `(step_type, start_time)`

### 6.3 新增表：经验池主表

建议新增：`agent_experience_pool`

用途：

- 存储可复用的全局经验卡片
- 作为 MySQL 权威经验来源

建议字段：

- `id`
- `experience_code`
- `scope_type`
- `experience_type`
- `title`
- `trigger_pattern`
- `strategy_summary`
- `recommended_agent_code`
- `recommended_tool_codes`
- `success_sample`
- `failure_avoidance`
- `quality_score`
- `hit_count`
- `success_count`
- `last_used_at`
- `enabled`
- `source_trace_id`
- `create_time`
- `update_time`

说明：

- `scope_type` 本期固定以 `GLOBAL` 为主，后续可扩展
- `recommended_tool_codes` 可使用 JSON 存储
- `strategy_summary`、`success_sample` 需控制长度，避免大字段失控

建议索引：

- `(experience_code)`
- `(scope_type, enabled)`
- `(experience_type, enabled)`
- `(quality_score, success_count, last_used_at)`
- `(source_trace_id)`

### 6.4 新增表：经验命中表

建议新增：`agent_experience_hit_log`

用途：

- 记录某次协作命中了哪些经验
- 建立经验效果反馈闭环

建议字段：

- `id`
- `trace_id`
- `experience_code`
- `agent_code`
- `applied_stage`
- `applied_snapshot`
- `success_flag`
- `create_time`

建议索引：

- `(trace_id)`
- `(experience_code, create_time)`

## 7. 经验召回设计

### 7.1 总体策略

本期采用：

`MySQL 粗筛 -> 向量层精召回 -> MemoryAgent 汇总 -> InitialAgent 决策使用`

### 7.2 召回流程

1. `MemoryAgent` 接收当前任务上下文
2. 先查询 `agent_experience_pool`
3. 按以下条件过滤：
   - `enabled = true`
   - `scope_type = GLOBAL`
   - `experience_type` 与任务场景匹配
4. 按 `quality_score`、`success_count`、`last_used_at` 排序
5. 取候选集，例如前 `50` 条
6. 将候选经验摘要送入现有向量检索链路
7. 返回 Top `5 ~ 8` 条经验摘要
8. 汇总为“经验摘要包”供 `InitialAgent` 使用

### 7.3 为什么不直接查 MySQL 大文本

原因：

- MySQL 适合作为事务主库与结构化过滤层
- 不适合承担高频语义召回
- 经验检索本质上更接近短文本知识召回，复用现有向量链路成本更低

### 7.4 为什么本期不加 Redis

原因：

- 当前仓库尚未引入缓存依赖
- 一期重点是架构落地与可用性，不是极致性能
- 先用 MySQL + 向量召回，可以更清晰验证经验池有效性

## 8. 经验沉淀设计

### 8.1 沉淀原则

本期只沉淀“全局成功经验”，不做自动策略生成。

### 8.2 沉淀触发条件

建议仅在以下条件同时满足时入池：

- 执行成功
- 协作链路完整
- 能抽取出明确可复用策略
- 不属于闲聊类请求

### 8.3 沉淀结果分类

#### 已命中已有经验且成功

更新：

- `hit_count`
- `success_count`
- `last_used_at`
- 必要时微调 `quality_score`

#### 未命中合适经验但本次成功

新增：

- 一条新的经验卡片
- 经验摘要
- `source_trace_id`
- 推荐 Agent/Tool 信息

同时同步：

- 经验摘要进入向量检索层

### 8.4 本期不做的能力

- 自动合并经验
- 自动淘汰经验
- 自动改写 Prompt
- 失败经验自动入池

失败信息本期仅记录到协作日志，不进入经验池主链路。

## 9. 协作日志设计

### 9.1 日志记录范围

以下关键步骤都需要记录日志：

- 请求进入入口 Agent
- 经验读取
- 决策过程
- 执行步骤
- Tool 调用
- 经验写入
- 最终响应
- 异常与降级

### 9.2 日志粒度

采用“每个步骤一条日志”的粒度，而不是“每次请求一条总日志”。

优点：

- 可按步骤定位问题
- 可展示多 Agent 时间线
- 可支持后续回放能力扩展

### 9.3 日志内容控制

为避免表膨胀和敏感信息风险：

- 只保存输入/输出摘要
- 长文本需截断或摘要化
- 重点存结构化决策与状态信息

## 10. 失败降级与可用性设计

### 10.1 MemoryAgent 失败

处理策略：

- 记录 `MEMORY_READ_FAILED`
- 主链路继续
- `InitialAgent` 按无经验模式继续调度

### 10.2 向量层失败

处理策略：

- 降级为仅使用 MySQL 候选经验
- 记录协作日志

### 10.3 ExecutionAgent 失败

处理策略：

- 记录 `EXECUTION_FAILED`
- 返回可理解错误信息
- 不沉淀成功经验

### 10.4 经验写入失败

处理策略：

- 主链路不回滚
- 只记日志
- 保证对用户最终回复不受影响

## 11. 后台管理页面设计

### 11.1 页面定位

新增后台菜单：`协作日志`

目标：

- 只读查看多 Agent 协作日志
- 支持分页、筛选、详情查看

### 11.2 页面结构

复用现有后台双栏布局模式：

- 左侧：协作日志列表
- 右侧：当前 `traceId` 对应步骤详情与时间线

### 11.3 顶部筛选项

- 时间范围
- `traceId`
- `sessionId`
- `agentCode`
- `stepType`
- `status`
- 关键词

### 11.4 左侧列表字段

- `traceId`
- `rootSessionId`
- `userId`
- `agentCode`
- `stepType`
- `status`
- `startTime`
- `durationMs`

### 11.5 右侧详情字段

- 步骤名称
- 输入摘要
- 决策摘要
- 输出摘要
- 错误信息
- 上下游关系
- 步骤时间线

### 11.6 页面边界

本期不提供：

- 重试
- 删除
- 导出
- 回放

## 12. 后端接口设计

全部放在 `/api/admin` 下，保持与现有体系一致。

### 12.1 协作日志接口

#### `GET /api/admin/collaboration-logs`

用途：

- 分页查询协作日志列表

入参：

- `pageNum`
- `pageSize`
- `traceId`
- `sessionId`
- `agentCode`
- `stepType`
- `status`
- `keyword`
- `startTime`
- `endTime`

返回：

- `TableDataInfo<AgentCollaborationLogVo>`

#### `GET /api/admin/collaboration-logs/{traceId}/steps`

用途：

- 查询某个协作链路下的全部步骤

返回：

- `R<List<AgentCollaborationLogStepVo>>`

### 12.2 可选过滤项接口

如实现需要，可补充：

- `GET /api/admin/collaboration-logs/filters/agents`
- `GET /api/admin/collaboration-logs/filters/step-types`

## 13. 前端改造点

### 13.1 路由

新增后台路由：

- `/admin/collaboration-logs`

### 13.2 页面文件

建议新增：

- `smartcrew-ui/src/views/admin/AdminCollaborationLogsView.vue`

### 13.3 API 封装

建议新增：

- `adminPortalApi.listCollaborationLogs(...)`
- `adminPortalApi.getCollaborationLogSteps(...)`

### 13.4 类型定义

建议新增：

- `CollaborationLogRecord`
- `CollaborationLogStepRecord`

### 13.5 风格要求

严格复用：

- `GlassPanel`
- `base.scss` 变量
- 现有分页参数与布局规范

页面文案全部使用中文。

## 14. 代码落地方向

### 14.1 后端模块

建议新增或修改的方向：

- `smartcrew-modules`
  - 编排服务
  - 执行 Agent
  - 记忆 Agent
  - 协作日志服务
  - 经验池服务
- `smartcrew-modules-api`
  - 协作日志实体、Query、VO、Mapper、Service 接口
  - 经验池实体、VO、Mapper、Service 接口
- `smartcrew-admin`
  - 协作日志后台 Controller
  - 数据库迁移脚本
- `smartcrew-ui`
  - 协作日志页面
  - 路由、API、类型定义

### 14.2 数据库脚本

建议新增迁移脚本，而不是直接修改旧初始化逻辑：

- `sql/migrations/20260509_multi_agent_collaboration.sql`

## 15. 实施拆分

### 阶段一：编排内核改造

- 新增 `MultiAgentOrchestrator`
- 改造 `InitialAgent`
- 接入 `ExecutionAgent`
- 接入 `MemoryAgent`

### 阶段二：数据层建设

- 新增协作日志表
- 新增经验池表
- 新增经验命中表
- 新增 Mapper / Service / Query / VO

### 阶段三：经验召回链路

- `MemoryAgent` 对接 MySQL 经验粗筛
- 复用现有向量链路做经验召回
- 建立经验沉淀与统计更新能力

### 阶段四：后台管理页

- 新增协作日志管理接口
- 新增后台页面
- 接入分页、筛选、详情查看

## 16. 验证方案

### 16.1 后端验证

至少覆盖：

- 用户消息经由 3 Agent 协作链处理
- 协作日志完整落盘
- 经验召回失败时主链路可退化
- 执行成功后经验统计可更新
- 后台日志分页与详情接口可用

### 16.2 前端验证

至少覆盖：

- 协作日志页分页正常
- 筛选项生效
- 详情时间线展示正常
- `1200px` 与 `768px` 下布局可用

### 16.3 构建验证

变更落地后至少执行：

- 后端编译或测试
- 前端构建

## 17. 风险与后续演进

### 17.1 一期风险

- 向量召回质量不足时，经验利用率可能偏低
- 经验摘要抽取不稳定时，经验池质量会受影响
- 若日志摘要字段设计过大，MySQL 表增长会偏快

### 17.2 风险控制

- 协作日志只存摘要
- 经验池只存提炼后的经验卡片
- 召回先粗筛再精召回
- 失败链路全部支持降级

### 17.3 后续可扩展方向

- 用户级经验池
- 失败经验规避池
- Redis 热点缓存
- OpenSearch 关键词检索
- 协作链路回放
- 自动经验治理与淘汰

## 18. 最终结论

本次设计采用：

- `InitialAgent` 唯一入口
- `ExecutionAgent` 负责执行
- `MemoryAgent` 负责经验召回与沉淀
- `MultiAgentOrchestrator` 统一编排
- `MySQL` 作为权威存储
- 现有向量链路作为经验召回层
- 后台新增协作日志只读查询页

该方案能够在现有 SmartCrew-Agent 代码基础上，以较小架构风险落地 3 Agent 协作体系，并为后续更强的智能化增强保留清晰扩展边界。
