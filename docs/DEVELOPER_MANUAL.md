# SmartCrew-Agent 开发手册

> 版本：v1.0  
> 适用仓库：`smartcrew-agent`  
> 目标读者：后端开发、平台开发、算法工程、技术负责人

---

## 1. 手册说明与阅读建议

本手册采用“项目落地优先”写法：

- 先讲当前仓库**已经具备**的能力和可直接复用的代码入口；
- 再讲 LLM、RAG、多 Agent、企微/飞书接入的**可执行扩展方案**；
- 明确区分“已实现”与“建议实现”，避免误判现状。

建议阅读路径：

1. 初次接手项目：先看第 2、3、5、8、10 章。
2. 要接入大模型：重点看第 4 章。
3. 要做知识库问答：重点看第 6 章。
4. 要做多 Agent 编排：重点看第 7 章。
5. 要做线上稳定性治理：重点看第 11、12、13 章。

### 1.1 当前能力边界（必须先确认）

- 已实现：
  - 代理注册与派发链路
  - 工具注册、启停、元数据管理
  - 用户偏好/会话记忆基础能力
  - ReAct 决策占位引擎
  - 平台网关与企微/飞书占位适配器
- 未完整实现（需扩展）：
  - 真正可用的 LLM 推理服务接入链路
  - 完整 RAG 管线（切分/向量化/检索/重排）
  - 生产级多 Agent 编排与调度
  - 平台回调验签、幂等、重试、回发闭环

---

## 2. 项目架构与模块映射

### 2.1 模块职责

- `smartcrew-admin`
  - Spring Boot 启动入口
  - REST 控制器（agent/tool/prompt/memory/decision/platform）
- `smartcrew-modules-api`
  - 领域实体、请求响应模型、Mapper、Service 接口契约
- `smartcrew-modules`
  - 核心实现：agent/tool/memory/decision/platform/prompt/mcp
- `smartcrew-common`
  - 公共配置、统一返回结构、异常处理、分页与工具类

### 2.2 核心调用链

1. `Controller` 接收请求并做参数校验。
2. `Service`/`Registry` 负责业务编排和对象路由。
3. `Mapper` 读写 MySQL。
4. 返回统一响应结构 `R<T>` 或 `TableDataInfo<T>`。

### 2.3 关键入口（当前代码）

- 代理入口：`/api/v1/agents`
- 工具入口：`/api/v1/tools`
- 记忆入口：`/api/v1/memory/preferences`
- 决策入口：`/api/v1/decision/plan`
- 平台入口：`/api/v1/platform/{platform}/events`

---

## 3. 核心模块详解：smartcrew-modules

本章节详细介绍 `smartcrew-modules` 模块中 `com.smartcrew.agent.core` 包下各个子包的作用、核心类、功能定位和使用场景。这是项目的核心实现层，包含了 Agent、工具、记忆、决策、LLM、平台适配等关键能力。

### 3.1 模块整体架构

`smartcrew-modules` 是项目的核心实现模块，采用分层架构设计：

```
com.smartcrew.agent.core/
├── agent/          # 代理核心实现
├── config/         # 配置管理
├── decision/       # 决策引擎
├── llm/            # 大模型客户端
├── mcp/            # MCP 服务管理
├── memory/         # 记忆体系
├── platform/       # 平台适配器
├── prompt/         # 提示词模板
└── tool/           # 工具体系
```

### 3.2 agent 包 - 代理核心实现

#### 3.2.1 包的作用与意义

`agent` 包是整个项目的核心，负责代理的生命周期管理、注册发现、消息派发和协调调度。它实现了多代理系统的核心架构，为构建智能对话系统提供了基础设施。

#### 3.2.2 核心类详解

**1. AgentCoordinatorImpl - 代理协调器**

- **作用**：作为代理系统的中央调度器，负责接收外部请求、组装派发命令、发布消息并调用目标代理。
- **核心功能**：
  - 接收来自 API 网关的请求
  - 生成全局唯一的 traceId 用于链路追踪
  - 组装 `AgentDispatchCommand` 和 `MessageEnvelope`
  - 通过消息总线发布消息
  - 调用目标代理的 `handle` 方法
