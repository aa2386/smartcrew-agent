package com.smartcrew.agent.core.agent;

import com.smartcrew.agent.api.agent.domain.model.AgentDispatchCommand;
import com.smartcrew.agent.api.agent.domain.model.MessageEnvelope;
import com.smartcrew.agent.api.agent.domain.request.AgentDispatchRequest;
import com.smartcrew.agent.api.agent.domain.vo.AgentDispatchResponse;
import com.smartcrew.agent.api.agent.service.Agent;
import com.smartcrew.agent.api.agent.service.AgentCoordinator;
import com.smartcrew.agent.api.agent.service.AgentMessageBus;
import com.smartcrew.agent.api.agent.service.AgentRegistry;
import com.smartcrew.agent.common.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 代理协调服务实现，负责组装派发命令、发布消息并调用目标代理。
 */
@RequiredArgsConstructor
@Service
public class AgentCoordinatorImpl implements AgentCoordinator {

    /**
     * 代理注册表。
     */
    private final AgentRegistry agentRegistry;
    /**
     * 代理消息总线。
     */
    private final AgentMessageBus agentMessageBus;

    /**
     * 按代理编码派发请求。
     */
    @Override
    public AgentDispatchResponse dispatch(String agentCode, AgentDispatchRequest request) {
        Agent agent = agentRegistry.get(agentCode)
                .orElseThrow(() -> new ServiceException("Unknown agent: " + agentCode));
        String traceId = UUID.randomUUID().toString();
        AgentDispatchCommand command = AgentDispatchCommand.builder()
                .traceId(traceId)
                .agentCode(agentCode)
                .userId(request.getUserId())
                .sessionId(request.getSessionId())
                .message(request.getMessage())
                .context(request.getContext())
                .build();
        MessageEnvelope envelope = MessageEnvelope.builder()
                .traceId(traceId)
                .sourceAgent("api-gateway")
                .targetAgent(agentCode)
                .userId(request.getUserId())
                .sessionId(request.getSessionId())
                .payload(request.getMessage())
                .metadata(request.getContext())
                .createdAt(LocalDateTime.now())
                .build();
        agentMessageBus.publish(agentCode, envelope);
        return agent.handle(command);
    }
}
