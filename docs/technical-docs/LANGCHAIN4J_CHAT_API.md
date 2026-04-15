# LangChain4j 聊天模型 API 详解

> 版本：v1.0  
> 适用版本：LangChain4j 1.0.0-beta2  
> 目标读者：Java 初学者、AI 应用开发者

---

## 1. 概述

### 1.1 什么是 ChatLanguageModel？

`ChatLanguageModel` 是 LangChain4j 框架中用于与大语言模型（LLM）进行对话交互的核心接口。它提供了一套标准化的 API，让开发者可以轻松地调用各种大模型（如 OpenAI 的 GPT 系列、阿里云的千问系列、智谱的 GLM 系列等），而无需关心底层实现细节。

### 1.2 为什么需要学习这个接口？

- **统一抽象**：无论使用哪个大模型供应商，API 调用方式一致
- **易于切换**：更换模型只需修改配置，无需改动业务代码
- **功能完整**：支持多轮对话、工具调用、多模态输入、流式输出等高级功能
- **贴合本项目**：本项目使用 LangChain4j 作为 LLM 调用的核心框架

### 1.3 LLM API 类型对比

| API 类型 | 说明 | 状态 | 适用场景 |
| :--- | :--- | :--- | :--- |
| `LanguageModel` | 输入输出均为字符串 | 过时 | 简单文本生成 |
| `ChatLanguageModel` | 支持多消息、多模态 | 推荐 | 对话、工具调用、复杂任务 |
| `StreamingChatLanguageModel` | 支持流式输出 | 推荐 | 实时对话、长文本生成 |

---

## 2. ChatLanguageModel 接口详解

### 2.1 接口定义

```java
public interface ChatLanguageModel {
    
    // 便捷方法：输入字符串，返回字符串
    String chat(String userMessage);
    
    // 核心方法：输入消息数组，返回响应对象
    ChatResponse chat(ChatMessage... messages);
    
    // 核心方法：输入消息列表，返回响应对象
    ChatResponse chat(List<ChatMessage> messages);
    
    // 高级方法：通过 ChatRequest 配置参数
    ChatResponse chat(ChatRequest chatRequest);
}

// 流式输出接口
public interface StreamingChatLanguageModel {
    
    // 流式输出：输入消息列表，返回流式响应
    void chat(List<ChatMessage> messages, StreamingChatLanguageModel.Callback callback);
    
    // 流式输出：通过 ChatRequest 配置参数
    void chat(ChatRequest chatRequest, StreamingChatLanguageModel.Callback callback);
    
    interface Callback {
        void onNext(String token);
        void onComplete(ChatResponse response);
        void onError(Throwable error);
    }
}
```

### 2.2 方法详解

#### 2.2.1 `String chat(String userMessage)`

**最简单的调用方式**，适合快速测试和简单场景。

```java
// 创建模型实例
ChatLanguageModel model = QwenChatModel.builder()
        .apiKey("your-api-key")
        .modelName("qwen-plus")
        .build();

// 直接发送消息
String response = model.chat("你好，请介绍一下你自己");

System.out.println(response);
// 输出：你好！我是阿里云开发的大规模语言模型...
```

**特点**：
- 输入：用户消息字符串
- 输出：AI 回复字符串
- 优点：简单直接，适合单轮对话
- 缺点：无法传递系统提示词、历史对话等上下文

---

#### 2.2.5 流式输出方法

**实时获取 AI 生成的内容**，适合需要实时反馈的场景。

```java
// 创建流式模型实例
StreamingChatLanguageModel streamingModel = QwenStreamingChatModel.builder()
        .apiKey(System.getenv("DASHSCOPE_API_KEY"))
        .modelName("qwen-plus")
        .build();

// 构建消息列表
List<ChatMessage> messages = Arrays.asList(
    SystemMessage.from("你是一个友好的助手"),
    UserMessage.from("请详细解释什么是 Spring Boot")
);

// 流式调用
System.out.println("AI 回复：");
StringBuilder fullResponse = new StringBuilder();

streamingModel.chat(messages, new StreamingChatLanguageModel.Callback() {
    @Override
    public void onNext(String token) {
        System.out.print(token);  // 实时输出每个 token
        fullResponse.append(token);
    }
    
    @Override
    public void onComplete(ChatResponse response) {
        System.out.println("\n\n对话完成！");
        // 可以获取完整的响应对象
    }
    
    @Override
    public void onError(Throwable error) {
        System.err.println("错误：" + error.getMessage());
    }
});

// 最终完整响应
System.out.println("\n完整回复：" + fullResponse.toString());
```

**特点**：
- 输入：消息列表或 ChatRequest
- 输出：通过回调函数实时获取
- 优点：实时反馈，用户体验更好
- 缺点：代码稍复杂，需要处理回调

**本项目应用**：
在 `ChatAgent` 中实现流式响应，提升用户体验：

```java
// 伪代码：在 ChatAgent 中使用流式输出
@Override
public AgentDispatchResponse handle(AgentDispatchCommand command) {
    String userMessage = command.getMessage();
    
    // 构建消息
    List<ChatMessage> messages = buildMessages(userMessage);
    
    // 构建回调
    StringBuilder responseBuilder = new StringBuilder();
    CountDownLatch latch = new CountDownLatch(1);
    
    streamingModel.chat(messages, new StreamingChatLanguageModel.Callback() {
        @Override
        public void onNext(String token) {
            responseBuilder.append(token);
            // 可以通过 WebSocket 实时推送给前端
        }
        
        @Override
        public void onComplete(ChatResponse response) {
            latch.countDown();
        }
        
        @Override
        public void onError(Throwable error) {
            log.error("流式输出错误", error);
            latch.countDown();
        }
    });
    
    // 等待完成
    try {
        latch.await(30, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
    
    return AgentDispatchResponse.builder()
            .traceId(command.getTraceId())
            .agentCode(code())
            .accepted(true)
            .message(responseBuilder.toString())
            .build();
}
```

---

#### 2.2.2 `ChatResponse chat(ChatMessage... messages)`

**支持多消息输入**，可以传递系统提示词和历史对话。

```java
// 构建消息数组
ChatResponse response = model.chat(
    SystemMessage.from("你是一个专业的 Java 开发工程师"),
    UserMessage.from("请解释一下什么是 Spring Boot")
);

// 获取 AI 回复
String reply = response.aiMessage().text();
System.out.println(reply);
```

**特点**：
- 输入：可变参数，支持多个消息
- 输出：`ChatResponse` 对象，包含更多信息
- 优点：支持系统提示词、多轮对话
- 缺点：需要手动管理对话历史

---

#### 2.2.3 `ChatResponse chat(List<ChatMessage> messages)`

**与数组版本功能相同**，只是参数类型为列表，方便动态构建消息。

```java
// 动态构建消息列表
List<ChatMessage> messages = new ArrayList<>();
messages.add(SystemMessage.from("你是一个友好的助手"));
messages.add(UserMessage.from("你好"));

ChatResponse response = model.chat(messages);
```

---

#### 2.2.4 `ChatResponse chat(ChatRequest chatRequest)`

**最完整的调用方式**，支持自定义所有参数。

```java
// 构建 ChatRequest
ChatRequest request = ChatRequest.builder()
        .messages(Arrays.asList(
            SystemMessage.from("你是一个专业的翻译"),
            UserMessage.from("将以下英文翻译成中文：Hello World")
        ))
        .parameters(ChatRequestParameters.builder()
                .temperature(0.3)      // 控制输出随机性
                .maxTokens(500)        // 最大输出 token 数
                .topP(0.9)             // 核采样参数
                .build())
        .build();

// 调用模型
ChatResponse response = model.chat(request);
```

**特点**：
- 输入：`ChatRequest` 对象，可配置所有参数
- 输出：`ChatResponse` 对象
- 优点：功能最完整，支持所有高级配置
- 缺点：代码稍复杂

---

## 3. ChatMessage 消息类型详解

### 3.1 消息类型概览

在对话中，存在不同角色的消息，LangChain4j 通过不同的消息类型来区分：

| 消息类型 | 类名 | 角色 | 说明 |
| :--- | :--- | :--- | :--- |
| 系统消息 | `SystemMessage` | system | 定义 AI 的角色和行为规范 |
| 用户消息 | `UserMessage` | user | 用户输入的内容 |
| AI 消息 | `AiMessage` | assistant | AI 生成的回复 |
| 工具结果 | `ToolExecutionResultMessage` | tool | 工具执行的返回结果 |

### 3.2 SystemMessage（系统消息）

**作用**：定义 AI 的角色、行为规范和回复风格。

```java
// 方式一：直接创建
SystemMessage systemMsg = SystemMessage.from("你是一个专业的 Java 开发工程师，回答问题时请提供代码示例");

// 方式二：使用 builder（支持更多配置）
SystemMessage systemMsg = SystemMessage.builder()
        .content("你是一个翻译助手，请将用户输入翻译成英文")
        .build();
```

**使用场景**：
- 定义 AI 的专业领域（如医生、律师、程序员）
- 设置回复风格（如简洁、详细、幽默）
- 添加约束条件（如"不要回答敏感问题"）

**注意事项**：
- 系统消息通常放在消息列表的第一位
- 不同模型对系统消息的遵循程度不同
- 避免让用户直接修改系统消息

---

### 3.3 UserMessage（用户消息）

**作用**：表示用户输入的内容，支持纯文本和多模态内容。

#### 3.3.1 纯文本消息

```java
// 方式一：直接创建
UserMessage userMsg = UserMessage.from("请解释什么是 Spring Boot");

// 方式二：使用 builder
UserMessage userMsg = UserMessage.builder()
        .content("请解释什么是 Spring Boot")
        .build();
```

#### 3.3.2 多模态消息（文本 + 图片）

```java
// 包含文本和图片的消息
UserMessage userMsg = UserMessage.builder()
        .addContent(TextContent.of("请描述这张图片的内容"))
        .addContent(ImageContent.fromUrl("https://example.com/image.jpg"))
        .build();

// 或者使用本地图片
UserMessage userMsg = UserMessage.builder()
        .addContent(TextContent.of("分析这张截图"))
        .addContent(ImageContent.fromFile(new File("screenshot.png")))
        .build();
```

#### 3.3.3 支持的内容类型

| 内容类型 | 类名 | 说明 |
| :--- | :--- | :--- |
| 文本 | `TextContent` | 纯文本内容 |
| 图片 | `ImageContent` | 支持 URL 或本地文件 |
| 音频 | `AudioContent` | 音频文件 |
| 视频 | `VideoContent` | 视频文件 |
| PDF | `PdfFileContent` | PDF 文档 |

---

### 3.4 AiMessage（AI 消息）

**作用**：表示 AI 生成的回复，可能包含文本或工具调用请求。

```java
// 从响应中获取 AI 消息
ChatResponse response = model.chat(userMessage);
AiMessage aiMessage = response.aiMessage();

// 获取文本内容
String text = aiMessage.text();

// 检查是否包含工具调用请求
if (aiMessage.hasToolExecutionRequests()) {
    List<ToolExecutionRequest> toolRequests = aiMessage.toolExecutionRequests();
    // 处理工具调用...
}
```

---

### 3.5 多模态消息处理

**多模态消息** 是指包含多种类型内容的消息，如文本 + 图片、文本 + 音频等。LangChain4j 提供了丰富的多模态内容类型支持。

#### 3.5.1 文本 + 图片

```java
// 创建包含文本和图片的用户消息
UserMessage multiModalMessage = UserMessage.builder()
        .addContent(TextContent.of("请详细描述这张图片的内容"))
        .addContent(ImageContent.fromUrl("https://example.com/image.jpg"))
        .build();

// 调用模型
ChatResponse response = model.chat(multiModalMessage);
System.out.println(response.aiMessage().text());
```

#### 3.5.2 文本 + 本地图片

```java
// 创建包含文本和本地图片的用户消息
UserMessage multiModalMessage = UserMessage.builder()
        .addContent(TextContent.of("分析这张截图中的问题"))
        .addContent(ImageContent.fromFile(new File("screenshot.png")))
        .build();

// 调用模型
ChatResponse response = model.chat(multiModalMessage);
System.out.println(response.aiMessage().text());
```

#### 3.5.3 文本 + 音频

```java
// 创建包含文本和音频的用户消息
UserMessage multiModalMessage = UserMessage.builder()
        .addContent(TextContent.of("请转录并总结这段音频内容"))
        .addContent(AudioContent.fromFile(new File("recording.mp3")))
        .build();

// 调用模型
ChatResponse response = model.chat(multiModalMessage);
System.out.println(response.aiMessage().text());
```

#### 3.5.4 文本 + PDF

```java
// 创建包含文本和 PDF 的用户消息
UserMessage multiModalMessage = UserMessage.builder()
        .addContent(TextContent.of("请总结这份 PDF 文档的主要内容"))
        .addContent(PdfFileContent.fromFile(new File("document.pdf")))
        .build();

// 调用模型
ChatResponse response = model.chat(multiModalMessage);
System.out.println(response.aiMessage().text());
```

