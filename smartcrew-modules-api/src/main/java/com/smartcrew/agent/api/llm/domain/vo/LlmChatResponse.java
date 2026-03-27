package com.smartcrew.agent.api.llm.domain.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 大模型对话响应模型。
 */
@Data
@Builder
public class LlmChatResponse {

    /**
     * 模型生成的回复内容。
     */
    private String content;

    /**
     * 总 Token 数。
     */
    private Integer totalTokens;

    /**
     * 输入 Token 数。
     */
    private Integer promptTokens;

    /**
     * 输出 Token 数。
     */
    private Integer completionTokens;

    /**
     * 实际使用的模型名称。
     */
    private String model;

    /**
     * 是否调用成功。
     */
    private Boolean success;

    /**
     * 错误信息。
     */
    private String errorMessage;

    /**
     * 调用耗时，单位毫秒。
     */
    private Long durationMs;
}
