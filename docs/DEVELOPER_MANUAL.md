# SmartCrew-Agent Developer Manual

> 版本：v2.0  
> 适用仓库：`smartcrew-agent`  
> 更新时间：2026-04-16  
> 面向读者：后端开发、平台开发、算法工程、前端管理后台开发、技术负责人

---

## 1. 文档定位

本文档不再采用“愿景式说明”，而是以**当前仓库已经实现的真实能力**为基础，回答下面四个问题：

1. SmartCrew-Agent 现在已经能做什么。
2. 各模块分别承担什么职责。
3. 新功能应该接到哪里、怎么扩。
4. 后续应该沿着哪些主流前沿方向继续演进。

阅读建议：

- 首次接手项目：先看第 2、3、4、8、9 章。
- 要理解主链路：重点看第 4 章。
- 要扩展 Agent / Prompt / Tool / RAG：重点看第 5、8 章。
- 要做架构规划：重点看第 9、10、11 章。

---

## 2. 当前真实能力总览

截至当前代码版本，SmartCrew-Agent 已经不是一个“骨架工程”，而是一套已经打通核心闭环的智能体平台雏形。

### 2.1 后端能力

- 已实现统一 Agent 注册、发现、派发与运行时查询。
- 已实现 `initial-agent` 主执行链路。
- 已实现 Prompt 模板管理、Agent-Prompt 绑定与运行时组装。
- 已实现 Tool 双层配置体系：
  - 代码 Tool（Bean Tool）
  - 数据库 Flow Tool（顺序 DSL）
  - 代码 + 数据库联动解析
- 已实现 Agent-Tool 绑定，并已接入 `initial-agent` 运行时。
- 已实现 RAG 基础设施：
  - 知识库
  - 文档上传
  - 文档切片
  - 向量化
  - Chroma 检索
  - Agent 与知识库绑定
  - 运行时检索增强
- 已实现 Web 端聊天会话、消息历史、用户登录态接入。
- 已实现企业微信 / 飞书平台事件接入到统一会话网关。
- 已实现 DashScope LLM 同步 / 流式对话与会话落库。

### 2.2 管理后台能力

当前已存在对应后台 API 与前端页面的能力包括：

- 用户管理
- 会话管理
- Agent 管理
- Prompt 模板管理
- Tool 管理
- 知识库管理
- 偏好设置管理
- 管理端登录

### 2.3 当前不应误判为“已完成”的部分

以下能力已有基础，但仍属于**下一阶段增强项**：

- 决策引擎仍以启发式规划为主，还不是成熟的 LLM Planner。
- Tool 编排已经闭环，但还没有直接切到模型原生 function calling。
- Tool 体系已经很完整，但还没有独立的 Skill 层。
- 多 Agent 体系已有注册与扩展基础，但还没有图式编排 / 持久化工作流。
- 平台接入已打通会话入口，但线上级别的签名校验、幂等、重试、审批流还需继续补强。

---

## 3. 仓库结构与模块职责

### 3.1 模块映射

- `smartcrew-admin`
  - Spring Boot 启动模块
  - 管理端 / Web 端 / 兼容 `api/v1` 控制器
  - 集成测试
- `smartcrew-modules-api`
  - 领域实体、VO、Request、Mapper、Service 接口契约
- `smartcrew-modules`
  - 核心实现层
  - 包含 Agent、Prompt、Tool、RAG、LLM、Platform、Chat、Memory 等实现
- `smartcrew-common`
  - 公共配置、统一返回结构、异常、分页与通用工具
- `smartcrew-ui`
  - Vue 3 前端工程
  - 包含公众页与管理后台
- `sql`
  - 初始化 SQL 与增量迁移脚本
- `docs`
  - 项目文档、技术手册、技术洞察与 skill 文档

### 3.2 推荐的定位顺序

遇到业务问题时，优先按下面顺序查：

1. `controller`
2. `service`
3. `registry / orchestrator / executor`
4. `mapper + entity`
5. `ui/api`

