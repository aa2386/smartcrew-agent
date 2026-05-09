package com.smartcrew.agent.core.agent;

import com.smartcrew.agent.api.agent.domain.model.AgentDispatchCommand;
import com.smartcrew.agent.api.agent.domain.vo.AgentDispatchResponse;
import com.smartcrew.agent.api.agent.service.Agent;
import com.smartcrew.agent.api.agent.service.AgentChatRuntimeService;
import org.springframework.stereotype.Component;

/**
 * 初始智能体实现，三 Agent 协作体系中的主 Agent。
 *
 * <p>作为生活日程方向的主 Agent，直接与用户对话，负责需求澄清、意图理解、
 * 任务拆解、委托工具 Agent/记忆 Agent，并汇总最终回复。</p>
 *
 * <p>核心能力：chat（对话）、orchestrate（编排调度）、rag（检索增强）、schedule（日程协调）。</p>
 *
 * <p>处理流程委托给 {@link AgentChatRuntimeService} 统一执行，该服务封装了
 * LLM 调用、RAG 增强、Prompt 组装、会话锁、ToolCallContext 管理与降级持久化的完整链路。</p>
 *
 * @see Agent
 * @see AgentChatRuntimeService
 */
@Component
public class InitialAgent implements Agent {

    private final AgentChatRuntimeService chatRuntimeService;

    /**
     * 构造主 Agent 实例。
     *
     * @param chatRuntimeService 通用 Agent 对话运行时服务
     */
    public InitialAgent(AgentChatRuntimeService chatRuntimeService) {
        this.chatRuntimeService = chatRuntimeService;
    }

    /**
     * 返回 Agent 编码标识。
     *
     * @return 固定值 "initial-agent"
     */
    @Override
    public String code() {
        return "initial-agent";
    }

    /**
     * 返回 Agent 显示名称。
     *
     * @return 固定值 "初始智能体"
     */
    @Override
    public String name() {
        return "初始智能体";
    }

    /**
     * 判断 Agent 是否支持指定能力。
     *
     * @param capability 能力标识
     * @return 支持 chat、orchestrate、rag、schedule 时返回 true
     */
    @Override
    public boolean supports(String capability) {
        return "chat".equalsIgnoreCase(capability)
                || "orchestrate".equalsIgnoreCase(capability)
                || "rag".equalsIgnoreCase(capability)
                || "schedule".equalsIgnoreCase(capability);
    }

    /**
     * 处理智能体调度命令，委托通用运行时执行完整对话推理流程。
     *
     * @param command 智能体调度命令，包含用户消息、会话信息及上下文
     * @return 调度响应，包含推理结果或错误信息
     */
    @Override
    public AgentDispatchResponse handle(AgentDispatchCommand command) {
        // 确保 agentCode 正确设置，以备委托链路透传
        command.setAgentCode(code());
        return chatRuntimeService.process(command);
    }
}