- **使用场景**：
  - 用户发起对话请求时
  - 平台事件需要派发给特定代理时
  - 多代理协作时的任务分发

**2. InMemoryAgentRegistry - 代理注册表**

- **作用**：基于内存的代理注册中心，维护运行期所有代理实例和定义的映射关系。
- **核心功能**：
  - 代理注册与更新
  - 按编码查询代理实例和定义
  - 列出所有已注册代理
  - 判断代理是否存在
- **使用场景**：
  - 应用启动时自动发现并注册代理
  - 派发请求时查询目标代理
  - 管理代理的生命周期

**3. AgentDiscoveryServiceImpl - 代理发现服务**

- **作用**：在应用启动时自动发现 Spring 容器中的所有 Agent Bean，并完成注册。
- **核心功能**：
  - 监听 `ApplicationReadyEvent` 事件
  - 扫描所有实现了 `Agent` 接口的 Bean
  - 自动注册代理到注册表
  - 为数据库中存在但无 Bean 的代理定义创建 StubAgent
- **使用场景**：
  - 应用启动时的自动发现机制
  - 支持动态扩展代理

**4. AgentDefinitionServiceImpl - 代理定义服务**

- **作用**：管理代理定义的持久化，负责代理定义的增删改查。
- **核心功能**：
  - 创建新的代理定义
  - 查询代理定义列表
  - 更新代理定义
  - 删除代理定义
- **使用场景**：
  - 通过 API 动态注册新代理
  - 管理代理的元数据配置

**5. EchoAgent - 回显代理**

- **作用**：示例代理实现，直接返回用户输入内容，用于测试和演示。
- **核心功能**：
  - 支持 `echo` 和 `chat` 能力
  - 简单的请求-响应模式
- **使用场景**：
  - 系统测试和验证
  - 作为新代理开发的模板参考

**6. PlannerAgent - 规划代理**

- **作用**：负责复杂任务的规划和分解，将用户意图拆解为可执行的步骤。
- **核心功能**：
  - 分析用户输入
  - 生成执行计划
  - 协调多个工具和代理
- **使用场景**：
  - 复杂任务处理
  - 多步骤工作流编排

**7. StubAgent - 占位代理**

- **作用**：为数据库中存在但无实际 Bean 的代理定义提供占位实现。
- **核心功能**：
  - 返回"代理未实现"的提示信息
  - 防止系统因找不到代理而崩溃
- **使用场景**：
  - 代理定义已存在但代码未部署时
  - 代理开发过程中的临时状态

**8. InMemoryAgentMessageBus - 代理消息总线**

- **作用**：基于内存的消息总线，用于代理间的消息传递和通信。
- **核心功能**：
  - 发布消息到指定代理
  - 订阅和监听消息
  - 支持异步消息处理
- **使用场景**：
  - 代理间协作通信
  - 事件驱动的消息传递

#### 3.2.3 核心调用链

```
API 请求
  ↓
AgentController.dispatch()
  ↓
AgentCoordinatorImpl.dispatch()
  ↓
AgentRegistry.get(agentCode)
  ↓
AgentMessageBus.publish()
  ↓
Agent.handle()
  ↓
返回响应
```

---

### 3.3 config 包 - 配置管理

#### 3.3.1 包的作用与意义

`config` 包负责管理项目的核心配置，特别是 LLM 客户端的初始化和配置管理。它确保配置的正确加载和客户端的正确初始化。

#### 3.3.2 核心类详解

**LlmConfig - LLM 配置类**

- **作用**：负责初始化大模型客户端，根据配置选择合适的供应商并完成客户端初始化。
- **核心功能**：
  - 读取 LLM 配置（供应商、API Key、模型名称等）
  - 根据供应商类型初始化对应的客户端
  - 支持条件化加载（`@ConditionalOnProperty`）
  - 提供配置验证和日志记录
- **使用场景**：
  - 应用启动时自动初始化 LLM 客户端
  - 支持多供应商切换
  - 配置热更新（需扩展）

---

