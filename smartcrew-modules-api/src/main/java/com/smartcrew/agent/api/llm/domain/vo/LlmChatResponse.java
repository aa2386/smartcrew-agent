package com.smartcrew.agent.api.llm.domain.vo;

import lombok.Builder;
import lombok.Data;

/**
 * LLM 聊天响应模型。
 */
@Data
@Builder
public class LlmChatResponse {
    /**
     * 生成的回复内容。
     */
    private String content;

    /**
     * 使用的 token 数量。
     */
    private Integer totalTokens;

    /**
     * 提示词 token 数量。
     */
    private Integer promptTokens;

    /**
     * 完成内容 token 数量。
     */
    private Integer completionTokens;

    /**
     * 模型名称。
     */
    private String model;

    /**
     * 是否成功。
     */
    private Boolean success;

    /**
     * 错误信息（如果失败）。
     */
    private String errorMessage;

    /**
     * 耗时（毫秒）。
     */
    private Long durationMs;
}
