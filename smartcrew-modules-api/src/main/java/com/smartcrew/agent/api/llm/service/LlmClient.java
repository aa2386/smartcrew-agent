package com.smartcrew.agent.api.llm.service;

import com.smartcrew.agent.api.llm.domain.request.LlmChatRequest;
import com.smartcrew.agent.api.llm.domain.vo.LlmChatResponse;

/**
 * 大模型客户端接口，定义统一的对话调用契约。
 */
public interface LlmClient {

    /**
     * 发送对话请求并获取模型响应。
     *
     * @param request 对话请求参数
     * @return 对话响应结果
     */
    LlmChatResponse chat(LlmChatRequest request);

    /**
     * 获取客户端标识。
     *
     * @return 客户端唯一标识
     */
    String getClientId();
}
