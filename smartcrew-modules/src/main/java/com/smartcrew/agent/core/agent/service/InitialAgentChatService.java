package com.smartcrew.agent.core.agent.service;

import dev.langchain4j.service.Result;

/**
 * 初始智能体对话服务接口，封装 LangChain4j 的对话调用能力。
 *
 * <p>该接口由 LangChain4j AI Service 机制自动实现，
 * 负责将用户消息与系统提示词发送给大模型并返回推理结果。</p>
 */
public interface InitialAgentChatService {

    /**
     * 执行对话推理。
     *
     * @param memoryId    会话记忆标识，用于关联历史上下文
     * @param userMessage 用户输入消息
     * @param systemPrompt 系统提示词
     * @return 对话结果，包含模型回复内容
     */
    Result<String> chat(String memoryId, String userMessage, String systemPrompt);
}
