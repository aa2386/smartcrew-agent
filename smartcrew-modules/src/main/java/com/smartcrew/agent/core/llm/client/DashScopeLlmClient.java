package com.smartcrew.agent.core.llm.client;

import com.smartcrew.agent.api.llm.domain.entity.LlmConversationMessage;
import com.smartcrew.agent.api.llm.domain.request.LlmChatRequest;
import com.smartcrew.agent.api.llm.domain.vo.LlmChatResponse;
import com.smartcrew.agent.api.llm.service.LlmClient;
import com.smartcrew.agent.common.config.SmartCrewProperties;
import com.smartcrew.agent.common.enums.ConversationHistoryEnum;
import com.smartcrew.agent.common.util.LogUtils;
import com.smartcrew.agent.common.util.StringUtils;
import com.smartcrew.agent.api.llm.service.LlmConversationStore;
import com.smartcrew.agent.core.llm.util.LlmClientUtils;
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
        String traceId = LlmClientUtils.resolveTraceId(request);
        String validationMessage = LlmClientUtils.validateRequest(request);
        if (validationMessage != null) {
            LogUtils.logValidationError(log, "大模型", traceId, validationMessage);
            return LlmClientUtils.buildFailureResponse(
                    validationMessage, startTime, properties.getLlm().getModel(), traceId);
        }

        String conversationKey = LlmClientUtils.buildConversationKey(request.getUserId(), request.getSessionId());// 构建会话key
        ReentrantLock lock = conversationLocks.computeIfAbsent(conversationKey, key -> new ReentrantLock());// 获取会话lock
        LlmConversationMessage savedUserMessage = null;
        lock.lock();
        try {
            ensureModelInitialized();// 确保模型已经初始化
            conversationStore.ensureSession(request.getUserId(), request.getSessionId());// 确保会话已创建
            // 加载最近历史消息
            List<LlmConversationMessage> historyMessages = conversationStore.loadRecentMessages(
                    request.getUserId(),
                    request.getSessionId(),
                    HISTORY_WINDOW_SIZE
            );
            LogUtils.logStartCall(log, "DashScope", conversationKey, traceId, properties.getLlm().getModel());
            LogUtils.logLoadHistory(log, historyMessages.size(), conversationKey, traceId);

            // 持久化消息记录
            long userMessageSeq = conversationStore.nextMessageSeq(request.getUserId(), request.getSessionId());// 信息顺序
            savedUserMessage = conversationStore.saveUserMessage(
                    request.getUserId(), request.getSessionId(), userMessageSeq, request.getUserMessage(), traceId);

            List<ChatMessage> messages = LlmClientUtils.buildChatMessages(request, historyMessages);
            ChatRequest chatRequest = ChatRequest.builder()
                    .messages(messages)
                    .parameters(LlmClientUtils.buildChatRequestParameters(request, properties.getLlm().getModel()))
                    .build();

            ChatResponse response = chatModel.chat(chatRequest);
            String assistantContent = LlmClientUtils.extractAssistantContent(response);
            TokenUsage tokenUsage = LlmClientUtils.extractTokenUsage(response);

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
            LogUtils.logCallSuccess(log, "DashScope", conversationKey, traceId, duration, 
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
            LogUtils.logCallError(log, "DashScope", conversationKey, traceId, duration, errorMessage, ex);

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
            LogUtils.logModelNotEnabled(log, "大模型");
            return;
        }
        if (!"dashscope".equalsIgnoreCase(llmConfig.getProvider())) {
            LogUtils.logProviderMismatch(log, "DashScope", llmConfig.getProvider());
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
        LogUtils.logModelInit(log, "DashScope", llmConfig.getModel(), !StringUtils.isBlank(llmConfig.getBaseUrl()));
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
            LogUtils.logPersistenceError(log, traceId, persistenceException.getMessage(), persistenceException);
        }
    }

}
