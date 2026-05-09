package com.smartcrew.agent.core.agent;

import com.smartcrew.agent.api.agent.domain.model.AgentDispatchCommand;
import com.smartcrew.agent.api.agent.domain.vo.AgentDispatchResponse;
import com.smartcrew.agent.api.agent.service.Agent;
import com.smartcrew.agent.api.agent.service.AgentChatRuntimeService;
import org.springframework.stereotype.Component;

/**
 * 生活日程工具 Agent，负责承接外部能力/平台工具调用。
 *
 * <p>不直接面对用户，由主 Agent 通过委托工具调用。只执行工具或解释工具失败原因，
 * 不进行寒暄或开放式闲聊，不保存长期记忆。</p>
 *
 * <p>核心能力：tool（工具执行）、schedule（日程操作）。</p>
 *
 * @see Agent
 * @see AgentChatRuntimeService
 */
@Component
public class LifeToolAgent implements Agent {

    private final AgentChatRuntimeService chatRuntimeService;

    /**
     * 构造工具 Agent 实例。
     *
     * @param chatRuntimeService 通用 Agent 对话运行时服务
     */
    public LifeToolAgent(AgentChatRuntimeService chatRuntimeService) {
        this.chatRuntimeService = chatRuntimeService;
    }

    /**
     * 返回 Agent 编码。
     *
     * @return 固定值 "life-tool-agent"
     */
    @Override
    public String code() {
        return "life-tool-agent";
    }

    /**
     * 返回 Agent 名称。
     *
     * @return 固定值 "生活日程工具 Agent"
     */
    @Override
    public String name() {
        return "生活日程工具 Agent";
    }

    /**
     * 判断是否支持指定能力。
     *
     * @param capability 能力标识
     * @return 支持 tool、schedule 时返回 true
     */
    @Override
    public boolean supports(String capability) {
        return "tool".equalsIgnoreCase(capability)
                || "schedule".equalsIgnoreCase(capability);
    }

    /**
     * 处理委托调度命令，通过 LLM 解析指令并调用工具。
     *
     * <p>工具 Agent 的 handle 由主 Agent 的委托工具触发，接收结构化指令作为
     * "message"，LLM 根据系统提示词解析指令并调用绑定的工具执行。</p>
     *
     * @param command 调度命令（message 为委托方传入的指令文本）
     * @return 工具执行结果
     */
    @Override
    public AgentDispatchResponse handle(AgentDispatchCommand command) {
        command.setAgentCode(code());
        return chatRuntimeService.process(command);
    }
}
