package com.smartcrew.agent.api.tool.service;

import java.util.Map;

/**
 * ToolExecutor 接口，定义工具执行与结果返回的标准行为。
 */
public interface ToolExecutor {

    Object execute(String toolCode, Map<String, Object> arguments);
}
