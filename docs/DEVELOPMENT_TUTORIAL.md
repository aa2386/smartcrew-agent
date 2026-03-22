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

## 教程二：接入大模型实现智能问答 (LLM Integration)

**场景说明**：本教程将引导你完成从零接入大模型到实现智能问答的最小实现。我们将基于 LangChain4j 框架，构建一个具备对话能力的智能 Agent，为后续的复杂功能奠定基础。

### 2.1 开发准备
- **目标包路径**：
  - API接口：`smartcrew-modules-api/src/main/java/com/smartcrew/agent/api/llm/`
  - 核心实现：`smartcrew-modules/src/main/java/com/smartcrew/agent/core/llm/`
  - Agent实现：`smartcrew-modules/src/main/java/com/smartcrew/agent/core/agent/`
- **核心依赖**：`langchain4j-open-ai` (已在项目依赖管理中)
- **配置文件**：`smartcrew-admin/src/main/resources/application.yml`

### 2.2 核心开发步骤

#### 第一步：创建 LLM 客户端接口
在 `smartcrew-modules-api` 中创建统一的 LLM 调用接口，为后续的多供应商支持预留扩展空间。

```java
// 文件路径：smartcrew-modules-api/src/main/java/com/smartcrew/agent/api/llm/service/LlmClient.java
package com.smartcrew.agent.api.llm.service;

import com.smartcrew.agent.api.llm.domain.request.LlmChatRequest;
import com.smartcrew.agent.api.llm.domain.vo.LlmChatResponse;

/**
 * LLM 客户端接口，定义大模型调用的统一契约。
 */
public interface LlmClient {
    
    /**
     * 发送聊天请求并获取响应。
     *
     * @param request 聊天请求参数
     * @return 聊天响应结果
     */
    LlmChatResponse chat(LlmChatRequest request);
    
    /**
     * 获取客户端标识。
     *
     * @return 客户端唯一标识
     */
    String getClientId();
}
```

#### 第二步：创建请求响应模型
在 `smartcrew-modules-api` 中创建 LLM 相关的领域模型。

```java
// 文件路径：smartcrew-modules-api/src/main/java/com/smartcrew/agent/api/llm/domain/request/LlmChatRequest.java
package com.smartcrew.agent.api.llm.domain.request;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * LLM 聊天请求模型。
 */
@Data
@Builder
public class LlmChatRequest {
    
    /**
     * 用户消息内容。
     */
    private String userMessage;
    
    /**
     * 系统提示词（可选）。
     */
    private String systemPrompt;
    
    /**
     * 历史对话上下文（可选）。
     */
    @Builder.Default
    private List<Map<String, String>> conversationHistory = new ArrayList<>();
    
    /**
     * 温度参数，控制输出随机性（0-2）。
     */
    @Builder.Default
    private Double temperature = 0.7;
    
    /**
     * 最大生成 token 数。
     */
    @Builder.Default
    private Integer maxTokens = 1000;
    
    /**
     * 追踪 ID，用于日志关联。
     */
    private String traceId;
}
```

```java
// 文件路径：smartcrew-modules-api/src/main/java/com/smartcrew/agent/api/llm/domain/vo/LlmChatResponse.java
package com.smartcrew.agent.api.llm.domain.vo;

import lombok.Builder;
import lombok.Data;

/**
 * LLM 聊天响应模型。
 */
@Data
@Builder
public class LlmChatResponse {
    
    /**
     * 生成的回复内容。
     */
    private String content;
    
    /**
     * 使用的 token 数量。
     */
    private Integer totalTokens;
    
    /**
     * 提示词 token 数量。
     */
    private Integer promptTokens;
    
    /**
     * 完成内容 token 数量。
     */
    private Integer completionTokens;
    
    /**
     * 模型名称。
     */
    private String model;
    
    /**
     * 是否成功。
     */
    private Boolean success;
    
    /**
     * 错误信息（如果失败）。
     */
    private String errorMessage;
    
    /**
     * 耗时（毫秒）。
     */
    private Long durationMs;
}
```

#### 第三步：实现 OpenAI 客户端
在 `smartcrew-modules` 中实现基于 LangChain4j 的 OpenAI 客户端。

