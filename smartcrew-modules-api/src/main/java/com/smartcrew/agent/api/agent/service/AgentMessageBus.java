package com.smartcrew.agent.api.agent.service;

import com.smartcrew.agent.api.agent.domain.model.MessageEnvelope;

/**
 * AgentMessageBus 接口，定义系统内消息投递与分发能力。
 */
public interface AgentMessageBus {

    /**
     * 向指定主题发布 Agent 消息。
     *
     * @param topic 消息主题。
     * @param envelope 消息封装对象。
     */
    void publish(String topic, MessageEnvelope envelope);

    /**
     * 订阅指定主题的 Agent 消息。
     *
     * @param topic 消息主题。
     * @param handler 消息处理器。
     */
    void subscribe(String topic, AgentMessageHandler handler);
}
