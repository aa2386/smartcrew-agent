package com.smartcrew.agent.api.agent.service;

import com.smartcrew.agent.api.agent.domain.entity.AgentDefinition;

import java.util.List;
import java.util.Optional;

/**
 * ????????????????????????
 */
public interface AgentRegistry {

    void register(Agent agent, AgentDefinition definition);

    Optional<Agent> get(String agentCode);

    Optional<AgentDefinition> getDefinition(String agentCode);

    List<AgentDefinition> listDefinitions();

    boolean contains(String agentCode);
}
