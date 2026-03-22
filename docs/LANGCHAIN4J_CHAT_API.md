# LangChain4j 聊天模型 API 详解

> 版本：v1.0  
> 适用版本：LangChain4j 1.0.0-beta2  
> 目标读者：Java 初学者、AI 应用开发者

---

## 1. 概述

### 1.1 什么是 ChatLanguageModel？

`ChatLanguageModel` 是 LangChain4j 框架中用于与大语言模型（LLM）进行对话交互的核心接口。它提供了一套标准化的 API，让开发者可以轻松地调用各种大模型（如 OpenAI 的 GPT 系列、阿里云的千问系列等），而无需关心底层实现细节。

### 1.2 为什么需要学习这个接口？

- **统一抽象**：无论使用哪个大模型供应商，API 调用方式一致
- **易于切换**：更换模型只需修改配置，无需改动业务代码
- **功能完整**：支持多轮对话、工具调用、多模态输入等高级功能

### 1.3 LLM API 类型对比

| API 类型 | 说明 | 状态 | 适用场景 |
| :--- | :--- | :--- | :--- |
| `LanguageModel` | 输入输出均为字符串 | 过时 | 简单文本生成 |
| `ChatLanguageModel` | 支持多消息、多模态 | 推荐 | 对话、工具调用、复杂任务 |

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

### 5.4 FinishReason 结束原因

| 值 | 说明 |
| :--- | :--- |
| `STOP` | 正常完成 |
| `LENGTH` | 达到最大 token 限制 |
| `TOOL_EXECUTION` | 需要执行工具 |
| `CONTENT_FILTER` | 内容被过滤 |
| `OTHER` | 其他原因 |

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
}

// 使用示例
ChatSession session = new ChatSession(model, "你是一个友好的助手");
System.out.println(session.chat("你好，我叫李四"));
System.out.println(session.chat("我叫什么名字？"));  // AI 会记住你叫李四
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

---

## 8. 常见问题与解决方案

### 8.1 问题：API Key 未配置

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

### 8.2 问题：响应超时

**错误信息**：`Timeout waiting for response`

**解决方案**：
```java
ChatLanguageModel model = QwenChatModel.builder()
        .apiKey(apiKey)
        .modelName("qwen-plus")
        .timeout(Duration.ofSeconds(60))  // 增加超时时间
        .build();
```

### 8.3 问题：Token 超限

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

### 8.4 问题：输出内容被截断

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

## 9. 最佳实践

### 9.1 模型选择建议

| 场景 | 推荐模型 | 说明 |
| :--- | :--- | :--- |
| 简单问答 | qwen-turbo | 速度快，成本低 |
| 复杂推理 | qwen-plus | 平衡性能和成本 |
| 高质量输出 | qwen-max | 效果最好，成本较高 |
| 图片理解 | qwen-vl-plus | 支持多模态 |

### 9.2 参数配置建议

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

### 9.3 错误处理建议

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

## 10. 总结

### 10.1 核心要点回顾

1. **ChatLanguageModel** 是与 LLM 交互的核心接口
2. **ChatMessage** 用于表示不同角色的消息
3. **ChatRequest** 用于配置请求参数
4. **ChatResponse** 包含 AI 回复和元数据
5. 多轮对话需要手动维护历史消息

### 10.2 学习路径建议

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
