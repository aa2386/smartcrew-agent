package com.smartcrew.agent.api.agent.service;

import com.smartcrew.agent.api.agent.domain.model.MessageEnvelope;

/**
 * ?????????????????????????
 */
@FunctionalInterface
public interface AgentMessageHandler {

    void handle(MessageEnvelope envelope);
}
