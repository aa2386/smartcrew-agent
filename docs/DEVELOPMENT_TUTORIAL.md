# SmartCrew-Agent 开发教程

本教程按项目当前真实代码状态编写，目标不是泛泛介绍概念，而是让你可以按照步骤直接往下做，最终从“现有 LLM 调用能力”一步步落到“初始 Agent”再演进到“多 Agent 协作”。

本文档的宗旨是：

- 尽量复用项目现有基础设施
- 对必须重构的地方写清楚“为什么要改”和“怎么改”
- 对暂时不该进入主线的能力明确标记
- 每一阶段都给出关键代码块，尽量做到可照着实现

---

## 1. 先明确最终路线

你的规划是合理的，推荐主线就是下面两段：

### 第一阶段：做单一“初始 Agent”

先做一个真正能对外工作的 `InitialAgent`，具备：

- 基本聊天
- 提示词配置
- 长期偏好
- RAG
- 工具调用
- 接入企业微信或飞书

### 第二阶段：演进到多 Agent 协作

等 `InitialAgent` 稳定后，再让它变成 orchestrator：

- `InitialAgent` 继续直接和用户交流
- 简单任务自己处理
- 复杂任务拆解后交给专用 Agent
- 最后仍由 `InitialAgent` 汇总并返回给用户

为什么这样安排：

- 先做单 Agent，更容易把主链路跑通
- 先把会话、提示词、偏好、RAG、工具这些基础链路做扎实
- 多 Agent 最难的是拆解、协作、隔离、汇总，这些都依赖一个成熟的主入口 Agent

---

## 2. 项目现有基础设施全盘点

先把现有代码分成三类：直接复用、必须重构、暂不进入主线。

### 2.1 直接复用

这些能力当前就能用，不建议重写。

#### Agent 基础框架

- `Agent`
- `AgentRegistry`
- `InMemoryAgentRegistry`
- `AgentDiscoveryServiceImpl`
- `AgentCoordinatorImpl`
- `AgentController`

为什么直接复用：

- Agent Bean 的发现、注册、分发链路已经有了
- 你现在缺的不是“怎么注册 Agent”，而是“写一个真正有业务能力的 Agent”

#### LLM 能力

- `LlmClient`
- `LlmStreamingCallback`
- `DashScopeLlmClient`
- `LlmConversationStore`
- `LlmConversationStoreImpl`
- `llm_conversation_session`
- `llm_conversation_message`

为什么直接复用：

- 同步 chat 已经可用
- 流式 chat 已经可用
- 会话历史已经能入库
- 出错消息也能记录

这部分是当前项目最成熟的基础设施，后续所有 Agent 都应该建立在它上面。

#### Prompt 配置层

- `PromptTemplateService`
- `PromptTemplateServiceImpl`
- `PromptController`
- `prompt_template`

为什么直接复用：

- 提示词的配置入口已经有了
- 当前缺的是“运行时怎么把配置读出来并注入到 Agent”

#### 长期偏好和轻量记忆

- `UserPreferenceService`
- `ConversationMemoryService`
- `ConversationMemoryServiceImpl`
- `MemoryController`
- `user_preference`

为什么直接复用：

- 存用户长期偏好已经足够
- 不适合存短期聊天历史，但非常适合存用户画像和稳定偏好

#### 平台接入骨架

- `PlatformAdapter`
- `PlatformAdapterRegistry`
- `InMemoryPlatformAdapterRegistry`
- `PlatformController`

为什么只说“骨架可复用”：

- 框架结构已经对了
- 但 `WecomPlatformAdapter`、`FeishuPlatformAdapter` 目前还只是占位实现

#### 工具元数据层

- `SmartCrewTool`
- `ToolRegistry`
- `InMemoryToolRegistry`
- `ToolDefinitionService`
- `ToolDefinitionServiceImpl`
- `ToolController`
- `tool_definition`

为什么直接复用这层：

- 工具注册和元数据管理已经有了
- 目前缺的是执行链，不是元数据层

### 2.2 必须重构后再进入主线

#### `WecomPlatformAdapter` / `FeishuPlatformAdapter`

当前问题：

- 只返回 placeholder
- 没有转发给 `AgentCoordinator`
- 没有用户映射
- 没有 session 映射

结论：

- 保留类结构
- 必须改造成真正的平台接入器

#### `DefaultToolExecutor`

当前问题：

- 只是占位响应
- 没有真正执行工具逻辑
- 没有把 `tool_definition`、`agent_tool_binding` 接到执行链

结论：

- 工具体系不用推翻
- 但执行链必须重构

#### `AgentDefinitionService` / `agent_definition`

当前问题：

- 当前更像“配置注册层”
- `systemPrompt`、`configJson` 还不会自动作用到运行时
- 没有真实 Bean 时还会退回 `StubAgent`

结论：

