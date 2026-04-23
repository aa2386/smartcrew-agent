package com.smartcrew.agent.core.tool;

import java.util.Collections;
import java.util.Map;

/**
 * 工具调用上下文持有器，基于 ThreadLocal 在请求线程内传递追踪ID与上下文数据。
 *
 * <p>在智能体处理请求时设置上下文，在工具执行时读取，
 * 请求处理完成后自动清理，避免内存泄漏。</p>
 *
 * @see ToolCallContext
 */
public final class ToolCallContextHolder {

    private static final ThreadLocal<ToolCallContext> HOLDER = new ThreadLocal<>();

    /**
     * 私有构造，防止实例化。
     */
    private ToolCallContextHolder() {
    }

    /**
     * 设置当前线程的工具调用上下文。
     *
     * @param traceId 追踪ID，用于链路追踪
     * @param context 上下文数据映射，为 null 时使用空 Map
     */
    public static void set(String traceId, Map<String, Object> context) {
        HOLDER.set(new ToolCallContext(traceId, context == null ? Collections.emptyMap() : context));
    }

    /**
     * 获取当前线程的工具调用上下文。
     *
     * @return 工具调用上下文，未设置时返回 null
     */
    public static ToolCallContext get() {
        return HOLDER.get();
    }

    /**
     * 清除当前线程的工具调用上下文，防止内存泄漏。
     */
    public static void clear() {
        HOLDER.remove();
    }

    /**
     * 工具调用上下文数据，包含追踪ID和附加上下文信息。
     *
     * @param traceId 追踪ID
     * @param context 上下文数据映射
     */
    public record ToolCallContext(String traceId, Map<String, Object> context) {
    }
}
