package com.smartcrew.agent.api.tool.domain.model;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Flow Tool 单个步骤定义。
 */
@Data
public class ToolFlowStep {

    /**
     * 步骤类型：template / tool_call / return。
     */
    private String type;

    /**
     * 输出变量名。
     */
    private String output;

    /**
     * 模板内容，可为字符串、对象或数组。
     */
    private Object template;

    /**
     * 被调用工具编码。
     */
    private String toolCode;

    /**
     * 被调用动作名称。
     */
    private String actionName;

    /**
     * 被调用工具参数模板。
     */
    private Map<String, Object> arguments = new LinkedHashMap<>();
}
