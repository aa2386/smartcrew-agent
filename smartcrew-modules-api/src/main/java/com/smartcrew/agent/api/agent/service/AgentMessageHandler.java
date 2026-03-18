package com.smartcrew.agent.api.agent.service;

import com.smartcrew.agent.api.agent.domain.model.MessageEnvelope;

/**
 * AgentMessageHandler 接口，定义消息处理器的处理约定。
 */
@FunctionalInterface
public interface AgentMessageHandler {

    /**
     * 处理派发命令并返回执行结果。
     *
     * @param envelope 消息封装对象。
     */
    void handle(MessageEnvelope envelope);
}