- 保留这套配置层
- 但要补一层运行时装配服务

#### `ReActDecisionEngine`

当前问题：

- 是静态 placeholder
- 没有真正接入 LLM
- 没有真正接入 Agent 和 Tool 的能力集合

结论：

- 当前阶段不要依赖它承载产品主链路
- 到多 Agent 阶段再升级

#### `AgentMessageBus` / `InMemoryAgentMessageBus`

当前问题：

- 内存版总线只适合本地样例
- 没有可靠投递、重试、持久化

结论：

- 暂时不要把单 Agent 主链路建立在它上面
- 等多 Agent 真的需要异步协作时再升级

### 2.3 暂不进入主线，或只保留样例

#### 示例 Agent

- `EchoAgent`
- `PlannerAgent`
- `StubAgent`

建议定位：

- 保留给样例、测试、占位
- 不再继续在这些类上叠业务功能

#### MCP 配置层

- `McpInfoService`
- `McpInfoServiceImpl`
- `mcp_info`

建议定位：

- 先保留
- 但不进入当前主线
- 等工具系统稳定后，再考虑把 MCP 作为“外部工具源”接入

---

## 3. 数据表现在应该怎么用

`sql/init-smartcrew-agent.sql` 已经定义了当前主线所需的大部分表。

### 3.1 当前主线直接使用

- `prompt_template`
- `user_preference`
- `agent_definition`
- `tool_definition`
- `agent_tool_binding`
- `llm_conversation_session`
- `llm_conversation_message`

### 3.2 当前先不接主线

- `mcp_info`

### 3.3 后续需要新增

当前仓库里还没有完整 RAG 和多 Agent 任务表，后续大概率要新增：

- 知识文档表
- 文档切片表
- 向量索引或向量存储表
- 任务执行表
- 协作步骤表

为什么现在就要知道这个：

- 因为你要避免把 RAG 数据、任务状态错误地塞进 `user_preference` 或 LLM 消息表里

---

## 4. 先定建模原则：user、session、agent 应该怎么处理

这部分必须先定，否则后面一定返工。

### 4.1 `userId` 代表真实用户，不代表 Agent

不要把 Agent 当作 LLM 的 user。

正确做法：

- `userId` 表示真实业务用户
- 企业微信或飞书用户先映射到系统内统一 `userId`

为什么：

- 偏好归属正确
- 会话审计不会串
- 后面多 Agent 时不会把“用户身份”和“Agent 身份”混掉

### 4.2 `sessionId` 代表对话线程，但要按 Agent 维度隔离

推荐统一规则：

```text
llmSessionId = agentCode + "::" + platformSessionId
```

示例：

```text
initial-agent::wecom-001
retrieval-agent::wecom-001
tool-agent::wecom-001
```

为什么：

- 不同 Agent 不会共享同一份 LLM 历史
- 多 Agent 上下文边界更清晰
- 根 session 仍然能追踪整条协作链

### 4.3 长期偏好和短期会话必须分开

固定原则：

- 长期偏好放 `user_preference`
- 短期多轮上下文放 `llm_conversation_*`
- 中间任务状态不要塞进偏好表

为什么：

- 长期偏好和短期上下文的生命周期完全不同
- 混在一起后面很难治理

---

## 5. 先实现最小可用的 `InitialAgent`

这一阶段的目标非常明确：先把一个真正能调 LLM 的 Agent 跑起来。

### 5.1 为什么第一步先写 `InitialAgent`

因为你后面所有能力都会挂在它身上：

- 提示词配置
- 长期偏好
- RAG
- 工具调用
- 平台接入
- 多 Agent 编排

如果第一步不是把 `InitialAgent` 跑起来，而是先改平台、先做 MCP、先做复杂编排，主线很容易失控。

### 5.2 建议新增类

建议新增：

```text
smartcrew-modules/src/main/java/com/smartcrew/agent/core/agent/InitialAgent.java
```

### 5.3 第一版职责边界

第一版 `InitialAgent` 只做三件事：

1. 接收用户消息
2. 组装 `LlmChatRequest`
3. 调用 `LlmClient.chat(...)`

不要一开始就把所有能力都塞进去。

### 5.4 关键实现代码