#### 3.5.5 本项目中的多模态处理实现

```java
// 文件：smartcrew-modules/src/main/java/com/smartcrew/agent/core/llm/MultiModalLlmClient.java
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "smartcrew.llm", name = "provider", havingValue = "qwen")
public class MultiModalLlmClient implements LlmClient {
    
    private final SmartCrewProperties properties;
    private ChatLanguageModel chatModel;
    
    @PostConstruct
    public void initialize() {
        SmartCrewProperties.Llm llmConfig = properties.getLlm();
        if (llmConfig.isEnabled()) {
            this.chatModel = QwenChatModel.builder()
                    .apiKey(llmConfig.getApiKey())
                    .modelName("qwen-vl-plus")  // 使用视觉模型
                    .baseUrl(llmConfig.getBaseUrl())
                    .temperature(0.7)
                    .timeout(Duration.ofSeconds(60))
                    .build();
            
            log.info("多模态 LLM 客户端初始化完成，模型：qwen-vl-plus");
        }
    }
    
    @Override
    public LlmChatResponse chat(LlmChatRequest request) {
        long startTime = System.currentTimeMillis();
        String traceId = request.getTraceId() != null ? request.getTraceId() : "unknown";
        
        try {
            log.info("[MultiModalLlmClient] 开始调用，traceId: {}, message: {}", traceId, request.getUserMessage());
            
            // 构建消息
            List<ChatMessage> messages = new ArrayList<>();
            
            // 添加系统提示词
            if (request.getSystemPrompt() != null) {
                messages.add(SystemMessage.from(request.getSystemPrompt()));
            }
            
            // 处理多模态内容
            if (request.getMultiModalContent() != null && !request.getMultiModalContent().isEmpty()) {
                UserMessage.Builder messageBuilder = UserMessage.builder()
                        .addContent(TextContent.of(request.getUserMessage()));
                
                for (MultiModalContent content : request.getMultiModalContent()) {
                    switch (content.getType()) {
                        case IMAGE_URL:
                            messageBuilder.addContent(ImageContent.fromUrl(content.getContent()));
                            break;
                        case IMAGE_FILE:
                            messageBuilder.addContent(ImageContent.fromFile(new File(content.getContent())));
                            break;
                        case AUDIO_FILE:
                            messageBuilder.addContent(AudioContent.fromFile(new File(content.getContent())));
                            break;
                        case PDF_FILE:
                            messageBuilder.addContent(PdfFileContent.fromFile(new File(content.getContent())));
                            break;
                    }
                }
                
                messages.add(messageBuilder.build());
            } else {
                // 普通文本消息
                messages.add(UserMessage.from(request.getUserMessage()));
            }
            
            // 构建请求
            ChatRequest chatRequest = ChatRequest.builder()
                    .messages(messages)
                    .parameters(ChatRequestParameters.builder()
                            .temperature(request.getTemperature())
                            .maxTokens(request.getMaxTokens())
                            .build())
                    .build();
            
            // 调用模型
            ChatResponse response = chatModel.chat(chatRequest);
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("[MultiModalLlmClient] 调用完成，traceId: {}, duration: {}ms", traceId, duration);
            
            // 构建响应
            TokenUsage tokenUsage = response.metadata().tokenUsage();
            return LlmChatResponse.builder()
                    .content(response.aiMessage().text())
                    .totalTokens(tokenUsage != null ? tokenUsage.totalTokenCount() : null)
                    .promptTokens(tokenUsage != null ? tokenUsage.inputTokenCount() : null)
                    .completionTokens(tokenUsage != null ? tokenUsage.outputTokenCount() : null)
                    .model("qwen-vl-plus")
                    .success(true)
                    .durationMs(duration)
                    .build();
                    
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[MultiModalLlmClient] 调用失败，traceId: {}, duration: {}ms, error: {}", 
                     traceId, duration, e.getMessage(), e);
            
            return LlmChatResponse.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .durationMs(duration)
                    .build();
        }
    }
    
    @Override
    public void streamingChat(LlmChatRequest request, LlmStreamingCallback callback) {
        // 流式调用实现...
    }
    
    @Override
    public LlmChatResponse chatWithMessages(List<ChatMessage> messages, LlmChatRequest request) {
        // 带消息列表的调用实现...
    }
    
    @Override
    public String getClientId() {
        return "multi-modal-client";
    }
}

// 多模态内容类
public class MultiModalContent {
    public enum Type {
        IMAGE_URL,
        IMAGE_FILE,
        AUDIO_FILE,
        PDF_FILE
    }
    
    private Type type;
    private String content;
    
    // 构造方法、getter、setter...
}

// 扩展 LlmChatRequest
public class LlmChatRequest {
    // 现有字段...
    private List<MultiModalContent> multiModalContent;
    
    // 构造方法、getter、setter、builder...
}
```

#### 3.5.6 多模态输入示例

**示例1：图片分析**

```java
// 用户输入："请分析这张图片中的内容"

// 构建多模态请求
LlmChatRequest request = LlmChatRequest.builder()
        .userMessage("请分析这张图片中的内容")
        .multiModalContent(Collections.singletonList(
                new MultiModalContent(MultiModalContent.Type.IMAGE_URL, "https://example.com/product.jpg")
        ))
        .systemPrompt("你是一个专业的图片分析专家，请详细描述图片中的内容")
        .temperature(0.7)
        .maxTokens(1000)
        .build();

// 调用多模态模型
LlmChatResponse response = multiModalLlmClient.chat(request);
System.out.println(response.getContent());
// 输出：这张图片展示了一款智能手机，背面有三个摄像头...
```

**示例2：音频转录**

```java
// 用户输入："请转录并总结这段音频"

// 构建多模态请求
LlmChatRequest request = LlmChatRequest.builder()
        .userMessage("请转录并总结这段音频")
        .multiModalContent(Collections.singletonList(
                new MultiModalContent(MultiModalContent.Type.AUDIO_FILE, "meeting_recording.mp3")
        ))
        .systemPrompt("你是一个专业的音频转录专家，请先转录音频内容，然后总结主要观点")
        .temperature(0.3)
        .maxTokens(2000)
        .build();

// 调用多模态模型
LlmChatResponse response = multiModalLlmClient.chat(request);
System.out.println(response.getContent());
// 输出：转录内容：... 总结：本次会议讨论了项目进度和下一步计划...
```

---

## 4. ChatRequest 请求对象详解

### 4.1 ChatRequest 结构

`ChatRequest` 是对聊天请求的完整封装，包含消息列表和配置参数。

```java
ChatRequest request = ChatRequest.builder()
        .messages(messages)           // 消息列表
        .parameters(parameters)       // 请求参数
        .toolSpecifications(tools)    // 工具定义（可选）
        .build();
```

### 4.2 ChatRequestParameters 参数详解

```java
ChatRequestParameters parameters = ChatRequestParameters.builder()
        .modelName("qwen-plus")           // 模型名称
        .temperature(0.7)                  // 温度参数
        .topP(0.9)                         // 核采样
        .maxTokens(1000)                   // 最大输出 token
        .maxCompletionTokens(1000)         // 最大完成 token
        .stopSequences(List.of("END"))     // 停止序列
        .frequencyPenalty(0.5)             // 频率惩罚
        .presencePenalty(0.5)              // 存在惩罚
        .build();
```

### 4.3 参数说明

| 参数 | 类型 | 说明 | 取值范围 | 推荐值 |
| :--- | :--- | :--- | :--- | :--- |
| `temperature` | Double | 控制输出随机性，值越高越随机 | 0.0 - 2.0 | 0.7（创意）/ 0.3（精确） |
| `topP` | Double | 核采样参数，控制词汇选择范围 | 0.0 - 1.0 | 0.9 |
| `maxTokens` | Integer | 最大输出 token 数 | 1 - 模型上限 | 根据需求 |
| `stopSequences` | List<String> | 遇到这些字符串时停止生成 | - | 自定义 |
| `frequencyPenalty` | Double | 降低重复词的出现频率 | -2.0 - 2.0 | 0.0 - 1.0 |
| `presencePenalty` | Double | 鼓励谈论新话题 | -2.0 - 2.0 | 0.0 - 1.0 |

### 4.4 工具调用配置

**ToolSpecification** 用于向 LLM 描述可用的工具：

```java
// 定义工具规范
ToolSpecification searchTool = ToolSpecification.builder()
        .name("search_web")
        .description("搜索网络获取信息")
        .parameters(JsonSchema.builder()
                .type(JsonSchema.Type.OBJECT)
                .properties(Map.of(
                        "query", JsonSchema.builder()
                                .type(JsonSchema.Type.STRING)
                                .description("搜索查询词")
                                .build()
                ))
                .requiredParameters(List.of("query"))
                .build())
        .build();

// 在 ChatRequest 中添加工具
ChatRequest request = ChatRequest.builder()
        .messages(messages)
        .toolSpecifications(List.of(searchTool))
        .build();
```

**本项目应用**：
将项目中的 `SmartCrewTool` 转换为 LangChain4j 的工具规范：

```java
// 伪代码：将 SmartCrewTool 转换为 ToolSpecification
public List<ToolSpecification> convertToToolSpecifications(List<SmartCrewTool> tools) {
    return tools.stream()
            .map(this::convertTool)
            .collect(Collectors.toList());
}

private ToolSpecification convertTool(SmartCrewTool tool) {
    // 反射获取工具方法
    List<Method> toolMethods = getToolMethods(tool.getClass());
    
    return ToolSpecification.builder()
            .name(tool.toolCode())
            .description(tool.description())
            .parameters(buildParameters(toolMethods))
            .build();
}
```

### 4.4 Temperature 参数详解

`temperature` 是最重要的参数之一，直接影响 AI 回复的风格：

```
温度值低（0.0 - 0.3）：
├── 输出更加确定和一致
├── 适合：代码生成、数据分析、翻译
└── 示例：temperature = 0.1

温度值中等（0.4 - 0.7）：
├── 平衡创造力和一致性
├── 适合：日常对话、问答
└── 示例：temperature = 0.7

温度值高（0.8 - 1.0+）：
├── 输出更加随机和创意
├── 适合：创意写作、头脑风暴
└── 示例：temperature = 0.9
```

---

## 5. ChatResponse 响应对象详解

### 5.1 ChatResponse 结构

```java
public interface ChatResponse {
    
    // 获取 AI 消息
    AiMessage aiMessage();
    
    // 获取元数据
    ChatResponseMetadata metadata();
    
    // 获取工具执行请求（如果有）
    List<ToolExecutionRequest> toolExecutionRequests();
}
```

### 5.2 获取响应内容

```java
ChatResponse response = model.chat(request);

// 1. 获取 AI 回复文本
AiMessage aiMessage = response.aiMessage();
String text = aiMessage.text();
System.out.println("AI 回复：" + text);

// 2. 获取元数据
ChatResponseMetadata metadata = response.metadata();

// 3. 获取 token 使用情况
TokenUsage tokenUsage = metadata.tokenUsage();
System.out.println("输入 token：" + tokenUsage.inputTokenCount());
System.out.println("输出 token：" + tokenUsage.outputTokenCount());
System.out.println("总 token：" + tokenUsage.totalTokenCount());

// 4. 获取模型信息
String modelName = metadata.modelName();
System.out.println("使用模型：" + modelName);

// 5. 获取结束原因
FinishReason finishReason = metadata.finishReason();
System.out.println("结束原因：" + finishReason);
```

### 5.3 ChatResponseMetadata 元数据详解

| 字段 | 类型 | 说明 |
| :--- | :--- | :--- |
| `id` | String | 响应唯一标识 |
| `modelName` | String | 使用的模型名称 |
| `tokenUsage` | TokenUsage | Token 使用统计 |
| `finishReason` | FinishReason | 生成结束原因 |
| `responseTime` | Duration | 响应时间 |

### 5.4 FinishReason 结束原因

| 值 | 说明 |
| :--- | :--- |
| `STOP` | 正常完成 |
| `LENGTH` | 达到最大 token 限制 |
| `TOOL_EXECUTION` | 需要执行工具 |
| `CONTENT_FILTER` | 内容被过滤 |
| `OTHER` | 其他原因 |

### 5.5 工具调用处理

**处理 AI 工具调用请求**：

```java
ChatResponse response = model.chat(request);

// 检查是否需要执行工具
if (response.aiMessage().hasToolExecutionRequests()) {
    List<ToolExecutionRequest> toolRequests = response.aiMessage().toolExecutionRequests();
    
    // 处理每个工具请求
    for (ToolExecutionRequest toolRequest : toolRequests) {
        String toolName = toolRequest.name();
        Map<String, Object> arguments = toolRequest.arguments();
        
        // 执行工具
        Object result = executeTool(toolName, arguments);
        
        // 创建工具执行结果消息
        ToolExecutionResultMessage toolResultMessage = ToolExecutionResultMessage.builder()
                .toolName(toolName)
                .result(result.toString())
                .build();
        
        // 将工具结果添加到对话历史
        history.add(toolResultMessage);
        
        // 再次调用模型获取最终回复
        ChatResponse finalResponse = model.chat(history);
        System.out.println("AI 最终回复：" + finalResponse.aiMessage().text());
    }
}
```

