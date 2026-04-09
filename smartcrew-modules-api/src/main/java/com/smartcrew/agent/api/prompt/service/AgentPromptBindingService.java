package com.smartcrew.agent.api.prompt.service;

import com.smartcrew.agent.api.admin.domain.request.AgentPromptBindingUpdateRequest;
import com.smartcrew.agent.api.prompt.domain.vo.AgentPromptBindingVo;

import java.util.List;

/**
 * Agent Prompt 绑定关系服务。
 */
public interface AgentPromptBindingService {

    /**
     * 查询 Agent 当前配置的绑定列表，供后台展示。
     */
    List<AgentPromptBindingVo> listByAgentCode(String agentCode);

    /**
     * 查询运行时可用的 Prompt 绑定列表，缺失模板会被跳过。
     */
    List<AgentPromptBindingVo> listResolvedByAgentCode(String agentCode);

    /**
     * 用整列表替换 Agent 的 Prompt 绑定关系。
     */
    List<AgentPromptBindingVo> replaceBindings(String agentCode, AgentPromptBindingUpdateRequest request);
}