```java
package com.smartcrew.agent.core.agent;

import com.smartcrew.agent.api.agent.domain.request.AgentDispatchCommand;
import com.smartcrew.agent.api.agent.domain.vo.AgentDispatchResponse;
import com.smartcrew.agent.api.agent.service.Agent;
import com.smartcrew.agent.api.llm.domain.request.LlmChatRequest;
import com.smartcrew.agent.api.llm.domain.vo.LlmChatResponse;
import com.smartcrew.agent.api.llm.service.LlmClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InitialAgent implements Agent {

    private final LlmClient llmClient;

    @Override
    public String code() {
        return "initial-agent";
    }

    @Override
    public String name() {
        return "Initial Agent";
    }

    @Override
    public boolean supports(String capability) {
        return "chat".equalsIgnoreCase(capability)
                || "orchestrate".equalsIgnoreCase(capability);
    }

    @Override
    public AgentDispatchResponse handle(AgentDispatchCommand command) {
        String llmSessionId = code() + "::" + command.getSessionId();

        LlmChatRequest request = LlmChatRequest.builder()
                .userId(command.getUserId())
                .sessionId(llmSessionId)
                .userMessage(command.getMessage())
                .systemPrompt("你是 SmartCrew 的初始 Agent，负责直接与用户交流。")
                .traceId(command.getTraceId())
                .build();

        LlmChatResponse response = llmClient.chat(request);
        if (!Boolean.TRUE.equals(response.getSuccess())) {
            return AgentDispatchResponse.builder()
                    .traceId(command.getTraceId())
                    .agentCode(code())
                    .accepted(false)
                    .message("暂时无法处理你的请求，请稍后再试。")
                    .build();
        }

        return AgentDispatchResponse.builder()
                .traceId(command.getTraceId())
                .agentCode(code())
                .accepted(true)
                .message(response.getContent())
                .build();
    }
}
```

### 5.5 为什么先允许硬编码一个最小 `systemPrompt`

因为这一阶段的目标只是把主入口 Agent 跑通。

这时先允许有一个最小硬编码提示词，原因是：

- 有默认行为
- 先验证 Agent -> LLM -> Response 主链路
- 下一阶段再把提示词抽出来，不会影响主链路正确性

### 5.6 这一阶段怎么验证

调用：

```http
POST /api/v1/agents/initial-agent/dispatch
```

示例请求体：

```json
{
  "userId": 1001,
  "sessionId": "demo-session-001",
  "message": "你好，请介绍一下你自己"
}
```

完成标准：

- 能返回 `accepted=true`
- 同一 `userId + sessionId` 多轮提问时能延续上下文
- `llm_conversation_*` 有落库数据

---

## 6. 再把平台入口真正接到 `InitialAgent`

这一步做的是：让企业微信或飞书消息真正进入 Agent，而不是只返回 placeholder。

### 6.1 为什么平台接入一定要放在 `InitialAgent` 后面

因为平台本质上只是“消息入口”，不是核心业务。

如果先做平台再做 Agent，你很容易把业务判断写进平台适配器里，后面会非常乱。

### 6.2 平台适配器应该承担什么职责

平台层只负责：

- 解析平台消息
- 识别平台用户
- 生成平台 session
- 转发给 `AgentCoordinator`
- 把 Agent 返回结果适配回平台格式

不要在平台层写：

- 提示词逻辑
- 工具调用逻辑
- RAG 逻辑
- 多 Agent 路由逻辑

### 6.3 建议新增两个辅助服务

建议新增：

- `PlatformUserMappingService`
- `PlatformSessionService`

#### `PlatformUserMappingService` 示例

```java
public interface PlatformUserMappingService {

    Long resolveUserId(String platformCode, String platformUserId);
}
```

#### `PlatformSessionService` 示例

```java
public interface PlatformSessionService {

    String resolveSessionId(String platformCode, String chatId, String threadId);
}
```

为什么要单独抽出来：

- 用户映射和 session 规则本身就是平台层能力
- 后面换平台或调整规则时不会去改 Agent 代码

### 6.4 平台适配器改造示例

下面用企业微信适配器示意主链路怎么写。

```java
package com.smartcrew.agent.core.platform;

import com.smartcrew.agent.api.agent.domain.request.AgentDispatchCommand;
import com.smartcrew.agent.api.agent.domain.vo.AgentDispatchResponse;
import com.smartcrew.agent.api.agent.service.AgentCoordinator;
import com.smartcrew.agent.api.platform.domain.request.PlatformEventRequest;
import com.smartcrew.agent.api.platform.domain.vo.PlatformDispatchResponse;
import com.smartcrew.agent.api.platform.service.PlatformAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WecomPlatformAdapter implements PlatformAdapter {

    private final AgentCoordinator agentCoordinator;
    private final PlatformUserMappingService platformUserMappingService;
    private final PlatformSessionService platformSessionService;

    @Override
    public String platformCode() {
        return "wecom";
    }

    @Override
    public boolean supports(String platform) {
        return platformCode().equalsIgnoreCase(platform);
    }

    @Override
    public PlatformDispatchResponse handleEvent(PlatformEventRequest request) {
        Long userId = platformUserMappingService.resolveUserId(
                platformCode(),
                request.getUserId()
        );
        String sessionId = platformSessionService.resolveSessionId(
                platformCode(),
                request.getChatId(),
                request.getThreadId()
        );

        AgentDispatchCommand command = AgentDispatchCommand.builder()
                .userId(userId)
                .sessionId(sessionId)
                .message(request.getContent())
                .traceId(request.getTraceId())
                .build();

        AgentDispatchResponse response = agentCoordinator.dispatch("initial-agent", command);
        return PlatformDispatchResponse.builder()
                .platform(platformCode())
                .handled(response.getAccepted())
                .message(response.getMessage())
                .build();
    }
}
```