这套顺序适用于 Agent、Tool、Prompt、RAG、Chat 几乎所有主链路。

---

## 4. 核心运行链路

### 4.1 Web 聊天主链路

主入口：

- `POST /api/web/chat/sessions`
- `GET /api/web/chat/sessions`
- `GET /api/web/chat/sessions/{sessionId}/messages`
- `POST /api/web/chat/sessions/{sessionId}/messages`

链路如下：

```text
WebChatController
-> ConversationGatewayService
-> AgentCoordinator
-> initial-agent
-> (可选) RAG augment
-> (可选) Decision plan
-> (可选) Tool orchestrator
-> LlmClient
-> ConversationStore 落库
-> 返回回答
```

职责分工：

- `WebChatController` 只负责会话接口与登录态读取。
- `ConversationGatewayServiceImpl` 负责把 Web 请求转换为统一 Agent 派发请求。
- `AgentCoordinator` 负责 traceId、派发与目标 Agent 路由。
- `InitialAgent` 负责把 Prompt、RAG、Tool、LLM 串起来。

### 4.2 平台事件主链路

平台入口：

- `POST /api/v1/platform/{platform}/events`

当前支持：

- `wecom`
- `feishu`

链路如下：

```text
PlatformController
-> PlatformAdapter(Wecom / Feishu)
-> ConversationGatewayService.chatFromPlatform(...)
-> AgentCoordinator
-> initial-agent
```

当前特征：

- 企业微信、飞书都已经不是占位适配器，而是已接入统一会话网关。
- 平台用户会通过 `UserIdentityResolver` 解析或自动创建本地用户身份。
- 平台会话根 ID 会按 provider/chat/thread 组合生成。

### 4.3 `initial-agent` 执行链路

当前 `initial-agent` 是 SmartCrew 的默认总入口 Agent，也是 Tool 与 RAG 真正落地的地方。

执行顺序：

1. 读取当前 Agent 的 Prompt 组装结果。
2. 如果启用了 RAG，则按 Agent 已绑定知识库执行检索增强。
3. 查询当前 Agent 已绑定且启用的 Tool。
4. 调用 `DecisionEngine.plan()` 生成结构化 Tool 计划。
5. 调用 `AgentToolOrchestrator` 顺序执行 Tool。
6. 将 Tool 结果与 RAG 结果一起拼入最终 LLM 请求。
7. 调用 `LlmClient` 完成最终回答。

这意味着当前系统已经不是“纯 LLM 聊天”，而是：

**Prompt + RAG + Tool + 会话持久化** 的组合式智能体执行链。

### 4.4 管理后台配置链路

后台当前已经不仅是展示页，而是实际生效的配置入口。

典型配置链路如下：

```text
Admin Controller
-> Definition/Binding Service
-> Mapper 持久化
-> Registry refresh / runtime read
-> initial-agent 在运行时消费
```

例如：

- 修改 Tool 配置后会刷新 `ToolRegistry`
- 修改 Agent 配置后会影响 Agent 详情与绑定关系
- 修改 Prompt 绑定后，下次请求就会走新组装结果
- 修改知识库绑定后，下次 RAG 检索就会生效

---

## 5. 已实现核心能力详解

### 5.1 Agent 体系

核心类：

- `InMemoryAgentRegistry`
- `AgentDiscoveryServiceImpl`
- `AgentCoordinatorImpl`
- `AgentDefinitionServiceImpl`
- `InitialAgent`
- `StubAgent`

当前设计特点：

- 代码 Bean Agent 与数据库 Agent 定义统一收敛到运行时注册中心。
- 启动时自动扫描 `Agent` Bean。
- 对数据库存在但代码暂未实现的 Agent，系统会注册 `StubAgent`，避免链路直接崩掉。
- `AgentCoordinator` 负责统一派发，而不是让控制器直接调用具体 Agent。

这意味着：

- Agent 可以工程化开发。
- Agent 也可以先在数据库建模、后补代码。
- 运行时派发路径统一，便于后续接监控、权限和多 Agent 编排。