### 3.4 decision 包 - 决策引擎

#### 3.4.1 包的作用与意义

`decision` 包实现了智能决策引擎，负责分析用户意图、规划执行步骤、选择合适的工具和代理。它是实现智能对话和工作流编排的核心组件。

#### 3.4.2 核心类详解

**ReActDecisionEngine - ReAct 决策引擎**

- **作用**：基于 ReAct（Reasoning and Acting）模式的决策引擎，通过"观察-思考-行动"的循环来处理复杂任务。
- **核心功能**：
  - 分析用户输入和上下文
  - 生成多步骤执行计划
  - 选择候选工具和代理
  - 返回结构化的决策结果
- **决策流程**：
  1. **Observe（观察）**：检查输入和上下文
  2. **Think（思考）**：分解意图为子问题
  3. **Act（行动）**：选择工具和代理执行
  4. **Summarize（总结）**：返回结构化结果
- **使用场景**：
  - 复杂任务的自动规划
  - 多工具协作的场景
  - 需要推理和决策的对话

---

### 3.5 llm 包 - 大模型客户端

#### 3.5.1 包的作用与意义

`llm` 包封装了对大语言模型（LLM）的调用，提供统一的接口和实现，支持多供应商切换。它是项目与 AI 模型交互的核心桥梁。

#### 3.5.2 核心类详解

**DashScopeLlmClient - 千问客户端**

- **作用**：基于 LangChain4j 框架实现的阿里云千问（DashScope）LLM 客户端。
- **核心功能**：
  - 初始化 ChatLanguageModel 实例
  - 发送聊天请求并获取响应
  - 记录调用日志（traceId、耗时）
  - 错误处理和异常捕获
- **使用场景**：
  - 对话生成
  - 文本分析和理解
  - 意图识别
- **配置要求**：
  - `smartcrew.llm.enabled=true`
  - `smartcrew.llm.provider=dashscope`
  - `smartcrew.llm.api-key=<your-api-key>`
  - `smartcrew.llm.model=qwen-plus`

---

### 3.6 mcp 包 - MCP 服务管理

#### 3.6.1 包的作用与意义

`mcp` 包管理 Model Context Protocol (MCP) 服务的配置和生命周期，支持外部工具和服务的集成。

#### 3.6.2 核心类详解

**McpInfoServiceImpl - MCP 服务管理**

- **作用**：负责 MCP 服务配置的新增、更新、查询和删除。
- **核心功能**：
  - 保存或更新 MCP 服务配置
  - 查询所有 MCP 服务
  - 按服务名称查询
  - 管理 MCP 服务的状态
- **使用场景**：
  - 注册外部工具服务
  - 管理工具服务的生命周期
  - 支持动态工具扩展

---

### 3.7 memory 包 - 记忆体系

#### 3.7.1 包的作用与意义

`memory` 包实现了智能体的记忆系统，包括用户偏好管理和会话记忆。它是实现个性化对话和上下文保持的关键组件。

#### 3.7.2 核心类详解

**1. UserPreferenceServiceImpl - 用户偏好服务**

- **作用**：管理用户的个性化偏好设置，支持偏好的增删改查。
- **核心功能**：
  - 查询用户的所有偏好
  - 按键查询特定偏好
  - 新增或更新偏好
  - 删除偏好
- **使用场景**：
  - 存储用户的语言偏好、风格偏好
  - 记录用户的常用设置
  - 实现个性化对话体验

**2. ConversationMemoryServiceImpl - 会话记忆服务**

- **作用**：管理会话级别的记忆，维护对话上下文。
- **核心功能**：
  - 加载会话记忆
  - 更新会话记忆
  - 清除会话记忆
- **使用场景**：
  - 多轮对话的上下文保持
  - 会话状态管理
  - 对话历史记录

---

### 3.8 platform 包 - 平台适配器

#### 3.8.1 包的作用与意义

`platform` 包实现了多平台适配器，支持企业微信、飞书等外部平台的接入。它提供了统一的平台事件处理接口，屏蔽不同平台的差异。

#### 3.8.2 核心类详解

