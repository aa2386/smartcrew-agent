# SmartCrew-Agent 核心技术洞察
本文档记录 SmartCrew-Agent 项目中的关键技术实现。每一个技术点都按统一格式组织，以便于求职简历引用、方案复盘以及后续技术深挖。

---

## 1. 动静结合的混合注册中心 (Hybrid Registry)
**简介**：系统采用“静态 Bean 扫描”与“动态数据库配置”相结合的注册机制，实现了插件式开发与动态配置的高效融合，确保系统具备较高的灵活性与可扩展性。

### 1.1 技术亮点
- **Spring 集合注入 (Collection Injection)**：利用 Spring 的依赖注入特性，在 `AgentDiscoveryServiceImpl` 中自动收集容器中所有实现了 `Agent` 接口的 Bean。这是一种典型的插件式架构设计。
- **事件驱动初始化 (Event-Driven Initialization)**：监听 `ApplicationReadyEvent` 事件，确保在 Spring 容器完全就绪后再执行 Agent 注册逻辑，规避启动期间的依赖顺序问题。
- **Stub 占位模式 (Stub Pattern)**：对于数据库中存在配置但代码层尚未实现的 Agent，系统自动注册 `StubAgent`。这种“先配置、后实现”的方式天然支持配置驱动开发。
- **内存优先调度 (Memory-First Scheduling)**：调度逻辑优先通过 `InMemoryAgentRegistry` 查询 Agent，而不是在请求路径上频繁访问数据库，从而降低高并发场景下的数据库 I/O 压力。
- **动态配置热生效 (Dynamic Synchronization)**：通过 API 新增或修改 Agent 配置后，会同时写入数据库并刷新运行时注册表，使配置能够在不重启服务的前提下即时生效。

### 1.2 简历描述建议
- **主导设计并实现动静结合的 Agent 注册与发现体系**，支持插件式扩展和数据库配置热生效。
- **基于 Spring 集合注入与应用事件机制** 实现 Agent 自动发现与生命周期装配，降低模块之间的耦合度。
- **引入 Stub 占位模型**，支持“先配置、后实现”的配置驱动开发模式，提升研发协作效率。
- **基于 ConcurrentHashMap 构建高性能内存注册中心**，将 Agent 调度查询降为 O(1)，显著缓解数据库压力。

### 1.3 核心组件职责映射
| 组件 | 核心职责 | 相关类 |
| :--- | :--- | :--- |
| **持久层 (Persistence)** | 持久化 Agent 数据库配置 | `AgentDefinitionMapper` |
| **业务服务 (Service)** | 协调 Agent 注册、更新与同步 | `AgentDefinitionServiceImpl` |
| **内存注册中心 (Registry)** | 保存运行时 Agent 实例与定义缓存 | `InMemoryAgentRegistry` |
| **自动发现引擎 (Discovery)** | 启动时完成 Bean Agent 与数据库 Agent 的统一装配 | `AgentDiscoveryServiceImpl` |
| **代理模型 (Model)** | 定义 Agent 契约与 Stub 占位能力 | `Agent`、`StubAgent` |

---

## 2. 会话级细粒度锁机制 (Conversation-Level Fine-Grained Locking)
**简介**：在大模型对话场景中，为确保同一会话下消息处理的顺序性和数据一致性，系统采用基于 `ConcurrentHashMap<String, ReentrantLock>` 的细粒度锁机制。该设计避免了全局锁带来的吞吐瓶颈，实现了高并发下的会话隔离。

### 2.1 技术亮点
- **细粒度锁 (Fine-Grained Locking)**：为每一个会话动态分配独立锁对象，而非使用全局锁。不同会话之间互不阻塞，显著提升并发能力。
- **懒加载锁创建 (Lazy Lock Initialization)**：利用 `ConcurrentHashMap.computeIfAbsent()` 在首次访问某个会话时创建锁，避免预分配大量锁对象带来的资源浪费。
- **线程安全锁容器 (Thread-Safe Lock Container)**：使用 `ConcurrentHashMap` 管理所有会话锁，确保在多线程场景下锁的创建、获取和复用安全可靠。
- **可重入锁 (ReentrantLock)**：选择 `ReentrantLock` 而不是 `synchronized`，为后续扩展超时、可中断、公平锁等高级特性预留空间。
- **会话隔离的并发模型**：同一会话串行处理，不同会话可并行执行，在一致性和吞吐量之间取得平衡。