### 5.2 Prompt 体系

核心类：

- `InitialAgentPromptServiceImpl`
- `AgentPromptBindingServiceImpl`
- `AdminPromptController`

当前采用的是**分层 Prompt 组装**：

1. Agent 自身 `system_prompt`
2. 绑定的 Prompt 模板内容
3. 用户偏好（language / nickname / tone 等）
4. RAG prompt block（运行时附加）

当前优点：

- Agent 人设、工作流模板、用户偏好职责清晰。
- Prompt 模板是可运营的，而不是写死在代码里。
- Agent 与 Prompt 模板是显式绑定关系，不走模糊匹配。

### 5.3 Tool 体系

当前 Tool 体系已经是本项目最完整的基础设施之一。

核心类：

- `InMemoryToolRegistry`
- `DefaultToolExecutor`
- `BeanToolExecutor`
- `FlowToolExecutor`
- `ToolDefinitionServiceImpl`
- `AgentToolBindingServiceImpl`
- `AdminToolController`
- `ReActDecisionEngine`
- `AgentToolOrchestrator`

#### 5.3.1 当前 Tool 设计

当前 Tool 有两种执行模式：

- `BEAN`
  - 调用代码中的 Spring Bean Tool
- `FLOW`
  - 调用数据库中保存的顺序 DSL

当前 Tool 的运行时来源状态：

- `CODE_ONLY`
- `DB_ONLY`
- `LINKED`

运行时统一视图使用：

- `ResolvedToolDefinition`

这意味着当前 Tool 已经不是简单的“代码函数列表”，而是统一了：

- 来源状态
- 动作元数据
- 是否可执行
- 解析错误
- 管理后台展示

#### 5.3.2 当前 Flow DSL 能力边界

当前顺序 DSL 只支持三类步骤：

- `template`
- `tool_call`
- `return`

适合的场景：

- 串联已有 Tool
- 对结果做轻量包装
- 配置化定义简单执行流程

不适合的场景：

- 分支
- 循环
- 任意脚本
- 直接 SQL
- 直接 shell

所以当前 Flow Tool 的定位是：

**轻量可配置工作流，而不是通用脚本引擎。**

#### 5.3.3 当前 Agent 与 Tool 的接法

当前并没有直接走模型原生 function calling，而是：

```text
规划一次
-> 执行一次
-> 总结一次
```

当前收益：

- 模型协议解耦
- 执行平面统一
- 更容易加权限、审计、风控
- 更利于后续引入 Skill 层

当前代价：

- 多一次规划与汇总，时延高于原生函数调用
- 决策质量受启发式 Planner 限制

### 5.4 RAG 体系

核心类：

- `KnowledgeBaseAdminServiceImpl`
- `DocumentLoaderServiceImpl`
- `DocumentSplitterServiceImpl`
- `DashScopeEmbeddingServiceImpl`
- `ChromaVectorStoreServiceImpl`
- `RagAugmentationServiceImpl`
- `AdminKnowledgeBaseController`

当前闭环已经打通：

1. 管理端创建知识库
2. 上传文档
3. 异步处理文档
4. 文档切片
5. 生成向量
6. 写入 Chroma
7. 绑定 Agent
8. 运行时检索增强

当前设计取舍：

- 向量库通过 `VectorStoreService` 抽象，当前默认落到 Chroma。
- 切片策略由配置驱动，而不是写死。
- 检索增强只在运行时按 Agent 已绑定知识库执行，不是全库盲搜。
- 检索失败会降级，不会直接打崩主对话链路。

### 5.5 会话、LLM 与持久化

核心类：

- `DashScopeLlmClient`
- `LlmConversationStore`
- `ConversationGatewayServiceImpl`
- `ConversationQueryService`

当前特征：

- 已支持同步对话与流式对话。
- 已支持多轮历史消息读取。
- 已支持用户消息与助手消息落库。
- 已对会话级并发做细粒度锁控制，避免同会话消息乱序。