**本项目应用**：
在 `ChatAgent` 中集成工具调用：

```java
// 伪代码：在 ChatAgent 中处理工具调用
@Override
public AgentDispatchResponse handle(AgentDispatchCommand command) {
    String userMessage = command.getMessage();
    List<ChatMessage> history = buildHistory(command);
    
    // 构建包含工具的请求
    ChatRequest request = ChatRequest.builder()
            .messages(history)
            .toolSpecifications(getAvailableTools())
            .build();
    
    ChatResponse response = llmClient.chat(request);
    
    // 处理工具调用
    if (response.aiMessage().hasToolExecutionRequests()) {
        return handleToolExecution(command, response, history);
    } else {
        // 直接返回 AI 回复
        return AgentDispatchResponse.builder()
                .traceId(command.getTraceId())
                .agentCode(code())
                .accepted(true)
                .message(response.aiMessage().text())
                .build();
    }
}

private AgentDispatchResponse handleToolExecution(
        AgentDispatchCommand command, 
        ChatResponse response, 
        List<ChatMessage> history) {
    // 执行工具并获取结果
    List<ToolExecutionResultMessage> toolResults = executeTools(response.aiMessage().toolExecutionRequests());
    
    // 将工具结果添加到历史
    history.addAll(toolResults);
    
    // 再次调用模型
    ChatResponse finalResponse = llmClient.chat(history);
    
    return AgentDispatchResponse.builder()
            .traceId(command.getTraceId())
            .agentCode(code())
            .accepted(true)
            .message(finalResponse.aiMessage().text())
            .build();
}
```

---

## 6. 多轮对话实现

### 6.1 问题：LLM 是无状态的

大语言模型本身是无状态的，每次调用都是独立的。要实现多轮对话，需要手动维护对话历史。

```java
// ❌ 错误示例：AI 无法记住之前的对话
model.chat("我叫张三");
model.chat("我叫什么名字？");  // AI 不知道你叫什么

// ✅ 正确示例：传递完整对话历史
List<ChatMessage> history = new ArrayList<>();
history.add(UserMessage.from("我叫张三"));
history.add(model.chat(history).aiMessage());  // 保存 AI 回复

history.add(UserMessage.from("我叫什么名字？"));
ChatResponse response = model.chat(history);  // AI 会回答"你叫张三"
```

### 6.2 完整的多轮对话示例

```java
public class ChatSession {
    private final ChatLanguageModel model;
    private final List<ChatMessage> history = new ArrayList<>();
    
    public ChatSession(ChatLanguageModel model, String systemPrompt) {
        this.model = model;
        // 添加系统消息
        history.add(SystemMessage.from(systemPrompt));
    }
    
    public String chat(String userMessage) {
        // 添加用户消息
        history.add(UserMessage.from(userMessage));
        
        // 调用模型
        ChatResponse response = model.chat(history);
        AiMessage aiMessage = response.aiMessage();
        
        // 保存 AI 回复到历史
        history.add(aiMessage);
        
        return aiMessage.text();
    }
    
    public void clearHistory() {
        // 保留系统消息，清除其他历史
        ChatMessage systemMsg = history.get(0);
        history.clear();
        history.add(systemMsg);
    }
    
    public void trimHistory(int maxMessages) {
        // 保留系统消息，只保留最近的 maxMessages 条消息
        if (history.size() > maxMessages + 1) {
            ChatMessage systemMsg = history.get(0);
            List<ChatMessage> recentMessages = history.subList(
                history.size() - maxMessages, 
                history.size()
            );
            history.clear();
            history.add(systemMsg);
            history.addAll(recentMessages);
        }
    }
}

// 使用示例
ChatSession session = new ChatSession(model, "你是一个友好的助手");
System.out.println(session.chat("你好，我叫李四"));
System.out.println(session.chat("我叫什么名字？"));  // AI 会记住你叫李四
```

### 6.3 本项目中的多轮对话实现

在本项目中，我们可以结合 `ConversationMemoryService` 实现持久化的多轮对话：

```java
// 伪代码：结合 ConversationMemoryService 实现多轮对话
@Component
public class MemoryAwareChatSession {
    private final ChatLanguageModel model;
    private final ConversationMemoryService memoryService;
    private final Map<Long, List<ChatMessage>> userSessions = new ConcurrentHashMap<>();
    
    public MemoryAwareChatSession(ChatLanguageModel model, ConversationMemoryService memoryService) {
        this.model = model;
        this.memoryService = memoryService;
    }
    
    public String chat(long userId, String userMessage, String sessionId) {
        // 获取或创建用户会话
        List<ChatMessage> history = userSessions.computeIfAbsent(userId, k -> {
            List<ChatMessage> newHistory = new ArrayList<>();
            // 从内存服务加载历史
            loadHistoryFromMemory(userId, newHistory);
            return newHistory;
        });
        
        // 添加用户消息
        history.add(UserMessage.from(userMessage));
        
        // 调用模型
        ChatResponse response = model.chat(history);
        AiMessage aiMessage = response.aiMessage();
        
        // 保存 AI 回复到历史
        history.add(aiMessage);
        
        // 保存到内存服务
        saveHistoryToMemory(userId, history);
        
        // 修剪历史，避免 token 超限
        if (history.size() > 50) {
            trimHistory(history, 30);
        }
        
        return aiMessage.text();
    }
    
    private void loadHistoryFromMemory(long userId, List<ChatMessage> history) {
        Map<String, String> memory = memoryService.loadMemory(userId);
        
        // 添加系统消息
        String systemPrompt = memory.getOrDefault("system_prompt", "你是一个友好的助手");
        history.add(SystemMessage.from(systemPrompt));
        
        // 加载最近的对话历史
        for (int i = 1; i <= 10; i++) {
            String userMsg = memory.get("user_msg_" + i);
            String aiMsg = memory.get("ai_msg_" + i);
            
            if (userMsg != null) {
                history.add(UserMessage.from(userMsg));
            }
            if (aiMsg != null) {
                history.add(AiMessage.from(aiMsg));
            }
        }
    }
    
    private void saveHistoryToMemory(long userId, List<ChatMessage> history) {
        // 保存系统消息
        if (!history.isEmpty() && history.get(0) instanceof SystemMessage) {
            SystemMessage systemMsg = (SystemMessage) history.get(0);
            memoryService.appendOrUpdate(userId, "system_prompt", systemMsg.text());
        }
        
        // 保存最近的 10 轮对话
        int startIdx = Math.max(1, history.size() - 20);  // 跳过系统消息
        int count = 1;
        
        for (int i = startIdx; i < history.size(); i += 2) {
            if (i < history.size() && history.get(i) instanceof UserMessage) {
                UserMessage userMsg = (UserMessage) history.get(i);
                memoryService.appendOrUpdate(userId, "user_msg_" + count, userMsg.text());
            }
            
            if (i + 1 < history.size() && history.get(i + 1) instanceof AiMessage) {
                AiMessage aiMsg = (AiMessage) history.get(i + 1);
                memoryService.appendOrUpdate(userId, "ai_msg_" + count, aiMsg.text());
            }
            
            count++;
            if (count > 10) break;
        }
    }
    
    private void trimHistory(List<ChatMessage> history, int maxMessages) {
        if (history.size() > maxMessages + 1) {
            ChatMessage systemMsg = history.get(0);
            List<ChatMessage> recentMessages = history.subList(
                history.size() - maxMessages, 
                history.size()
            );
            history.clear();
            history.add(systemMsg);
            history.addAll(recentMessages);
        }
    }
}

// 在 ChatAgent 中使用
@Autowired
private MemoryAwareChatSession chatSession;

@Override
public AgentDispatchResponse handle(AgentDispatchCommand command) {
    String reply = chatSession.chat(
        command.getUserId(),
        command.getMessage(),
        command.getSessionId()
    );
    
    return AgentDispatchResponse.builder()
            .traceId(command.getTraceId())
            .agentCode(code())
            .accepted(true)
            .message(reply)
            .build();
}
```

---

## 7. 实战案例

### 7.1 案例1：简单问答

```java
public class SimpleQA {
    public static void main(String[] args) {
        ChatLanguageModel model = QwenChatModel.builder()
                .apiKey(System.getenv("DASHSCOPE_API_KEY"))
                .modelName("qwen-plus")
                .build();
        
        String answer = model.chat("Java 中什么是多态？");
        System.out.println(answer);
    }
}
```

### 7.2 案例2：带系统提示词的对话

```java
public class ProfessionalChat {
    public static void main(String[] args) {
        ChatLanguageModel model = QwenChatModel.builder()
                .apiKey(System.getenv("DASHSCOPE_API_KEY"))
                .modelName("qwen-plus")
                .build();
        
        ChatResponse response = model.chat(
            SystemMessage.from("""
                你是一个资深的 Java 架构师，具有 15 年开发经验。
                回答问题时请：
                1. 提供代码示例
                2. 解释核心原理
                3. 给出最佳实践建议
                """),
            UserMessage.from("如何设计一个高并发系统？")
        );
        
        System.out.println(response.aiMessage().text());
    }
}
```

### 7.3 案例3：控制输出格式

```java
public class StructuredOutput {
    public static void main(String[] args) {
        ChatLanguageModel model = QwenChatModel.builder()
                .apiKey(System.getenv("DASHSCOPE_API_KEY"))
                .modelName("qwen-plus")
                .build();
        
        ChatRequest request = ChatRequest.builder()
                .messages(List.of(
                    SystemMessage.from("你是一个数据分析专家，请用 JSON 格式回复"),
                    UserMessage.from("分析一下 Spring Boot 的优缺点")
                ))
                .parameters(ChatRequestParameters.builder()
                        .temperature(0.3)  // 低温度，输出更稳定
                        .maxTokens(500)
                        .build())
                .build();
        
        ChatResponse response = model.chat(request);
        System.out.println(response.aiMessage().text());
        
        // 打印 token 使用情况
        TokenUsage usage = response.metadata().tokenUsage();
        System.out.printf("Token 使用：输入 %d，输出 %d，总计 %d%n",
                usage.inputTokenCount(),
                usage.outputTokenCount(),
                usage.totalTokenCount());
    }
}
```

### 7.4 案例4：多模态输入（图片理解）

```java
public class ImageUnderstanding {
    public static void main(String[] args) {
        ChatLanguageModel model = QwenChatModel.builder()
                .apiKey(System.getenv("DASHSCOPE_API_KEY"))
                .modelName("qwen-vl-plus")  // 使用视觉模型
                .build();
        
        UserMessage message = UserMessage.builder()
                .addContent(TextContent.of("请详细描述这张图片的内容"))
                .addContent(ImageContent.fromUrl("https://example.com/photo.jpg"))
                .build();
        
        ChatResponse response = model.chat(message);
        System.out.println(response.aiMessage().text());
    }
}
```

### 7.5 案例5：本项目中的 LLM 客户端实现

```java
// 文件：smartcrew-modules/src/main/java/com/smartcrew/agent/core/llm/OpenAiLlmClient.java
@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAiLlmClient implements LlmClient {
    
    private final SmartCrewProperties properties;
    private ChatLanguageModel chatModel;
    
    @PostConstruct
    public void initialize() {
        SmartCrewProperties.Llm llmConfig = properties.getLlm();
        if (llmConfig.isEnabled()) {
            this.chatModel = OpenAiChatModel.builder()
                    .apiKey(llmConfig.getApiKey())
                    .modelName(llmConfig.getModel())
                    .baseUrl(llmConfig.getBaseUrl())
                    .temperature(0.7)
                    .timeout(Duration.ofSeconds(30))
                    .build();
            log.info("OpenAI LLM 客户端初始化完成，模型：{}", llmConfig.getModel());
        }
    }
    
    @Override
    public LlmChatResponse chat(LlmChatRequest request) {
        long startTime = System.currentTimeMillis();
        String traceId = request.getTraceId() != null ? request.getTraceId() : "unknown";
        
        try {
            log.info("[LLM] 开始调用，traceId: {}, message: {}", traceId, request.getUserMessage());
            
            // 构建消息
            List<ChatMessage> messages = new ArrayList<>();
            
            // 添加系统提示词
            if (request.getSystemPrompt() != null) {
                messages.add(SystemMessage.from(request.getSystemPrompt()));
            }
            
            // 添加历史对话
            if (request.getConversationHistory() != null && !request.getConversationHistory().isEmpty()) {
                for (Map<String, String> historyItem : request.getConversationHistory()) {
                    String role = historyItem.get("role");
                    String content = historyItem.get("content");
                    
                    if ("user".equals(role)) {
                        messages.add(UserMessage.from(content));
                    } else if ("assistant".equals(role)) {
                        messages.add(AiMessage.from(content));
                    }
                }
            }
            
            // 添加当前用户消息
            messages.add(UserMessage.from(request.getUserMessage()));
            
            // 构建请求
            ChatRequest chatRequest = ChatRequest.builder()
                    .messages(messages)
                    .parameters(ChatRequestParameters.builder()
                            .temperature(request.getTemperature())
                            .maxTokens(request.getMaxTokens())
                            .build())
                    .build();
            
            // 调用模型
            ChatResponse response = chatModel.chat(chatRequest);
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("[LLM] 调用完成，traceId: {}, duration: {}ms", traceId, duration);
            
            // 构建响应
            TokenUsage tokenUsage = response.metadata().tokenUsage();
            return LlmChatResponse.builder()
                    .content(response.aiMessage().text())
                    .totalTokens(tokenUsage != null ? tokenUsage.totalTokenCount() : null)
                    .promptTokens(tokenUsage != null ? tokenUsage.inputTokenCount() : null)
                    .completionTokens(tokenUsage != null ? tokenUsage.outputTokenCount() : null)
                    .model(properties.getLlm().getModel())
                    .success(true)
                    .durationMs(duration)
                    .build();
                    
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[LLM] 调用失败，traceId: {}, duration: {}ms, error: {}", 
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
}
```