### 6.5 流式能力这一步先不要硬接平台

当前 LLM 层已经有流式 chat，但平台层和 Agent dispatch 还主要是同步响应模式。

为什么这一步先不做流式平台回推：

- 单条同步消息更容易先打通主链路
- 企业微信和飞书的回推策略差异比较大
- 平台回写失败、分段消息、超时策略都会把复杂度大幅抬高

推荐顺序：

1. 先打通同步收发
2. 再评估是否需要平台流式输出

---

## 7. 把提示词配置正式融入系统

这一阶段是关键，因为现在项目虽然有 `PromptTemplateService`，但提示词不会自动生效。

### 7.1 为什么提示词配置必须单独做一阶段

因为后面这些能力都会依赖“提示词装配入口”：

- 用户长期偏好
- RAG 检索结果
- 工具调用结果
- 多 Agent 角色差异

如果现在不把提示词装配独立出来，后面这些内容就会继续被硬编码进 `InitialAgent.handle(...)`。

### 7.2 建议新增 `InitialAgentPromptService`

建议新增：

```text
smartcrew-modules/src/main/java/com/smartcrew/agent/core/agent/service/InitialAgentPromptService.java
```

### 7.3 建议接口设计

```java
public interface InitialAgentPromptService {

    String buildSystemPrompt(Long userId, String scene);
}
```

为什么先保持简单：

- 先让“提示词可配置”落地
- 复杂版再慢慢把偏好、RAG、工具上下文拼进去

### 7.4 关键实现代码

```java
package com.smartcrew.agent.core.agent.service;

import com.smartcrew.agent.api.prompt.domain.entity.PromptTemplate;
import com.smartcrew.agent.api.prompt.service.PromptTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class InitialAgentPromptServiceImpl implements InitialAgentPromptService {

    private static final String DEFAULT_PROMPT =
            "你是 SmartCrew 的初始 Agent，负责直接与用户交流，并在必要时协调其他能力。";

    private final PromptTemplateService promptTemplateService;

    @Override
    public String buildSystemPrompt(Long userId, String scene) {
        String category = resolveCategory(scene);
        Optional<PromptTemplate> template = promptTemplateService.findByCategory(category);
        return template.map(PromptTemplate::getTemplateContent).orElse(DEFAULT_PROMPT);
    }

    private String resolveCategory(String scene) {
        if ("routing".equalsIgnoreCase(scene)) {
            return "initial-agent-routing";
        }
        if ("rag".equalsIgnoreCase(scene)) {
            return "initial-agent-rag";
        }
        if ("tool".equalsIgnoreCase(scene)) {
            return "initial-agent-tool";
        }
        return "initial-agent";
    }
}
```

### 7.5 在 `InitialAgent` 中接入

```java
private final InitialAgentPromptService initialAgentPromptService;

String systemPrompt = initialAgentPromptService.buildSystemPrompt(
        command.getUserId(),
        "default"
);

LlmChatRequest request = LlmChatRequest.builder()
        .userId(command.getUserId())
        .sessionId(llmSessionId)
        .userMessage(command.getMessage())
        .systemPrompt(systemPrompt)
        .traceId(command.getTraceId())
        .build();
```

### 7.6 推荐的提示词来源优先级

建议从现在就固定优先级：

1. 运行时硬约束
2. `prompt_template`
3. `agent_definition.systemPrompt`
4. 代码默认值

为什么：

- 运行时约束通常最紧急
- prompt 模板适合日常调参
- `agent_definition` 更像 Agent 元配置
- 代码默认值只做兜底

### 7.7 推荐预留的 `category`

- `initial-agent`
- `initial-agent-routing`
- `initial-agent-rag`
- `initial-agent-tool`
- `retrieval-agent`
- `tool-agent`
- `report-agent`

为什么现在就预留：

- 你现在虽然还是单 Agent
- 但多 Agent 迟早要来
- 现在先把提示词分类规则设计好，后面不会返工

---

## 8. 再把长期偏好接入 `InitialAgent`

这一步要做的是：让 Agent 记住用户的稳定偏好，而不是每次都从零开始回答。

### 8.1 为什么偏好现在接，而不是更晚

因为偏好是“用户真实体验”的重要组成部分。

同一个 Agent 能不能记住：

- 你喜欢中文还是英文
- 喜欢简洁还是详细
- 喜欢什么称呼

这会直接决定它像不像一个长期可用的助手。

### 8.2 当前项目里应该如何理解 `ConversationMemoryService`

