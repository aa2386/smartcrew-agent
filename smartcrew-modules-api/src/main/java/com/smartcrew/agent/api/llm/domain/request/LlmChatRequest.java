package com.smartcrew.agent.api.llm.domain.request;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 大模型对话请求模型。
 */
@Data
@Builder
public class LlmChatRequest {

    /**
     * 用户 ID，用于隔离用户级别的会话。
     */
    private Long userId;

    /**
     * 会话 ID，用于隔离同一用户下的不同会话。
     */
    private String sessionId;

    /**
     * 用户本轮输入的消息内容。
     */
    private String userMessage;

    /**
     * 系统提示词，可选。
     */
    private String systemPrompt;

    /**
     * 兼容字段，允许外部附带历史消息。
     * 实现层默认优先使用持久化历史，而不是依赖该字段完成多轮对话。
     */
    @Builder.Default
    private List<Map<String, String>> conversationHistory = new ArrayList<>();

    /**
     * 温度参数，控制输出随机性。
     */
    @Builder.Default
    private Double temperature = 0.7;

    /**
     * 最大输出 Token 数。
     */
    @Builder.Default
    private Integer maxTokens = 1000;

    /**
     * 调用链追踪 ID。
     */
    private String traceId;
}
