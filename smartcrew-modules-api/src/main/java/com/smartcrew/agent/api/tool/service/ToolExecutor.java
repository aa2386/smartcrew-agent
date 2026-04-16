package com.smartcrew.agent.api.tool.service;

import com.smartcrew.agent.api.tool.domain.model.ToolExecutionResult;

import java.util.Collections;
import java.util.Map;

/**
 * ToolExecutor 接口，定义 Tool 执行与结果返回的标准行为。
 */
public interface ToolExecutor {

    /**
     * 执行指定 Tool 动作并返回结构化结果。
     *
     * @param toolCode 工具编码
     * @param actionName 动作名称，允许为空；为空时会尝试解析默认动作
     * @param arguments 动作参数
     * @param executionContext 执行上下文
     * @return 执行结果
     */
    ToolExecutionResult execute(String toolCode,
                                String actionName,
                                Map<String, Object> arguments,
                                Map<String, Object> executionContext);

    /**
     * 兼容旧签名：当 Tool 只有一个动作时可继续直接调用。
     */
    default ToolExecutionResult execute(String toolCode, Map<String, Object> arguments) {
        return execute(toolCode, null, arguments, Collections.emptyMap());
    }
}
