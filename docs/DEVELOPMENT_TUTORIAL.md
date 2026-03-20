# SmartCrew-Agent 开发实战教程

欢迎来到 SmartCrew-Agent 开发实战教程。本教程旨在通过一系列手把手的案例，带你快速掌握 Agent 平台的各项核心能力开发。

---

## 教程一：构建你的第一个智能体 (WeatherAgent)

**场景说明**：我们将创建一个简单的“天气查询助手”，通过它你将掌握 Agent 的基本结构、自动发现机制以及消息处理流程。

### 1.1 开发准备
- **目标包路径**：`smartcrew-modules/src/main/java/com/smartcrew/agent/core/agent/`
- **核心接口**：`com.smartcrew.agent.api.agent.service.Agent`

### 1.2 核心开发步骤 (傻瓜式指引)

#### 第一步：创建 Java 类并标注组件
在目标路径下新建 `WeatherAgent.java`，使用 `@Component` 注解使其能被 Spring 自动扫描。

```java
@Component
public class WeatherAgent implements Agent {
    // 待实现方法
}
```

#### 第二步：定义身份与能力
实现 `code()`、`name()` 和 `supports()` 方法，为 Agent 提供唯一的“身份证”和能力说明。

```java
@Override
public String code() {
    return "weather-assistant"; // 全局唯一标识
}

@Override
public String name() {
    return "天气查询助手";
}

@Override
public boolean supports(String capability) {
    return "weather".equalsIgnoreCase(capability); // 声明支持天气能力
}
```

#### 第三步：编写业务逻辑
实现 `handle()` 方法。这是 Agent 的核心，负责接收用户指令并返回响应。

```java
@Override
public AgentDispatchResponse handle(AgentDispatchCommand command) {
    String userMessage = command.getMessage();
    String reply = "你询问的是：" + userMessage + "。目前我只能告诉你今天天气不错！☀️";
    
    return AgentDispatchResponse.builder()
            .traceId(command.getTraceId()) // 必填，用于链路追踪
            .agentCode(code())
            .accepted(true)
            .message(reply)
            .build();
}
```

### 1.3 验证与测试
1.  **启动项目**：运行 `smartcrew-admin` 模块。
2.  **调用接口**：使用任意 API 工具（如 Postman）调用：
    - **Method**: `POST`
    - **URL**: `http://localhost:8080/api/v1/agents/weather-assistant/dispatch`
    - **Body (JSON)**: `{"message": "北京天气"}`
3.  **预期结果**：收到 JSON 格式的回复消息。

### 1.4 进阶建议
- **依赖注入**：你可以通过构造函数注入其他 Service 来增强 `handle` 的能力。
- **配置同步**：虽然自动发现能让 Agent 运行，但建议通过 `/api/v1/agents/register` 接口在数据库中完善其元数据（如描述、图标等）。

---

## 教程二：自定义工具 (Tools) 开发

**场景说明**：Agent 的强大之处在于能够使用工具。本教程将教你如何创建一个“数学计算工具”，并让 Agent 能够通过注解自动识别和调用它。

### 2.1 开发准备
- **目标包路径**：`smartcrew-modules/src/main/java/com/smartcrew/agent/core/tool/`
- **核心接口**：`com.smartcrew.agent.api.tool.service.SmartCrewTool`
- **关键注解**：`dev.langchain4j.agent.tool.Tool` (用于声明工具方法)

### 2.2 核心开发步骤

#### 第一步：创建工具类
新建 `MathTools.java`，实现 `SmartCrewTool` 接口并标记 `@Component`。

```java
@Component
public class MathTools implements SmartCrewTool {
    @Override
    public String toolCode() { return "math-utils"; }

    @Override
    public String toolName() { return "数学工具箱"; }

    @Override
    public String description() { return "提供基础的数学计算能力"; }
}
```

#### 第二步：编写工具方法 (使用注解)
在工具类中编写具体的方法，并使用 `@Tool` 注解描述该方法的功能，`@P` 注解描述参数。这些描述将提供给 LLM 进行函数调用决策。

