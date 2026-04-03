package com.smartcrew.agent.core.agent.service;

import com.smartcrew.agent.api.agent.domain.entity.AgentDefinition;
import com.smartcrew.agent.api.agent.service.Agent;
import com.smartcrew.agent.api.agent.service.AgentDefinitionService;
import com.smartcrew.agent.api.agent.service.AgentDiscoveryService;
import com.smartcrew.agent.api.agent.service.AgentRegistry;
import com.smartcrew.agent.core.agent.StubAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 代理发现服务实现，负责在应用启动时注册内置代理和数据库代理。
 */
@RequiredArgsConstructor
@Service
public class AgentDiscoveryServiceImpl implements AgentDiscoveryService {

    /**
     * 内置代理列表。
     */
    private final List<Agent> builtinAgents;
    /**
     * 代理注册表。
     */
    private final AgentRegistry agentRegistry;
    /**
     * 代理定义服务。
     */
    private final AgentDefinitionService agentDefinitionService;

    /**
     * 发现并注册代理。
     */
    @Override
    @EventListener(ApplicationReadyEvent.class)
    public void discoverAndRegister() {
        builtinAgents.forEach(agent -> {
            AgentDefinition definition = agentDefinitionService.findByCode(agent.code())
                    .orElseGet(() -> defaultDefinition(agent));
            agentRegistry.register(agent, definition);
        });
        if (agentDefinitionService instanceof AgentDefinitionServiceImpl impl) {
            impl.listDatabaseDefinitions().forEach(definition -> {
                if (!agentRegistry.contains(definition.getAgentCode())) {
                    agentRegistry.register(new StubAgent(definition), definition);
                }
            });
        }
    }

    /**
     * 构建默认代理定义。
     */
    private AgentDefinition defaultDefinition(Agent agent) {
        AgentDefinition definition = new AgentDefinition();
        definition.setAgentCode(agent.code());
        definition.setAgentName(agent.name());
        definition.setAgentType("BUILTIN");
        definition.setDescription("Builtin agent bean");
        definition.setStrategyType("REACT");
        definition.setEnabled(true);
        return definition;
    }
}
