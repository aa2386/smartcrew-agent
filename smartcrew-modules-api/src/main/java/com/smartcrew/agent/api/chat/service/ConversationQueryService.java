package com.smartcrew.agent.api.chat.service;

import com.smartcrew.agent.api.chat.domain.vo.ChatMessageVo;
import com.smartcrew.agent.api.chat.domain.vo.ChatSessionVo;

import java.util.List;

/**
 * 会话查询服务。
 */
public interface ConversationQueryService {

    /**
     * 创建 Web 会话。
     */
    ChatSessionVo createWebSession(Long userId);

    /**
     * 查询用户自己的 Web 会话。
     */
    List<ChatSessionVo> listWebSessions(Long userId);

    /**
     * 查询指定会话消息。
     */
    List<ChatMessageVo> listSessionMessages(Long userId, String rootSessionId);

    /**
     * 后台查询全部会话。
     */
    List<ChatSessionVo> listAllSessions(Long userId, String provider, String keyword);

    /**
     * 后台查询消息记录。
     */
    List<ChatMessageVo> listMessages(Long userId, String rootSessionId);
}
