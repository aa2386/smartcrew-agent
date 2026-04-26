package com.smartcrew.agent.api.llm.service;

import com.smartcrew.agent.api.llm.domain.entity.LlmConversationMessage;
import com.smartcrew.agent.api.llm.domain.entity.LlmConversationSession;

import java.util.List;

/**
 * 大模型会话存储接口，负责会话创建、历史加载与消息持久化。
 */
public interface LlmConversationStore {

    /**
     * 确保指定用户会话存在，不存在时自动创建。
     */
    LlmConversationSession ensureSession(Long userId, String sessionId);

    /**
     * 加载指定会话最近若干条历史消息，结果按消息顺序升序返回。
     */
    List<LlmConversationMessage> loadRecentMessages(Long userId, String sessionId, int limit);

    /**
     * 生成指定会话的下一条消息顺序号。
     */
    long nextMessageSeq(Long userId, String sessionId);

    /**
     * 保存用户消息。
     */
    LlmConversationMessage saveUserMessage(Long userId,
                                           String sessionId,
                                           long messageSeq,
                                           String content,
                                           String traceId);

    /**
     * 保存助手消息。
     */
    LlmConversationMessage saveAssistantMessage(Long userId,
                                                String sessionId,
                                                long messageSeq,
                                                String content,
                                                String traceId,
                                                String model,
                                                Integer promptTokens,
                                                Integer completionTokens,
                                                Integer totalTokens);
}
