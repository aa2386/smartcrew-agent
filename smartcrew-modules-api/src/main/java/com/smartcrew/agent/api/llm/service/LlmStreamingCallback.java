package com.smartcrew.agent.api.llm.service;

import com.smartcrew.agent.api.llm.domain.vo.LlmChatResponse;

/**
 * 大模型流式对话回调接口。
 */
public interface LlmStreamingCallback {

    /**
     * 接收模型本次流式输出的片段内容。
     *
     * @param content 流式片段
     */
    void onNext(String content);

    /**
     * 流式对话结束后返回最终结果。
     *
     * @param response 对话响应结果
     */
    void onComplete(LlmChatResponse response);
}
