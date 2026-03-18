package com.smartcrew.agent.api.agent.service;

import com.smartcrew.agent.api.agent.domain.model.MessageEnvelope;

/**
 * AgentMessageHandler 接口，定义消息处理器的处理约定。
 */
@FunctionalInterface
public interface AgentMessageHandler {

    void handle(MessageEnvelope envelope);
}
