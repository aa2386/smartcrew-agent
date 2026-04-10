package com.smartcrew.agent.core.chat;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.smartcrew.agent.api.chat.domain.vo.ChatMessageVo;
import com.smartcrew.agent.api.chat.domain.vo.ChatSessionVo;
import com.smartcrew.agent.api.chat.service.ConversationQueryService;
import com.smartcrew.agent.api.llm.domain.entity.LlmConversationMessage;
import com.smartcrew.agent.api.llm.domain.entity.LlmConversationSession;
import com.smartcrew.agent.api.llm.mapper.LlmConversationMessageMapper;
import com.smartcrew.agent.api.llm.mapper.LlmConversationSessionMapper;
import com.smartcrew.agent.core.page.PageQuery;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * 会话查询服务实现。
 */
@Service
public class ConversationQueryServiceImpl implements ConversationQueryService {

    /**
     * 初始智能体会话前缀。
     */
    private static final String INITIAL_AGENT_PREFIX = "initial-agent::";

    /**
     * 会话 Mapper。
     */
    private final LlmConversationSessionMapper sessionMapper;

    /**
     * 消息 Mapper。
     */
    private final LlmConversationMessageMapper messageMapper;

    public ConversationQueryServiceImpl(LlmConversationSessionMapper sessionMapper,
                                        LlmConversationMessageMapper messageMapper) {
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
    }

    @Override
    public ChatSessionVo createWebSession(Long userId) {
        return ChatSessionVo.builder()
                .sessionId("web::" + UUID.randomUUID())
                .title("新对话")
                .preview("开始新的智能对话")
                .messageCount(0)
                .lastMessageAt(LocalDateTime.now())
                .source("WEB")
                .build();
    }

    @Override
    public List<ChatSessionVo> listWebSessions(Long userId) {
        return sessionMapper.selectList(Wrappers.lambdaQuery(LlmConversationSession.class)
                        .eq(LlmConversationSession::getUserId, userId))
                .stream()
                .filter(session -> extractRootSessionId(session.getSessionId()).startsWith("web::"))
                .sorted(Comparator.comparing(LlmConversationSession::getLastMessageAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toSessionVo)
                .toList();
    }

    @Override
    public List<ChatMessageVo> listSessionMessages(Long userId, String rootSessionId) {
        String llmSessionId = INITIAL_AGENT_PREFIX + rootSessionId;
        return messageMapper.selectList(Wrappers.lambdaQuery(LlmConversationMessage.class)
                        .eq(userId != null, LlmConversationMessage::getUserId, userId)
                        .eq(LlmConversationMessage::getSessionId, llmSessionId)
                        .orderByAsc(LlmConversationMessage::getMessageSeq))
                .stream()
                .map(this::toMessageVo)
                .toList();
    }

    @Override
    public List<ChatSessionVo> listAllSessions(Long userId, String provider, String keyword) {
        return sessionMapper.selectAdminSessions(userId, normalizeProvider(provider), normalizeKeyword(keyword));
    }

    @Override
    public IPage<ChatSessionVo> listSessionsPage(PageQuery pageQuery, Long userId, String provider, String keyword) {
        return sessionMapper.selectAdminSessionPage(
                pageQuery.build(),
                userId,
                normalizeProvider(provider),
                normalizeKeyword(keyword)
        );
    }

    @Override
    public List<ChatMessageVo> listMessages(Long userId, String rootSessionId) {
        if (rootSessionId != null && !rootSessionId.isBlank()) {
            return listSessionMessages(userId, rootSessionId);
        }
        return messageMapper.selectList(Wrappers.lambdaQuery(LlmConversationMessage.class)
                        .eq(userId != null, LlmConversationMessage::getUserId, userId)
                        .orderByDesc(LlmConversationMessage::getCreateTime)
                        .last("limit 200"))
                .stream()
                .map(this::toMessageVo)
                .toList();
    }

    /**
     * 转换为会话视图对象。
     */
    private ChatSessionVo toSessionVo(LlmConversationSession session) {
        LlmConversationMessage latestMessage = messageMapper.selectLatestMessage(session.getUserId(), session.getSessionId());
        String preview = latestMessage == null ? "暂无消息" : latestMessage.getContent();
        String title = preview == null || preview.isBlank() ? "新对话" : preview.substring(0, Math.min(preview.length(), 16));
        String rootSessionId = extractRootSessionId(session.getSessionId());
        return ChatSessionVo.builder()
                .sessionId(rootSessionId)
                .title(title)
                .preview(preview)
                .messageCount(session.getMessageCount())
                .lastMessageAt(session.getLastMessageAt())
                .source(resolveSource(rootSessionId))
                .build();
    }

    /**
     * 转换为消息视图对象。
     */
    private ChatMessageVo toMessageVo(LlmConversationMessage message) {
        return ChatMessageVo.builder()
                .id(message.getId())
                .sessionId(extractRootSessionId(message.getSessionId()))
                .messageSeq(message.getMessageSeq())
                .role(message.getRole())
                .content(message.getContent())
                .traceId(message.getTraceId())
                .createTime(message.getCreateTime())
                .build();
    }

    /**
     * 提取根会话 ID。
     */
    private String extractRootSessionId(String llmSessionId) {
        if (llmSessionId == null) {
            return "";
        }
        return llmSessionId.startsWith(INITIAL_AGENT_PREFIX)
                ? llmSessionId.substring(INITIAL_AGENT_PREFIX.length())
                : llmSessionId;
    }

    /**
     * 解析会话来源。
     */
    private String resolveSource(String rootSessionId) {
        if (rootSessionId.startsWith("platform::wecom::")) {
            return "WECOM";
        }
        if (rootSessionId.startsWith("platform::feishu::")) {
            return "FEISHU";
        }
        return "WEB";
    }

    /**
     * 统一标准化平台来源参数。
     */
    private String normalizeProvider(String provider) {
        return provider == null ? "" : provider.trim().toUpperCase();
    }

    /**
     * 统一标准化关键词。
     */
    private String normalizeKeyword(String keyword) {
        return keyword == null ? "" : keyword.trim();
    }
}
