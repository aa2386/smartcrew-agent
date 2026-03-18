package com.smartcrew.agent.api.tool.service;

import java.util.Map;

/**
 * ToolExecutor 接口，定义工具执行与结果返回的标准行为。
 */
public interface ToolExecutor {

    /**
     * 执行指定工具并返回执行结果。
     *
     * @param toolCode 工具编码。
     * @param Map<String 方法参数。
     * @param arguments 工具执行参数。
     * @return 工具执行结果。
     */
    Object execute(String toolCode, Map<String, Object> arguments);
}
