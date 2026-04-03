package com.smartcrew.agent.api.llm.service;

import com.smartcrew.agent.api.llm.domain.entity.LlmConversationMessage;
import com.smartcrew.agent.api.llm.domain.entity.LlmConversationSession;
import com.smartcrew.agent.api.llm.domain.request.LlmChatRequest;
import org.slf4j.Logger;

import java.util.List;

/**
 * 大模型会话存储接口，负责会话创建、历史装载与消息持久化。
 */
public interface LlmConversationStore {

    /**
     * 确保指定用户会话存在，不存在时自动创建。
     *
     * @param userId 用户 ID
     * @param sessionId 会话 ID
     * @return 会话实体
     */
    LlmConversationSession ensureSession(Long userId, String sessionId);

    /**
     * 加载指定会话最近若干条历史消息。
     *
     * @param userId 用户 ID
     * @param sessionId 会话 ID
     * @param limit 查询条数
     * @return 按时间正序排列的消息列表
     */
    List<LlmConversationMessage> loadRecentMessages(Long userId, String sessionId, int limit);

    /**
     * 生成指定会话的下一条消息顺序号。
     *
     * @param userId 用户 ID
     * @param sessionId 会话 ID
     * @return 下一条消息顺序号
     */
    long nextMessageSeq(Long userId, String sessionId);

    /**
     * 保存用户消息。
     *
     * @param userId 用户 ID
     * @param sessionId 会话 ID
     * @param messageSeq 消息顺序号
     * @param content 消息内容
     * @param traceId 追踪 ID
     * @return 已保存的消息实体
     */
    LlmConversationMessage saveUserMessage(Long userId,
                                           String sessionId,
                                           long messageSeq,
                                           String content,
                                           String traceId);

    /**
     * 保存助手消息。
     *
     * @param userId 用户 ID
     * @param sessionId 会话 ID
     * @param messageSeq 消息顺序号
     * @param content 消息内容
     * @param traceId 追踪 ID
     * @param model 模型名称
     * @param promptTokens 输入 Token 数
     * @param completionTokens 输出 Token 数
     * @param totalTokens 总 Token 数
     * @return 已保存的消息实体
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

    /**
     * 将指定用户消息标记为失败。
     *
     * @param messageId 消息 ID
     * @param errorMessage 错误信息
     */
    void markUserMessageFailed(Long messageId, String errorMessage);

    /**
     * 在没有现成用户消息记录时补记失败消息，便于审计。
     */
    void handleFailurePersistence(LlmChatRequest request, String traceId, String errorMessage, Logger logger);
}
