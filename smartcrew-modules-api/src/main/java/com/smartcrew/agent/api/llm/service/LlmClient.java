package com.smartcrew.agent.api.llm.service;

import com.smartcrew.agent.api.llm.domain.request.LlmChatRequest;
import com.smartcrew.agent.api.llm.domain.vo.LlmChatResponse;

/**
 * LLM 客户端接口，定义大模型调用的统一契约。
 */
public interface LlmClient {
    /**
     * 发送聊天请求并获取响应。
     *
     * @param request 聊天请求参数
     * @return 聊天响应结果
     */
    LlmChatResponse chat(LlmChatRequest request);

    /**
     * 获取客户端标识。
     *
     * @return 客户端唯一标识
     */
    String getClientId();
}