**1. WecomPlatformAdapter - 企业微信适配器**

- **作用**：处理企业微信平台的事件回调，实现企微消息的接收和响应。
- **核心功能**：
  - 验证回调签名
  - 解析企微事件
  - 标准化事件模型
  - 返回处理结果
- **使用场景**：
  - 企业微信机器人
  - 企微群聊助手
  - 企微应用集成

**2. FeishuPlatformAdapter - 飞书适配器**

- **作用**：处理飞书平台的事件回调，实现飞书消息的接收和响应。
- **核心功能**：
  - 处理飞书 challenge 验证
  - 校验事件签名
  - 解析飞书事件
  - 标准化事件模型
- **使用场景**：
  - 飞书机器人
  - 飞书群聊助手
  - 飞书应用集成

**3. InMemoryPlatformAdapterRegistry - 平台适配器注册表**

- **作用**：管理所有平台适配器的注册和查询。
- **核心功能**：
  - 注册平台适配器
  - 按平台编码查询适配器
  - 列出所有适配器
- **使用场景**：
  - 多平台统一接入
  - 平台适配器的生命周期管理

---

### 3.9 prompt 包 - 提示词模板

#### 3.9.1 包的作用与意义

`prompt` 包管理提示词模板的创建、存储和查询，支持模板化提示词的管理和复用。

#### 3.9.2 核心类详解

**PromptTemplateServiceImpl - 提示词模板服务**

- **作用**：管理提示词模板的增删改查，支持按分类查询模板。
- **核心功能**：
  - 创建提示词模板
  - 查询所有模板
  - 按分类查询最新模板
  - 模板版本管理
- **使用场景**：
  - 系统提示词管理
  - 角色扮演提示词
  - 任务特定提示词

---

### 3.10 tool 包 - 工具体系

#### 3.10.1 包的作用与意义

`tool` 包实现了完整的工具体系，包括工具注册、执行、元数据管理和多种内置工具。它是智能体与外部世界交互的核心能力。

#### 3.10.2 核心类详解

**1. InMemoryToolRegistry - 工具注册表**

- **作用**：管理所有工具的注册、发现和状态控制。
- **核心功能**：
  - 启动时扫描工具 Bean
  - 与数据库合并启停状态
  - 查询工具元数据
  - 设置工具启用/禁用状态
- **使用场景**：
  - 工具生命周期管理
  - 工具状态控制
  - 工具发现和查询

**2. DefaultToolExecutor - 默认工具执行器**

- **作用**：执行工具调用，处理参数校验和结果返回。
- **核心功能**：
  - 校验工具状态
  - 定位工具 Bean
  - 执行工具方法
  - 返回执行结果
- **使用场景**：
  - 代理调用工具
  - 工具执行流程控制

**3. ToolDefinitionServiceImpl - 工具定义服务**

- **作用**：管理工具定义的持久化，支持工具定义的增删改查。
- **核心功能**：
  - 创建工具定义
  - 查询工具定义列表
  - 更新工具定义
  - 删除工具定义
- **使用场景**：
  - 动态注册新工具
  - 管理工具元数据

#### 3.10.3 内置工具详解

**1. BasicTools - 基础工具**

- **作用**：提供基础的工具能力，包括随机标识生成和时间查询。
- **工具方法**：
  - `generateId(prefix)`：生成随机标识符
  - `currentTime()`：获取当前服务器时间
- **使用场景**：
  - 生成唯一标识
  - 获取系统时间

**2. FileTools - 文件工具**

- **作用**：在工具工作目录中读写文件。
- **工具方法**：
  - `readFile(fileName)`：读取文件内容
  - `writeFile(fileName, content)`：写入文件内容
- **使用场景**：
  - 文档处理
  - 数据持久化
  - 文件操作

**3. WebSearchTools - 网页搜索工具**

- **作用**：使用 Tavily 进行网络搜索。
- **工具方法**：
  - `search(query)`：执行网络搜索
- **使用场景**：
  - 实时信息查询
  - 知识检索

**4. WebPageTools - 网页内容工具**

