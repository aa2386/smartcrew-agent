package com.smartcrew.agent.core.agent;

import com.smartcrew.agent.api.agent.domain.entity.AgentDefinition;
import com.smartcrew.agent.api.agent.service.Agent;
import com.smartcrew.agent.api.agent.service.AgentRegistry;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于内存的代理注册表实现，用于保存运行期代理实例与定义。
 */
@Component
public class InMemoryAgentRegistry implements AgentRegistry {

    /**
     * 代理实例缓存。
     */
    private final ConcurrentHashMap<String, Agent> agents = new ConcurrentHashMap<>();
    /**
     * 代理定义缓存。
     */
    private final ConcurrentHashMap<String, AgentDefinition> definitions = new ConcurrentHashMap<>();

    /**
     * 注册或更新目标对象。
     */
    @Override
    public void register(Agent agent, AgentDefinition definition) {
        agents.put(agent.code(), agent);
        definitions.put(agent.code(), definition);
    }

    /**
     * 按标识获取对象。
     */
    @Override
    public Optional<Agent> get(String agentCode) {
        return Optional.ofNullable(agents.get(agentCode));
    }

    /**
     * 按代理编码获取代理定义。
     */
    @Override
    public Optional<AgentDefinition> getDefinition(String agentCode) {
        return Optional.ofNullable(definitions.get(agentCode));
    }

    /**
     * 返回当前注册表中的全部代理定义。
     */
    @Override
    public List<AgentDefinition> listDefinitions() {
        return new ArrayList<>(definitions.values());
    }

    /**
     * 判断是否包含指定编码。
     */
    @Override
    public boolean contains(String agentCode) {
        return agents.containsKey(agentCode);
    }
}