### 7.6 案例6：本项目中的 ChatAgent 实现

```java
// 文件：smartcrew-modules/src/main/java/com/smartcrew/agent/core/agent/ChatAgent.java
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "smartcrew.llm", name = "enabled", havingValue = "true")
public class ChatAgent implements Agent {
    
    private final LlmClient llmClient;
    private final ConversationMemoryService memoryService;
    
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
        Long userId = command.getUserId();
        
        log.info("[ChatAgent] 处理请求，traceId: {}, userId: {}, message: {}", 
                traceId, userId, userMessage);
        
        // 构建 LLM 请求
        LlmChatRequest llmRequest = LlmChatRequest.builder()
                .userMessage(userMessage)
                .systemPrompt("你是一个智能对话助手，
                请用简洁、友好的方式回答用户的问题。
                如果不确定答案，请如实告知。")
                .conversationHistory(loadConversationHistory(userId))
                .temperature(0.7)
                .maxTokens(1000)
                .traceId(traceId)
                .build();
        
        // 调用 LLM
        LlmChatResponse llmResponse = llmClient.chat(llmRequest);
        
        if (llmResponse.getSuccess()) {
            log.info("[ChatAgent] LLM 响应成功，traceId: {}, tokens: {}", 
                    traceId, llmResponse.getTotalTokens());
            
            // 保存对话历史
            saveConversationHistory(userId, userMessage, llmResponse.getContent());
            
            return AgentDispatchResponse.builder()
                    .traceId(traceId)
                    .agentCode(code())
                    .accepted(true)
                    .message(llmResponse.getContent())
                    .build();
        } else {
            log.error("[ChatAgent] LLM 请求失败，traceId: {}, error: {}", 
                     traceId, llmResponse.getErrorMessage());
            
            return AgentDispatchResponse.builder()
                    .traceId(traceId)
                    .agentCode(code())
                    .accepted(false)
                    .message("抱歉，我现在无法处理您的请求，请稍后再试。")
                    .build();
        }
    }
    
    private List<Map<String, String>> loadConversationHistory(Long userId) {
        List<Map<String, String>> history = new ArrayList<>();
        Map<String, String> memory = memoryService.loadMemory(userId);
        
        // 加载最近的 5 轮对话
        for (int i = 1; i <= 5; i++) {
            String userMsg = memory.get("user_msg_" + i);
            String aiMsg = memory.get("ai_msg_" + i);
            
            if (userMsg != null) {
                Map<String, String> userItem = new HashMap<>();
                userItem.put("role", "user");
                userItem.put("content", userMsg);
                history.add(userItem);
            }
            
            if (aiMsg != null) {
                Map<String, String> aiItem = new HashMap<>();
                aiItem.put("role", "assistant");
                aiItem.put("content", aiMsg);
                history.add(aiItem);
            }
        }
        
        return history;
    }
    
    private void saveConversationHistory(Long userId, String userMessage, String aiMessage) {
        // 加载现有历史
        Map<String, String> memory = memoryService.loadMemory(userId);
        
        // 移位历史记录
        for (int i = 5; i > 1; i--) {
            String prevUserMsg = memory.get("user_msg_" + (i - 1));
            String prevAiMsg = memory.get("ai_msg_" + (i - 1));
            
            if (prevUserMsg != null) {
                memoryService.appendOrUpdate(userId, "user_msg_" + i, prevUserMsg);
            }
            if (prevAiMsg != null) {
                memoryService.appendOrUpdate(userId, "ai_msg_" + i, prevAiMsg);
            }
        }
        
        // 保存最新消息
        memoryService.appendOrUpdate(userId, "user_msg_1", userMessage);
        memoryService.appendOrUpdate(userId, "ai_msg_1", aiMessage);
    }
}
```

---

## 8. 本项目集成指南

### 8.1 配置 LLM 客户端

**步骤1：配置文件设置**

```yaml
# application.yml
smartcrew:
  llm:
    enabled: true
    provider: openai  # 可选：openai, qwen, glm
    baseUrl: https://api.openai.com/v1
    apiKey: your-api-key-here
    model: gpt-4o-mini
```

**步骤2：创建 LLM 客户端实现**

#### 2.1 通义千问模型实现

```java
// 文件：smartcrew-modules/src/main/java/com/smartcrew/agent/core/llm/QwenLlmClient.java
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "smartcrew.llm", name = "provider", havingValue = "qwen")
public class QwenLlmClient implements LlmClient {
    
    private final SmartCrewProperties properties;
    private ChatLanguageModel chatModel;
    private StreamingChatLanguageModel streamingChatModel;
    
    @PostConstruct
    public void initialize() {
        SmartCrewProperties.Llm llmConfig = properties.getLlm();
        if (llmConfig.isEnabled()) {
            this.chatModel = QwenChatModel.builder()
                    .apiKey(llmConfig.getApiKey())
                    .modelName(llmConfig.getModel())
                    .baseUrl(llmConfig.getBaseUrl())
                    .temperature(0.7)
                    .timeout(Duration.ofSeconds(30))
                    .build();
            
            this.streamingChatModel = QwenStreamingChatModel.builder()
                    .apiKey(llmConfig.getApiKey())
                    .modelName(llmConfig.getModel())
                    .baseUrl(llmConfig.getBaseUrl())
                    .temperature(0.7)
                    .timeout(Duration.ofSeconds(60))
                    .build();
            
            log.info("通义千问 LLM 客户端初始化完成，模型：{}", llmConfig.getModel());
        }
    }
    
    @Override
    public LlmChatResponse chat(LlmChatRequest request) {
        long startTime = System.currentTimeMillis();
        String traceId = request.getTraceId() != null ? request.getTraceId() : "unknown";
        
        try {
            log.info("[LLM-Qwen] 开始调用，traceId: {}, message: {}", traceId, request.getUserMessage());
            
            // 构建消息
            List<ChatMessage> messages = buildMessages(request);
            
            // 构建请求
            ChatRequest chatRequest = ChatRequest.builder()
                    .messages(messages)
                    .parameters(ChatRequestParameters.builder()
                            .temperature(request.getTemperature())
                            .maxTokens(request.getMaxTokens())
                            .build())
                    .build();
            
            // 调用模型
            ChatResponse response = chatModel.chat(chatRequest);
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("[LLM-Qwen] 调用完成，traceId: {}, duration: {}ms", traceId, duration);
            
            // 构建响应
            TokenUsage tokenUsage = response.metadata().tokenUsage();
            return LlmChatResponse.builder()
                    .content(response.aiMessage().text())
                    .totalTokens(tokenUsage != null ? tokenUsage.totalTokenCount() : null)
                    .promptTokens(tokenUsage != null ? tokenUsage.inputTokenCount() : null)
                    .completionTokens(tokenUsage != null ? tokenUsage.outputTokenCount() : null)
                    .model(properties.getLlm().getModel())
                    .success(true)
                    .durationMs(duration)
                    .build();
                    
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[LLM-Qwen] 调用失败，traceId: {}, duration: {}ms, error: {}", 
                     traceId, duration, e.getMessage(), e);
            
            return LlmChatResponse.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .durationMs(duration)
                    .build();
        }
    }
    
    @Override
    public void streamingChat(LlmChatRequest request, LlmStreamingCallback callback) {
        long startTime = System.currentTimeMillis();
        String traceId = request.getTraceId() != null ? request.getTraceId() : "unknown";
        
        try {
            log.info("[LLM-Qwen-Streaming] 开始调用，traceId: {}", traceId);
            
            // 构建消息
            List<ChatMessage> messages = buildMessages(request);
            
            // 构建请求
            ChatRequest chatRequest = ChatRequest.builder()
                    .messages(messages)
                    .parameters(ChatRequestParameters.builder()
                            .temperature(request.getTemperature())
                            .maxTokens(request.getMaxTokens())
                            .build())
                    .build();
            
            // 流式调用
            streamingChatModel.chat(chatRequest, new StreamingChatLanguageModel.Callback() {
                @Override
                public void onNext(String token) {
                    callback.onNext(token);
                }
                
                @Override
                public void onComplete(ChatResponse response) {
                    long duration = System.currentTimeMillis() - startTime;
                    log.info("[LLM-Qwen-Streaming] 调用完成，traceId: {}, duration: {}ms", traceId, duration);
                    
                    TokenUsage tokenUsage = response.metadata().tokenUsage();
                    LlmChatResponse llmResponse = LlmChatResponse.builder()
                            .content(response.aiMessage().text())
                            .totalTokens(tokenUsage != null ? tokenUsage.totalTokenCount() : null)
                            .promptTokens(tokenUsage != null ? tokenUsage.inputTokenCount() : null)
                            .completionTokens(tokenUsage != null ? tokenUsage.outputTokenCount() : null)
                            .model(properties.getLlm().getModel())
                            .success(true)
                            .durationMs(duration)
                            .build();
                    
                    callback.onComplete(llmResponse);
                }
                
                @Override
                public void onError(Throwable error) {
                    long duration = System.currentTimeMillis() - startTime;
                    log.error("[LLM-Qwen-Streaming] 调用失败，traceId: {}, duration: {}ms, error: {}", 
                             traceId, duration, error.getMessage(), error);
                    
                    callback.onError(error);
                }
            });
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[LLM-Qwen-Streaming] 初始化失败，traceId: {}, duration: {}ms, error: {}", 
                     traceId, duration, e.getMessage(), e);
            
            callback.onError(e);
        }
    }
    
    private List<ChatMessage> buildMessages(LlmChatRequest request) {
        List<ChatMessage> messages = new ArrayList<>();
        
        // 添加系统提示词
        if (request.getSystemPrompt() != null) {
            messages.add(SystemMessage.from(request.getSystemPrompt()));
        }
        
        // 添加历史对话
        if (request.getConversationHistory() != null && !request.getConversationHistory().isEmpty()) {
            for (Map<String, String> historyItem : request.getConversationHistory()) {
                String role = historyItem.get("role");
                String content = historyItem.get("content");
                
                if ("user".equals(role)) {
                    messages.add(UserMessage.from(content));
                } else if ("assistant".equals(role)) {
                    messages.add(AiMessage.from(content));
                }
            }
        }
        
        // 添加当前用户消息
        messages.add(UserMessage.from(request.getUserMessage()));
        
        return messages;
    }
    
    @Override
    public String getClientId() {
        return "qwen-client";
    }
}
```

#### 2.2 GLM 模型实现

