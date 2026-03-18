package com.smartcrew.agent.api.agent.service;

import com.smartcrew.agent.api.agent.domain.request.AgentDispatchRequest;
import com.smartcrew.agent.api.agent.domain.vo.AgentDispatchResponse;

/**
 * AgentCoordinator 接口，负责多组件之间的协同与流程编排。
 */
public interface AgentCoordinator {

    AgentDispatchResponse dispatch(String agentCode, AgentDispatchRequest request);
}
