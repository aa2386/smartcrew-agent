package com.smartcrew.agent.api.agent.domain.vo;

import com.smartcrew.agent.api.tool.domain.vo.ToolDefinitionVo;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Agent Tool 绑定视图。
 */
@Data
public class AgentToolBindingVo {

    /**
     * Agent 编码。
     */
    private String agentCode;

    /**
     * 已绑定 Tool 列表。
     */
    private List<ToolDefinitionVo> boundTools = new ArrayList<>();

    /**
     * 可选 Tool 列表。
     */
    private List<ToolDefinitionVo> availableTools = new ArrayList<>();
}
