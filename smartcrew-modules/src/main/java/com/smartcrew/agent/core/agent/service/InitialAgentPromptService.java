package com.smartcrew.agent.core.agent.service;

/**
 * 初始智能体提示词构建服务。
 */
public interface InitialAgentPromptService {

    /**
     * 按 Agent 维度构建最终系统提示词。
     */
    String buildSystemPrompt(String agentCode, Long userId);
}
