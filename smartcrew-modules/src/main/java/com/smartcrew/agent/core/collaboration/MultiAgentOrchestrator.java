package com.smartcrew.agent.core.collaboration;

import com.smartcrew.agent.api.agent.domain.model.AgentDispatchCommand;
import com.smartcrew.agent.api.agent.domain.vo.AgentDispatchResponse;

/**
 * 多智能体编排接口。
 */
public interface MultiAgentOrchestrator {

    /**
     * 执行一次多智能体协作。
     */
    AgentDispatchResponse orchestrate(AgentDispatchCommand command);
}