当前模型接入现状：

- 主实现为 DashScope / Qwen 链路。
- `SmartCrewProperties` 已为模型配置留出了统一入口。
- 会话数据保存在 `llm_conversation_session` / `llm_conversation_message`。

### 5.6 用户与身份体系

当前已支持两类入口用户：

- Web 用户
- 平台用户（企业微信 / 飞书）

能力包括：

- 后台管理员登录
- Web 用户登录态读取
- 平台用户身份解析 / 自动建档
- 会话与用户关联

### 5.7 前端与后台页面

当前前端不是空壳，已有完整后台信息架构：

- 管理端登录
- 总览 / 会话 / 用户 / Agent / Prompt / Tool / 知识库 / 偏好
- Agent 详情页已支持 Prompt 模板与 Tool 绑定
- Tool 页已支持列表、详情、执行模式、动作预览、手动执行
- 知识库页已支持文档、切片、Agent 绑定联动
- 公众页已支持登录与聊天会话交互

前端风格基线：

- 统一液态玻璃风格
- 固定视口 + 区块内滚动
- 后台以双栏管理视图为主

---

## 6. 数据模型与关键表

下面只列核心表，不展开所有字段。

### 6.1 Agent / Prompt / Tool

- `agent_definition`
- `prompt_template`
- `agent_prompt_binding`
- `tool_definition`
- `agent_tool_binding`

### 6.2 RAG

- `knowledge_base`
- `knowledge_document`
- `document_chunk`
- `agent_knowledge_binding`

### 6.3 会话与用户

- `sc_user`
- `user_identity`
- `user_preference`
- `llm_conversation_session`
- `llm_conversation_message`

### 6.4 当前特别注意

Tool 双层配置相关新增字段并不完全体现在旧初始化脚本中，当前还依赖增量迁移：

- `sql/migrations/20260416_tool_dual_layer.sql`

这意味着：

- 如果直接用旧基线 SQL 落库，需要再补跑迁移。
- 后续建议整理新的初始化基线，避免“初始化脚本落后于真实结构”。

---

## 7. 接口分层约定

项目当前已经形成比较清晰的接口分层：

- `/api/web/*`
  - 面向公众页
  - 如聊天、登录
- `/api/admin/*`
  - 面向后台管理
  - 如 Agent、Prompt、Tool、知识库、用户、会话管理
- `/api/v1/*`
  - 历史兼容与基础能力接口
  - 不应轻易破坏既有语义

开发约定：

- 新的后台管理接口优先放 `/api/admin/*`
- 新的公众页接口优先放 `/api/web/*`
- 需要兼容旧能力时，保留 `/api/v1/*`

---

## 8. 常见扩展任务指南

### 8.1 新增一个 Agent

推荐路径：

1. 先在代码中实现 `Agent` Bean。
2. 在数据库中补 Agent 定义与元数据。
3. 如果要复用现有主链路能力，可直接仿照 `InitialAgent`。
4. 通过后台页管理 Prompt / Tool / 知识库绑定。

如果只是预留配置，也可以先建数据库定义，系统会先生成 `StubAgent`。

### 8.2 新增一个代码 Tool

推荐路径：

1. 实现 `SmartCrewTool` Bean。
2. 用 `@Tool` 暴露动作。
3. 用 `@P` 补参数说明。
4. 启动后由 `ToolRegistry` 自动发现。
5. 如需运营化展示，再补数据库配置覆盖名称、描述、风险等级等元数据。

### 8.3 新增一个纯数据库 Flow Tool

推荐路径：

1. 通过后台或接口创建 `tool_definition`
2. `executionMode = FLOW`
3. 填写 `flowDefinitionJson`
4. 保存后刷新注册中心
5. 绑定到 Agent

适合轻量串联，不建议承载复杂核心业务。

### 8.4 给 Agent 增加 Prompt / Tool / 知识库能力

当前三类绑定都已经有现成模式：