```java
// 文件路径：smartcrew-modules/src/main/java/com/smartcrew/agent/core/llm/OpenAiLlmClient.java
package com.smartcrew.agent.core.llm;

import com.smartcrew.agent.api.llm.domain.request.LlmChatRequest;
import com.smartcrew.agent.api.llm.domain.vo.LlmChatResponse;
import com.smartcrew.agent.api.llm.service.LlmClient;
import com.smartcrew.agent.common.config.SmartCrewProperties;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * OpenAI LLM 客户端实现，基于 LangChain4j 框架。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAiLlmClient implements LlmClient {
    
    private final SmartCrewProperties properties;
    private ChatLanguageModel chatModel;
    
    @Override
    public LlmChatResponse chat(LlmChatRequest request) {
        long startTime = System.currentTimeMillis();
        String traceId = request.getTraceId() != null ? request.getTraceId() : "unknown";
        
        try {
            log.info("[LLM] Starting chat request, traceId: {}, message: {}", traceId, request.getUserMessage());
            
            String response = chatModel.generate(request.getUserMessage());
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("[LLM] Chat completed, traceId: {}, duration: {}ms", traceId, duration);
            
            return LlmChatResponse.builder()
                    .content(response)
                    .model(properties.getLlm().getModel())
                    .success(true)
                    .durationMs(duration)
                    .build();
                    
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[LLM] Chat failed, traceId: {}, duration: {}ms, error: {}", 
                     traceId, duration, e.getMessage(), e);
            
            return LlmChatResponse.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .durationMs(duration)
                    .build();
        }
    }
    
    @Override
    public String getClientId() {
        return "openai-client";
    }
    
    /**
     * 初始化 ChatLanguageModel 实例。
     * 此方法应在配置加载后调用。
     */
    public void initializeModel() {
        SmartCrewProperties.Llm llmConfig = properties.getLlm();
        
        this.chatModel = OpenAiChatModel.builder()
                .baseUrl(llmConfig.getBaseUrl())
                .apiKey(llmConfig.getApiKey())
                .modelName(llmConfig.getModel())
                .temperature(0.7)
                .timeout(Duration.ofSeconds(30))
                .build();
                
        log.info("[LLM] OpenAI client initialized with model: {}", llmConfig.getModel());
    }
}
```

#### 第四步：创建 LLM 配置类
创建配置类来初始化 LLM 客户端。

```java
// 文件路径：smartcrew-modules/src/main/java/com/smartcrew/agent/core/config/LlmConfig.java
package com.smartcrew.agent.core.config;

import com.smartcrew.agent.common.config.SmartCrewProperties;
import com.smartcrew.agent.core.llm.OpenAiLlmClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * LLM 配置类，负责初始化大模型客户端。
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "smartcrew.llm", name = "enabled", havingValue = "true")
public class LlmConfig {
    
    private final SmartCrewProperties properties;
    private final OpenAiLlmClient openAiLlmClient;
    
    @PostConstruct
    public void initializeLlmClients() {
        String provider = properties.getLlm().getProvider();
        log.info("[LLM] Initializing LLM client for provider: {}", provider);
        
        if ("openai".equalsIgnoreCase(provider)) {
            openAiLlmClient.initializeModel();
            log.info("[LLM] OpenAI client initialized successfully");
        } else {
            log.warn("[LLM] Unsupported LLM provider: {}", provider);
        }
    }
}
```

#### 第五步：创建智能对话 Agent
创建一个使用 LLM 的智能对话 Agent。

```java
// 文件路径：smartcrew-modules/src/main/java/com/smartcrew/agent/core/agent/ChatAgent.java
package com.smartcrew.agent.core.agent;

import com.smartcrew.agent.api.agent.domain.model.AgentDispatchCommand;
import com.smartcrew.agent.api.agent.domain.vo.AgentDispatchResponse;
import com.smartcrew.agent.api.agent.service.Agent;
import com.smartcrew.agent.api.llm.domain.request.LlmChatRequest;
import com.smartcrew.agent.api.llm.domain.vo.LlmChatResponse;
import com.smartcrew.agent.api.llm.service.LlmClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 智能对话 Agent，使用 LLM 提供对话能力。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "smartcrew.llm", name = "enabled", havingValue = "true")
public class ChatAgent implements Agent {
    
    private final LlmClient llmClient;
    
    @Override
    public String code() {
        return "chat-agent";
    }
    
    @Override
    public String name() {
        return "智能对话助手";
    }
    
    @Override
    public boolean supports(String capability) {
        return "chat".equalsIgnoreCase(capability) || "conversation".equalsIgnoreCase(capability);
    }
    
    @Override
    public AgentDispatchResponse handle(AgentDispatchCommand command) {
        String traceId = command.getTraceId();
        String userMessage = command.getMessage();
        
        log.info("[ChatAgent] Processing request, traceId: {}, message: {}", traceId, userMessage);
        
        LlmChatRequest llmRequest = LlmChatRequest.builder()
                .userMessage(userMessage)
                .systemPrompt("你是一个智能助手，请用简洁、友好的方式回答用户的问题。")
                .temperature(0.7)
                .maxTokens(1000)
                .traceId(traceId)
                .build();
        
        LlmChatResponse llmResponse = llmClient.chat(llmRequest);
        
        if (llmResponse.getSuccess()) {
            log.info("[ChatAgent] LLM response received, traceId: {}, tokens: {}", 
                    traceId, llmResponse.getTotalTokens());
                    
            return AgentDispatchResponse.builder()
                    .traceId(traceId)
                    .agentCode(code())
                    .accepted(true)
                    .message(llmResponse.getContent())
                    .build();
        } else {
            log.error("[ChatAgent] LLM request failed, traceId: {}, error: {}", 
                     traceId, llmResponse.getErrorMessage());
                    
            return AgentDispatchResponse.builder()
                    .traceId(traceId)
                    .agentCode(code())
                    .accepted(false)
                    .message("抱歉，我现在无法处理您的请求，请稍后再试。")
                    .build();
        }
    }
}
```

