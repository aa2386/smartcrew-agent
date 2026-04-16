package com.smartcrew.agent.api.agent.service;

import com.smartcrew.agent.api.admin.domain.request.AgentToolBindingUpdateRequest;
import com.smartcrew.agent.api.agent.domain.vo.AgentToolBindingVo;
import com.smartcrew.agent.api.tool.domain.model.ResolvedToolDefinition;

import java.util.List;
import java.util.Set;

/**
 * Agent Tool 绑定服务。
 */
public interface AgentToolBindingService {

    /**
     * 查询 Agent 的 Tool 绑定关系视图。
     */
    AgentToolBindingVo getBindings(String agentCode);

    /**
     * 替换 Agent 的 Tool 绑定关系。
     */
    AgentToolBindingVo replaceBindings(String agentCode, AgentToolBindingUpdateRequest request);

    /**
     * 查询 Agent 已绑定的 Tool 编码。
     */
    Set<String> listBoundToolCodes(String agentCode);

    /**
     * 查询 Agent 已绑定的运行时 Tool 定义。
     */
    List<ResolvedToolDefinition> listResolvedToolsByAgentCode(String agentCode);

    /**
     * 查询 Agent 已绑定且启用的运行时 Tool 定义。
     */
    List<ResolvedToolDefinition> listEnabledResolvedToolsByAgentCode(String agentCode);
}
