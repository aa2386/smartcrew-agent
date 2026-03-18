package com.smartcrew.agent.core.agent;

import com.smartcrew.agent.api.agent.domain.model.MessageEnvelope;
import com.smartcrew.agent.api.agent.service.AgentMessageBus;
import com.smartcrew.agent.api.agent.service.AgentMessageHandler;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 基于内存的代理消息总线实现，适用于本地开发和占位场景。
 */
@Component
public class InMemoryAgentMessageBus implements AgentMessageBus {

    /**
     * 按主题维护的消息处理器集合。
     */
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<AgentMessageHandler>> handlers = new ConcurrentHashMap<>();

    /**
     * 发布消息到指定主题。
     */
    @Override
    public void publish(String topic, MessageEnvelope envelope) {
        List<AgentMessageHandler> topicHandlers = handlers.getOrDefault(topic, new CopyOnWriteArrayList<>());
        topicHandlers.forEach(handler -> handler.handle(envelope));
    }

    /**
     * 订阅指定主题消息。
     */
    @Override
    public void subscribe(String topic, AgentMessageHandler handler) {
        handlers.computeIfAbsent(topic, key -> new CopyOnWriteArrayList<>()).add(handler);
    }
}
