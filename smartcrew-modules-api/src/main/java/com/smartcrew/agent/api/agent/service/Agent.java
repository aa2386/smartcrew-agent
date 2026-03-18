package com.smartcrew.agent.api.agent.service;

import com.smartcrew.agent.api.agent.domain.model.AgentDispatchCommand;
import com.smartcrew.agent.api.agent.domain.vo.AgentDispatchResponse;

/**
 * Agent 接口，定义智能体实现需要遵循的基础能力约定。
 */
public interface Agent {

    /**
     * 返回 Agent 编码。
     *
     * @return 对应编码。
     */
    String code();

    /**
     * 返回 Agent 名称。
     *
     * @return 对应名称。
     */
    String name();

    /**
     * 判断当前实现是否支持指定能力。
     *
     * @param capability 能力标识。
     * @return `true` 表示支持，`false` 表示不支持。
     */
    boolean supports(String capability);

    /**
     * 处理派发命令并返回执行结果。
     *
     * @param command 派发命令。
     * @return Agent 处理后的响应结果。
     */
    AgentDispatchResponse handle(AgentDispatchCommand command);
}
