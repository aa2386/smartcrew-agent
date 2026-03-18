package com.smartcrew.agent.core.agent;

import com.smartcrew.agent.api.agent.domain.entity.AgentDefinition;
import com.smartcrew.agent.api.agent.domain.model.AgentDispatchCommand;
import com.smartcrew.agent.api.agent.domain.vo.AgentDispatchResponse;
import com.smartcrew.agent.api.agent.service.Agent;

/**
 * 占位代理实现，用于为数据库中存在但未提供 Bean 的代理生成兜底行为。
 */
public class StubAgent implements Agent {

    /**
     * 代理定义信息。
     */
    private final AgentDefinition definition;

    /**
     * 构造 StubAgent 所需的依赖对象。
     */
    public StubAgent(AgentDefinition definition) {
        this.definition = definition;
    }

    /**
     * 返回编码标识。
     */
    @Override
    public String code() {
        return definition.getAgentCode();
    }

    /**
     * 返回名称。
     */
    @Override
    public String name() {
        return definition.getAgentName();
    }

    /**
     * 判断是否支持指定能力或目标。
     */
    @Override
    public boolean supports(String capability) {
        return true;
    }

    /**
     * 处理当前请求。
     */
    @Override
    public AgentDispatchResponse handle(AgentDispatchCommand command) {
        return AgentDispatchResponse.builder()
                .traceId(command.getTraceId())
                .agentCode(command.getAgentCode())
                .accepted(true)
                .message("Stub agent accepted request: " + command.getMessage())
                .build();
    }
}