当前实现更适合存：

- 长期偏好
- 稳定画像
- 轻量记忆

不适合存：

- 全量聊天记录
- 每轮对话消息
- 高频变化的任务状态

为什么：

- 聊天历史已经有 `llm_conversation_*`
- 偏好和短期上下文是两种不同数据

### 8.3 建议偏好读取代码

```java
package com.smartcrew.agent.core.agent.service;

import com.smartcrew.agent.api.memory.service.ConversationMemoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class InitialAgentMemoryService {

    private final ConversationMemoryService conversationMemoryService;

    public Map<String, String> loadUserPreferences(Long userId) {
        return conversationMemoryService.loadMemory(userId);
    }
}
```

### 8.4 把偏好拼进 prompt

```java
private final InitialAgentMemoryService initialAgentMemoryService;

Map<String, String> memory = initialAgentMemoryService.loadUserPreferences(command.getUserId());
String nickname = memory.getOrDefault("nickname", "用户");
String language = memory.getOrDefault("language", "zh-CN");
String tone = memory.getOrDefault("tone", "professional");

String basePrompt = initialAgentPromptService.buildSystemPrompt(command.getUserId(), "default");
String systemPrompt = basePrompt + "\n"
        + "请使用语言：" + language + "\n"
        + "用户称呼偏好：" + nickname + "\n"
        + "回答风格偏好：" + tone;
```

### 8.5 为什么偏好要通过 prompt 注入，而不是单独扔给业务逻辑

因为偏好本质上是“影响回答方式的上下文”，而不是一个独立业务流程。

把它先合并到 prompt，有几个好处：

- 实现最简单
- 和 RAG、工具上下文的注入方式一致
- 后面好统一迁移到 `AgentRuntimeConfigService`

### 8.6 回写偏好的建议规则

不是用户说什么都存。

建议只在下面情况回写：

- 用户明确表达长期偏好
- 偏好稳定、不易频繁变动
- 偏好是通用性的，不是某一轮任务的临时条件

例如：

- “以后都用中文回答我”
- “叫我老王”
- “回答尽量简洁”

不建议存：

- “这次帮我写详细点”
- “这一条先别查工具”

---

## 9. 补齐 RAG 能力

这一阶段开始让 `InitialAgent` 具备“先查资料，再回答”的能力。

### 9.1 为什么 RAG 先做成 `InitialAgent` 的内建能力

不建议一上来就拆独立 `rag-agent`。

原因：

- 单 Agent 阶段更容易调试
- 检索增强本质上先是回答前的准备动作
- 等检索链够复杂时，再拆成子 Agent 更合理

### 9.2 当前仓库没有哪些 RAG 能力

你需要自己补：

- 文档入库
- 文档切片
- 向量化
- 检索服务
- 检索结果拼装服务

### 9.3 建议新增接口

```java
public interface RetrievalService {

    List<String> retrieve(String query, int topK);
}
```

```java
public interface RagAugmentationService {

    String buildRagContext(String query);
}
```

### 9.4 `RagAugmentationService` 示例实现

```java
package com.smartcrew.agent.core.rag;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RagAugmentationServiceImpl implements RagAugmentationService {

    private final RetrievalService retrievalService;

    @Override
    public String buildRagContext(String query) {
        List<String> documents = retrievalService.retrieve(query, 3);
        if (documents.isEmpty()) {
            return "";
        }
        return "以下是可参考资料：\n" + String.join("\n---\n", documents);
    }
}
```

### 9.5 在 `InitialAgent` 中接入 RAG

```java
private final RagAugmentationService ragAugmentationService;

String ragContext = ragAugmentationService.buildRagContext(command.getMessage());
String systemPrompt = basePrompt;
if (!ragContext.isBlank()) {
    systemPrompt = systemPrompt + "\n" + ragContext;
}
```

### 9.6 为什么检索结果不要直接当最终答案返回

因为检索只负责“找材料”，不是“替你组织语言”。

最终答案仍然应该由 `InitialAgent` 统一生成，原因是：

- 回答风格要统一
- 偏好要统一
- 后面多 Agent 阶段也要汇总多来源结果

---

## 10. 把工具系统从“能注册”升级成“能执行”

这是当前项目里另一个必须补齐的点。

### 10.1 为什么现有工具系统不能直接说“已经完成”

因为当前系统只是：

- 知道有哪些工具
- 知道工具元数据

但还不能做到：

- 根据 Agent 决策真正执行工具
- 校验工具是否启用
- 校验 Agent 是否有权限调用

### 10.2 推荐的最小执行协议

为了尽快落地，先别设计太复杂。

建议先统一成：

```java
public interface ExecutableSmartCrewTool extends SmartCrewTool {

    Object execute(Map<String, Object> arguments);
}
```

为什么先这么设计：

