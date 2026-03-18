package com.smartcrew.agent.core.agent;

import com.smartcrew.agent.api.agent.domain.model.AgentDispatchCommand;
import com.smartcrew.agent.api.agent.domain.vo.AgentDispatchResponse;
import com.smartcrew.agent.api.agent.service.Agent;
import org.springframework.stereotype.Component;

/**
 * 示例回显代理实现，直接返回用户输入内容。
 */
@Component
public class EchoAgent implements Agent {

    /**
     * 返回编码标识。
     */
    @Override
    public String code() {
        return "echo-agent";
    }

    /**
     * 返回名称。
     */
    @Override
    public String name() {
        return "Echo Agent";
    }

    /**
     * 判断是否支持指定能力或目标。
     */
    @Override
    public boolean supports(String capability) {
        return "echo".equalsIgnoreCase(capability) || "chat".equalsIgnoreCase(capability);
    }

    /**
     * 处理当前请求。
     */
    @Override
    public AgentDispatchResponse handle(AgentDispatchCommand command) {
        return AgentDispatchResponse.builder()
                .traceId(command.getTraceId())
                .agentCode(code())
                .accepted(true)
                .message("Echo response: " + command.getMessage())
                .build();
    }
}