- **作用**：提取网页的文本内容。
- **工具方法**：
  - `fetchContent(url)`：获取网页文本内容
- **使用场景**：
  - 网页内容分析
  - 信息提取

**5. DocumentTools - 文档工具**

- **作用**：处理各种文档格式（PDF、Word 等）。
- **工具方法**：
  - `extractText(filePath)`：提取文档文本
- **使用场景**：
  - 文档分析
  - 内容提取

**6. ImageSearchTools - 图片搜索工具**

- **作用**：使用 Pexels 进行图片搜索。
- **工具方法**：
  - `search(query)`：搜索图片
- **使用场景**：
  - 图片检索
  - 视觉内容查找

**7. PlantUmlTools - PlantUML 工具**

- **作用**：生成 PlantUML 图表。
- **工具方法**：
  - `generateDiagram(umlCode)`：生成图表
- **使用场景**：
  - 架构图生成
  - 流程图绘制

**8. TerminalTools - 终端工具**

- **作用**：执行终端命令（高风险，默认禁用）。
- **工具方法**：
  - `execute(command)`：执行命令
- **风险等级**：HIGH
- **使用场景**：
  - 系统管理
  - 自动化脚本执行

---

### 3.11 模块间协作关系

```
┌─────────────────────────────────────────────────────────────┐
│                        API Layer                             │
│  (AgentController, ToolController, PlatformController)      │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                      Core Layer                              │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐   │
│  │  Agent   │  │ Decision │  │   LLM    │  │ Platform │   │
│  │  System  │←→│  Engine  │←→│  Client  │  │ Adapter  │   │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘   │
│       ↓              ↓                           ↓          │
│  ┌──────────┐  ┌──────────┐              ┌──────────┐     │
│  │   Tool   │  │  Memory  │              │  Prompt  │     │
│  │  System  │  │  System  │              │ Template │     │
│  └──────────┘  └──────────┘              └──────────┘     │
│       ↓                                                     │
│  ┌──────────┐                                              │
│  │   MCP    │                                              │
│  │ Services │                                              │
│  └──────────┘                                              │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                     Data Layer                               │
│        (MySQL, Redis, Vector DB - 可扩展)                    │
└─────────────────────────────────────────────────────────────┘
```

---

## 4. 快速启动与关键配置

### 4.1 运行前提

- Java 17
- Maven 3.9+
- MySQL 8.x

> 注意：本机如果是 Java 8，会在 `maven-compiler-plugin` 的 `--release 17` 报错。

### 4.2 初始化步骤

1. 建库：`smartcrew_agent`
2. 执行脚本：`sql/init-smartcrew-agent.sql`
3. 修改：`smartcrew-admin/src/main/resources/application-dev.yml`
4. 编译：`mvn clean package`
5. 启动：`smartcrew-admin` 模块

### 4.3 配置项重点

主配置文件：`smartcrew-admin/src/main/resources/application.yml`

- `smartcrew.llm.enabled`
- `smartcrew.llm.provider`
- `smartcrew.llm.model`
- `smartcrew.tools.enabled.*`
- `smartcrew.tooling.file.save-dir`

开发配置文件：`smartcrew-admin/src/main/resources/application-dev.yml`

- MySQL 数据源
- Tavily（网页搜索）
- Pexels（图片搜索）

---

## 5. 接入大模型（LLM）

### 5.1 当前现状

- 项目已有 LLM 配置模型：`SmartCrewProperties.Llm`
- 默认配置为：
  - `enabled=false`
  - `provider=dashscope`
  - `model=qwen-plus`
- 当前代码中尚未形成"统一 LLM 调用服务层"。

### 5.2 目标能力

形成统一的 LLM Gateway，支持：

- 模型路由（按任务选择模型）
- 统一超时、重试、熔断
- 统一日志与成本记录
- 可插拔供应商（千问 / 本地模型 / 其他）

### 5.3 最小可用接入步骤

1. 在 `modules-api` 新增 LLM 服务接口（如 `LlmClient`）。
2. 在 `modules` 新增实现（如 `DashScopeLlmClient`）。
3. 在 `SmartCrewProperties` 中补足 `apiKey/model` 的读取逻辑。
4. 在 `DecisionEngine` 或 `Agent` 的处理链路引入该客户端。
5. 为调用结果增加 traceId、耗时、token 统计日志。