- 简单
- 易落地
- 不妨碍后面再演进成更完整的 function calling 协议

### 10.3 `DefaultToolExecutor` 重构示例

```java
package com.smartcrew.agent.core.tool;

import com.smartcrew.agent.api.tool.domain.entity.ToolDefinition;
import com.smartcrew.agent.api.tool.service.ToolDefinitionService;
import com.smartcrew.agent.api.tool.service.ToolExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class DefaultToolExecutor implements ToolExecutor {

    private final ToolDefinitionService toolDefinitionService;
    private final AgentToolBindingService agentToolBindingService;
    private final ApplicationContext applicationContext;

    @Override
    public Object execute(String agentCode, String toolCode, Map<String, Object> arguments) {
        ToolDefinition definition = toolDefinitionService.findByToolCode(toolCode)
                .orElseThrow(() -> new IllegalArgumentException("Tool not found: " + toolCode));

        if (!Boolean.TRUE.equals(definition.getEnabled())) {
            throw new IllegalStateException("Tool is disabled: " + toolCode);
        }

        if (!agentToolBindingService.isBound(agentCode, toolCode)) {
            throw new IllegalStateException("Tool is not bound to agent: " + toolCode);
        }

        Object bean = applicationContext.getBean(definition.getBeanName());
        if (!(bean instanceof ExecutableSmartCrewTool executableTool)) {
            throw new IllegalStateException("Tool bean is not executable: " + definition.getBeanName());
        }

        return executableTool.execute(arguments);
    }
}
```

### 10.4 建议新增 `AgentToolBindingService`

```java
public interface AgentToolBindingService {

    boolean isBound(String agentCode, String toolCode);
}
```

为什么一定要引入这层：

- `agent_tool_binding` 当前已经有表
- 如果不接入执行链，这张表就永远只是摆设
- 后面多 Agent 时，工具权限差异也会很重要

### 10.5 在 `InitialAgent` 中先用“代码判断 + 显式调用”方式接工具

注意：这一阶段不建议一开始就做特别复杂的 LLM 自动函数调用。

建议先做显式编排：

```java
if (command.getMessage().contains("搜索")) {
    Object toolResult = toolExecutor.execute(
            code(),
            "web-search",
            Map.of("query", command.getMessage())
    );
    systemPrompt = systemPrompt + "\n工具结果：" + toolResult;
}
```

为什么：

- 更容易调试
- 更容易看清是 Agent 决策错，还是工具执行错
- 后面再逐步演进成自动工具选择

### 10.6 首批推荐接入工具

- `web-search`
- `web-page`
- `document`
- `file`

暂不建议默认开放：

- `terminal`

原因很简单：

- 风险高
- 权限边界复杂
- 容易在系统还没稳定时把问题扩大

---

## 11. 让配置层真正进入运行时

这一阶段要解决的是：数据库里已经有 `agent_definition`、`prompt_template`、`tool_definition`，但现在很多还只是“存了”。

### 11.1 为什么这一步不能跳过

因为如果不补这一层，你会得到：

- 一套数据库配置
- 一套代码默认值
- 一套运行时临时逻辑

三套逻辑彼此不统一，后面一定乱。

### 11.2 建议新增 `AgentRuntimeConfigService`

```java
public interface AgentRuntimeConfigService {

    AgentRuntimeConfig resolve(String agentCode, Long userId, String scene);
}
```

对应配置对象示例：

```java
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AgentRuntimeConfig {
    String systemPrompt;
    Double temperature;
    Integer maxTokens;
}
```

### 11.3 关键实现示例

```java
package com.smartcrew.agent.core.agent.service;

import com.smartcrew.agent.api.agent.domain.entity.AgentDefinition;
import com.smartcrew.agent.api.agent.service.AgentDefinitionService;
import com.smartcrew.agent.api.prompt.domain.entity.PromptTemplate;
import com.smartcrew.agent.api.prompt.service.PromptTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AgentRuntimeConfigServiceImpl implements AgentRuntimeConfigService {

    private final AgentDefinitionService agentDefinitionService;
    private final PromptTemplateService promptTemplateService;

    @Override
    public AgentRuntimeConfig resolve(String agentCode, Long userId, String scene) {
        String prompt = "你是 SmartCrew Agent。";

        Optional<AgentDefinition> definition = agentDefinitionService.findByCode(agentCode);
        if (definition.isPresent() && definition.get().getSystemPrompt() != null) {
            prompt = definition.get().getSystemPrompt();
        }

        Optional<PromptTemplate> template = promptTemplateService.findByCategory(agentCode);
        if (template.isPresent()) {
            prompt = template.get().getTemplateContent();
        }

        return AgentRuntimeConfig.builder()
                .systemPrompt(prompt)
                .temperature(0.4D)
                .maxTokens(1000)
                .build();
    }
}
```

### 11.4 推荐优先级