### 2.2 核心代码实现
```java
private final ConcurrentHashMap<String, ReentrantLock> conversationLocks = new ConcurrentHashMap<>();

@Override
public LlmChatResponse chat(LlmChatRequest request) {
    String conversationKey = buildConversationKey(request.getUserId(), request.getSessionId());
    ReentrantLock lock = conversationLocks.computeIfAbsent(conversationKey, key -> new ReentrantLock());

    lock.lock();
    try {
        // 临界区：会话消息写入、模型调用与结果持久化
        // ...
    } finally {
        lock.unlock();
    }
}
```

### 2.3 为什么需要会话级锁
| 场景 | 无锁并发 | 全局锁 | 会话级细粒度锁 |
| :--- | :--- | :--- | :--- |
| **消息顺序性** | 可能乱序 | 有保障 | 有保障 |
| **吞吐能力** | 高，但有一致性风险 | 低 | 高 |
| **资源利用率** | 高 | 低 | 高 |
| **实现复杂度** | 低 | 低 | 中 |

**典型问题示例**：如果没有会话级锁，同一个会话的两条消息可能由两个线程并行处理，最终造成数据库中消息顺序与真实对话顺序不一致，进而影响大模型上下文。

### 2.4 简历描述建议
- **设计并实现会话级细粒度锁机制**，解决大模型对话场景下的消息顺序性与数据一致性问题。
- **基于 ConcurrentHashMap + ReentrantLock 组合**，实现按会话维度的动态锁管理，兼顾线程安全与并发性能。
- **采用按需创建锁对象的策略**，避免不必要的资源预分配，提高系统整体资源利用率。
- **为后续超时控制、读写分离、公平锁调度等能力预留扩展空间**。

### 2.5 潜在优化方向
- **锁对象回收机制**：可引入定时清理任务，回收长时间未使用的会话锁。
- **锁超时控制**：可考虑使用 `tryLock(timeout, TimeUnit)`，避免异常场景下锁长时间占用。
- **读写分离优化**：若未来读操作远多于写操作，可进一步考虑 `ReentrantReadWriteLock`。

### 2.6 核心组件职责映射
| 组件 | 核心职责 | 相关类 |
| :--- | :--- | :--- |
| **锁容器 (Lock Container)** | 管理所有会话锁对象 | `ConcurrentHashMap<String, ReentrantLock>` |
| **锁工厂 (Lock Factory)** | 按需创建锁 | `computeIfAbsent()` |
| **会话 Key 生成器 (Key Generator)** | 生成唯一会话标识 | `buildConversationKey(userId, sessionId)` |
| **锁管理器 (Lock Manager)** | 获取锁、释放锁与异常兜底 | `DashScopeLlmClient.chat()` |

---

## 3. Agent Prompt 分层组装与关联绑定机制 (Layered Agent Prompt Orchestration)
**简介**：系统将 Agent 提示词从“单点配置”升级为“基础人格层 + 工作流模板层 + 用户偏好层”的分层组装模型，并通过 `agent_prompt_binding` 建立 Agent 与具体 Prompt 模板记录的多对多关系。在保证配置灵活性的同时，也让运行时提示词的优先级、变更边界和生效路径变得清晰可控。

### 3.1 技术亮点
- **Prompt 分层建模 (Layered Prompt Modeling)**：将 `agent_definition.system_prompt` 定位为 Agent 的基础人格层，用于定义角色、人设、语气和安全边界；将 `prompt_template` 定位为工作流模板层，用于定义任务步骤、执行规则和业务流程；将 `user_preference` 作为用户偏好层，用于对语言、称呼、风格等做个性化叠加。
- **具体模板绑定 (Concrete Template Binding)**：没有采用“按分类自动匹配”的模糊策略，而是通过 `agent_prompt_binding` 直接绑定具体 Prompt 模板记录 ID，避免模板版本升级后自动漂移到未知内容，提升生产环境可控性。
- **多模板顺序拼接 (Ordered Composition)**：一个 Agent 可以绑定多个 Prompt 模板，并通过 `sort_order` 维护拼接顺序，适合复杂任务拆分、阶段式流程引导与策略组合。
- **运行时实时组装 (Runtime Assembly Without Prompt Cache)**：Prompt 模板和绑定关系不单独做缓存，而是在运行时动态读取。这样可以减少多层缓存带来的一致性问题，使 Prompt 管理修改后更容易直达运行链路。
- **缓存与配置职责分离 (Cache / Config Separation)**：`agent_definition` 的增删改会同步进入 `AgentRegistry`，确保 Agent 基础人格层可以热更新；而模板层与偏好层保持实时读取，形成“运行时缓存 + 配置实时读取”的清晰分工。
- **脏数据容错 (Graceful Degradation)**：当某个绑定的 Prompt 模板缺失时，系统会跳过该模板并记录 warning，而不是直接中断整个聊天流程。这让 Prompt 管理具备更强的运维韧性。

