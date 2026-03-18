package com.smartcrew.agent.api.agent.service;

import com.smartcrew.agent.api.agent.domain.model.MessageEnvelope;

/**
 * AgentMessageBus 接口，定义系统内消息投递与分发能力。
 */
public interface AgentMessageBus {

    void publish(String topic, MessageEnvelope envelope);

    void subscribe(String topic, AgentMessageHandler handler);
}
