package com.smartcrew.agent.api.tool.domain.request;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tool 手动执行请求。
 */
@Data
public class ToolExecutionRequest {

    /**
     * 动作名称。
     */
    private String actionName;

    /**
     * 工具参数。
     */
    private Map<String, Object> arguments = new LinkedHashMap<>();

    /**
     * 执行上下文。
     */
    private Map<String, Object> executionContext = new LinkedHashMap<>();
}