- Prompt：`AgentPromptBindingService`
- Tool：`AgentToolBindingService`
- 知识库：知识库绑定服务

推荐做法：

- Prompt 负责角色与任务指令
- Tool 负责动作执行
- 知识库负责检索上下文

不要把三者职责混成一个“大 Prompt”。

### 8.5 扩展 RAG

当前适合继续增强的方向：

- 检索重排器
- Hybrid Search
- 引用片段可追溯展示
- 评测集与命中质量评估
- 文档版本化与增量重建

### 8.6 扩展后台页面

前端开发必须遵循现有视觉基线：

- 复用现有液态玻璃风格
- 不另起主题系统
- 保持固定视口 + 区域内滚动
- 列表页优先复用已有管理页骨架

---

## 9. 当前架构边界与已知风险

### 9.1 决策引擎仍偏启发式

`ReActDecisionEngine` 当前更像结构化启发式 Planner：

- 支持显式 `tool:code#action {}` 指令
- 支持部分关键词推断
- 支持简单动作候选匹配

但它还不是：

- 基于 LLM 的强规划器
- 支持复杂多步反思的 Planner
- 支持并行工具计划的成熟代理框架

### 9.2 Tool 体系不等于 Skill 体系

当前 Tool 已经解决的是：

- 动作执行
- 流程配置
- Agent 白名单绑定

但还没有解决：

- 会话级临时技能挂载
- 技能说明 / 策略 / 约束模板
- Skill 对 Tool 的上层编排

如果后续目标是接近 Codex 式 Skill 体系，建议在 Tool 之上补一层 `skill_definition`，而不是继续把所有语义都塞进 Tool。

### 9.3 Tool 编排尚未切到模型原生 function calling

当前使用“规划 -> 执行 -> 总结”骨架的优势很明显，但也意味着：

- 时延更高
- 规划精度依赖外部 Planner
- Tool schema 约束能力还不如原生 tool calling 强

### 9.4 多 Agent 仍处于“可扩展”而非“已编排”

当前系统已经具备多 Agent 的注册与派发基础，但还没有：

- Graph 级工作流引擎
- Durable execution
- Agent 间状态编排
- 人工审批断点恢复

### 9.5 初始化 SQL 与真实结构存在时间差

当前增量迁移脚本比初始化基线更新，这在多人协作或新环境初始化时容易造成误判。

### 9.6 平台链路仍需线上级治理

当前企业微信 / 飞书已经接入统一会话入口，但如果要面向生产公网环境，还应补：

- 回调签名校验
- 幂等控制
- 事件去重
- 失败重试
- 超时与审计

---

## 10. 后续演进方向

这一章只讨论**主流且值得落地到本项目**的方向，不做空泛技术展望。

### 10.1 方向一：升级到“模型原生 Tool Calling + 平台统一治理”

当前主流趋势：

- OpenAI 已以 Responses API 作为新一代 agent primitive，统一承载 tool calling、built-in tools、remote MCP 等能力。
- Anthropic 的 Tool Use 也已经把 client tools / server tools 做成一等概念。

对 SmartCrew 的建议：

- 保留当前 `ToolRegistry + ToolExecutor + AgentToolOrchestrator` 作为治理平面。
- 在 Planner 之上增加“模型原生 tool calling 适配层”。
- 短期可让 `initial-agent` 支持两种策略：
  - 当前外部规划模式
  - 原生 function calling 模式

这样做的好处：

- 性能更好
- 参数 schema 更严格
- 与主流模型生态更一致
- 同时不丢失项目现有治理能力

优先级：**高**

### 10.2 方向二：把当前启发式 Planner 升级为结构化 LLM Planner

当前主流趋势：

- 大多数成熟 Agent 系统不会长期停留在关键词启发式规划。
- 更常见的是：
  - 结构化 JSON 计划输出
  - 显式工具白名单
  - tool choice 约束
  - 可观测计划对象

对 SmartCrew 的建议：

