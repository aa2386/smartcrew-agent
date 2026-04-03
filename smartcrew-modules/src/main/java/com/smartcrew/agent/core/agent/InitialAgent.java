package com.smartcrew.agent.core.agent;

import com.smartcrew.agent.api.agent.domain.model.AgentDispatchCommand;
import com.smartcrew.agent.api.agent.domain.vo.AgentDispatchResponse;
import com.smartcrew.agent.api.agent.service.Agent;

/**
 * - 它是用户唯一入口 Agent
 * - 它是会话主控 Agent
 * - 它是长期偏好统一读写者
 * - 它是 RAG 检索增强的决策者
 * - 它是工具调用协调者
 * - 它未来还是多 Agent 协作的总控 Agent
 * - 改多Agent时再将该Agent逐步拆解分权
 */
public class InitialAgent implements Agent {
    @Override
    public String code() {
        return null;
    }

    @Override
    public String name() {
        return null;
    }

    @Override
    public boolean supports(String capability) {
        return false;
    }

    @Override
    public AgentDispatchResponse handle(AgentDispatchCommand command) {
        return null;
    }
}