```java
// 文件：smartcrew-modules/src/main/java/com/smartcrew/agent/core/llm/GlmLlmClient.java
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "smartcrew.llm", name = "provider", havingValue = "glm")
public class GlmLlmClient implements LlmClient {
    
    private final SmartCrewProperties properties;
    private ChatLanguageModel chatModel;
    private StreamingChatLanguageModel streamingChatModel;
    
    @PostConstruct
    public void initialize() {
        SmartCrewProperties.Llm llmConfig = properties.getLlm();
        if (llmConfig.isEnabled()) {
            this.chatModel = ZhipuAiChatModel.builder()
                    .apiKey(llmConfig.getApiKey())
                    .modelName(llmConfig.getModel())
                    .baseUrl(llmConfig.getBaseUrl())
                    .temperature(0.7)
                    .timeout(Duration.ofSeconds(30))
                    .build();
            
            this.streamingChatModel = ZhipuAiStreamingChatModel.builder()
                    .apiKey(llmConfig.getApiKey())
                    .modelName(llmConfig.getModel())
                    .baseUrl(llmConfig.getBaseUrl())
                    .temperature(0.7)
                    .timeout(Duration.ofSeconds(60))
                    .build();
            
            log.info("GLM LLM 客户端初始化完成，模型：{}", llmConfig.getModel());
        }
    }
    
    @Override
    public LlmChatResponse chat(LlmChatRequest request) {
        long startTime = System.currentTimeMillis();
        String traceId = request.getTraceId() != null ? request.getTraceId() : "unknown";
        
        try {
            log.info("[LLM-GLM] 开始调用，traceId: {}, message: {}", traceId, request.getUserMessage());
            
            // 构建消息
            List<ChatMessage> messages = buildMessages(request);
            
            // 构建请求
            ChatRequest chatRequest = ChatRequest.builder()
                    .messages(messages)
                    .parameters(ChatRequestParameters.builder()
                            .temperature(request.getTemperature())
                            .maxTokens(request.getMaxTokens())
                            .build())
                    .build();
            
            // 调用模型
            ChatResponse response = chatModel.chat(chatRequest);
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("[LLM-GLM] 调用完成，traceId: {}, duration: {}ms", traceId, duration);
            
            // 构建响应
            TokenUsage tokenUsage = response.metadata().tokenUsage();
            return LlmChatResponse.builder()
                    .content(response.aiMessage().text())
                    .totalTokens(tokenUsage != null ? tokenUsage.totalTokenCount() : null)
                    .promptTokens(tokenUsage != null ? tokenUsage.inputTokenCount() : null)
                    .completionTokens(tokenUsage != null ? tokenUsage.outputTokenCount() : null)
                    .model(properties.getLlm().getModel())
                    .success(true)
                    .durationMs(duration)
                    .build();
                    
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[LLM-GLM] 调用失败，traceId: {}, duration: {}ms, error: {}", 
                     traceId, duration, e.getMessage(), e);
            
            return LlmChatResponse.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .durationMs(duration)
                    .build();
        }
    }
    
    @Override
    public void streamingChat(LlmChatRequest request, LlmStreamingCallback callback) {
        long startTime = System.currentTimeMillis();
        String traceId = request.getTraceId() != null ? request.getTraceId() : "unknown";
        
        try {
            log.info("[LLM-GLM-Streaming] 开始调用，traceId: {}", traceId);
            
            // 构建消息
            List<ChatMessage> messages = buildMessages(request);
            
            // 构建请求
            ChatRequest chatRequest = ChatRequest.builder()
                    .messages(messages)
                    .parameters(ChatRequestParameters.builder()
                            .temperature(request.getTemperature())
                            .maxTokens(request.getMaxTokens())
                            .build())
                    .build();
            
            // 流式调用
            streamingChatModel.chat(chatRequest, new StreamingChatLanguageModel.Callback() {
                @Override
                public void onNext(String token) {
                    callback.onNext(token);
                }
                
                @Override
                public void onComplete(ChatResponse response) {
                    long duration = System.currentTimeMillis() - startTime;
                    log.info("[LLM-GLM-Streaming] 调用完成，traceId: {}, duration: {}ms", traceId, duration);
                    
                    TokenUsage tokenUsage = response.metadata().tokenUsage();
                    LlmChatResponse llmResponse = LlmChatResponse.builder()
                            .content(response.aiMessage().text())
                            .totalTokens(tokenUsage != null ? tokenUsage.totalTokenCount() : null)
                            .promptTokens(tokenUsage != null ? tokenUsage.inputTokenCount() : null)
                            .completionTokens(tokenUsage != null ? tokenUsage.outputTokenCount() : null)
                            .model(properties.getLlm().getModel())
                            .success(true)
                            .durationMs(duration)
                            .build();
                    
                    callback.onComplete(llmResponse);
                }
                
                @Override
                public void onError(Throwable error) {
                    long duration = System.currentTimeMillis() - startTime;
                    log.error("[LLM-GLM-Streaming] 调用失败，traceId: {}, duration: {}ms, error: {}", 
                             traceId, duration, error.getMessage(), error);
                    
                    callback.onError(error);
                }
            });
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[LLM-GLM-Streaming] 初始化失败，traceId: {}, duration: {}ms, error: {}", 
                     traceId, duration, e.getMessage(), e);
            
            callback.onError(e);
        }
    }
    
    private List<ChatMessage> buildMessages(LlmChatRequest request) {
        List<ChatMessage> messages = new ArrayList<>();
        
        // 添加系统提示词
        if (request.getSystemPrompt() != null) {
            messages.add(SystemMessage.from(request.getSystemPrompt()));
        }
        
        // 添加历史对话
        if (request.getConversationHistory() != null && !request.getConversationHistory().isEmpty()) {
            for (Map<String, String> historyItem : request.getConversationHistory()) {
                String role = historyItem.get("role");
                String content = historyItem.get("content");
                
                if ("user".equals(role)) {
                    messages.add(UserMessage.from(content));
                } else if ("assistant".equals(role)) {
                    messages.add(AiMessage.from(content));
                }
            }
        }
        
        // 添加当前用户消息
        messages.add(UserMessage.from(request.getUserMessage()));
        
        return messages;
    }
    
    @Override
    public String getClientId() {
        return "glm-client";
    }
}
```

#### 2.3 模型配置示例

**通义千问配置**

```yaml
# application.yml - 通义千问配置
smartcrew:
  llm:
    enabled: true
    provider: qwen
    baseUrl: https://dashscope.aliyuncs.com/api/v1
    apiKey: your-dashscope-api-key
    model: qwen-plus
```

**GLM 配置**

```yaml
# application.yml - GLM 配置
smartcrew:
  llm:
    enabled: true
    provider: glm
    baseUrl: https://open.bigmodel.cn/api/mock_gpt
    apiKey: your-zhipu-api-key
    model: glm-4
```

### 8.2 工具调用集成

**工具调用是 LangChain4j 的核心功能之一，允许 AI 调用外部工具来获取信息、执行操作等。以下是完整的工具调用流程：**

#### 8.2.1 工具定义

**步骤1：创建工具接口**

```java
// 文件：smartcrew-modules/src/main/java/com/smartcrew/agent/core/tool/SmartCrewTool.java
public interface SmartCrewTool {
    
    /**
     * 工具唯一标识
     */
    String toolCode();
    
    /**
     * 工具描述
     */
    String description();
    
    /**
     * 工具版本
     */
    default String version() {
        return "1.0.0";
    }
}
```

**步骤2：实现具体工具**

```java
// 文件：smartcrew-modules/src/main/java/com/smartcrew/agent/core/tool/FileTools.java
@Slf4j
@Component
public class FileTools implements SmartCrewTool {
    
    @Override
    public String toolCode() {
        return "file_tools";
    }
    
    @Override
    public String description() {
        return "文件操作工具，用于读取、写入和管理文件";
    }
    
    /**
     * 读取文件内容
     * @param filePath 文件路径
     * @return 文件内容
     */
    @Tool("Read a file from the tool workspace")
    public String readFile(
            @P("filePath") @Description("The path to the file to read") String filePath) {
        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                return "File not found: " + filePath;
            }
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Error reading file: {}", filePath, e);
            return "Error reading file: " + e.getMessage();
        }
    }
    
    /**
     * 写入文件内容
     * @param filePath 文件路径
     * @param content 文件内容
     * @return 操作结果
     */
    @Tool("Write content to a file")
    public String writeFile(
            @P("filePath") @Description("The path to the file to write") String filePath,
            @P("content") @Description("The content to write to the file") String content) {
        try {
            Path path = Paths.get(filePath);
            Files.createDirectories(path.getParent());
            Files.writeString(path, content, StandardCharsets.UTF_8);
            return "File written successfully: " + filePath;
        } catch (Exception e) {
            log.error("Error writing file: {}", filePath, e);
            return "Error writing file: " + e.getMessage();
        }
    }
}
```

#### 8.2.2 工具注册

**步骤1：创建工具注册服务**

```java
// 文件：smartcrew-modules/src/main/java/com/smartcrew/agent/core/tool/ToolRegistryServiceImpl.java
@Service
public class ToolRegistryServiceImpl implements ToolRegistry {
    
    private final Map<String, SmartCrewTool> toolMap = new ConcurrentHashMap<>();
    private final List<SmartCrewTool> tools;
    
    @Autowired
    public ToolRegistryServiceImpl(List<SmartCrewTool> tools) {
        this.tools = tools;
    }
    
    // 注册工具
    @PostConstruct
    public void registerTools() {
        for (SmartCrewTool tool : tools) {
            register(tool);
        }
        log.info("Tool registry initialized with {} tools", toolMap.size());
    }
    
    @Override
    public void register(SmartCrewTool tool) {
        toolMap.put(tool.toolCode(), tool);
        log.info("Registered tool: {}", tool.toolCode());
    }
    
    @Override
    public Optional<SmartCrewTool> get(String toolCode) {
        return Optional.ofNullable(toolMap.get(toolCode));
    }
    
    @Override
    public List<SmartCrewTool> getAll() {
        return new ArrayList<>(toolMap.values());
    }
    
    @Override
    public List<ToolSpecification> getToolSpecifications() {
        List<ToolSpecification> specifications = new ArrayList<>();
        for (SmartCrewTool tool : toolMap.values()) {
            specifications.addAll(convertToToolSpecifications(tool));
        }
        return specifications;
    }
    
    private List<ToolSpecification> convertToToolSpecifications(SmartCrewTool tool) {
        List<ToolSpecification> specifications = new ArrayList<>();
        
        // 反射获取工具方法
        for (Method method : tool.getClass().getDeclaredMethods()) {
            Tool toolAnnotation = method.getAnnotation(Tool.class);
            if (toolAnnotation != null) {
                ToolSpecification specification = buildToolSpecification(tool, method, toolAnnotation);
                specifications.add(specification);
            }
        }
        
        return specifications;
    }
    
    private ToolSpecification buildToolSpecification(SmartCrewTool tool, Method method, Tool toolAnnotation) {
        // 构建参数 schema
        JsonSchema parameters = buildParametersSchema(method);
        
        return ToolSpecification.builder()
                .name(tool.toolCode() + "." + method.getName())
                .description(toolAnnotation.value())
                .parameters(parameters)
                .build();
    }
    
    private JsonSchema buildParametersSchema(Method method) {
        Map<String, JsonSchema> properties = new HashMap<>();
        List<String> requiredParameters = new ArrayList<>();
        
        for (Parameter parameter : method.getParameters()) {
            P paramAnnotation = parameter.getAnnotation(P.class);
            Description descAnnotation = parameter.getAnnotation(Description.class);
            
            if (paramAnnotation != null) {
                String paramName = paramAnnotation.value();
                String description = descAnnotation != null ? descAnnotation.value() : "";
                
                JsonSchema paramSchema = JsonSchema.builder()
                        .type(getJsonSchemaType(parameter.getType()))
                        .description(description)
                        .build();
                
                properties.put(paramName, paramSchema);
                requiredParameters.add(paramName);
            }
        }
        
        return JsonSchema.builder()
                .type(JsonSchema.Type.OBJECT)
                .properties(properties)
                .requiredParameters(requiredParameters)
                .build();
    }
    
    private JsonSchema.Type getJsonSchemaType(Class<?> type) {
        if (type == String.class) return JsonSchema.Type.STRING;
        if (type == int.class || type == Integer.class) return JsonSchema.Type.INTEGER;
        if (type == boolean.class || type == Boolean.class) return JsonSchema.Type.BOOLEAN;
        if (type == double.class || type == Double.class) return JsonSchema.Type.NUMBER;
        return JsonSchema.Type.STRING;
    }
}
```

#### 8.2.3 工具调用处理

**步骤1：在 ChatAgent 中添加工具支持**

