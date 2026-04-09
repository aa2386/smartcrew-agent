package com.smartcrew.agent.api.chat.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天会话视图对象。
 */
@Data
@Builder
public class ChatSessionVo {

    /**
     * 根会话 ID。
     */
    private String sessionId;

    /**
     * 会话标题。
     */
    private String title;

    /**
     * 最新预览内容。
     */
    private String preview;

    /**
     * 消息总数。
     */
    private Integer messageCount;

    /**
     * 最近更新时间。
     */
    private LocalDateTime lastMessageAt;

    /**
     * 会话来源。
     */
    private String source;
}