### 3.2 核心代码实现
```java
@Override
public String buildSystemPrompt(String agentCode, Long userId) {
    StringBuilder builder = new StringBuilder();

    // 第一层：Agent 基础人格层
    agentRegistry.getDefinition(agentCode)
            .map(AgentDefinition::getSystemPrompt)
            .ifPresent(systemPrompt -> appendSection(builder, systemPrompt));

    // 第二层：工作流 Prompt 模板层
    agentPromptBindingService.listResolvedByAgentCode(agentCode).stream()
            .map(AgentPromptBindingVo::getTemplateContent)
            .forEach(templateContent -> appendSection(builder, templateContent));

    // 第三层：用户长期偏好层
    appendPreference(builder, userId, "language", "用户偏好语言：");
    appendPreference(builder, userId, "nickname", "用户偏好称呼：");
    appendPreference(builder, userId, "tone", "用户偏好风格：");

    // 三层都为空时兜底
    return builder.length() == 0 ? DEFAULT_PROMPT : builder.toString();
}
```

### 3.3 为什么这套设计有价值
| 维度 | 传统单点 Prompt 配置 | 当前分层 Prompt 设计 |
| :--- | :--- | :--- |
| **角色定位** | 所有规则混在一个 Prompt 中 | 人格层与流程层职责分离 |
| **模板复用** | 需要反复复制大段 Prompt | 支持一个 Agent 绑定多个可复用模板 |
| **版本稳定性** | 修改后影响范围不透明 | 绑定具体模板记录，版本边界清晰 |
| **个性化能力** | 用户偏好往往写死在 Prompt 中 | 用户偏好作为独立第三层动态叠加 |
| **配置灵活性** | Agent 与 Prompt 强耦合 | Agent 基础配置与模板库松耦合组合 |
| **运营风险** | 一次修改可能牵动整条链路 | 人格层、模板层、偏好层可分开管理 |

**典型场景示例**：同一个 Agent 可以复用相同的“服务态度与安全边界”人格层，但在不同业务场景下绑定不同的工作流模板，例如“商品问答”“售后处理”“投诉升级”等，而不需要为每个场景重新写整份大 Prompt。

### 3.4 简历描述建议
- **设计并实现 Agent Prompt 分层组装机制**，将角色人设、工作流模板与用户偏好从单点 Prompt 中解耦，形成可组合、可运营的提示词体系。
- **引入 Agent-Prompt Binding 关系模型**，支持一个 Agent 按顺序绑定多个 Prompt 模板，并通过具体模板记录 ID 降低配置漂移风险。
- **构建“运行时缓存 + 实时配置组装”的 Prompt 生效模型**，在保证热更新效率的同时，避免多层缓存失效带来的一致性问题。
- **实现 Prompt 链路的容错降级能力**，当模板缺失时跳过异常节点并记录告警，提升生产环境稳定性。

### 3.5 核心组件职责映射
| 组件 | 核心职责 | 相关类 |
| :--- | :--- | :--- |
| **Agent 人格层 (Persona Layer)** | 定义 Agent 的角色、人设、语气与安全边界 | `AgentDefinition` |
| **模板绑定层 (Binding Layer)** | 管理 Agent 与多个 Prompt 模板的绑定关系及顺序 | `AgentPromptBinding`、`AgentPromptBindingServiceImpl` |
| **模板库层 (Template Layer)** | 提供可复用的任务流程与执行 Prompt 内容 | `PromptTemplate` |
| **偏好层 (Preference Layer)** | 根据用户维度叠加语言、称呼、风格等偏好 | `UserPreferenceService` |
| **组装引擎 (Assembler)** | 按固定优先级生成最终 System Prompt | `InitialAgentPromptServiceImpl` |
| **运行时入口 (Runtime Entry)** | 将最终 Prompt 接入智能体执行链路 | `InitialAgent` |

---

