package com.smartcrew.agent.core.llm;

import com.smartcrew.agent.api.llm.domain.entity.LlmConversationMessage;
import com.smartcrew.agent.api.llm.domain.request.LlmChatRequest;
import com.smartcrew.agent.api.llm.domain.vo.LlmChatResponse;
import com.smartcrew.agent.api.llm.service.LlmClient;
import com.smartcrew.agent.common.config.SmartCrewProperties;
import com.smartcrew.agent.common.enums.ConversationHistoryEnum;
import com.smartcrew.agent.common.util.StringUtils;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.TokenUsage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * DashScope 大模型客户端，负责多轮上下文管理、模型调用与消息持久化。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashScopeLlmClient implements LlmClient {

    /**
     * 最近上下文窗口大小。
     */
    private static final int HISTORY_WINDOW_SIZE = 20;

    private final SmartCrewProperties properties;
    private final LlmConversationStore conversationStore;

    /**
     * 会话级串行锁，避免同一会话下消息顺序错乱。
     */
    private final ConcurrentHashMap<String, ReentrantLock> conversationLocks = new ConcurrentHashMap<>();

    /**
     * DashScope 聊天模型实例。
     */
    private ChatLanguageModel chatModel;

    @Override
    public LlmChatResponse chat(LlmChatRequest request) {
        long startTime = System.currentTimeMillis();
        String traceId = resolveTraceId(request);
        String validationMessage = validateRequest(request);
        if (validationMessage != null) {
            log.warn("大模型请求参数无效，traceId: {}，原因: {}", traceId, validationMessage);
            return buildFailureResponse(validationMessage, traceId, startTime);
        }

        String conversationKey = buildConversationKey(request.getUserId(), request.getSessionId());
        ReentrantLock lock = conversationLocks.computeIfAbsent(conversationKey, key -> new ReentrantLock());
        LlmConversationMessage savedUserMessage = null;
        lock.lock();
        try {
            ensureModelInitialized();
            conversationStore.ensureSession(request.getUserId(), request.getSessionId());

            List<LlmConversationMessage> historyMessages = conversationStore.loadRecentMessages(
                    request.getUserId(), request.getSessionId(), HISTORY_WINDOW_SIZE);
            log.info("开始调用 DashScope 对话，用户会话: {}，traceId: {}，模型: {}", conversationKey, traceId, properties.getLlm().getModel());
            log.info("已装载最近历史消息 {} 条，用户会话: {}，traceId: {}", historyMessages.size(), conversationKey, traceId);

            long userMessageSeq = conversationStore.nextMessageSeq(request.getUserId(), request.getSessionId());
            savedUserMessage = conversationStore.saveUserMessage(
                    request.getUserId(), request.getSessionId(), userMessageSeq, request.getUserMessage(), traceId);

            List<ChatMessage> messages = buildChatMessages(request, historyMessages);
            ChatRequest chatRequest = ChatRequest.builder()
                    .messages(messages)
                    .parameters(buildChatRequestParameters(request))
                    .build();

            ChatResponse response = chatModel.chat(chatRequest);
            String assistantContent = extractAssistantContent(response);
            TokenUsage tokenUsage = extractTokenUsage(response);

            conversationStore.saveAssistantMessage(
                    request.getUserId(),
                    request.getSessionId(),
                    userMessageSeq + 1,
                    assistantContent,
                    traceId,
                    properties.getLlm().getModel(),
                    tokenUsage != null ? tokenUsage.inputTokenCount() : null,
                    tokenUsage != null ? tokenUsage.outputTokenCount() : null,
                    tokenUsage != null ? tokenUsage.totalTokenCount() : null);

            long duration = System.currentTimeMillis() - startTime;
            log.info(
                    "DashScope 对话完成，用户会话: {}，traceId: {}，耗时: {}ms，总 Token: {}",
                    conversationKey,
                    traceId,
                    duration,
                    tokenUsage != null ? tokenUsage.totalTokenCount() : null);

            return LlmChatResponse.builder()
                    .content(assistantContent)
                    .model(properties.getLlm().getModel())
                    .success(Boolean.TRUE)
                    .durationMs(duration)
                    .totalTokens(tokenUsage != null ? tokenUsage.totalTokenCount() : null)
                    .promptTokens(tokenUsage != null ? tokenUsage.inputTokenCount() : null)
                    .completionTokens(tokenUsage != null ? tokenUsage.outputTokenCount() : null)
                    .build();
        } catch (Exception ex) {
            long duration = System.currentTimeMillis() - startTime;
            String errorMessage = ex.getMessage() == null ? "调用 DashScope 时发生未知异常" : ex.getMessage();
            log.error("DashScope 对话失败，用户会话: {}，traceId: {}，耗时: {}ms，原因: {}",
                    conversationKey, traceId, duration, errorMessage, ex);

            if (savedUserMessage != null && savedUserMessage.getId() != null) {
                conversationStore.markUserMessageFailed(savedUserMessage.getId(), errorMessage);
            } else {
                handleFailurePersistence(request, traceId, errorMessage);
            }
            return LlmChatResponse.builder()
                    .success(Boolean.FALSE)
                    .errorMessage(errorMessage)
                    .durationMs(duration)
                    .model(properties.getLlm().getModel())
                    .build();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public String getClientId() {
        return "dashscope-client";
    }

    /**
     * 初始化 DashScope 模型。
     */
    public void initializeModel() {
        SmartCrewProperties.Llm llmConfig = properties.getLlm();
        if (!llmConfig.isEnabled()) {
            log.warn("检测到大模型能力未启用，跳过 DashScope 模型初始化");
            return;
        }
        if (!"dashscope".equalsIgnoreCase(llmConfig.getProvider())) {
            log.warn("当前大模型提供商不是 DashScope，跳过模型初始化，provider: {}", llmConfig.getProvider());
            return;
        }
        if (StringUtils.isBlank(llmConfig.getApiKey())) {
            throw new IllegalArgumentException("DashScope 的 API Key 未配置，无法初始化模型");
        }
        if (StringUtils.isBlank(llmConfig.getModel())) {
            throw new IllegalArgumentException("DashScope 的模型名称未配置，无法初始化模型");
        }

        QwenChatModel.QwenChatModelBuilder builder = QwenChatModel.builder()
                .apiKey(llmConfig.getApiKey())
                .modelName(llmConfig.getModel())
                .temperature(0.7F)
                .maxTokens(2048);
        if (!StringUtils.isBlank(llmConfig.getBaseUrl())) {
            builder.baseUrl(llmConfig.getBaseUrl());
        }
        this.chatModel = builder.build();
        log.info("DashScope 模型初始化完成，模型: {}，是否自定义 baseUrl: {}",
                llmConfig.getModel(), !StringUtils.isBlank(llmConfig.getBaseUrl()));
    }

    /**
     * 校验对话请求是否满足最小调用条件。
     */
    private String validateRequest(LlmChatRequest request) {
        if (request == null) {
            return "对话请求不能为空";
        }
        if (request.getUserId() == null) {
            return "用户 ID 不能为空";
        }
        if (StringUtils.isBlank(request.getSessionId())) {
            return "会话 ID 不能为空";
        }
        if (StringUtils.isBlank(request.getUserMessage())) {
            return "用户消息不能为空";
        }
        return null;
    }

    /**
     * 确保模型已初始化完成。
     */
    private void ensureModelInitialized() {
        if (chatModel == null) {
            throw new IllegalStateException("DashScope 模型尚未初始化，请先检查配置并完成初始化");
        }
    }

    /**
     * 构造发送给模型的完整消息列表。
     */
    private List<ChatMessage> buildChatMessages(LlmChatRequest request, List<LlmConversationMessage> historyMessages) {
        List<ChatMessage> messages = new ArrayList<>();
        if (!StringUtils.isBlank(request.getSystemPrompt())) {
            messages.add(SystemMessage.from(request.getSystemPrompt()));
        }

        for (LlmConversationMessage historyMessage : historyMessages) {
            ChatMessage chatMessage = mapPersistedMessage(historyMessage);
            if (chatMessage != null) {
                messages.add(chatMessage);
            }
        }

        appendCompatibleHistory(messages, request.getConversationHistory(), request.getTraceId());
        messages.add(UserMessage.from(request.getUserMessage()));
        return messages;
    }

    /**
     * 将持久化消息转换为 LangChain4j 消息对象。
     */
    private ChatMessage mapPersistedMessage(LlmConversationMessage message) {
        if (message == null || StringUtils.isBlank(message.getRole()) || StringUtils.isBlank(message.getContent())) {
            return null;
        }
        return mapRoleToChatMessage(message.getRole(), message.getContent(), message.getTraceId());
    }

    /**
     * 将兼容历史消息追加到当前消息列表中。
     */
    private void appendCompatibleHistory(List<ChatMessage> messages,
                                         List<Map<String, String>> conversationHistory,
                                         String traceId) {
        if (conversationHistory == null || conversationHistory.isEmpty()) {
            return;
        }
        for (Map<String, String> historyItem : conversationHistory) {
            if (historyItem == null || historyItem.isEmpty()) {
                continue;
            }
            String role = firstNonBlank(historyItem.get("role"), historyItem.get("type"));
            String content = firstNonBlank(historyItem.get("content"), historyItem.get("message"), historyItem.get("text"));
            ChatMessage chatMessage = mapRoleToChatMessage(role, content, traceId);
            if (chatMessage != null) {
                messages.add(chatMessage);
            }
        }
    }

    /**
     * 将角色和值映射为模型消息。
     */
    private ChatMessage mapRoleToChatMessage(String role, String content, String traceId) {
        if (StringUtils.isBlank(role) || StringUtils.isBlank(content)) {
            return null;
        }
        if (ConversationHistoryEnum.SYSTEM.getCode().equalsIgnoreCase(role)) {
            return SystemMessage.from(content);
        }
        if (ConversationHistoryEnum.USER.getCode().equalsIgnoreCase(role)) {
            return UserMessage.from(content);
        }
        if (ConversationHistoryEnum.ASSISTANT.getCode().equalsIgnoreCase(role)
                || ConversationHistoryEnum.AI.getCode().equalsIgnoreCase(role)) {
            return AiMessage.from(content);
        }
        log.warn("检测到未知历史消息角色，已跳过。traceId: {}，角色: {}", traceId, role);
        return null;
    }

    /**
     * 构造模型调用参数。
     */
    private ChatRequestParameters buildChatRequestParameters(LlmChatRequest request) {
        return ChatRequestParameters.builder()
                .modelName(properties.getLlm().getModel())
                .temperature(request.getTemperature())
                .maxOutputTokens(request.getMaxTokens())
                .build();
    }

    /**
     * 提取模型返回的文本内容。
     */
    private String extractAssistantContent(ChatResponse response) {
        if (response == null || response.aiMessage() == null || response.aiMessage().text() == null) {
            return "";
        }
        return response.aiMessage().text();
    }

    /**
     * 提取 Token 使用信息。
     */
    private TokenUsage extractTokenUsage(ChatResponse response) {
        if (response == null || response.metadata() == null) {
            return null;
        }
        return response.metadata().tokenUsage();
    }

    /**
     * 在没有现成用户消息记录时补记失败消息，便于审计。
     */
    private void handleFailurePersistence(LlmChatRequest request, String traceId, String errorMessage) {
        if (request == null || request.getUserId() == null || StringUtils.isBlank(request.getSessionId())
                || StringUtils.isBlank(request.getUserMessage())) {
            return;
        }
        try {
            conversationStore.ensureSession(request.getUserId(), request.getSessionId());
            long messageSeq = conversationStore.nextMessageSeq(request.getUserId(), request.getSessionId());
            LlmConversationMessage userMessage = conversationStore.saveUserMessage(
                    request.getUserId(), request.getSessionId(), messageSeq, request.getUserMessage(), traceId);
            conversationStore.markUserMessageFailed(userMessage.getId(), errorMessage);
        } catch (Exception persistenceException) {
            log.error("记录失败消息时发生异常，traceId: {}，原因: {}", traceId, persistenceException.getMessage(), persistenceException);
        }
    }

    /**
     * 生成或补齐追踪 ID。
     */
    private String resolveTraceId(LlmChatRequest request) {
        if (request != null && !StringUtils.isBlank(request.getTraceId())) {
            return request.getTraceId();
        }
        return UUID.randomUUID().toString();
    }

    /**
     * 构造统一的会话键。
     */
    private String buildConversationKey(Long userId, String sessionId) {
        return userId + "::" + sessionId;
    }

    /**
     * 构建统一的失败响应。
     */
    private LlmChatResponse buildFailureResponse(String errorMessage, String traceId, long startTime) {
        long duration = System.currentTimeMillis() - startTime;
        log.warn("大模型请求直接返回失败，traceId: {}，耗时: {}ms，原因: {}", traceId, duration, errorMessage);
        return LlmChatResponse.builder()
                .success(Boolean.FALSE)
                .errorMessage(errorMessage)
                .durationMs(duration)
                .model(properties.getLlm().getModel())
                .build();
    }

    /**
     * 返回第一个非空白字符串。
     */
    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (!StringUtils.isBlank(value)) {
                return value;
            }
        }
        return null;
    }
}