建议明确优先级：

1. 运行时硬约束
2. `prompt_template`
3. `agent_definition.systemPrompt`
4. 代码默认值

为什么这样排：

- `prompt_template` 更适合日常调 prompt
- `agent_definition` 更适合存 Agent 的默认配置
- 代码默认值只作为兜底

---

## 12. 把 `InitialAgent` 重构成可编排结构

到这一步，`InitialAgent` 很容易变成一个巨型 `handle()` 方法。

如果不拆，后面多 Agent 基本没法平滑演进。

### 12.1 为什么必须先拆服务，再做多 Agent

多 Agent 的本质，是把原来 `InitialAgent` 自己做的部分，拆给其他 Agent 做。

如果你现在连 `InitialAgent` 内部边界都没有拆清楚，后面根本不知道该把哪部分职责交给子 Agent。

### 12.2 推荐拆出的内部服务

- `InitialAgentPromptService`
- `InitialAgentMemoryService`
- `InitialAgentRagService`
- `InitialAgentToolService`
- `InitialAgentResponseService`

### 12.3 重构后的 `InitialAgent` 示意代码

```java
@Component
@RequiredArgsConstructor
public class InitialAgent implements Agent {

    private final LlmClient llmClient;
    private final InitialAgentPromptService promptService;
    private final InitialAgentMemoryService memoryService;
    private final RagAugmentationService ragAugmentationService;
    private final ToolExecutor toolExecutor;

    @Override
    public String code() {
        return "initial-agent";
    }

    @Override
    public String name() {
        return "Initial Agent";
    }

    @Override
    public boolean supports(String capability) {
        return true;
    }

    @Override
    public AgentDispatchResponse handle(AgentDispatchCommand command) {
        String llmSessionId = code() + "::" + command.getSessionId();

        String systemPrompt = promptService.buildSystemPrompt(command.getUserId(), "default");
        String ragContext = ragAugmentationService.buildRagContext(command.getMessage());

        if (!ragContext.isBlank()) {
            systemPrompt = systemPrompt + "\n" + ragContext;
        }

        if (command.getMessage().contains("搜索")) {
            Object toolResult = toolExecutor.execute(
                    code(),
                    "web-search",
                    Map.of("query", command.getMessage())
            );
            systemPrompt = systemPrompt + "\n工具结果：" + toolResult;
        }

        LlmChatRequest request = LlmChatRequest.builder()
                .userId(command.getUserId())
                .sessionId(llmSessionId)
                .userMessage(command.getMessage())
                .systemPrompt(systemPrompt)
                .traceId(command.getTraceId())
                .build();

        LlmChatResponse response = llmClient.chat(request);
        return AgentDispatchResponse.builder()
                .traceId(command.getTraceId())
                .agentCode(code())
                .accepted(Boolean.TRUE.equals(response.getSuccess()))
                .message(response.getContent())
                .build();
    }
}
```

这个版本虽然还不是多 Agent，但已经具备“主控协调器”的雏形了。

---

## 13. 正式进入多 Agent 协作

当 `InitialAgent` 已经稳定具备：

- 聊天
- 提示词配置
- 长期偏好
- RAG
- 工具调用

这时才开始拆多 Agent。

### 13.1 为什么不要一上来就做很多子 Agent

因为多 Agent 的复杂度不是“多写几个类”，而是：

- 任务拆解
- 上下文裁剪
- 会话隔离
- 工具权限差异
- 结果汇总

所以推荐先只加 2 到 3 个：

- `retrieval-agent`
- `tool-agent`
- `report-agent`

### 13.2 初始 Agent 在多 Agent 阶段的职责

- 判断是简单任务还是复杂任务
- 简单任务自己做
- 复杂任务生成计划
- 调度子 Agent
- 汇总结果

### 13.3 建议先做“同步式多 Agent”

当前阶段先不要急着把 `AgentMessageBus` 拉进主链路。

先做同步式调用，原因是：

- 更容易观察
- 更容易排错
- 更适合当前项目现状

### 13.4 关键代码示意

```java
private final AgentCoordinator agentCoordinator;

private String delegateToRetrievalAgent(AgentDispatchCommand rootCommand) {
    AgentDispatchCommand subCommand = AgentDispatchCommand.builder()
            .userId(rootCommand.getUserId())
            .sessionId("retrieval-agent::" + rootCommand.getSessionId())
            .message(rootCommand.getMessage())
            .traceId(rootCommand.getTraceId())
            .build();

    AgentDispatchResponse response = agentCoordinator.dispatch("retrieval-agent", subCommand);
    return response.getMessage();
}
```

### 13.5 子 Agent 不要拿完整主会话

不要直接把这些全塞给子 Agent：

- 用户完整聊天历史
- 用户所有长期偏好
- `InitialAgent` 的完整系统提示词

为什么：

