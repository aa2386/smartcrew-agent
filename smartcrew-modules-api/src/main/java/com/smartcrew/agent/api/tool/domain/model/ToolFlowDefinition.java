package com.smartcrew.agent.api.tool.domain.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据库 Tool 流程定义。
 */
@Data
public class ToolFlowDefinition {

    /**
     * Flow Tool 对外动作名，默认为 execute。
     */
    private String actionName = "execute";

    /**
     * Flow Tool 动作描述。
     */
    private String description;

    /**
     * 顺序步骤列表。
     */
    private List<ToolFlowStep> steps = new ArrayList<>();
}