```java
// 文件：smartcrew-modules/src/main/java/com/smartcrew/agent/core/agent/ChatAgent.java
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "smartcrew.llm", name = "enabled", havingValue = "true")
public class ChatAgent implements Agent {
    
    private final LlmClient llmClient;
    private final ConversationMemoryService memoryService;
    private final ToolRegistry toolRegistry;
    
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
        Long userId = command.getUserId();
        
        log.info("[ChatAgent] 处理请求，traceId: {}, userId: {}, message: {}", 
                traceId, userId, userMessage);
        
        // 构建 LLM 请求
        LlmChatRequest llmRequest = LlmChatRequest.builder()
                .userMessage(userMessage)
                .systemPrompt("你是一个智能对话助手，
                请用简洁、友好的方式回答用户的问题。
                如果不确定答案，请如实告知。
                当需要使用工具获取信息时，请使用可用的工具。")
                .conversationHistory(loadConversationHistory(userId))
                .temperature(0.7)
                .maxTokens(1000)
                .traceId(traceId)
                .toolSpecifications(toolRegistry.getToolSpecifications())
                .build();
        
        // 调用 LLM
        LlmChatResponse llmResponse = llmClient.chat(llmRequest);
        
        if (llmResponse.getSuccess()) {
            log.info("[ChatAgent] LLM 响应成功，traceId: {}, tokens: {}", 
                    traceId, llmResponse.getTotalTokens());
            
            // 检查是否需要执行工具
            if (llmResponse.hasToolExecutionRequests()) {
                return handleToolExecution(command, llmResponse, userId);
            } else {
                // 保存对话历史
                saveConversationHistory(userId, userMessage, llmResponse.getContent());
                
                return AgentDispatchResponse.builder()
                        .traceId(traceId)
                        .agentCode(code())
                        .accepted(true)
                        .message(llmResponse.getContent())
                        .build();
            }
        } else {
            log.error("[ChatAgent] LLM 请求失败，traceId: {}, error: {}", 
                     traceId, llmResponse.getErrorMessage());
            
            return AgentDispatchResponse.builder()
                    .traceId(traceId)
                    .agentCode(code())
                    .accepted(false)
                    .message("抱歉，我现在无法处理您的请求，请稍后再试。")
                    .build();
        }
    }
    
    private AgentDispatchResponse handleToolExecution(AgentDispatchCommand command, 
                                                   LlmChatResponse llmResponse, 
                                                   Long userId) {
        String traceId = command.getTraceId();
        String userMessage = command.getMessage();
        
        log.info("[ChatAgent] 处理工具调用，traceId: {}", traceId);
        
        try {
            // 执行工具
            List<ToolExecutionResultMessage> toolResults = executeTools(llmResponse.getToolExecutionRequests());
            
            // 构建包含工具结果的消息
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(SystemMessage.from("你是一个智能对话助手，
                    请用简洁、友好的方式回答用户的问题。
                    如果不确定答案，请如实告知。"));
            messages.add(UserMessage.from(userMessage));
            messages.add(AiMessage.from(llmResponse.getContent())); // AI 的工具调用请求
            
            // 添加工具执行结果
            for (ToolExecutionResultMessage resultMessage : toolResults) {
                messages.add(resultMessage);
            }
            
            // 再次调用 LLM 获取最终回复
            LlmChatRequest followUpRequest = LlmChatRequest.builder()
                    .userMessage("") // 空消息，只是为了触发后续处理
                    .systemPrompt("你是一个智能对话助手，
                    请用简洁、友好的方式回答用户的问题。
                    如果不确定答案，请如实告知。")
                    .temperature(0.7)
                    .maxTokens(1000)
                    .traceId(traceId)
                    .build();
            
            // 这里需要在 LlmClient 中添加一个方法来处理包含工具结果的消息
            LlmChatResponse finalResponse = llmClient.chatWithMessages(messages, followUpRequest);
            
            if (finalResponse.getSuccess()) {
                // 保存对话历史
                saveConversationHistory(userId, userMessage, finalResponse.getContent());
                
                return AgentDispatchResponse.builder()
                        .traceId(traceId)
                        .agentCode(code())
                        .accepted(true)
                        .message(finalResponse.getContent())
                        .build();
            } else {
                log.error("[ChatAgent] 工具调用后 LLM 请求失败，traceId: {}, error: {}", 
                         traceId, finalResponse.getErrorMessage());
                
                return AgentDispatchResponse.builder()
                        .traceId(traceId)
                        .agentCode(code())
                        .accepted(false)
                        .message("抱歉，工具执行后无法获取最终回复，请稍后再试。")
                        .build();
            }
        } catch (Exception e) {
            log.error("[ChatAgent] 工具执行失败，traceId: {}", traceId, e);
            
            return AgentDispatchResponse.builder()
                    .traceId(traceId)
                    .agentCode(code())
                    .accepted(false)
                    .message("抱歉，工具执行失败，请稍后再试。")
                    .build();
        }
    }
    
    private List<ToolExecutionResultMessage> executeTools(List<ToolExecutionRequest> toolRequests) {
        List<ToolExecutionResultMessage> results = new ArrayList<>();
        
        for (ToolExecutionRequest request : toolRequests) {
            String toolName = request.name();
            Map<String, Object> arguments = request.arguments();
            
            log.info("[ChatAgent] 执行工具: {}, 参数: {}", toolName, arguments);
            
            // 解析工具名，格式为 "toolCode.methodName"
            String[] parts = toolName.split("\\.");
            if (parts.length != 2) {
                log.error("[ChatAgent] 无效的工具名格式: {}", toolName);
                ToolExecutionResultMessage errorResult = ToolExecutionResultMessage.builder()
                        .toolName(toolName)
                        .result("Error: Invalid tool name format")
                        .build();
                results.add(errorResult);
                continue;
            }
            
            String toolCode = parts[0];
            String methodName = parts[1];
            
            // 查找工具
            Optional<SmartCrewTool> optionalTool = toolRegistry.get(toolCode);
            if (!optionalTool.isPresent()) {
                log.error("[ChatAgent] 工具不存在: {}", toolCode);
                ToolExecutionResultMessage errorResult = ToolExecutionResultMessage.builder()
                        .toolName(toolName)
                        .result("Error: Tool not found")
                        .build();
                results.add(errorResult);
                continue;
            }
            
            SmartCrewTool tool = optionalTool.get();
            
            try {
                // 执行工具方法
                Object result = executeToolMethod(tool, methodName, arguments);
                
                // 创建工具执行结果
                ToolExecutionResultMessage resultMessage = ToolExecutionResultMessage.builder()
                        .toolName(toolName)
                        .result(result != null ? result.toString() : "")
                        .build();
                
                results.add(resultMessage);
                log.info("[ChatAgent] 工具执行成功: {}", toolName);
                
            } catch (Exception e) {
                log.error("[ChatAgent] 工具执行异常: {}", toolName, e);
                ToolExecutionResultMessage errorResult = ToolExecutionResultMessage.builder()
                        .toolName(toolName)
                        .result("Error: " + e.getMessage())
                        .build();
                results.add(errorResult);
            }
        }
        
        return results;
    }
    
    private Object executeToolMethod(SmartCrewTool tool, String methodName, Map<String, Object> arguments) throws Exception {
        Method method = findMethod(tool.getClass(), methodName);
        if (method == null) {
            throw new Exception("Method not found: " + methodName);
        }
        
        // 准备参数
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];
        
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            P paramAnnotation = parameter.getAnnotation(P.class);
            
            if (paramAnnotation != null) {
                String paramName = paramAnnotation.value();
                Object argValue = arguments.get(paramName);
                
                if (argValue != null) {
                    // 类型转换
                    args[i] = convertToType(argValue, parameter.getType());
                } else {
                    throw new Exception("Missing required parameter: " + paramName);
                }
            }
        }
        
        // 执行方法
        return method.invoke(tool, args);
    }
    
    private Method findMethod(Class<?> clazz, String methodName) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                return method;
            }
        }
        return null;
    }
    
    private Object convertToType(Object value, Class<?> targetType) {
        if (value == null) return null;
        
        if (targetType == String.class) {
            return value.toString();
        } else if (targetType == int.class || targetType == Integer.class) {
            return Integer.parseInt(value.toString());
        } else if (targetType == boolean.class || targetType == Boolean.class) {
            return Boolean.parseBoolean(value.toString());
        } else if (targetType == double.class || targetType == Double.class) {
            return Double.parseDouble(value.toString());
        }
        
        return value;
    }
    
    private List<Map<String, String>> loadConversationHistory(Long userId) {
        // 现有代码...
    }
    
    private void saveConversationHistory(Long userId, String userMessage, String aiMessage) {
        // 现有代码...
    }
}
```

**步骤2：扩展 LlmClient 接口**

```java
// 文件：smartcrew-modules/src/main/java/com/smartcrew/agent/core/llm/LlmClient.java
public interface LlmClient {
    
    /**
     * 普通聊天调用
     */
    LlmChatResponse chat(LlmChatRequest request);
    
    /**
     * 流式聊天调用
     */
    void streamingChat(LlmChatRequest request, LlmStreamingCallback callback);
    
    /**
     * 带消息列表的聊天调用（用于工具执行后）
     */
    default LlmChatResponse chatWithMessages(List<ChatMessage> messages, LlmChatRequest request) {
        // 默认实现，子类可以重写
        LlmChatRequest newRequest = LlmChatRequest.builder()
                .userMessage("")
                .systemPrompt(request.getSystemPrompt())
                .temperature(request.getTemperature())
                .maxTokens(request.getMaxTokens())
                .traceId(request.getTraceId())
                .build();
        
        // 这里需要在具体实现中处理消息列表
        return chat(newRequest);
    }
    
    /**
     * 获取客户端 ID
     */
    String getClientId();
}
```

**步骤3：在具体实现中添加消息列表支持**

```java
// 在 QwenLlmClient 和 GlmLlmClient 中添加 chatWithMessages 方法实现
@Override
public LlmChatResponse chatWithMessages(List<ChatMessage> messages, LlmChatRequest request) {
    long startTime = System.currentTimeMillis();
    String traceId = request.getTraceId() != null ? request.getTraceId() : "unknown";
    
    try {
        log.info("[LLM-{}] 开始调用（带消息列表），traceId: {}", getClientId(), traceId);
        
        // 构建请求
        ChatRequest chatRequest = ChatRequest.builder()
                .messages(messages)
                .parameters(ChatRequestParameters.builder()
                        .temperature(request.getTemperature())
                        .maxTokens(request.getMaxTokens())
                        .build())
                .build();
        
        // 调用模型
        ChatResponse response = chatModel.chat(chatRequest);
        
        long duration = System.currentTimeMillis() - startTime;
        log.info("[LLM-{}] 调用完成，traceId: {}, duration: {}ms", getClientId(), traceId, duration);
        
        // 构建响应
        TokenUsage tokenUsage = response.metadata().tokenUsage();
        return LlmChatResponse.builder()
                .content(response.aiMessage().text())
                .totalTokens(tokenUsage != null ? tokenUsage.totalTokenCount() : null)
                .promptTokens(tokenUsage != null ? tokenUsage.inputTokenCount() : null)
                .completionTokens(tokenUsage != null ? tokenUsage.outputTokenCount() : null)
                .model(properties.getLlm().getModel())
                .success(true)
                .durationMs(duration)
                .build();
                
    } catch (Exception e) {
        long duration = System.currentTimeMillis() - startTime;
        log.error("[LLM-{}] 调用失败，traceId: {}, duration: {}ms, error: {}", 
                 getClientId(), traceId, duration, e.getMessage(), e);
        
        return LlmChatResponse.builder()
                .success(false)
                .errorMessage(e.getMessage())
                .durationMs(duration)
                .build();
    }
}
```

#### 8.2.4 工具调用示例

**示例1：使用文件工具读取文件**

```java
// 用户输入："请读取当前目录下的 README.md 文件内容"

// LLM 生成工具调用请求
ToolExecutionRequest toolRequest = ToolExecutionRequest.builder()
        .name("file_tools.readFile")
        .arguments(Map.of("filePath", "README.md"))
        .build();

// 执行工具
Object result = executeToolMethod(fileTools, "readFile", Map.of("filePath", "README.md"));

// 工具执行结果
ToolExecutionResultMessage resultMessage = ToolExecutionResultMessage.builder()
        .toolName("file_tools.readFile")
        .result("# SmartCrew Agent\n\n智能助手系统...")
        .build();

// 再次调用 LLM 获取最终回复
ChatResponse finalResponse = model.chat(Arrays.asList(
        SystemMessage.from("你是一个智能对话助手..."),
        UserMessage.from("请读取当前目录下的 README.md 文件内容"),
        AiMessage.from("我需要使用文件工具来读取 README.md 文件"),
        resultMessage
));

// 最终回复
System.out.println(finalResponse.aiMessage().text());
// 输出：README.md 文件的内容如下：
// # SmartCrew Agent
// 
// 智能助手系统...
```

**示例2：使用文件工具写入文件**

```java
// 用户输入："请创建一个名为 test.txt 的文件，内容为 'Hello, SmartCrew!'"

// LLM 生成工具调用请求
ToolExecutionRequest toolRequest = ToolExecutionRequest.builder()
        .name("file_tools.writeFile")
        .arguments(Map.of(
                "filePath", "test.txt",
                "content", "Hello, SmartCrew!"
        ))
        .build();

// 执行工具
Object result = executeToolMethod(fileTools, "writeFile", Map.of(
        "filePath", "test.txt",
        "content", "Hello, SmartCrew!"
));

// 工具执行结果
ToolExecutionResultMessage resultMessage = ToolExecutionResultMessage.builder()
        .toolName("file_tools.writeFile")
        .result("File written successfully: test.txt")
        .build();

// 再次调用 LLM 获取最终回复
ChatResponse finalResponse = model.chat(Arrays.asList(
        SystemMessage.from("你是一个智能对话助手..."),
        UserMessage.from("请创建一个名为 test.txt 的文件，内容为 'Hello, SmartCrew!'"),
        AiMessage.from("我需要使用文件工具来创建 test.txt 文件"),
        resultMessage
));

// 最终回复
System.out.println(finalResponse.aiMessage().text());
// 输出：文件已成功创建，内容为 "Hello, SmartCrew!"
```

### 8.3 项目集成示例

**1. 完整的 Spring Boot 集成**

