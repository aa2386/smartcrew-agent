package com.smartcrew.agent.api.chat.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天消息视图对象。
 */
@Data
@Builder
public class ChatMessageVo {

    /**
     * 消息 ID。
     */
    private Long id;

    /**
     * 会话 ID。
     */
    private String sessionId;

    /**
     * 消息顺序号。
     */
    private Long messageSeq;

    /**
     * 角色。
     */
    private String role;

    /**
     * 内容。
     */
    private String content;

    /**
     * 追踪 ID。
     */
    private String traceId;

    /**
     * 创建时间。
     */
    private LocalDateTime createTime;
}
