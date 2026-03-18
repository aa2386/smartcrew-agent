package com.smartcrew.agent.api.agent.service;

import com.smartcrew.agent.api.agent.domain.model.AgentDispatchCommand;
import com.smartcrew.agent.api.agent.domain.vo.AgentDispatchResponse;

/**
 * ???????????????????????????
 */
public interface Agent {

    String code();

    String name();

    boolean supports(String capability);

    AgentDispatchResponse handle(AgentDispatchCommand command);
}