### 5.4 回退策略（建议）

- 主模型失败时降级到小模型或规则引擎输出兜底文案。
- LLM 超时后立即返回“受理中”并改异步处理。
- 高风险请求（命令执行类）触发二次确认机制。

---

## 6. 构建 Agent

### 6.1 Agent 契约

接口：`com.smartcrew.agent.api.agent.service.Agent`

- `code()`
- `name()`
- `supports(String capability)`
- `handle(AgentDispatchCommand command)`

### 6.2 当前注册与发现机制

- `AgentDiscoveryServiceImpl` 在 `ApplicationReadyEvent` 自动发现并注册 Agent Bean。
- 内置代理：`EchoAgent`、`PlannerAgent`。
- 数据库中存在但无 Bean 的代理定义会注册为 `StubAgent`。

### 6.3 派发链路

`AgentController.dispatch` -> `AgentCoordinatorImpl.dispatch` -> `AgentRegistry.get` -> `agent.handle`

派发时会组装：

- `traceId`
- `AgentDispatchCommand`
- `MessageEnvelope`（投递到消息总线）

### 6.4 新增一个 Agent 的模板流程

1. 新建类实现 `Agent` 接口并标记 `@Component`。
2. 实现 `code/name/supports/handle`。
3. 若需要数据库持久化配置，调用 `AgentDefinitionService.register` 写入定义。
4. 通过 `/api/v1/agents/{code}/dispatch` 验证行为。

### 6.5 生产建议

- `supports` 规则保持互斥或有明确优先级。
- `handle` 输出统一结构，避免自由文本难解析。
- 对外部依赖（LLM/工具）调用增加超时与异常隔离。

---

## 7. 构建 RAG（建议扩展）

### 7.1 当前现状

- 项目有 `DocumentTools`（拉取文档文本）和 `WebPageTools`（网页转文本）；
- 尚未实现向量数据库、嵌入模型、检索与重排链路。

### 7.2 推荐目标架构

`数据采集` -> `文本清洗/切分` -> `Embedding` -> `向量索引` -> `召回` -> `重排` -> `生成`

### 7.3 最小可用 RAG 实施步骤

1. 新增知识文档入库接口（来源：文件、URL、平台消息）。
2. 设计切分策略（固定窗口 + overlap）。
3. 接入 embedding 服务并生成向量。
4. 选择向量存储（可先用轻量方案，再升级）。
5. 查询时执行召回 + 可选重排。
6. 将召回片段拼接到提示词后送入 LLM。
7. 响应中返回引用片段来源（source、chunkId）。

### 7.4 质量建议

- 先做离线评测集，再做线上灰度。
- 召回与生成分开观测指标。
- 对低置信结果启用“拒答 + 请求补充信息”。

---

## 8. 构建多 Agent 协作（建议扩展）

### 8.1 当前现状

- 已有多代理基础模型（Agent 接口、Registry、Coordinator、MessageBus）；
- 还缺任务分解、路由策略、状态机、冲突仲裁。

### 8.2 协作模式建议

- Router 模式：入口 Agent 根据能力路由到单 Agent。
- Planner-Executor 模式：Planner 拆解任务，Executor 执行。
- Supervisor 模式：总控 Agent 管理子 Agent 生命周期。

### 8.3 最小可用编排方案

1. 新增“任务计划模型”（阶段、负责人、输入输出契约）。
2. 在 `DecisionEngine` 输出可执行步骤而非占位文本。
3. 引入“轮次上限”和“停止条件”防止无限循环。
4. 统一会话上下文存储结构（sessionId + stepId）。
5. 对失败步骤启用重试与降级（跳过/替代 Agent）。

### 8.4 防失控策略

- 每轮最大步数限制。
- 每个 Agent 的职责边界白名单。
- 超时后强制中断并返回部分结果。
- 关键任务引入仲裁 Agent 复核。

---

## 9. 工具体系与安全治理

