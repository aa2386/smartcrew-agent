package com.smartcrew.agent.api.tool.domain.model;

/**
 * 工具执行模式常量，定义工具的执行方式。
 *
 * <p>当前支持的模式：</p>
 * <ul>
 *   <li>{@link #BEAN} - Spring Bean 模式，通过容器中注册的 Bean 执行工具逻辑</li>
 * </ul>
 */
public final class ToolExecutionModes {

    public static final String BEAN = "BEAN";

    private ToolExecutionModes() {
    }
}