- 容易上下文污染
- 容易失控
- 容易让子 Agent 的边界模糊

正确做法是只给：

- 子任务描述
- 必要摘要
- 必要材料
- 独立 session

---

## 14. 再升级 `ReActDecisionEngine`

只有在多 Agent 真正开始使用后，才值得升级决策引擎。

### 14.1 为什么决策引擎不应该太早做

因为如果单 Agent 主链路都还不稳定，决策引擎再聪明也没意义。

它依赖的是：

- 可用 Agent 集合
- 可用工具集合
- 稳定的提示词和上下文
- 稳定的能力边界

这些都要先有。

### 14.2 升级方向

建议让决策引擎接入：

- `LlmClient`
- `AgentRegistry`
- `ToolRegistry`
- `AgentRuntimeConfigService`

### 14.3 结构化计划对象示例

```java
@Value
@Builder
public class AgentExecutionPlan {
    List<AgentExecutionStep> steps;
}
```

```java
@Value
@Builder
public class AgentExecutionStep {
    String agentCode;
    String action;
    List<String> tools;
    String input;
}
```

### 14.4 决策引擎输出建议

让它输出：

- 这是不是复杂任务
- 如果复杂，要拆几步
- 每一步由哪个 Agent 执行
- 每一步需要什么工具

---

## 15. `AgentMessageBus` 和 MCP 应该什么时候接

这两个基础设施当前都存在，但都不应该太早进入主线。

### 15.1 `AgentMessageBus`

什么时候值得接：

- 子 Agent 执行耗时长
- 需要异步回传
- 需要发布订阅式协作

在这之前，不建议接入主线。

为什么：

- 同步链路更清晰
- 当前内存总线不适合作生产基础设施

### 15.2 MCP

什么时候值得接：

- 你已经有稳定的工具执行链
- 你想让某些能力以 MCP 方式提供

正确接法：

- 把 MCP 当成工具来源
- 统一接入到 `ToolExecutor`

不要把 MCP 做成平行于工具系统的另一条主链路。

---

## 16. 现有控制器在教程里应该怎么用

### `AgentController`

用来：

- 联调 Agent dispatch
- 查看 AgentDefinition
- 管理 Agent 注册状态

注意：

- “注册定义”不等于“写好了真实 Agent”
- 真正有业务能力的 Agent 还是要写 Spring Bean

### `PromptController`

用来：

- 配置 `prompt_template`
- 验证 prompt category 是否正确生效

### `MemoryController`

用来：

- 查看和维护用户长期偏好

### `ToolController`

用来：

- 查看工具元数据
- 启停工具

### `PlatformController`

用来：

- 联调平台事件入口

### `DecisionController`

用来：

- 后续调试决策计划

它现在不是产品主入口，但很适合作为调试接口保留。

---

## 17. 推荐的实际开发顺序

如果要少返工，建议严格按下面顺序做：

1. 写 `InitialAgent`
2. 打通 `/api/v1/agents/initial-agent/dispatch`
3. 改平台适配器，让平台消息接到 `InitialAgent`
4. 新增 `InitialAgentPromptService`
5. 接入 `ConversationMemoryService`
6. 补 RAG 相关接口和实现
7. 重构 `DefaultToolExecutor`
8. 启用 `agent_tool_binding`
9. 新增 `AgentRuntimeConfigService`
10. 拆 `InitialAgent` 内部服务
11. 再新增 `retrieval-agent`、`tool-agent`
12. 最后升级 `ReActDecisionEngine`
13. 真正需要时再接 `AgentMessageBus`
14. 最后再考虑 MCP

---

## 18. 每个阶段的完成标准

### 单 Agent 阶段

- `InitialAgent` 成为唯一主入口
- `/dispatch` 可稳定工作
- 会话上下文正确

### 平台阶段

- 至少一个平台接入打通
- 平台不再返回 placeholder

### 提示词阶段

- 提示词不再硬编码在 Agent 内
- 改 `prompt_template` 可以生效

### 偏好阶段

- 用户称呼、语言、风格可以保存并生效

### RAG 阶段

- 知识问题可命中知识库并反映到回答里

### 工具阶段

- 至少两个工具可被真实执行
- `agent_tool_binding` 真正参与权限控制

### 多 Agent 阶段

- `InitialAgent` 仍然是唯一用户入口
- 至少两个子 Agent 可被调度
- 子 Agent 会话独立
- 结果可汇总

---

## 19. 一句话总结

不要一开始就做“大而全的多 Agent 系统”。最正确的路线是：先把 `InitialAgent` 做成真正能工作的单 Agent，再基于现有 LLM、Prompt、Memory、Platform、Tool、Agent 配置基础设施，逐步补齐提示词、偏好、RAG、工具和运行时配置，最后自然演进到由 `InitialAgent` 主导的多 Agent 协作架构。