- 用模型输出 `PlannedToolCall[]`
- 增加计划校验器
- 对计划执行前做白名单、参数与风险校验
- 保留当前启发式逻辑作为兜底 fallback

优先级：**高**

### 10.3 方向三：引入 Durable Execution 与 Human-in-the-Loop

当前主流趋势：

- LangGraph 等框架已经把 durable execution、interrupt、resume、human approval 做成标准能力。
- 对高风险操作，越来越强调“工具调用前审批”。

对 SmartCrew 的建议：

- 为 ToolExecution 引入可持久化执行状态。
- 对高风险 Tool 增加审批断点。
- 支持：
  - approve
  - edit arguments
  - reject
- 后台或前台可展示“待审批工具动作”。

这将显著提升系统处理高风险操作时的可治理性。

优先级：**高**

### 10.4 方向四：在 Tool 之上增加独立 Skill 层

当前主流趋势：

- 越来越多系统把“执行动作”和“任务方法”拆开。
- Tool 负责执行，Skill 负责组织：
  - 指令模板
  - 调用约束
  - 推荐 Tool 组合
  - 适用场景
  - 上下文注入规则

对 SmartCrew 的建议：

- 新增 `skill_definition`
- 允许 Skill 绑定多个 Tool
- 支持 Agent 绑定 Skill，而不是直接全暴露 Tool
- 后续支持“页面临时挂 Skill 到当前会话”

这会更接近你们想要的“可运营 Skill 系统”，也更贴近 Codex 类产品的能力形态。

优先级：**高**

### 10.5 方向五：引入 MCP，做标准化外部工具 / 资源接入

当前主流趋势：

- MCP 正在成为外部工具与上下文接入的重要标准协议。
- OpenAI、Anthropic、IDE 工具链都在逐步强化对 MCP 的支持。

对 SmartCrew 的建议：

- 先做 MCP Client：
  - 让 SmartCrew 能消费外部 MCP server 暴露的 tools/resources/prompts
- 再做 MCP Server：
  - 将 SmartCrew 的内部 Tool / Knowledge / Prompt 暴露给外部客户端

推荐顺序：

1. 内部 Tool 体系稳定
2. 增加 MCP Client 适配
3. 再视需要做 MCP Server

优先级：**中高**

### 10.6 方向六：把 RAG 升级到“检索质量治理”阶段

当前主流趋势：

- 主流 RAG 已经不只看“能检索”，而是强调：
  - Hybrid Search
  - Rerank
  - 引用可追溯
  - 命中率评测
  - 数据 freshness

对 SmartCrew 的建议：

- 增加关键词检索与向量检索混合召回
- 增加 rerank 层
- 最终回答中附片段来源
- 建立评测集与离线评估脚本

优先级：**中高**

### 10.7 方向七：建设 Agent Observability / Evals / Replay

当前主流趋势：

- 越来越多 Agent 平台强调：
  - trace
  - event timeline
  - tool spans
  - response evals
  - replay

对 SmartCrew 的建议：

- 给 Agent / Tool / RAG / LLM 全链路打 traceId 与 span
- 后台增加执行回放视图
- 建立 prompt/tool/rag 的回归评测集

这会直接影响系统能否从“能跑”进入“能稳定迭代”。

优先级：**中高**

### 10.8 方向八：建设图式多 Agent 编排

当前主流趋势：

- Google ADK、LangGraph 等都强调 workflow agents / graph orchestration / multi-agent patterns。
- 多 Agent 不再只是“注册多个 Agent”，而是：
  - supervisor
  - sequential
  - parallel
  - critique
  - refinement

对 SmartCrew 的建议：

- 在现有 AgentCoordinator 之上增加 graph / workflow 层
- 支持：
  - 顺序编排
  - 并行 fan-out
  - review / critic
  - 失败恢复

优先级：**中**

### 10.9 方向九：把 Memory 从“偏好/会话”升级到长期记忆

当前主流趋势：

