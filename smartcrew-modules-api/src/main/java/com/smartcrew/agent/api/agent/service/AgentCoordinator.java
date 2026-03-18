package com.smartcrew.agent.api.agent.service;

import com.smartcrew.agent.api.agent.domain.request.AgentDispatchRequest;
import com.smartcrew.agent.api.agent.domain.vo.AgentDispatchResponse;

/**
 * AgentCoordinator 接口，负责多组件之间的协同与流程编排。
 */
public interface AgentCoordinator {

    /**
     * 按 Agent 编码派发请求。
     *
     * @param agentCode Agent 编码。
     * @param request 请求参数。
     * @return 派发执行结果。
     */
    AgentDispatchResponse dispatch(String agentCode, AgentDispatchRequest request);
}
