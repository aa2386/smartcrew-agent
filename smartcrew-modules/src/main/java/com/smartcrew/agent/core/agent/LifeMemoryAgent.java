package com.smartcrew.agent.core.agent;

import com.smartcrew.agent.api.agent.domain.model.AgentDispatchCommand;
import com.smartcrew.agent.api.agent.domain.vo.AgentDispatchResponse;
import com.smartcrew.agent.api.agent.service.Agent;
import com.smartcrew.agent.api.agent.service.AgentChatRuntimeService;
import org.springframework.stereotype.Component;

/**
 * 生活日程记忆 Agent，负责用户偏好、历史对话摘要、任务记录等记忆读写。
 *
 * <p>不直接面对用户，由主 Agent 通过委托工具调用。只做用户范围内的记忆处理，
 * 不调用外部执行工具，不保存敏感信息，写入推断偏好时保持保守。</p>
 *
 * <p>核心能力：memory（记忆读写）、preference（偏好管理）、history（历史读取）、task-record（任务记录）。</p>
 *
 * @see Agent
 * @see AgentChatRuntimeService
 */
@Component
public class LifeMemoryAgent implements Agent {

    private final AgentChatRuntimeService chatRuntimeService;

    /**
     * 构造记忆 Agent 实例。
     *
     * @param chatRuntimeService 通用 Agent 对话运行时服务
     */
    public LifeMemoryAgent(AgentChatRuntimeService chatRuntimeService) {
        this.chatRuntimeService = chatRuntimeService;
    }

    /**
     * 返回 Agent 编码。
     *
     * @return 固定值 "life-memory-agent"
     */
    @Override
    public String code() {
        return "life-memory-agent";
    }

    /**
     * 返回 Agent 名称。
     *
     * @return 固定值 "生活日程记忆 Agent"
     */
    @Override
    public String name() {
        return "生活日程记忆 Agent";
    }

    /**
     * 判断是否支持指定能力。
     *
     * @param capability 能力标识
     * @return 支持 memory、preference、history、task-record 时返回 true
     */
    @Override
    public boolean supports(String capability) {
        return "memory".equalsIgnoreCase(capability)
                || "preference".equalsIgnoreCase(capability)
                || "history".equalsIgnoreCase(capability)
                || "task-record".equalsIgnoreCase(capability);
    }

    /**
     * 处理委托调度命令，通过 LLM 解析指令并调用记忆相关工具。
     *
     * <p>记忆 Agent 的 handle 由主 Agent 的委托工具触发，接收结构化指令作为
     * "message"，LLM 根据系统提示词解析指令并调用记忆读写工具。</p>
     *
     * @param command 调度命令（message 为委托方传入的指令文本）
     * @return 记忆操作结果
     */
    @Override
    public AgentDispatchResponse handle(AgentDispatchCommand command) {
        command.setAgentCode(code());
        return chatRuntimeService.process(command);
    }
}