#### 第六步：配置 LLM 参数
在配置文件中添加 LLM 相关配置。

```yaml
# 文件路径：smartcrew-admin/src/main/resources/application.yml
smartcrew:
  name: smartcrew-agent
  version: 0.0.1-SNAPSHOT
  llm:
    enabled: true              # 启用 LLM 功能
    provider: openai           # 供应商：openai
    baseUrl: https://api.openai.com/v1  # OpenAI API 地址
    apiKey: your-api-key-here  # 替换为你的 OpenAI API Key
    model: gpt-4o-mini         # 使用的模型
  tools:
    enabled:
      basic: true
      file: true
      image-search: true
      plantuml: true
      web-search: true
      web-page: true
      document: true
      terminal: false
  tooling:
    file:
      save-dir: ./tmp
```

### 2.3 验证与测试

#### 第一步：启动项目
确保已配置好 LLM 参数，然后启动 `smartcrew-admin` 模块。

#### 第二步：调用接口测试
使用 API 工具（如 Postman）调用聊天接口：

- **Method**: `POST`
- **URL**: `http://localhost:8085/api/v1/agents/chat-agent/dispatch`
- **Headers**: `Content-Type: application/json`
- **Body (JSON)**:
```json
{
  "message": "你好，请介绍一下你自己",
  "userId": 1,
  "sessionId": "test-session-001"
}
```

#### 第三步：预期结果
成功响应示例：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "traceId": "550e8400-e29b-41d4-a716-446655440000",
    "agentCode": "chat-agent",
    "accepted": true,
    "message": "你好！我是智能对话助手，很高兴为你服务。我可以回答各种问题，帮助你解决问题，或者陪你聊天。有什么我可以帮助你的吗？"
  }
}
```

### 2.4 扩展与优化建议

#### 2.4.1 多供应商支持
为支持其他 LLM 供应商，可以创建新的客户端实现：

```java
// 示例：本地模型客户端
@Service
@ConditionalOnProperty(prefix = "smartcrew.llm", name = "provider", havingValue = "local")
public class LocalLlmClient implements LlmClient {
    // 实现本地模型调用逻辑
}
```

#### 2.4.2 对话历史管理
增强对话能力，支持多轮对话：

```java
// 在 ChatAgent 中添加对话历史管理
private final ConversationMemoryService memoryService;

@Override
public AgentDispatchResponse handle(AgentDispatchCommand command) {
    // 加载历史对话
    Map<String, String> history = memoryService.loadMemory(command.getUserId());
    
    // 构建包含历史的请求
    LlmChatRequest llmRequest = LlmChatRequest.builder()
            .userMessage(command.getMessage())
            .conversationHistory(buildConversationHistory(history))
            .build();
    
    // ... 处理逻辑
    
    // 保存当前对话到历史
    memoryService.appendOrUpdate(command.getUserId(), "last_message", command.getMessage());
}
```

#### 2.4.3 错误处理与降级
增强系统的健壮性：

```java
// 在 ChatAgent 中添加降级逻辑
@Override
public AgentDispatchResponse handle(AgentDispatchCommand command) {
    try {
        LlmChatResponse response = llmClient.chat(buildRequest(command));
        
        if (!response.getSuccess()) {
            return handleFallback(command, response.getErrorMessage());
        }
        
        return buildSuccessResponse(command, response);
        
    } catch (Exception e) {
        log.error("[ChatAgent] Unexpected error", e);
        return handleFallback(command, "系统异常");
    }
}

