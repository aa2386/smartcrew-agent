package com.smartcrew.agent.api.agent.service;

import com.smartcrew.agent.api.agent.domain.model.MessageEnvelope;

/**
 * ?????????????????????
 */
public interface AgentMessageBus {

    void publish(String topic, MessageEnvelope envelope);

    void subscribe(String topic, AgentMessageHandler handler);
}
