package com.smartcrew.agent.api.agent.service;

import com.smartcrew.agent.api.agent.domain.entity.AgentDefinition;
import com.smartcrew.agent.api.agent.domain.request.AgentRegisterRequest;
import com.smartcrew.agent.api.agent.domain.vo.AgentDefinitionVo;

import java.util.List;
import java.util.Optional;

/**
 * ??????????????????????
 */
public interface AgentDefinitionService {

    AgentDefinition register(AgentRegisterRequest request);

    List<AgentDefinitionVo> listAll();

    Optional<AgentDefinition> findByCode(String agentCode);
}
