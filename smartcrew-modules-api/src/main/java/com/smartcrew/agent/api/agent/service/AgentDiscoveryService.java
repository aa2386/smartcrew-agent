package com.smartcrew.agent.api.agent.service;

/**
 * AgentDiscoveryService 接口，负责发现并提供可用组件信息。
 */
public interface AgentDiscoveryService {

    /**
     * 发现并注册可用 Agent。
     */
    void discoverAndRegister();
}
