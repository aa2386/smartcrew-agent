package com.smartcrew.agent.core.llm;

import com.smartcrew.agent.api.llm.domain.entity.LlmConversationMessage;
import com.smartcrew.agent.api.llm.domain.entity.LlmConversationSession;
import com.smartcrew.agent.api.llm.mapper.LlmConversationMessageMapper;
import com.smartcrew.agent.api.llm.mapper.LlmConversationSessionMapper;
import com.smartcrew.agent.common.enums.ConversationHistoryEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 大模型会话存储实现，负责读写会话与消息持久化数据。
 */
@Service
@RequiredArgsConstructor
public class LlmConversationStoreImpl implements LlmConversationStore {

    private static final String STATUS_SUCCESS = "SUCCESS";

    private final LlmConversationSessionMapper sessionMapper;
    private final LlmConversationMessageMapper messageMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LlmConversationSession ensureSession(Long userId, String sessionId) {
        LlmConversationSession session = sessionMapper.selectByUserIdAndSessionId(userId, sessionId);
        if (session != null) {
            return session;
        }

        LlmConversationSession entity = new LlmConversationSession();
        entity.setUserId(userId);
        entity.setSessionId(sessionId);
        entity.setLastMessageAt(LocalDateTime.now());
        entity.setMessageCount(0);
        sessionMapper.insert(entity);
        return entity;
    }

    @Override
    public List<LlmConversationMessage> loadRecentMessages(Long userId, String sessionId, int limit) {
        List<LlmConversationMessage> messages = new ArrayList<>(messageMapper.selectRecentMessages(userId, sessionId, limit));
        Collections.reverse(messages);
        return messages;
    }

    @Override
    public long nextMessageSeq(Long userId, String sessionId) {
        Long maxMessageSeq = messageMapper.selectMaxMessageSeq(userId, sessionId);
        return (maxMessageSeq == null ? 0L : maxMessageSeq) + 1L;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LlmConversationMessage saveUserMessage(Long userId,
                                                  String sessionId,
                                                  long messageSeq,
                                                  String content,
                                                  String traceId) {
        LlmConversationMessage message = new LlmConversationMessage();
        message.setUserId(userId);
        message.setSessionId(sessionId);
        message.setMessageSeq(messageSeq);
        message.setRole(ConversationHistoryEnum.USER.getCode());
        message.setContent(content);
        message.setTraceId(traceId);
        message.setStatus(STATUS_SUCCESS);
        messageMapper.insert(message);
        refreshSessionStats(userId, sessionId);
        return message;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LlmConversationMessage saveAssistantMessage(Long userId,
                                                       String sessionId,
                                                       long messageSeq,
                                                       String content,
                                                       String traceId,
                                                       String model,
                                                       Integer promptTokens,
                                                       Integer completionTokens,
                                                       Integer totalTokens) {
        LlmConversationMessage message = new LlmConversationMessage();
        message.setUserId(userId);
        message.setSessionId(sessionId);
        message.setMessageSeq(messageSeq);
        message.setRole(ConversationHistoryEnum.ASSISTANT.getCode());
        message.setContent(content);
        message.setTraceId(traceId);
        message.setModel(model);
        message.setPromptTokens(promptTokens);
        message.setCompletionTokens(completionTokens);
        message.setTotalTokens(totalTokens);
        message.setStatus(STATUS_SUCCESS);
        messageMapper.insert(message);
        refreshSessionStats(userId, sessionId);
        return message;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markUserMessageFailed(Long messageId, String errorMessage) {
        messageMapper.markMessageFailed(messageId, errorMessage);
    }

    /**
     * 刷新会话的最近消息时间和总消息数。
     */
    private void refreshSessionStats(Long userId, String sessionId) {
        LlmConversationSession session = sessionMapper.selectByUserIdAndSessionId(userId, sessionId);
        if (session == null) {
            return;
        }
        Long maxMessageSeq = messageMapper.selectMaxMessageSeq(userId, sessionId);
        session.setMessageCount(maxMessageSeq == null ? 0 : maxMessageSeq.intValue());
        session.setLastMessageAt(LocalDateTime.now());
        sessionMapper.updateById(session);
    }
}