## 4. RAG 基础设施与知识库运营链路 (Production-Ready RAG Foundation)
**简介**：项目构建了可运营的 RAG 基础层，覆盖“知识库建模、文档加载、切片、向量化、向量库存储、Agent 关联管理”的完整闭环。设计上强调“上传即自动入库”的易用性与“向量库可替换”的长期可演进性，兼顾工程效率、线上稳定性与后续扩展空间。
### 4.1 技术亮点
- **领域建模清晰且可扩展**：通过 `knowledge_base`、`knowledge_document`、`document_chunk`、`agent_knowledge_binding` 四张核心表拆分“知识库、文档、切片、接入关系”职责，支持多知识库并行管理与按 Agent 精细化接入。
- **端到端入库编排可观测**：文档处理流程采用状态机 `pending -> processing -> completed / failed`，后台可追踪错误信息、切片数量与处理进度，便于运营与排障。
- **文档切片参数化设计**：切片服务支持 `paragraph / sentence` 两种切分策略，关键参数由配置驱动：`smartcrew.rag.document.splitter.type`、`max-chunk-size`、`overlap-size`。当前默认 `paragraph + maxChunkSize=200 + overlapSize=50`，在语义完整性与召回粒度之间做平衡。
- **嵌入模型配置可治理**：嵌入模型采用 `smartcrew.rag.embedding.*` 独立配置，默认 `text-embedding-v3`；`api-key/base-url` 未单独配置时可回退到 LLM 配置，减少重复运维成本。
- **向量库抽象先行**：业务侧仅依赖 `VectorStoreService(namespace, ...)`，由 `namespace` 映射 `knowledge_base.collection_name`。当前落地远程 Chroma，实现上层与底层解耦，后续替换 Milvus/PgVector/ES 时无需改文档处理主链路。
- **集合级存储与缓存策略**：远程 Chroma 按 `collectionName` 动态创建/缓存 `EmbeddingStore`，避免反复构建连接对象，提升吞吐并降低请求延迟抖动。
- **配置冻结规则保障一致性**：知识库一旦存在文档，`collectionName` 不允许修改；一旦存在已完成文档，`embeddingModel` 不允许修改，避免向量维度或集合漂移导致检索污染。
- **异步化提升后台体验**：上传接口先落库再异步处理（`ragDocumentTaskExecutor`），页面通过轮询文档状态展示处理进度，实现“单次上传、自动完成”的管理体验。

### 4.2 核心代码实现
```java
// 文档处理主链路：加载 -> 切片 -> 向量化 -> 写入向量库 -> 切片落库
public void processDocument(Long documentId) {
    updateStatus(documentId, "processing", null);

    KnowledgeDocument document = requireDocument(documentId);
    KnowledgeBase base = requireBase(document.getBaseId());

    Document loaded = documentLoaderService.load(document.getFilePath(), document.getFileType());
    List<TextSegment> segments = documentSplitterService.split(loaded);

    List<Embedding> embeddings = embeddingService.embedAll(segments);
    List<String> vectorIds = vectorStoreService.addAll(base.getCollectionName(), embeddings, segments);

    persistChunks(documentId, segments, vectorIds);
    updateStatus(documentId, "completed", null);
}
```

### 4.3 关键设计取舍
| 设计点 | 取舍方案 | 价值 |
| :--- | :--- | :--- |
| **上传交互** | 选择“上传即自动入库”而非手动三步 | 降低操作复杂度，提升后台运营效率 |
| **向量库适配** | 先抽象 `VectorStoreService`，再实现 Chroma | 减少供应商绑定，便于后续迁移 |
| **切片策略** | 可配置切分类型与重叠窗口 | 支持按场景平衡召回率与上下文完整性 |
| **一致性治理** | 锁定已投产知识库的关键字段 | 降低向量污染和线上行为漂移风险 |
| **执行模式** | 接口快速返回 + 后台异步处理 | 提升用户体验，降低请求超时风险 |

### 4.4 简历描述建议
- **主导搭建企业级 RAG 基础设施**，完成知识库、文档、切片、Agent 绑定的领域建模与多知识库管理能力落地。
- **设计并实现配置驱动的文档切片与向量化链路**，支持按段落/句子切分与 `chunk size + overlap` 参数化调优。
- **实现向量库抽象层并落地远程 Chroma**，通过命名空间隔离与集合级缓存机制提升可扩展性与吞吐表现。
- **构建上传异步编排与状态机追踪机制**，实现文档处理全流程可观测与失败可恢复，显著优化后台运维体验。
- **制定知识库关键字段冻结规则**，避免模型维度漂移与向量集合错配，保障检索链路稳定性。

