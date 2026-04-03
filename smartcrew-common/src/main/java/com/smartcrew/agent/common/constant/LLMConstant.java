package com.smartcrew.agent.common.constant;

/**
 * LLM 相关常量
 */
public final class LLMConstant {

    private LLMConstant() {
    }

    /**
     * 默认 traceId
     */
    public static final String DEFAULT_TRACE_ID = "unknown";

    /**
     * 默认温度参数
     */
    public static final float DEFAULT_TEMPERATURE = 0.7F;

    /**
     * 客户端 ID
     */
    public static final String DASHSCOPE_CLIENT_ID = "dashscope-client";// 千问客户端ID

    /**
     * 日志前缀
     */
    public static final String QWEN_LOG_PREFIX = "[LLM-Qwen]";// 千问日志前缀
    public static final String LLM_LOG_PREFIX = "[LLM]";// LLM 日志前缀
}