private AgentDispatchResponse handleFallback(AgentDispatchCommand command, String error) {
    // 根据错误类型选择不同的降级策略
    if (error.contains("timeout")) {
        return AgentDispatchResponse.builder()
                .traceId(command.getTraceId())
                .message("抱歉，响应超时，请稍后再试。")
                .build();
    }
    
    return AgentDispatchResponse.builder()
            .traceId(command.getTraceId())
            .message("抱歉，我现在无法处理您的请求。")
            .build();
}
```

#### 2.4.4 性能监控
添加性能监控和日志：

```java
// 在 OpenAiLlmClient 中添加详细监控
@Override
public LlmChatResponse chat(LlmChatRequest request) {
    long startTime = System.currentTimeMillis();
    
    try {
        // ... 调用逻辑
        
        // 记录性能指标
        MetricsRegistry.recordLlmCall(
            request.getTraceId(),
            duration,
            response.getTotalTokens(),
            true
        );
        
        return response;
        
    } catch (Exception e) {
        // 记录失败指标
        MetricsRegistry.recordLlmFailure(request.getTraceId(), e.getMessage());
        throw e;
    }
}
```

### 2.5 常见问题排查

#### 问题1：启动时报错 "LLM client not initialized"
**解决方案**：检查配置文件中 `smartcrew.llm.enabled` 是否设置为 `true`。

#### 问题2：调用 LLM 时超时
**解决方案**：
- 检查网络连接是否正常
- 增加 `timeout` 参数配置
- 检查 API Key 是否有效

#### 问题3：响应内容为空或异常
**解决方案**：
- 检查 API Key 权限是否足够
- 查看日志中的详细错误信息
- 验证模型名称是否正确

### 2.6 后续扩展方向

完成基础 LLM 接入后，可以考虑以下扩展：

1. **RAG 集成**：结合向量数据库实现知识库问答
2. **工具调用**：让 LLM 能够调用系统工具
3. **多模态支持**：支持图片、文档等多模态输入
4. **流式输出**：实现流式响应，提升用户体验
5. **模型路由**：根据任务复杂度选择不同模型
6. **成本优化**：实现缓存、批处理等优化策略

通过本教程，你已经掌握了接入大模型的最小实现方法。这个实现预留了足够的扩展空间，可以根据业务需求逐步增强功能。

---

## 教程三：自定义工具 (Tools) 开发

**场景说明**：Agent 的强大之处在于能够使用工具。本教程将教你如何创建一个"数学计算工具"，并让 Agent 能够通过注解自动识别和调用它。

### 3.1 开发准备
- **目标包路径**：`smartcrew-modules/src/main/java/com/smartcrew/agent/core/tool/`
- **核心接口**：`com.smartcrew.agent.api.tool.service.SmartCrewTool`
- **关键注解**：`dev.langchain4j.agent.tool.Tool` (用于声明工具方法)

### 3.2 核心开发步骤

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

### 3.3 原理解析
- **自动注册**：项目启动时，`InMemoryToolRegistry` 会扫描所有 `@Component` 且实现了 `SmartCrewTool` 的类。
- **能力暴露**：通过 `@Tool` 注解，该方法会被提取为 LLM 能够理解的 JSON Schema，从而在 ReAct 决策链路中被选中。

---

## 教程四：RAG 知识库挂载 (扩展教程)

**场景说明**：当通用 LLM 无法回答公司内部知识时，我们需要接入 RAG。由于本项目目前仅提供了基础框架，本教程将引导你如何基于 `langchain4j` 扩展实现 RAG 管线。

### 4.1 扩展思路
RAG 的核心是将“私有文档”转换为“向量数据”并存储，查询时先检索再生成。

### 4.2 核心步骤指引

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

## 教程五：多 Agent 协作流编排 (扩展教程)

**场景说明**：复杂任务（如：写一份带数据的行业报告）需要多个 Agent 协作。本教程介绍如何在现有框架下实现“总控-子代理”协作模式。

### 5.1 协作模式：Planner-Executor
- **Planner (规划者)**：负责拆解任务。
- **Executor (执行者)**：负责具体执行。

### 5.2 核心实现指引

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

### 5.3 生产级建议
- **状态机**：对于长流程，建议在数据库中维护一个 `TaskSession` 表，记录每一步的状态。
- **消息总线**：利用项目中已有的 `MessageBus` 实现异步协作，避免 HTTP 阻塞。