```java
// 文件：smartcrew-modules/src/main/java/com/smartcrew/agent/config/LangChain4jConfig.java
@Configuration
public class LangChain4jConfig {
    
    @Bean
    @ConditionalOnProperty(prefix = "smartcrew.llm", name = "enabled", havingValue = "true")
    public ChatLanguageModel chatLanguageModel(SmartCrewProperties properties) {
        SmartCrewProperties.Llm llmConfig = properties.getLlm();
        
        switch (llmConfig.getProvider()) {
            case "qwen":
                return QwenChatModel.builder()
                        .apiKey(llmConfig.getApiKey())
                        .modelName(llmConfig.getModel())
                        .baseUrl(llmConfig.getBaseUrl())
                        .temperature(0.7)
                        .timeout(Duration.ofSeconds(30))
                        .build();
            case "glm":
                return ZhipuAiChatModel.builder()
                        .apiKey(llmConfig.getApiKey())
                        .modelName(llmConfig.getModel())
                        .baseUrl(llmConfig.getBaseUrl())
                        .temperature(0.7)
                        .timeout(Duration.ofSeconds(30))
                        .build();
            case "openai":
            default:
                return OpenAiChatModel.builder()
                        .apiKey(llmConfig.getApiKey())
                        .modelName(llmConfig.getModel())
                        .baseUrl(llmConfig.getBaseUrl())
                        .temperature(0.7)
                        .timeout(Duration.ofSeconds(30))
                        .build();
        }
    }
    
    @Bean
    @ConditionalOnProperty(prefix = "smartcrew.llm", name = "enabled", havingValue = "true")
    public StreamingChatLanguageModel streamingChatLanguageModel(SmartCrewProperties properties) {
        SmartCrewProperties.Llm llmConfig = properties.getLlm();
        
        switch (llmConfig.getProvider()) {
            case "qwen":
                return QwenStreamingChatModel.builder()
                        .apiKey(llmConfig.getApiKey())
                        .modelName(llmConfig.getModel())
                        .baseUrl(llmConfig.getBaseUrl())
                        .temperature(0.7)
                        .timeout(Duration.ofSeconds(60))
                        .build();
            case "glm":
                return ZhipuAiStreamingChatModel.builder()
                        .apiKey(llmConfig.getApiKey())
                        .modelName(llmConfig.getModel())
                        .baseUrl(llmConfig.getBaseUrl())
                        .temperature(0.7)
                        .timeout(Duration.ofSeconds(60))
                        .build();
            case "openai":
            default:
                return OpenAiStreamingChatModel.builder()
                        .apiKey(llmConfig.getApiKey())
                        .modelName(llmConfig.getModel())
                        .baseUrl(llmConfig.getBaseUrl())
                        .temperature(0.7)
                        .timeout(Duration.ofSeconds(60))
                        .build();
        }
    }
    
    @Bean
    public ToolRegistry toolRegistry(List<SmartCrewTool> tools) {
        return new ToolRegistryServiceImpl(tools);
    }
    
    @Bean
    public LlmCacheService llmCacheService() {
        return new LlmCacheService();
    }
}
```

**2. 使用 LangChain4j 实现智能客服**

```java
// 文件：smartcrew-modules/src/main/java/com/smartcrew/agent/core/agent/CustomerServiceAgent.java
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "smartcrew.llm", name = "enabled", havingValue = "true")
public class CustomerServiceAgent implements Agent {
    
    private final LlmClient llmClient;
    private final ConversationMemoryService memoryService;
    private final ToolRegistry toolRegistry;
    private final LlmCacheService cacheService;
    
    @Override
    public String code() {
        return "customer-service-agent";
    }
    
    @Override
    public String name() {
        return "智能客服助手";
    }
    
    @Override
    public boolean supports(String capability) {
        return "customer_service".equalsIgnoreCase(capability) || "support".equalsIgnoreCase(capability);
    }
    
    @Override
    public AgentDispatchResponse handle(AgentDispatchCommand command) {
        String traceId = command.getTraceId();
        String userMessage = command.getMessage();
        Long userId = command.getUserId();
        
        log.info("[CustomerServiceAgent] 处理请求，traceId: {}, userId: {}, message: {}", 
                traceId, userId, userMessage);
        
        // 尝试从缓存获取响应
        String cacheKey = generateCacheKey(userId, userMessage);
        Optional<String> cachedResponse = cacheService.getCachedResponse(cacheKey);
        if (cachedResponse.isPresent()) {
            log.info("[CustomerServiceAgent] 从缓存获取响应，traceId: {}", traceId);
            return AgentDispatchResponse.builder()
                    .traceId(traceId)
                    .agentCode(code())
                    .accepted(true)
                    .message(cachedResponse.get())
                    .build();
        }
        
        // 构建 LLM 请求
        LlmChatRequest llmRequest = LlmChatRequest.builder()
                .userMessage(userMessage)
                .systemPrompt("你是一个专业的智能客服助手，
                请用友好、专业的语言回答用户的问题。
                如果你不确定答案，请如实告知，并提供相关的联系方式。
                当需要查询用户信息或订单状态时，请使用可用的工具。")
                .conversationHistory(loadConversationHistory(userId))
                .temperature(0.7)
                .maxTokens(1000)
                .traceId(traceId)
                .toolSpecifications(toolRegistry.getToolSpecifications())
                .build();
        
        // 调用 LLM
        LlmChatResponse llmResponse = llmClient.chat(llmRequest);
        
        if (llmResponse.getSuccess()) {
            log.info("[CustomerServiceAgent] LLM 响应成功，traceId: {}, tokens: {}", 
                    traceId, llmResponse.getTotalTokens());
            
            // 缓存响应
            cacheService.cacheResponse(cacheKey, llmResponse.getContent());
            
            // 保存对话历史
            saveConversationHistory(userId, userMessage, llmResponse.getContent());
            
            return AgentDispatchResponse.builder()
                    .traceId(traceId)
                    .agentCode(code())
                    .accepted(true)
                    .message(llmResponse.getContent())
                    .build();
        } else {
            log.error("[CustomerServiceAgent] LLM 请求失败，traceId: {}, error: {}", 
                     traceId, llmResponse.getErrorMessage());
            
            return AgentDispatchResponse.builder()
                    .traceId(traceId)
                    .agentCode(code())
                    .accepted(false)
                    .message("抱歉，我现在无法处理您的请求，请稍后再试。")
                    .build();
        }
    }
    
    private String generateCacheKey(Long userId, String message) {
        return userId + "_" + message.hashCode();
    }
    
    private List<Map<String, String>> loadConversationHistory(Long userId) {
        // 现有代码...
    }
    
    private void saveConversationHistory(Long userId, String userMessage, String aiMessage) {
        // 现有代码...
    }
}
```

**3. 实现流式响应的 WebSocket 接口**

```java
// 文件：smartcrew-modules/src/main/java/com/smartcrew/agent/web/websocket/ChatWebSocketHandler.java
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {
    
    private final LlmClient llmClient;
    private final ConversationMemoryService memoryService;
    private final ToolRegistry toolRegistry;
    
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        sessions.put(sessionId, session);
        log.info("WebSocket 连接建立: {}", sessionId);
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();
        sessions.remove(sessionId);
        log.info("WebSocket 连接关闭: {}", sessionId);
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String sessionId = session.getId();
        String payload = message.getPayload();
        
        try {
            // 解析请求
            ChatWebSocketRequest request = objectMapper.readValue(payload, ChatWebSocketRequest.class);
            Long userId = request.getUserId();
            String userMessage = request.getMessage();
            String traceId = UUID.randomUUID().toString();
            
            log.info("[WebSocket] 接收消息，sessionId: {}, userId: {}, message: {}", 
                    sessionId, userId, userMessage);
            
            // 构建 LLM 请求
            LlmChatRequest llmRequest = LlmChatRequest.builder()
                    .userMessage(userMessage)
                    .systemPrompt("你是一个智能对话助手，
                    请用简洁、友好的方式回答用户的问题。
                    如果不确定答案，请如实告知。")
                    .conversationHistory(loadConversationHistory(userId))
                    .temperature(0.7)
                    .maxTokens(1000)
                    .traceId(traceId)
                    .toolSpecifications(toolRegistry.getToolSpecifications())
                    .build();
            
            // 流式调用
            llmClient.streamingChat(llmRequest, new LlmStreamingCallback() {
                @Override
                public void onNext(String token) {
                    try {
                        ChatWebSocketResponse response = ChatWebSocketResponse.builder()
                                .type("token")
                                .content(token)
                                .traceId(traceId)
                                .build();
                        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
                    } catch (Exception e) {
                        log.error("[WebSocket] 发送消息失败", e);
                    }
                }
                
                @Override
                public void onComplete(LlmChatResponse llmResponse) {
                    try {
                        // 保存对话历史
                        saveConversationHistory(userId, userMessage, llmResponse.getContent());
                        
                        ChatWebSocketResponse response = ChatWebSocketResponse.builder()
                                .type("complete")
                                .content(llmResponse.getContent())
                                .traceId(traceId)
                                .tokens(llmResponse.getTotalTokens())
                                .build();
                        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
                    } catch (Exception e) {
                        log.error("[WebSocket] 发送完成消息失败", e);
                    }
                }
                
                @Override
                public void onError(Throwable error) {
                    try {
                        ChatWebSocketResponse response = ChatWebSocketResponse.builder()
                                .type("error")
                                .content("抱歉，我现在无法处理您的请求，请稍后再试。")
                                .traceId(traceId)
                                .build();
                        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
                    } catch (Exception e) {
                        log.error("[WebSocket] 发送错误消息失败", e);
                    }
                }
            });
            
        } catch (Exception e) {
            log.error("[WebSocket] 处理消息失败", e);
            ChatWebSocketResponse response = ChatWebSocketResponse.builder()
                    .type("error")
                    .content("抱歉，处理您的请求时出现错误，请稍后再试。")
                    .build();
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
        }
    }
    
    private List<Map<String, String>> loadConversationHistory(Long userId) {
        // 现有代码...
    }
    
    private void saveConversationHistory(Long userId, String userMessage, String aiMessage) {
        // 现有代码...
    }
}
```

**4. 多模型切换与负载均衡**

```java
// 文件：smartcrew-modules/src/main/java/com/smartcrew/agent/core/llm/MultiLlmClient.java
@Slf4j
@Service
@RequiredArgsConstructor
public class MultiLlmClient implements LlmClient {
    
    private final List<LlmClient> llmClients;
    private final AtomicInteger counter = new AtomicInteger(0);
    
    @Override
    public LlmChatResponse chat(LlmChatRequest request) {
        // 轮询选择客户端
        int index = counter.getAndIncrement() % llmClients.size();
        LlmClient client = llmClients.get(index);
        
        log.info("[MultiLlmClient] 使用客户端: {}", client.getClientId());
        
        try {
            return client.chat(request);
        } catch (Exception e) {
            log.error("[MultiLlmClient] 客户端 {} 失败，尝试下一个", client.getClientId(), e);
            // 尝试下一个客户端
            for (int i = 1; i < llmClients.size(); i++) {
                int nextIndex = (index + i) % llmClients.size();
                LlmClient nextClient = llmClients.get(nextIndex);
                try {
                    log.info("[MultiLlmClient] 尝试客户端: {}", nextClient.getClientId());
                    return nextClient.chat(request);
                } catch (Exception ex) {
                    log.error("[MultiLlmClient] 客户端 {} 也失败", nextClient.getClientId(), ex);
                }
            }
            throw new ServiceException("所有 LLM 客户端都失败");
        }
    }
    
    @Override
    public void streamingChat(LlmChatRequest request, LlmStreamingCallback callback) {
        // 轮询选择客户端
        int index = counter.getAndIncrement() % llmClients.size();
        LlmClient client = llmClients.get(index);
        
        log.info("[MultiLlmClient] 使用客户端: {}", client.getClientId());
        
        try {
            client.streamingChat(request, callback);
        } catch (Exception e) {
            log.error("[MultiLlmClient] 客户端 {} 失败，尝试下一个", client.getClientId(), e);
            // 尝试下一个客户端
            for (int i = 1; i < llmClients.size(); i++) {
                int nextIndex = (index + i) % llmClients.size();
                LlmClient nextClient = llmClients.get(nextIndex);
                try {
                    log.info("[MultiLlmClient] 尝试客户端: {}", nextClient.getClientId());
                    nextClient.streamingChat(request, callback);
                    return;
                } catch (Exception ex) {
                    log.error("[MultiLlmClient] 客户端 {} 也失败", nextClient.getClientId(), ex);
                }
            }
            callback.onError(new ServiceException("所有 LLM 客户端都失败"));
        }
    }
    
    @Override
    public LlmChatResponse chatWithMessages(List<ChatMessage> messages, LlmChatRequest request) {
        // 轮询选择客户端
        int index = counter.getAndIncrement() % llmClients.size();
        LlmClient client = llmClients.get(index);
        
        log.info("[MultiLlmClient] 使用客户端: {}", client.getClientId());
        
        try {
            return client.chatWithMessages(messages, request);
        } catch (Exception e) {
            log.error("[MultiLlmClient] 客户端 {} 失败，尝试下一个", client.getClientId(), e);
            // 尝试下一个客户端
            for (int i = 1; i < llmClients.size(); i++) {
                int nextIndex = (index + i) % llmClients.size();
                LlmClient nextClient = llmClients.get(nextIndex);
                try {
                    log.info("[MultiLlmClient] 尝试客户端: {}", nextClient.getClientId());
                    return nextClient.chatWithMessages(messages, request);
                } catch (Exception ex) {
                    log.error("[MultiLlmClient] 客户端 {} 也失败", nextClient.getClientId(), ex);
                }
            }
            throw new ServiceException("所有 LLM 客户端都失败");
        }
    }
    
    @Override
    public String getClientId() {
        return "multi-llm-client";
    }
}
```

