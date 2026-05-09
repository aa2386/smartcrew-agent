package com.smartcrew.agent.api.agent.service;

import com.smartcrew.agent.api.agent.domain.model.AgentDispatchCommand;
import com.smartcrew.agent.api.agent.domain.vo.AgentDispatchResponse;

/**
 * Agent 通用对话运行时服务接口，为各 Agent 提供统一的 LLM 调用流程。
 *
 * <p>将 InitialAgent 中原有的 LLM 调用逻辑（RAG 增强、Prompt 组装、会话锁、
 * ToolCallContext 管理、异常降级持久化）抽取为可复用服务，所有 Agent 均可通过
 * agentCode 参数复用同一套运行时代码。</p>
 */
public interface AgentChatRuntimeService {

    /**
     * 执行 Agent 对话处理流程。
     *
     * <p>处理流程：检测大模型可用性 → RAG 增强 → 构建提示词 → 加锁调用对话 → 返回结果。
     * 异常时自动持久化降级对话记录。</p>
     *
     * @param command Agent 调度命令，必须包含有效的 agentCode、userId、sessionId
     * @return 调度响应，包含推理结果或错误信息
     */
    AgentDispatchResponse process(AgentDispatchCommand command);
}