```java
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

@Tool("计算两个数字的和")
public double add(@P("第一个数字") double a, @P("第二个数字") double b) {
    return a + b;
}

@Tool("计算一个数字的平方根")
public double sqrt(@P("待计算的数字") double n) {
    return Math.sqrt(n);
}
```

### 2.3 原理解析
- **自动注册**：项目启动时，`InMemoryToolRegistry` 会扫描所有 `@Component` 且实现了 `SmartCrewTool` 的类。
- **能力暴露**：通过 `@Tool` 注解，该方法会被提取为 LLM 能够理解的 JSON Schema，从而在 ReAct 决策链路中被选中。

---

## 教程三：RAG 知识库挂载 (扩展教程)

**场景说明**：当通用 LLM 无法回答公司内部知识时，我们需要接入 RAG。由于本项目目前仅提供了基础框架，本教程将引导你如何基于 `langchain4j` 扩展实现 RAG 管线。

### 3.1 扩展思路
RAG 的核心是将“私有文档”转换为“向量数据”并存储，查询时先检索再生成。

### 3.2 核心步骤指引

#### 第一步：引入向量存储 (Vector Store)
在 `smartcrew-modules` 中创建一个配置类，初始化向量数据库（如 H2 内置向量表或 Milvus）。

```java
// 建议位置：com.smartcrew.agent.core.config.RagConfig
@Bean
public EmbeddingStore<TextSegment> embeddingStore() {
    // 简单起见，可以使用内存存储或本地文件存储
    return new InMemoryEmbeddingStore<>();
}
```

#### 第二步：实现文档导入工具
利用项目中已有的 `DocumentTools` 读取文件，并使用 `EmbeddingModel` 进行向量化入库。

```java
// 伪代码示例
public void ingestDocument(String filePath) {
    Document doc = FileSystemDocumentLoader.loadDocument(filePath);
    DocumentSplitter splitter = DocumentSplitters.recursive(500, 50);
    List<TextSegment> segments = splitter.split(doc);
    embeddingStore.addAll(embeddingModel.embedAll(segments), segments);
}
```

#### 第三步：创建检索工具 (Retriever Tool)
创建一个新的 Tool，供 Agent 在 handle 逻辑中调用以获取背景知识。

```java
@Tool("搜索公司内部规章制度知识库")
public String searchInternalPolicy(@P("搜索关键词") String query) {
    List<EmbeddingMatch<TextSegment>> relevant = embeddingStore.findRelevant(embeddingModel.embed(query), 3);
    return relevant.stream().map(m -> m.embedded().text()).collect(Collectors.joining("\n"));
}
```

---

## 教程四：多 Agent 协作流编排 (扩展教程)

**场景说明**：复杂任务（如：写一份带数据的行业报告）需要多个 Agent 协作。本教程介绍如何在现有框架下实现“总控-子代理”协作模式。

### 4.1 协作模式：Planner-Executor
- **Planner (规划者)**：负责拆解任务。
- **Executor (执行者)**：负责具体执行。

### 4.2 核心实现指引

#### 第一步：在 Planner 中注入 AgentRegistry
Planner 需要知道系统中有哪些可用的 Agent。

```java
@Component
public class MasterPlannerAgent implements Agent {
    private final AgentRegistry agentRegistry;
    private final AgentCoordinator coordinator;

    // 构造函数注入...
}
```

#### 第二步：编排 handle 逻辑
在 Planner 的 `handle` 方法中，通过 LLM 将复杂任务拆分为子任务，并循环调用 `coordinator.dispatch`。

```java
@Override
public AgentDispatchResponse handle(AgentDispatchCommand command) {
    // 1. LLM 拆解任务：["weather-assistant", "math-utils"]
    // 2. 依次调用子 Agent
    AgentDispatchResponse res1 = coordinator.dispatch("weather-assistant", ...);
    AgentDispatchResponse res2 = coordinator.dispatch("data-analyst", ...);
    
    // 3. 汇总结果并返回
    return AgentDispatchResponse.builder().message("汇总报告：...").build();
}
```

### 4.3 生产级建议
- **状态机**：对于长流程，建议在数据库中维护一个 `TaskSession` 表，记录每一步的状态。
- **消息总线**：利用项目中已有的 `MessageBus` 实现异步协作，避免 HTTP 阻塞。