### 9.1 当前工具体系

核心契约：`SmartCrewTool`

- `toolCode`
- `toolName`
- `description`
- `riskLevel`
- `enabledByDefault`

注册中心：`InMemoryToolRegistry`

- 启动时扫描工具 Bean
- 与数据库 `tool_definition` 合并启停状态

执行器：`DefaultToolExecutor`

- 目前仅校验工具状态并定位 Bean
- 尚未完成“按方法动态执行”的生产级能力

### 9.2 工具风险分级建议

- LOW：纯计算、格式转换
- MEDIUM：外部 HTTP 读取
- HIGH：命令执行、文件写入

### 9.3 安全控制建议

- 高风险工具默认关闭（当前 `terminal` 已默认关闭）。
- 工具调用参数做白名单校验。
- 为工具调用加入审计日志（调用人、traceId、参数摘要、耗时、结果状态）。
- 对终端工具增加命令黑名单与目录沙箱。

---

## 10. 记忆体系（当前 + 扩展）

### 10.1 当前能力

- `UserPreferenceServiceImpl`：用户偏好增删改查
- `ConversationMemoryServiceImpl`：基于偏好服务加载/更新会话记忆

### 10.2 建议扩展为三层记忆

- 短期记忆：当前会话上下文
- 中期记忆：用户偏好和近期主题摘要
- 长期记忆：知识图谱/向量知识库

### 10.3 建议原则

- 记忆写入要有来源标记（手动/会话/系统）
- 建立 TTL 与过期清理机制
- 敏感信息不入长期记忆

---

## 11. 企业微信 / 飞书接入（重点）

### 11.1 当前代码映射

- 平台统一入口：`POST /api/v1/platform/{platform}/events`
- 平台编码：
  - 企业微信：`wecom`
  - 飞书：`feishu`
- 当前 `WecomPlatformAdapter` / `FeishuPlatformAdapter` 为 placeholder 实现，仅返回受理消息。

### 11.2 目标接入架构

`平台回调` -> `鉴权/验签/去重` -> `统一事件模型` -> `Agent 派发` -> `平台回发` -> `重试/告警`

### 11.3 企业微信接入方法（实施建议）

1. 回调验证：
  - 实现回调 URL 验证流程；
  - 验证签名、时间戳、随机串合法性。
2. 事件解析：
  - 将企微事件转换为统一事件模型（`platformUserId/eventType/content/metadata`）。
3. 身份映射：
  - 平台用户 ID 映射到内部 userId；
  - sessionKey 建议：`platform + corpId + userId`。
4. 幂等与去重：
  - 基于事件 ID + 时间窗口去重；
  - 消息重复投递时直接返回“已处理”。
5. 回发策略：
  - 同步响应失败时进入异步重试队列；
  - 明确最大重试次数与退避策略。
6. Token 管理：
  - 统一缓存 access token；
  - 到期前预刷新，失败时降级告警。

### 11.4 飞书接入方法（实施建议）

1. URL 验证：
  - 处理飞书 challenge 验证握手。
2. 签名校验：
  - 按飞书事件签名规范校验请求有效性。
3. 事件标准化：
  - 飞书事件体转换为统一事件模型；
  - 区分消息事件、成员事件、机器人事件。
4. 多租户隔离：
  - tenant key / app 配置分租户存储；
  - token 缓存按租户隔离。
5. 消息回发：
  - 统一消息发送接口；
  - 失败进入重试与告警流程。

### 11.5 平台抽象统一建议

- 统一事件字段：
  - `platform`
  - `platformUserId`
  - `eventType`
  - `content`
  - `metadata`
- 统一状态与错误码：
  - 参数错误、验签失败、重复事件、下游失败、限流触发
- 统一降级：
  - 平台 API 失败不阻塞主链路，转异步补偿

### 11.6 运维与安全建议

- 回调源 IP 白名单
- API 限流与突发保护
- 敏感字段脱敏（手机号、邮箱、内部ID）
- 审计日志：接收时间、来源平台、事件ID、处理结果、traceId
- 平台失败告警阈值：连续失败、超时比例、重试积压量

