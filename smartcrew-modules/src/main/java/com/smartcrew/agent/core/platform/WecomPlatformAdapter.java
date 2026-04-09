package com.smartcrew.agent.core.platform;

import com.smartcrew.agent.api.chat.service.ConversationGatewayService;
import com.smartcrew.agent.api.platform.domain.request.PlatformEventRequest;
import com.smartcrew.agent.api.platform.domain.vo.PlatformDispatchResponse;
import com.smartcrew.agent.api.platform.service.PlatformAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 企业微信平台适配器。
 */
@Component
@RequiredArgsConstructor
public class WecomPlatformAdapter implements PlatformAdapter {

    /**
     * 对话网关服务。
     */
    private final ConversationGatewayService conversationGatewayService;

    @Override
    public String platformCode() {
        return "wecom";
    }

    @Override
    public boolean supports(String platform) {
        return platformCode().equalsIgnoreCase(platform);
    }

    @Override
    public PlatformDispatchResponse handleEvent(PlatformEventRequest request) {
        String tenantKey = String.valueOf(request.getMetadata().getOrDefault("tenant", ""));
        String chatId = String.valueOf(request.getMetadata().getOrDefault("chatId", ""));
        String threadId = String.valueOf(request.getMetadata().getOrDefault("threadId", ""));
        String profileSnapshotJson = String.valueOf(request.getMetadata().getOrDefault("profileSnapshotJson", ""));
        var response = conversationGatewayService.chatFromPlatform(
                platformCode(),
                request.getPlatformUserId(),
                tenantKey,
                chatId,
                threadId,
                request.getContent(),
                profileSnapshotJson
        );
        return PlatformDispatchResponse.builder()
                .platform(platformCode())
                .handled(true)
                .message(response.getMessage())
                .build();
    }
}
