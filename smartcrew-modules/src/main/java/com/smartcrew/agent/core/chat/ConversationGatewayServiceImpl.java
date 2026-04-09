package com.smartcrew.agent.core.chat;

import com.smartcrew.agent.api.agent.domain.request.AgentDispatchRequest;
import com.smartcrew.agent.api.agent.domain.vo.AgentDispatchResponse;
import com.smartcrew.agent.api.agent.service.AgentCoordinator;
import com.smartcrew.agent.api.chat.service.ConversationGatewayService;
import com.smartcrew.agent.api.user.domain.entity.ScUser;
import com.smartcrew.agent.api.user.service.UserIdentityResolver;
import org.springframework.stereotype.Service;

/**
 * 对话网关服务实现。
 */
@Service
public class ConversationGatewayServiceImpl implements ConversationGatewayService {

    /**
     * 初始智能体编码。
     */
    private static final String INITIAL_AGENT_CODE = "initial-agent";

    /**
     * Agent 协调器。
     */
    private final AgentCoordinator agentCoordinator;

    /**
     * 用户身份解析服务。
     */
    private final UserIdentityResolver userIdentityResolver;

    public ConversationGatewayServiceImpl(AgentCoordinator agentCoordinator,
                                          UserIdentityResolver userIdentityResolver) {
        this.agentCoordinator = agentCoordinator;
        this.userIdentityResolver = userIdentityResolver;
    }

    @Override
    public AgentDispatchResponse chatFromWeb(Long userId, String rootSessionId, String message) {
        AgentDispatchRequest request = new AgentDispatchRequest();
        request.setUserId(userId);
        request.setSessionId(rootSessionId);
        request.setMessage(message);
        request.getContext().put("source", "WEB");
        return agentCoordinator.dispatch(INITIAL_AGENT_CODE, request);
    }

    @Override
    public AgentDispatchResponse chatFromPlatform(String provider,
                                                  String platformUserId,
                                                  String tenantKey,
                                                  String chatId,
                                                  String threadId,
                                                  String message,
                                                  String profileSnapshotJson) {
        ScUser user = userIdentityResolver.resolveOrCreatePlatformUser(
                provider,
                platformUserId,
                tenantKey,
                profileSnapshotJson
        );
        AgentDispatchRequest request = new AgentDispatchRequest();
        request.setUserId(user.getId());
        request.setSessionId(buildPlatformRootSession(provider, chatId, threadId, platformUserId));
        request.setMessage(message);
        request.getContext().put("source", "PLATFORM");
        request.getContext().put("provider", provider == null ? "" : provider.toUpperCase());
        request.getContext().put("platformUserId", platformUserId);
        request.getContext().put("tenantKey", tenantKey == null ? "" : tenantKey);
        request.getContext().put("profileSnapshotJson", profileSnapshotJson == null ? "" : profileSnapshotJson);
        return agentCoordinator.dispatch(INITIAL_AGENT_CODE, request);
    }

    /**
     * 构建平台根会话 ID。
     */
    private String buildPlatformRootSession(String provider, String chatId, String threadId, String platformUserId) {
        String normalizedProvider = provider == null ? "unknown" : provider.trim().toLowerCase();
        String anchor = firstNonBlank(chatId, threadId, platformUserId);
        String suffix = threadId != null && !threadId.isBlank() && !threadId.equals(anchor) ? "::" + threadId : "";
        return "platform::" + normalizedProvider + "::" + anchor + suffix;
    }

    /**
     * 返回第一个非空字符串。
     */
    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "unknown";
    }
}