---

## 12. 观测与评测

### 12.1 观测维度

- 请求维度：QPS、P95/P99、错误率
- 业务维度：派发成功率、工具调用成功率、平台回发成功率
- LLM 维度（扩展后）：token、耗时、失败率、成本
- RAG 维度（扩展后）：召回命中率、引用率、拒答率

### 12.2 最小可观测建议

- 全链路强制携带 `traceId`
- 关键节点统一日志结构（JSON）
- 错误按“可重试/不可重试”分类

### 12.3 评测建议

- 离线评测：
  - 构建标准问答集
  - 评估准确率、召回率、引用一致性
- 线上评测：
  - 采样人工质检
  - 用户反馈闭环

---

## 13. 常见技术挑战与粗略解决思路

### 13.1 大模型幻觉

问题：

- 模型编造不存在事实
- 回答看似合理但无法验证

粗略解法：

- 强制检索增强，优先基于证据生成
- 输出附带引用来源
- 结构化输出 + 校验器二次检查
- 低置信度场景拒答或追问

### 13.2 RAG 召回质量不稳定

问题：

- 分块不合理导致语义破碎
- 检索召回不到关键片段

粗略解法：

- 优化切分策略（语义切分 + overlap）
- 混合检索（关键词 + 向量）
- 增加重排（rerank）
- 建立文档更新与索引重建策略

### 13.3 多 Agent 协作失控

问题：

- 任务相互推诿、循环调用、上下文污染

粗略解法：

- 明确角色边界和职责输入输出
- 设置轮次上限与终止条件
- 引入仲裁 Agent
- 失败快速短路并回退到单 Agent 路径

### 13.4 成本与延迟压力

问题：

- LLM 成本高、响应慢

粗略解法：

- 模型分层路由（小模型优先）
- 缓存热点问答与工具结果
- 异步化长任务
- 超时降级与兜底文案

### 13.5 平台接入稳定性风险

问题：

- 回调重放、签名异常、平台 API 波动

粗略解法：

- 严格验签 + 幂等去重
- 失败重试（指数退避）
- 死信队列兜底
- 持续告警与可视化监控

---

## 14. 分阶段实施路线图（P0 / P1 / P2）

### P0（1-2 周）：跑通主链路

- 完成 LLM 最小接入（单供应商）
- 完成单 Agent 可用对话流程
- 完成平台回调验签与标准化事件模型
- 建立 traceId 日志和基本错误告警

### P1（2-4 周）：提升效果与稳定性

- 上线最小 RAG 管线（入库 + 检索 + 引用）
- 完成工具执行器增强（真实方法调用）
- 完成平台回发重试和幂等
- 建立离线评测集与质量看板

### P2（4-8 周）：迈向生产级

- 多 Agent 编排框架（Planner/Executor/Supervisor）
- 模型路由与成本治理
- 长期记忆体系
- 安全合规与审计完善（含高风险工具治理）

---

## 附录 A：当前主要 REST 接口速览

- Agent
  - `POST /api/v1/agents/register`
  - `GET /api/v1/agents`
  - `POST /api/v1/agents/{code}/dispatch`
- Tool
  - `GET /api/v1/tools`
  - `POST /api/v1/tools`
  - `POST /api/v1/tools/{toolCode}/enable`
  - `POST /api/v1/tools/{toolCode}/disable`
- Prompt
  - `GET /api/v1/prompts`
  - `POST /api/v1/prompts`
  - `GET /api/v1/prompts/category/{category}`
- Memory
  - `GET /api/v1/memory/preferences/{userId}`
  - `PUT /api/v1/memory/preferences/{userId}`
- Decision
  - `POST /api/v1/decision/plan`
- Platform
  - `POST /api/v1/platform/{platform}/events`

---

## 附录 B：扩展实现时的边界声明模板

建议在评审文档或 PR 描述里固定写明：

- 已实现能力：
- 仅提供配置入口但未落地能力：
- 本次新增能力：
- 不在本次范围内：
- 风险与回滚策略：

这样可以显著降低“以为已实现”的协作误差。