### 8.4 性能优化

**1. 缓存策略**

```java
// 缓存热点问答
@Service
public class LlmCacheService {
    private final Cache<String, String> responseCache;
    private final Cache<String, LlmChatResponse> fullResponseCache;
    
    public LlmCacheService() {
        this.responseCache = CacheBuilder.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(1, TimeUnit.HOURS)
                .build();
        
        this.fullResponseCache = CacheBuilder.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .build();
    }
    
    public Optional<String> getCachedResponse(String key) {
        return Optional.ofNullable(responseCache.getIfPresent(key));
    }
    
    public Optional<LlmChatResponse> getCachedFullResponse(String key) {
        return Optional.ofNullable(fullResponseCache.getIfPresent(key));
    }
    
    public void cacheResponse(String key, String response) {
        responseCache.put(key, response);
    }
    
    public void cacheFullResponse(String key, LlmChatResponse response) {
        fullResponseCache.put(key, response);
    }
    
    public void invalidateCache(String key) {
        responseCache.invalidate(key);
        fullResponseCache.invalidate(key);
    }
}
```

**2. 批量处理**

```java
// 批量处理多个请求
public List<LlmChatResponse> batchChat(List<LlmChatRequest> requests) {
    return requests.parallelStream()
            .map(this::chat)
            .collect(Collectors.toList());
}

// 带并发控制的批量处理
public List<LlmChatResponse> batchChatWithConcurrencyControl(List<LlmChatRequest> requests, int maxConcurrency) {
    Semaphore semaphore = new Semaphore(maxConcurrency);
    return requests.stream()
            .map(request -> {
                try {
                    semaphore.acquire();
                    try {
                        return chat(request);
                    } finally {
                        semaphore.release();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
            })
            .collect(Collectors.toList());
}
```

**3. 降级策略**

```java
// 降级到简单模型
public LlmChatResponse chatWithFallback(LlmChatRequest request) {
    try {
        return primaryClient.chat(request);
    } catch (Exception e) {
        log.warn("主模型失败，降级到备用模型", e);
        return fallbackClient.chat(request);
    }
}

// 多级降级策略
public LlmChatResponse chatWithMultiLevelFallback(LlmChatRequest request) {
    List<LlmClient> clients = Arrays.asList(primaryClient, secondaryClient, fallbackClient);
    
    for (int i = 0; i < clients.size(); i++) {
        LlmClient client = clients.get(i);
        try {
            log.info("尝试使用第 {} 级模型", i + 1);
            return client.chat(request);
        } catch (Exception e) {
            log.warn("第 {} 级模型失败，尝试下一级", i + 1, e);
            if (i == clients.size() - 1) {
                throw e;
            }
        }
    }
    
    throw new ServiceException("所有模型都失败");
}
```

**4. 异步处理**

```java
// 异步处理 LLM 请求
@Async
public CompletableFuture<LlmChatResponse> asyncChat(LlmChatRequest request) {
    return CompletableFuture.supplyAsync(() -> chat(request));
}

// 异步批量处理
@Async
public CompletableFuture<List<LlmChatResponse>> asyncBatchChat(List<LlmChatRequest> requests) {
    return CompletableFuture.supplyAsync(() -> batchChat(requests));
}
```

**5. Token 优化**

```java
// 计算消息的 Token 数
public int calculateTokenCount(String text) {
    // 使用模型特定的 Token 计算方法
    // 这里使用简单的估算方法，实际应该使用对应模型的 Tokenizer
    return text.length() / 4; // 粗略估算
}

// 智能截断历史对话
public List<ChatMessage> truncateHistory(List<ChatMessage> history, int maxTokens) {
    List<ChatMessage> truncated = new ArrayList<>();
    int totalTokens = 0;
    
    // 保留系统消息
    if (!history.isEmpty() && history.get(0) instanceof SystemMessage) {
        SystemMessage systemMsg = (SystemMessage) history.get(0);
        truncated.add(systemMsg);
        totalTokens += calculateTokenCount(systemMsg.text());
    }
    
    // 从最新的消息开始添加，直到达到 Token 限制
    for (int i = history.size() - 1; i >= 1; i--) {
        ChatMessage message = history.get(i);
        int messageTokens = calculateTokenCount(message.toString());
        
        if (totalTokens + messageTokens <= maxTokens) {
            truncated.add(1, message); // 添加到系统消息之后
            totalTokens += messageTokens;
        } else {
            break;
        }
    }
    
    log.info("历史对话截断后包含 {} 条消息，总 Token 数: {}", truncated.size(), totalTokens);
    return truncated;
}
```

**6. 连接池优化**

```java
// 自定义 HTTP 客户端配置
@Bean
public HttpClient httpClient() {
    return HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectionPool(new ConnectionPool(20, 30, TimeUnit.SECONDS))
            .build();
}

// 在 LLM 客户端中使用自定义 HTTP 客户端
public class CustomOpenAiChatModel extends OpenAiChatModel {
    public CustomOpenAiChatModel(Builder builder) {
        super(builder);
        // 替换默认的 HTTP 客户端
        this.httpClient = httpClient();
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder extends OpenAiChatModel.Builder {
        @Override
        public CustomOpenAiChatModel build() {
            return new CustomOpenAiChatModel(this);
        }
    }
}
```

**7. 限流策略**

```java
// 基于令牌桶的限流
@Service
public class RateLimiterService {
    private final RateLimiter rateLimiter;
    
    public RateLimiterService() {
        // 每秒最多 10 个请求
        this.rateLimiter = RateLimiter.create(10.0);
    }
    
    public <T> T withRateLimit(Supplier<T> supplier) {
        rateLimiter.acquire();
        return supplier.get();
    }
    
    public void withRateLimit(Runnable runnable) {
        rateLimiter.acquire();
        runnable.run();
    }
}

// 使用限流
public LlmChatResponse chatWithRateLimit(LlmChatRequest request) {
    return rateLimiterService.withRateLimit(() -> chat(request));
}
```

**8. 监控与指标**

```java
// 监控 LLM 调用
@Service
public class LlmMetricsService {
    private final MeterRegistry meterRegistry;
    private final Counter totalRequests;
    private final Counter successfulRequests;
    private final Counter failedRequests;
    private final Timer requestTimer;
    
    public LlmMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.totalRequests = Counter.builder("llm.requests.total")
                .description("Total LLM requests")
                .register(meterRegistry);
        this.successfulRequests = Counter.builder("llm.requests.successful")
                .description("Successful LLM requests")
                .register(meterRegistry);
        this.failedRequests = Counter.builder("llm.requests.failed")
                .description("Failed LLM requests")
                .register(meterRegistry);
        this.requestTimer = Timer.builder("llm.requests.duration")
                .description("LLM request duration")
                .register(meterRegistry);
    }
    
    public LlmChatResponse monitorChat(Supplier<LlmChatResponse> chatSupplier) {
        totalRequests.increment();
        
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            LlmChatResponse response = chatSupplier.get();
            if (response.getSuccess()) {
                successfulRequests.increment();
            } else {
                failedRequests.increment();
            }
            return response;
        } catch (Exception e) {
            failedRequests.increment();
            throw e;
        } finally {
            sample.stop(requestTimer);
        }
    }
}

// 使用监控
public LlmChatResponse monitoredChat(LlmChatRequest request) {
    return metricsService.monitorChat(() -> chat(request));
}
```

---

## 9. 常见问题与解决方案

### 9.1 问题：API Key 未配置

**错误信息**：`API key is not configured`

**解决方案**：
```java
// 方式一：环境变量
String apiKey = System.getenv("DASHSCOPE_API_KEY");

// 方式二：配置文件
@Value("${smartcrew.llm.api-key}")
private String apiKey;

// 方式三：启动时检查
if (apiKey == null || apiKey.isBlank()) {
    throw new ServiceException("请配置 DASHSCOPE_API_KEY 环境变量");
}
```

### 9.2 问题：响应超时

**错误信息**：`Timeout waiting for response`

**解决方案**：
```java
ChatLanguageModel model = QwenChatModel.builder()
        .apiKey(apiKey)
        .modelName("qwen-plus")
        .timeout(Duration.ofSeconds(60))  // 增加超时时间
        .build();
```

### 9.3 问题：Token 超限

**错误信息**：`Maximum context length exceeded`

**解决方案**：
```java
// 1. 减少输入内容
// 2. 使用支持更长上下文的模型
// 3. 截断历史对话

// 截断历史对话示例
if (history.size() > 20) {
    // 保留系统消息和最近 10 轮对话
    List<ChatMessage> truncated = new ArrayList<>();
    truncated.add(history.get(0));  // 系统消息
    truncated.addAll(history.subList(history.size() - 19, history.size()));
    history.clear();
    history.addAll(truncated);
}
```

### 9.4 问题：输出内容被截断

**原因**：达到 `maxTokens` 限制

**解决方案**：
```java
ChatRequestParameters parameters = ChatRequestParameters.builder()
        .maxTokens(2000)  // 增加最大 token 数
        .build();

// 或者检查结束原因
FinishReason reason = response.metadata().finishReason();
if (reason == FinishReason.LENGTH) {
    System.out.println("输出被截断，请增加 maxTokens");
}
```

---

## 10. 最佳实践

### 10.1 模型选择建议

| 场景 | 推荐模型 | 说明 |
| :--- | :--- | :--- |
| 简单问答 | qwen-turbo | 速度快，成本低 |
| 复杂推理 | qwen-plus | 平衡性能和成本 |
| 高质量输出 | qwen-max | 效果最好，成本较高 |
| 图片理解 | qwen-vl-plus | 支持多模态 |

### 10.2 参数配置建议

```java
// 创意写作场景
ChatRequestParameters creative = ChatRequestParameters.builder()
        .temperature(0.9)
        .topP(0.95)
        .build();

// 代码生成场景
ChatRequestParameters coding = ChatRequestParameters.builder()
        .temperature(0.1)
        .topP(0.9)
        .build();

// 数据分析场景
ChatRequestParameters analysis = ChatRequestParameters.builder()
        .temperature(0.3)
        .maxTokens(2000)
        .build();
```

### 10.3 错误处理建议

```java
public LlmChatResponse safeChat(ChatLanguageModel model, ChatRequest request) {
    try {
        ChatResponse response = model.chat(request);
        return LlmChatResponse.builder()
                .content(response.aiMessage().text())
                .success(true)
                .build();
    } catch (Exception e) {
        log.error("LLM 调用失败", e);
        return LlmChatResponse.builder()
                .success(false)
                .errorMessage(e.getMessage())
                .build();
    }
}
```

---

## 11. 总结

### 11.1 核心要点回顾

1. **ChatLanguageModel** 是与 LLM 交互的核心接口
2. **ChatMessage** 用于表示不同角色的消息
3. **ChatRequest** 用于配置请求参数
4. **ChatResponse** 包含 AI 回复和元数据
5. 多轮对话需要手动维护历史消息

### 11.2 学习路径建议

```
入门阶段：
├── 掌握 chat(String) 方法
├── 理解消息类型
└── 实现简单问答

进阶阶段：
├── 使用 ChatRequest 配置参数
├── 实现多轮对话
└── 处理错误和异常

高级阶段：
├── 工具调用（Function Calling）
├── 多模态输入
└── 流式输出
```

---

## 附录：常用代码片段

### A.1 快速创建模型

```java
ChatLanguageModel model = QwenChatModel.builder()
        .apiKey(System.getenv("DASHSCOPE_API_KEY"))
        .modelName("qwen-plus")
        .temperature(0.7)
        .timeout(Duration.ofSeconds(30))
        .build();
```

### A.2 快速发送消息

```java
String reply = model.chat("你的问题");
```

### A.3 带系统提示的对话

```java
ChatResponse response = model.chat(
    SystemMessage.from("系统提示"),
    UserMessage.from("用户消息")
);
```

### A.4 获取 Token 使用量

```java
TokenUsage usage = response.metadata().tokenUsage();
System.out.println("总 Token：" + usage.totalTokenCount());
```
