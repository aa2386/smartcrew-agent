package com.smartcrew.agent.core.llm.client;

import com.smartcrew.agent.api.llm.domain.entity.LlmConversationMessage;
import com.smartcrew.agent.api.llm.domain.request.LlmChatRequest;
import com.smartcrew.agent.api.llm.domain.vo.LlmChatResponse;
import com.smartcrew.agent.api.llm.service.LlmClient;
import com.smartcrew.agent.api.llm.service.LlmConversationStore;
import com.smartcrew.agent.api.llm.service.LlmStreamingCallback;
import com.smartcrew.agent.common.config.SmartCrewProperties;
import com.smartcrew.agent.common.util.LogUtils;
import com.smartcrew.agent.common.util.StringUtils;
import com.smartcrew.agent.core.llm.util.LlmClientUtils;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.community.model.dashscope.QwenStreamingChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.output.TokenUsage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * DashScope 大模型客户端，负责多轮上下文管理、模型调用与消息持久化。
 */
@Service
@RequiredArgsConstructor
public class DashScopeLlmClient implements LlmClient {

    private static final int HISTORY_WINDOW_SIZE = 20;
    private static final Logger log = LoggerFactory.getLogger(DashScopeLlmClient.class);

    private final SmartCrewProperties properties;
    private final LlmConversationStore conversationStore;
    private final ConcurrentHashMap<String, ReentrantLock> conversationLocks = new ConcurrentHashMap<>();

    private ChatLanguageModel chatModel;
    private StreamingChatLanguageModel streamingChatModel;

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

