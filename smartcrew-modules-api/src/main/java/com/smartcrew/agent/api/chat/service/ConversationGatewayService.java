package com.smartcrew.agent.api.chat.service;

import com.smartcrew.agent.api.agent.domain.vo.AgentDispatchResponse;

/**
 * 对话网关服务，统一接入 Web 与第三方平台消息。
 */
public interface ConversationGatewayService {

    /**
     * Web 端发送消息。
     */
    AgentDispatchResponse chatFromWeb(Long userId, String rootSessionId, String message);

    /**
     * 第三方平台发送消息。
     */
    AgentDispatchResponse chatFromPlatform(String provider,
                                           String platformUserId,
                                           String tenantKey,
                                           String chatId,
                                           String threadId,
                                           String message,
                                           String profileSnapshotJson);
}
