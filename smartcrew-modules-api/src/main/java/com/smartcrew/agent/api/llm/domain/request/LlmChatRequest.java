package com.smartcrew.agent.api.llm.domain.request;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * LLM 聊天请求模型。
 */
@Data
@Builder
public class LlmChatRequest {

    /**
     * 用户消息内容。
     */
    private String userMessage;

    /**
     * 系统提示词（可选）。
     */
    private String systemPrompt;

    /**
     * 历史对话上下文（可选）。
     */
    @Builder.Default
    private List<Map<String, String>> conversationHistory = new ArrayList<>();

    /**
     * 温度参数，控制输出随机性（0-2）。
     */
    @Builder.Default
    private Double temperature = 0.7;

    /**
     * 最大生成 token 数。
     */
    @Builder.Default
    private Integer maxTokens = 1000;

    /**
     * 追踪 ID，用于日志关联。
     */
    private String traceId;
}