        String conversationKey = LlmClientUtils.buildConversationKey(request.getUserId(), request.getSessionId());
        ReentrantLock lock = conversationLocks.computeIfAbsent(conversationKey, key -> new ReentrantLock());
        LlmConversationMessage savedUserMessage = null;
        lock.lock();
        try {
            ensureModelInitialized();
            conversationStore.ensureSession(request.getUserId(), request.getSessionId());
            List<LlmConversationMessage> historyMessages = conversationStore.loadRecentMessages(
                    request.getUserId(),
                    request.getSessionId(),
                    HISTORY_WINDOW_SIZE
            );
            LogUtils.logStartCall(log, "DashScope", conversationKey, traceId, properties.getLlm().getModel());
            LogUtils.logLoadHistory(log, historyMessages.size(), conversationKey, traceId);

            long userMessageSeq = conversationStore.nextMessageSeq(request.getUserId(), request.getSessionId());
            savedUserMessage = conversationStore.saveUserMessage(
                    request.getUserId(), request.getSessionId(), userMessageSeq, request.getUserMessage(), traceId);

            ChatRequest chatRequest = LlmClientUtils.buildChatRequest(
                    request, historyMessages, properties.getLlm().getModel());
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
            return LlmClientUtils.buildSuccessResponse(
                    assistantContent, tokenUsage, duration, properties.getLlm().getModel());
        } catch (Exception ex) {
            return handleFailure(request, traceId, startTime, conversationKey, savedUserMessage, ex);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void chat(LlmChatRequest request, LlmStreamingCallback callback) {
        long startTime = System.currentTimeMillis();
        String traceId = LlmClientUtils.resolveTraceId(request);
        String validationMessage = LlmClientUtils.validateRequest(request);
        if (validationMessage != null) {
            LogUtils.logValidationError(log, "大模型", traceId, validationMessage);
            safelyComplete(callback, LlmClientUtils.buildFailureResponse(
                    validationMessage, startTime, properties.getLlm().getModel(), traceId));
            return;
        }

        String conversationKey = LlmClientUtils.buildConversationKey(request.getUserId(), request.getSessionId());
        ReentrantLock lock = conversationLocks.computeIfAbsent(conversationKey, key -> new ReentrantLock());
        final LlmConversationMessage[] savedUserMessageHolder = new LlmConversationMessage[1];
        lock.lock();

        boolean releaseLock = true;
        try {
            ensureStreamingModelInitialized();
            conversationStore.ensureSession(request.getUserId(), request.getSessionId());
            List<LlmConversationMessage> historyMessages = conversationStore.loadRecentMessages(
                    request.getUserId(),
                    request.getSessionId(),
                    HISTORY_WINDOW_SIZE
            );
            LogUtils.logStartCall(log, "DashScope", conversationKey, traceId, properties.getLlm().getModel());
            LogUtils.logLoadHistory(log, historyMessages.size(), conversationKey, traceId);

            long userMessageSeq = conversationStore.nextMessageSeq(request.getUserId(), request.getSessionId());
            savedUserMessageHolder[0] = conversationStore.saveUserMessage(
                    request.getUserId(), request.getSessionId(), userMessageSeq, request.getUserMessage(), traceId);

            ChatRequest chatRequest = LlmClientUtils.buildChatRequest(
                    request, historyMessages, properties.getLlm().getModel());
            StringBuilder assistantContentBuilder = new StringBuilder();// 接收响应内容

            releaseLock = false;
            streamingChatModel.chat(chatRequest, new StreamingChatResponseHandler() {
                // 每次收到一部分响应内容时触发
                @Override
                public void onPartialResponse(String partialResponse) {
                    assistantContentBuilder.append(partialResponse);
                    try {
                        callback.onNext(partialResponse);
                    } catch (Exception callbackException) {
                        log.warn("DashScope 流式回调处理片段失败，traceId: {}", traceId, callbackException);
                    }
                }

                // 流式传输完全结束时触发
                @Override
                public void onCompleteResponse(ChatResponse response) {
                    try {
                        String assistantContent = assistantContentBuilder.length() > 0
                                ? assistantContentBuilder.toString()
                                : LlmClientUtils.extractAssistantContent(response);
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
                        safelyComplete(callback, LlmClientUtils.buildSuccessResponse(
                                assistantContent, tokenUsage, duration, properties.getLlm().getModel()));
                    } catch (Exception ex) {
                        safelyComplete(callback, handleFailure(
                                request, traceId, startTime, conversationKey, savedUserMessageHolder[0], ex));
                    } finally {
                        lock.unlock();
                    }
                }

                // 流式传输过程中发生异常时触发
                @Override
                public void onError(Throwable error) {
                    try {
                        Exception ex = error instanceof Exception
                                ? (Exception) error
                                : new RuntimeException(error);
                        safelyComplete(callback, handleFailure(
                                request, traceId, startTime, conversationKey, savedUserMessageHolder[0], ex));
                    } finally {
                        lock.unlock();
                    }
                }
            });
        } catch (Exception ex) {
            safelyComplete(callback, handleFailure(request, traceId, startTime, conversationKey, savedUserMessageHolder[0], ex));
        } finally {
            if (releaseLock && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public String getClientId() {
        return "dashscope-client";
    }

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
        QwenStreamingChatModel.QwenStreamingChatModelBuilder streamingBuilder = QwenStreamingChatModel.builder()
                .apiKey(llmConfig.getApiKey())
                .modelName(llmConfig.getModel())
                .temperature(0.7F)
                .maxTokens(2048);
        if (!StringUtils.isBlank(llmConfig.getBaseUrl())) {
            builder.baseUrl(llmConfig.getBaseUrl());
            streamingBuilder.baseUrl(llmConfig.getBaseUrl());
        }
        this.chatModel = builder.build();
        this.streamingChatModel = streamingBuilder.build();
        LogUtils.logModelInit(log, "DashScope", llmConfig.getModel(), !StringUtils.isBlank(llmConfig.getBaseUrl()));
    }

    private void ensureModelInitialized() {
        if (chatModel == null) {
            throw new IllegalStateException("DashScope 模型尚未初始化，请先检查配置并完成初始化");
        }
    }

    private void ensureStreamingModelInitialized() {
        if (streamingChatModel == null) {
            throw new IllegalStateException("DashScope 流式模型尚未初始化，请先检查配置并完成初始化");
        }
    }

    private LlmChatResponse handleFailure(LlmChatRequest request,
                                          String traceId,
                                          long startTime,
                                          String conversationKey,
                                          LlmConversationMessage savedUserMessage,
                                          Exception ex) {
        long duration = System.currentTimeMillis() - startTime;
        String errorMessage = ex.getMessage() == null ? "调用 DashScope 时发生未知异常" : ex.getMessage();
        LogUtils.logCallError(log, "DashScope", conversationKey, traceId, duration, errorMessage, ex);

        if (savedUserMessage != null && savedUserMessage.getId() != null) {
            conversationStore.markUserMessageFailed(savedUserMessage.getId(), errorMessage);
        } else {
            conversationStore.handleFailurePersistence(request, traceId, errorMessage, log);
        }
        return LlmChatResponse.builder()
                .success(Boolean.FALSE)
                .errorMessage(errorMessage)
                .durationMs(duration)
                .model(properties.getLlm().getModel())
                .build();
    }

    private void safelyComplete(LlmStreamingCallback callback, LlmChatResponse response) {
        try {
            callback.onComplete(response);
        } catch (Exception callbackException) {
            log.warn("DashScope 流式回调返回结果失败", callbackException);
        }
    }
}