### 4.5 核心组件职责映射
| 组件 | 核心职责 | 相关类 |
| :--- | :--- | :--- |
| **知识库应用服务层** | 聚合知识库、文档、切片、Agent 绑定管理能力 | `KnowledgeBaseAdminServiceImpl` |
| **文档加载器** | 按文件类型解析文档内容（txt/md/pdf/tika 回退） | `DocumentLoaderServiceImpl` |
| **文档切片器** | 按策略与参数完成语义切片 | `DocumentSplitterServiceImpl` |
| **嵌入服务** | 调用嵌入模型批量向量化 | `DashScopeEmbeddingServiceImpl` |
| **向量存储适配层** | 提供命名空间级向量写入/删除/检索接口 | `VectorStoreService`、`ChromaVectorStoreServiceImpl` |
| **异步任务执行器** | 承载文档入库后台任务 | `ragDocumentTaskExecutor` |
| **后台管理接口层** | 提供知识库、文档、切片、绑定的管理 API | `AdminKnowledgeBaseController` |

---

## 5. Tool 双层配置与 Agent 执行编排体系 (Dual-Layer Tool System with Agent Orchestration)
**简介**：项目在已有代码 Tool 的基础上，进一步补齐了数据库流程定义、运行时解析、后台配置、Agent 绑定与执行编排闭环，使 Tool 从“可被注册的代码能力”升级为“可配置、可治理、可被 Agent 消费的执行能力池”。这套设计的重点不是单个 Tool 的实现，而是把 Tool 做成一套可复用的基础设施，方便后续 Agent、后台配置和业务流程持续扩展。

### 5.1 技术亮点
- **代码层 + 数据库层双来源解析**：运行时以 `ResolvedToolDefinition` 作为统一真相，合并代码 Tool 与数据库配置，并通过 `sourceStatus` 明确区分 `CODE_ONLY / DB_ONLY / LINKED` 三种来源状态，避免后台视图和运行时状态割裂。
- **执行方式显式建模**：在 `tool_definition` 中新增 `executionMode` 与 `flowDefinitionJson`，把 `BEAN / FLOW` 做成一等概念，而不是把主流程混在 `configJson` 中，提升配置清晰度与长期可演进性。
- **动作级元数据自动发现**：通过反射 LangChain4j `@Tool` 和 `@P` 注解，自动生成 `ToolActionMetadata` 与参数说明，使代码方法能够转化为 Agent 可理解、后台可展示的动作级能力目录。
- **数据库 Flow Tool 可独立执行**：新增 `FlowToolExecutor` 解释顺序 DSL，支持 `template / tool_call / return` 三类步骤，使没有代码 Bean 的数据库 Tool 也能被直接执行。
- **统一执行协议**：`ToolExecutor` 升级为 `execute(toolCode, actionName, arguments, executionContext)`，将 Bean Tool 与 Flow Tool 收敛为统一调用协议，便于后台调试、Agent 编排和后续审计复用。
- **Agent 绑定与运行时真正打通**：新增 `AgentToolBindingService` 和后台绑定接口，`initial-agent` 运行时只消费当前 Agent 已绑定且可执行的 Tool 集合，形成“绑定约束 -> 决策规划 -> 工具执行 -> 结果汇总”的闭环。
- **渐进式接入策略稳健**：当前通过 `ReActDecisionEngine` 先实现结构化启发式规划，而不是一次性重构为模型原生函数调用，优先保证系统可控、可测、可逐步演进。
- **兼容性保留完整**：保留 `/api/v1/tools` 以及 `ToolExecutor` 旧签名，确保新基础设施落地后不破坏原有对外链路。

### 5.2 简历描述建议
- **主导设计并落地 Tool 双层配置基础设施**，将代码 Tool 与数据库 Tool 流程定义统一收敛到运行时解析模型中，支持 `BEAN / FLOW` 双执行模式与 `CODE_ONLY / DB_ONLY / LINKED` 多来源治理。
- **构建基于动作级元数据的 Tool Registry / Executor 体系**，通过反射 `@Tool` 注解自动发现 Tool Action，并以统一执行协议同时支持代码 Bean 调用与数据库 DSL 流程执行。
- **设计并实现 Agent-Tool 绑定与执行编排链路**，打通后台绑定配置、运行时可用 Tool 过滤、结构化工具规划、顺序执行与结果注入，完成 `initial-agent` 的 Tool 能力接入闭环。
- **在不破坏既有 API 的前提下完成基础设施升级**，保留 `/api/v1/tools` 兼容路径与旧执行签名，实现存量能力平滑迁移。