- Google ADK 等体系已经把 Session / State / Memory 分层得很清楚。
- 长期记忆越来越强调跨会话可检索与可治理。

对 SmartCrew 的建议：

- 保留当前 `user_preference` 的轻量偏好层
- 增加长期 MemoryService：
  - 从会话中提炼可保留事实
  - 跨 session 检索
  - 带 TTL 或人工确认的记忆写入策略

优先级：**中**

---

## 11. 建议的阶段性路线图

### Phase 1：把当前基础设施做稳

- 统一新的数据库初始化基线
- 补 Planner 回归测试
- 补 Tool / RAG / 平台链路的监控与错误可视化
- 补高风险 Tool 审计字段

### Phase 2：让 Agent 更像“可治理生产系统”

- LLM Planner 升级
- 原生 tool calling 适配
- 高风险 Tool 审批流
- 执行状态可恢复

### Phase 3：向平台化 Agent 演进

- Skill 层
- MCP Client / Server
- 多 Agent Graph
- 长期 Memory
- 完整评测与回放平台

---

## 12. 官方参考与主流方案基线

以下资料适合作为本项目后续演进时的外部参照：

- OpenAI Responses API  
  [https://platform.openai.com/docs/guides/migrate-to-responses](https://platform.openai.com/docs/guides/migrate-to-responses)

- OpenAI Function Calling  
  [https://platform.openai.com/docs/guides/function-calling](https://platform.openai.com/docs/guides/function-calling)

- OpenAI Tools / Remote MCP  
  [https://platform.openai.com/docs/guides/tools](https://platform.openai.com/docs/guides/tools)

- Anthropic Tool Use Overview  
  [https://docs.anthropic.com/en/docs/agents-and-tools/tool-use/overview](https://docs.anthropic.com/en/docs/agents-and-tools/tool-use/overview)

- Anthropic How to Implement Tool Use  
  [https://docs.anthropic.com/en/docs/agents-and-tools/tool-use/implement-tool-use](https://docs.anthropic.com/en/docs/agents-and-tools/tool-use/implement-tool-use)

- LangGraph Durable Execution  
  [https://docs.langchain.com/oss/python/langgraph/durable-execution](https://docs.langchain.com/oss/python/langgraph/durable-execution)

- LangGraph Human-in-the-Loop / Interrupts  
  [https://docs.langchain.com/oss/python/langgraph/human-in-the-loop](https://docs.langchain.com/oss/python/langgraph/human-in-the-loop)

- Model Context Protocol Introduction  
  [https://modelcontextprotocol.io/docs/start/tutorial](https://modelcontextprotocol.io/docs/start/tutorial)

- Model Context Protocol Specification Overview  
  [https://modelcontextprotocol.io/specification/2025-06-18/basic](https://modelcontextprotocol.io/specification/2025-06-18/basic)

- Google Agent Development Kit Overview  
  [https://google.github.io/adk-docs/get-started/about/](https://google.github.io/adk-docs/get-started/about/)

- Google ADK Workflow Agents  
  [https://google.github.io/adk-docs/agents/workflow-agents/](https://google.github.io/adk-docs/agents/workflow-agents/)

- Google ADK Multi-Agent Systems  
  [https://google.github.io/adk-docs/agents/multi-agents/](https://google.github.io/adk-docs/agents/multi-agents/)

- Google ADK Session / State / Memory  
  [https://google.github.io/adk-docs/sessions/](https://google.github.io/adk-docs/sessions/)

---

## 13. 一句话结论

SmartCrew-Agent 当前已经完成了从“多模块骨架工程”到“可运行智能体平台雏形”的跨越：

- 运行时主链路已经打通
- Tool / RAG / Prompt / Agent 后台配置闭环已经成立
- 平台化演进方向也已经比较明确

下一阶段最值得投入的不是再堆功能点，而是把：

**原生 Tool Calling、Skill 层、Durable Execution、MCP、RAG 质量治理、Observability**

这几件事补齐，推动系统从“能跑”走向“可规模化演进”。
