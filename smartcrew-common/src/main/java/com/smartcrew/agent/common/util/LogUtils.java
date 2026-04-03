package com.smartcrew.agent.common.util;

import org.slf4j.Logger;

/**
 * 日志工具类，提供统一的日志格式化和输出能力。
 */
public final class LogUtils {

    private LogUtils() {
    }

    // ==================== 日志格式模板常量 ====================

    /**
     * 开始调用日志格式
     */
    public static final String FORMAT_START_CALL = "开始调用 {} 对话，用户会话: {}，traceId: {}，模型: {}";

    /**
     * 调用完成日志格式
     */
    public static final String FORMAT_CALL_SUCCESS = "{} 对话完成，用户会话: {}，traceId: {}，耗时: {}ms，总 Token: {}";

    /**
     * 调用失败日志格式
     */
    public static final String FORMAT_CALL_ERROR = "{} 对话失败，用户会话: {}，traceId: {}，耗时: {}ms，原因: {}";

    /**
     * 参数验证失败日志格式
     */
    public static final String FORMAT_VALIDATION_ERROR = "{} 请求参数无效，traceId: {}，原因: {}";

    /**
     * 模型初始化完成日志格式
     */
    public static final String FORMAT_MODEL_INIT = "{} 模型初始化完成，模型: {}，是否自定义 baseUrl: {}";

    /**
     * 模型未启用日志格式
     */
    public static final String FORMAT_MODEL_NOT_ENABLED = "检测到{}能力未启用，跳过模型初始化";

    /**
     * 模型提供商不匹配日志格式
     */
    public static final String FORMAT_PROVIDER_MISMATCH = "当前大模型提供商不是 {}，跳过模型初始化，provider: {}";

    /**
     * 加载历史消息日志格式
     */
    public static final String FORMAT_LOAD_HISTORY = "已装载最近历史消息 {} 条，用户会话: {}，traceId: {}";

    /**
     * 持久化异常日志格式
     */
    public static final String FORMAT_PERSISTENCE_ERROR = "记录失败消息时发生异常，traceId: {}，原因: {}";

    // ==================== 通用日志输出方法 ====================

    /**
     * 输出 INFO 级别日志
     *
     * @param logger SLF4J Logger 实例
     * @param format 日志格式
     * @param args 参数列表
     */
    public static void info(Logger logger, String format, Object... args) {
        logger.info(format, args);
    }

    /**
     * 输出 WARN 级别日志
     *
     * @param logger SLF4J Logger 实例
     * @param format 日志格式
     * @param args 参数列表
     */
    public static void warn(Logger logger, String format, Object... args) {
        logger.warn(format, args);
    }

    /**
     * 输出 ERROR 级别日志
     *
     * @param logger SLF4J Logger 实例
     * @param format 日志格式
     * @param args 参数列表
     */
    public static void error(Logger logger, String format, Object... args) {
        logger.error(format, args);
    }

    /**
     * 输出 ERROR 级别日志（带异常）
     *
     * @param logger SLF4J Logger 实例
     * @param format 日志格式
     * @param throwable 异常对象
     * @param args 参数列表
     */
    public static void error(Logger logger, String format, Throwable throwable, Object... args) {
        logger.error(format, args, throwable);
    }

    // ==================== 便捷日志方法 ====================

    /**
     * 输出开始调用日志
     *
     * @param logger SLF4J Logger 实例
     * @param provider 提供商名称
     * @param conversationKey 用户会话键
     * @param traceId 追踪 ID
     * @param model 模型名称
     */
    public static void logStartCall(Logger logger, String provider, String conversationKey, 
                                    String traceId, String model) {
        logger.info(FORMAT_START_CALL, provider, conversationKey, traceId, model);
    }

    /**
     * 输出调用成功日志
     *
     * @param logger SLF4J Logger 实例
     * @param provider 提供商名称
     * @param conversationKey 用户会话键
     * @param traceId 追踪 ID
     * @param durationMs 耗时（毫秒）
     * @param totalTokens 总 Token 数
     */
    public static void logCallSuccess(Logger logger, String provider, String conversationKey, 
                                      String traceId, long durationMs, Integer totalTokens) {
        logger.info(FORMAT_CALL_SUCCESS, provider, conversationKey, traceId, durationMs, totalTokens);
    }

    /**
     * 输出调用失败日志
     *
     * @param logger SLF4J Logger 实例
     * @param provider 提供商名称
     * @param conversationKey 用户会话键
     * @param traceId 追踪 ID
     * @param durationMs 耗时（毫秒）
     * @param errorMessage 错误信息
     * @param throwable 异常对象
     */
    public static void logCallError(Logger logger, String provider, String conversationKey, 
                                    String traceId, long durationMs, String errorMessage, Throwable throwable) {
        logger.error(FORMAT_CALL_ERROR, provider, conversationKey, traceId, durationMs, errorMessage, throwable);
    }

    /**
     * 输出参数验证失败日志
     *
     * @param logger SLF4J Logger 实例
     * @param provider 提供商名称
     * @param traceId 追踪 ID
     * @param validationMessage 验证失败信息
     */
    public static void logValidationError(Logger logger, String provider, String traceId, String validationMessage) {
        logger.warn(FORMAT_VALIDATION_ERROR, provider, traceId, validationMessage);
    }

    /**
     * 输出加载历史消息日志
     *
     * @param logger SLF4J Logger 实例
     * @param historySize 历史消息数量
     * @param conversationKey 用户会话键
     * @param traceId 追踪 ID
     */
    public static void logLoadHistory(Logger logger, int historySize, String conversationKey, String traceId) {
        logger.info(FORMAT_LOAD_HISTORY, historySize, conversationKey, traceId);
    }

    /**
     * 输出模型初始化完成日志
     *
     * @param logger SLF4J Logger 实例
     * @param provider 提供商名称
     * @param model 模型名称
     * @param hasCustomBaseUrl 是否自定义 baseUrl
     */
    public static void logModelInit(Logger logger, String provider, String model, boolean hasCustomBaseUrl) {
        logger.info(FORMAT_MODEL_INIT, provider, model, hasCustomBaseUrl);
    }

    /**
     * 输出模型未启用日志
     *
     * @param logger SLF4J Logger 实例
     * @param provider 提供商名称
     */
    public static void logModelNotEnabled(Logger logger, String provider) {
        logger.warn(FORMAT_MODEL_NOT_ENABLED, provider);
    }

    /**
     * 输出提供商不匹配日志
     *
     * @param logger SLF4J Logger 实例
     * @param expectedProvider 期望的提供商
     * @param actualProvider 实际的提供商
     */
    public static void logProviderMismatch(Logger logger, String expectedProvider, String actualProvider) {
        logger.warn(FORMAT_PROVIDER_MISMATCH, expectedProvider, actualProvider);
    }

    /**
     * 输出持久化异常日志
     *
     * @param logger SLF4J Logger 实例
     * @param traceId 追踪 ID
     * @param errorMessage 错误信息
     * @param throwable 异常对象
     */
    public static void logPersistenceError(Logger logger, String traceId, String errorMessage, Throwable throwable) {
        logger.error(FORMAT_PERSISTENCE_ERROR, traceId, errorMessage, throwable);
    }
}