### 5.3 核心组件职责映射
| 组件 | 核心职责 | 相关类 |
| :--- | :--- | :--- |
| **Tool 持久化模型层** | 持久化 Tool 基础元数据、执行模式与 Flow DSL 定义 | `ToolDefinition`、`ToolDefinitionRequest` |
| **Tool 运行时解析层** | 合并代码 Tool 与数据库配置，生成统一可执行视图 | `ResolvedToolDefinition`、`InMemoryToolRegistry` |
| **动作元数据层** | 描述 Tool 下的动作与参数，用于后台展示和决策规划 | `ToolActionMetadata`、`ToolActionParameter` |
| **统一执行入口** | 按 Tool + Action 路由到对应执行器并返回结构化结果 | `ToolExecutor`、`DefaultToolExecutor`、`ToolExecutionResult` |
| **代码 Tool 执行层** | 反射调用代码 Bean 中的 Tool Action | `BeanToolExecutor` |
| **数据库 Flow 执行层** | 解释顺序 DSL，完成变量渲染、嵌套 Tool 调用与结果返回 | `FlowToolExecutor`、`ToolFlowDefinition`、`ToolFlowStep` |
| **后台管理接口层** | 提供 Tool 列表、详情、配置保存和手动执行能力 | `AdminToolController` |
| **Agent Tool 绑定层** | 管理 Agent 与 Tool 的绑定关系，并筛出可执行 Tool 集合 | `AgentToolBindingServiceImpl`、`AgentToolBindingVo` |
| **决策规划层** | 基于输入和可用 Tool 元数据生成结构化调用计划 | `ReActDecisionEngine`、`PlannedToolCall` |
| **执行编排层** | 顺序执行 Tool 计划并汇总结果回注 Agent 对话链路 | `AgentToolOrchestrator`、`InitialAgent` |

### 5.4 设计动机与面试回答要点（每个工作都有动机）
- **为什么做代码+数据库双层配置，而不是只做代码 Tool？**  
  动机是平衡“工程可控性”和“业务配置效率”。代码层负责高风险、强校验、可测试的原子能力；数据库层负责元数据治理与流程编排，避免每次小改都发版。
- **为什么把 `executionMode(BEAN/FLOW)` 显式建模？**  
  动机是避免隐式推断导致线上行为不可控。执行主体必须一眼可见，方便排障、审计和跨团队协作，降低“配置看不出来实际跑哪套逻辑”的风险。
- **为什么保留 `CODE_ONLY / DB_ONLY / LINKED` 运行时状态？**  
  动机是提升系统可解释性。我们不仅要知道 Tool 能不能执行，还要知道“为什么能/为什么不能”，这样后台运营、测试和研发定位问题更快。
- **为什么采用统一执行入口，而不是 Bean/Flow 各跑各的？**  
  动机是统一治理面。权限控制、风险分级、日志审计、超时策略都可以收敛到同一个执行平面，减少重复实现和策略漂移。
- **为什么 FLOW DSL 只支持顺序步骤（template/tool_call/return）？**  
  动机是先把安全边界和稳定性打牢。先覆盖 80% 的轻编排场景，再逐步扩展复杂控制流，避免一开始就把 DSL 做成高风险脚本引擎。
- **为什么不是直接走模型原生 function calling，而是“规划+执行+总结”？**  
  动机是降低模型供应商耦合，提升可迁移性和可观测性。牺牲一点性能换来跨模型复用、结构化回放和工程侧可控，是平台化场景下更稳妥的长期选择。
- **为什么 Tool 绑定做在 Agent 维度？**  
  动机是建立最小可治理边界。先限制“这个 Agent 能用哪些 Tool”，再让规划器在白名单内选择动作，避免模型直接接触全量工具造成越权调用风险。
- **为什么保留 `/api/v1/tools` 兼容链路？**  
  动机是保障增量演进。基础设施升级不能一次性打断存量调用，保留兼容接口可降低迁移风险，保证业务连续性与上线稳定性。

**面试可用总结话术**：这套设计的核心动机不是“炫技”，而是在生产环境里平衡四件事：**可控性、可配置性、可观测性、可演进性**。我们接受了部分性能与复杂度成本，换取长期平台能力和跨模型稳定运行能力。
