package com.smartcrew.agent.api.llm.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.smartcrew.agent.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 大模型会话实体，记录一条对话会话的基础信息。
 */
@Data
@TableName("llm_conversation_session")
@EqualsAndHashCode(callSuper = true)
public class LlmConversationSession extends BaseEntity {

    /**
     * 主键 ID。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户 ID。
     */
    private Long userId;

    /**
     * 会话 ID。
     */
    private String sessionId;

    /**
     * 最近一条消息时间。
     */
    private LocalDateTime lastMessageAt;

    /**
     * 会话内消息总数。
     */
    private Integer messageCount;
}
