package com.smartcrew.agent.api.agent.service;

import com.smartcrew.agent.api.agent.domain.model.AgentDispatchCommand;
import com.smartcrew.agent.api.agent.domain.vo.AgentDispatchResponse;

/**
 * Agent 接口，定义智能体实现需要遵循的基础能力约定。
 */
public interface Agent {

    String code();

    String name();

    boolean supports(String capability);

    AgentDispatchResponse handle(AgentDispatchCommand command);
}
