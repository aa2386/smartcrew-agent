package com.smartcrew.agent.api.agent.service;

import com.smartcrew.agent.api.agent.domain.entity.AgentDefinition;

import java.util.List;
import java.util.Optional;

/**
 * AgentRegistry 接口，负责相关组件的注册、查询与管理。
 */
public interface AgentRegistry {

    /**
     * 注册 Agent 实例及其定义。
     *
     * @param agent Agent 实例。
     * @param definition Agent 定义。
     */
    void register(Agent agent, AgentDefinition definition);

    /**
     * 按编码查询已注册 Agent。
     *
     * @param agentCode Agent 编码。
     * @return 匹配结果；未找到时返回空 `Optional`。
     */
    Optional<Agent> get(String agentCode);

    /**
     * 按编码查询 Agent 定义。
     *
     * @param agentCode Agent 编码。
     * @return 匹配结果；未找到时返回空 `Optional`。
     */
    Optional<AgentDefinition> getDefinition(String agentCode);

    /**
     * 返回当前已注册的 Agent 定义列表。
     *
     * @return 结果列表。
     */
    List<AgentDefinition> listDefinitions();

    /**
     * 判断是否包含指定编码。
     *
     * @param agentCode Agent 编码。
     * @return `true` 表示存在，`false` 表示不存在。
     */
    boolean contains(String agentCode);
}
