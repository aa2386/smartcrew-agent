package com.smartcrew.agent.api.llm.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.smartcrew.agent.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 大模型会话消息实体，表示一条持久化的对话消息。
 */
@Data
@TableName("llm_conversation_message")
@EqualsAndHashCode(callSuper = true)
public class LlmConversationMessage extends BaseEntity {

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
     * 会话内的消息顺序号。
     */
    private Long messageSeq;

    /**
     * 消息角色，例如 system、user、assistant、tool。
     */
    private String role;

    /**
     * 消息内容。
     */
    private String content;

    /**
     * 本轮调用链追踪 ID。
     */
    private String traceId;

    /**
     * 使用的模型名称。
     */
    private String model;

    /**
     * 输入 Token 数。
     */
    private Integer promptTokens;

    /**
     * 输出 Token 数。
     */
    private Integer completionTokens;

    /**
     * 总 Token 数。
     */
    private Integer totalTokens;

    /**
     * 消息处理状态，例如 SUCCESS、FAILED。
     */
    private String status;

    /**
     * 失败时的错误信息。
     */
    private String errorMessage;
}
