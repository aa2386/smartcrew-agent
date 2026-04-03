package com.smartcrew.agent.core.llm.util;

import com.smartcrew.agent.api.llm.domain.entity.LlmConversationMessage;
import com.smartcrew.agent.api.llm.domain.request.LlmChatRequest;
import com.smartcrew.agent.api.llm.domain.vo.LlmChatResponse;
import com.smartcrew.agent.common.enums.ConversationHistoryEnum;
import com.smartcrew.agent.common.util.StringUtils;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.TokenUsage;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
public final class LlmClientUtils {

    private LlmClientUtils() {
    }

    /**
     * 生成或补齐追踪 ID。
     */
    public static String resolveTraceId(LlmChatRequest request) {
        if (request != null && !StringUtils.isBlank(request.getTraceId())) {
            return request.getTraceId();
        }
        return UUID.randomUUID().toString();
    }

    /**
     * 构造统一的会话键。
     */
    public static String buildConversationKey(Long userId, String sessionId) {
        return userId + "::" + sessionId;
    }

    /**
     * 校验对话请求是否满足最小调用条件。
     */
    public static String validateRequest(LlmChatRequest request) {
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
     * 构造发送给模型的完整消息列表。
     */
    public static List<ChatMessage> buildChatMessages(LlmChatRequest request, List<LlmConversationMessage> historyMessages) {
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
    public static ChatMessage mapPersistedMessage(LlmConversationMessage message) {
        if (message == null || StringUtils.isBlank(message.getRole()) || StringUtils.isBlank(message.getContent())) {
            return null;
        }
        return mapRoleToChatMessage(message.getRole(), message.getContent(), message.getTraceId());
    }

    /**
     * 将兼容历史消息追加到当前消息列表中。
     */
    public static void appendCompatibleHistory(List<ChatMessage> messages,
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
     * 将角色和内容映射为模型消息。
     */
    public static ChatMessage mapRoleToChatMessage(String role, String content, String traceId) {
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
     * 构建统一的失败响应。
     */
    public static LlmChatResponse buildFailureResponse(String errorMessage, long startTime, String model, String traceId) {
        long duration = System.currentTimeMillis() - startTime;
        log.warn("大模型请求直接返回失败，traceId: {}，耗时: {}ms，原因: {}", traceId, duration, errorMessage);
        return LlmChatResponse.builder()
                .success(Boolean.FALSE)
                .errorMessage(errorMessage)
                .durationMs(duration)
                .model(model)
                .build();
    }

    /**
     * 构造模型调用参数。
     */
    public static ChatRequestParameters buildChatRequestParameters(LlmChatRequest request, String model) {
        return ChatRequestParameters.builder()
                .modelName(model)
                .temperature(request.getTemperature())
                .maxOutputTokens(request.getMaxTokens())
                .build();
    }

    /**
     * 构造完整的模型对话请求。
     */
    public static ChatRequest buildChatRequest(LlmChatRequest request,
                                               List<LlmConversationMessage> historyMessages,
                                               String model) {
        return ChatRequest.builder()
                .messages(buildChatMessages(request, historyMessages))
                .parameters(buildChatRequestParameters(request, model))
                .build();
    }

    /**
     * 提取模型返回的文本内容。
     */
    public static String extractAssistantContent(ChatResponse response) {
        if (response == null || response.aiMessage() == null || response.aiMessage().text() == null) {
            return "";
        }
        return response.aiMessage().text();
    }

    /**
     * 提取 Token 使用信息。
     */
    public static TokenUsage extractTokenUsage(ChatResponse response) {
        if (response == null || response.metadata() == null) {
            return null;
        }
        return response.metadata().tokenUsage();
    }

    /**
     * 构建统一的成功响应。
     */
    public static LlmChatResponse buildSuccessResponse(String content,
                                                       TokenUsage tokenUsage,
                                                       long duration,
                                                       String model) {
        return LlmChatResponse.builder()
                .content(content)
                .totalTokens(tokenUsage != null ? tokenUsage.totalTokenCount() : null)
                .promptTokens(tokenUsage != null ? tokenUsage.inputTokenCount() : null)
                .completionTokens(tokenUsage != null ? tokenUsage.outputTokenCount() : null)
                .model(model)
                .success(Boolean.TRUE)
                .durationMs(duration)
                .build();
    }

    /**
     * 返回第一个非空白字符串。
     */
    public static String firstNonBlank(String... values) {
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
