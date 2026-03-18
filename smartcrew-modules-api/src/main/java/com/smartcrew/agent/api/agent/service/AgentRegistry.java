package com.smartcrew.agent.api.agent.service;

import com.smartcrew.agent.api.agent.domain.entity.AgentDefinition;

import java.util.List;
import java.util.Optional;

/**
 * AgentRegistry 接口，负责相关组件的注册、查询与管理。
 */
public interface AgentRegistry {

    void register(Agent agent, AgentDefinition definition);

    Optional<Agent> get(String agentCode);

    Optional<AgentDefinition> getDefinition(String agentCode);

    List<AgentDefinition> listDefinitions();

    boolean contains(String agentCode);
}
