package com.smartcrew.agent.core.agent;

import com.smartcrew.agent.api.agent.domain.model.AgentDispatchCommand;
import com.smartcrew.agent.api.agent.domain.vo.AgentDispatchResponse;
import com.smartcrew.agent.api.agent.service.Agent;
import org.springframework.stereotype.Component;

/**
 * 示例规划代理实现，用于返回占位的规划受理结果。
 */
@Component
public class PlannerAgent implements Agent {

    /**
     * 返回编码标识。
     */
    @Override
    public String code() {
        return "planner-agent";
    }

    /**
     * 返回名称。
     */
    @Override
    public String name() {
        return "Planner Agent";
    }

    /**
     * 判断是否支持指定能力或目标。
     */
    @Override
    public boolean supports(String capability) {
        return "plan".equalsIgnoreCase(capability) || "decision".equalsIgnoreCase(capability);
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
                .message("Planner agent accepted task for session " + command.getSessionId())
                .build();
    }
}
