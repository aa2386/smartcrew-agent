package com.smartcrew.agent.api.agent.service;

import com.smartcrew.agent.api.agent.domain.request.AgentDispatchRequest;
import com.smartcrew.agent.api.agent.domain.vo.AgentDispatchResponse;

/**
 * ?????????????????????
 */
public interface AgentCoordinator {

    AgentDispatchResponse dispatch(String agentCode, AgentDispatchRequest request);
}
