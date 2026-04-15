package com.smartcrew.agent.core.agent.service;

/**
 * 初始智能体提示词构建服务。
 */
public interface InitialAgentPromptService {

    /**
     * 按 Agent 维度构建最终系统提示词。
     *
     * @param agentCode Agent 编码
     * @param userId 用户 ID
     * @return 系统提示词
     */
    String buildSystemPrompt(String agentCode, Long userId);

    /**
     * 在基础提示词上追加知识库增强片段。
     *
     * @param agentCode Agent 编码
     * @param userId 用户 ID
     * @param ragPromptBlock 知识库增强片段
     * @return 最终系统提示词
     */
    String buildSystemPrompt(String agentCode, Long userId, String ragPromptBlock);
}
