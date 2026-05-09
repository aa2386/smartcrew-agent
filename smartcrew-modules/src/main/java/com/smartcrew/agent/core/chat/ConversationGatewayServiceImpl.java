package com.smartcrew.agent.core.chat;

import com.smartcrew.agent.api.agent.domain.request.AgentDispatchRequest;
import com.smartcrew.agent.api.agent.domain.vo.AgentDispatchResponse;
import com.smartcrew.agent.api.agent.service.AgentCoordinator;
import com.smartcrew.agent.api.agent.service.AgentRegistry;
import com.smartcrew.agent.api.agentlog.entity.AgentBehaviorLog;
import com.smartcrew.agent.api.agentlog.service.AgentBehaviorLogService;
import com.smartcrew.agent.api.chat.service.ConversationGatewayService;
import com.smartcrew.agent.api.user.domain.entity.ScUser;
import com.smartcrew.agent.api.user.service.UserIdentityResolver;
import com.smartcrew.agent.common.config.SmartCrewProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class ConversationGatewayServiceImpl implements ConversationGatewayService {

    private static final Logger logger = LoggerFactory.getLogger(ConversationGatewayServiceImpl.class);

    private static final String FALLBACK_AGENT_CODE = "initial-agent";

    private final AgentCoordinator agentCoordinator;
    private final UserIdentityResolver userIdentityResolver;
    private final SmartCrewProperties properties;
    private final AgentRegistry agentRegistry;
    private final AgentBehaviorLogService agentBehaviorLogService;

    public ConversationGatewayServiceImpl(AgentCoordinator agentCoordinator,
                                          UserIdentityResolver userIdentityResolver,
                                          SmartCrewProperties properties,
                                          AgentRegistry agentRegistry,
                                          AgentBehaviorLogService agentBehaviorLogService) {
        this.agentCoordinator = agentCoordinator;
        this.userIdentityResolver = userIdentityResolver;
        this.properties = properties;
        this.agentRegistry = agentRegistry;
        this.agentBehaviorLogService = agentBehaviorLogService;
    }

    private String resolveDefaultAgentCode() {
        SmartCrewProperties.Agent agentConfig = properties.getAgent();
        String configured = agentConfig != null ? agentConfig.getDefaultChatAgent() : null;
        if (configured == null || configured.isBlank()) {
            return FALLBACK_AGENT_CODE;
        }
        if (!agentRegistry.contains(configured)) {
            logger.warn("Configured default chat agent '{}' is missing, fallback to '{}'",
                    configured, FALLBACK_AGENT_CODE);
            return FALLBACK_AGENT_CODE;
        }
        return configured;
    }

    @Override
    public AgentDispatchResponse chatFromWeb(Long userId, String rootSessionId, String message) {
        String agentCode = resolveDefaultAgentCode();
        String traceId = UUID.randomUUID().toString();
        AgentDispatchRequest request = new AgentDispatchRequest();
        request.setUserId(userId);
        request.setSessionId(rootSessionId);
        request.setMessage(message);
        request.getContext().put("source", "WEB");
        request.getContext().put("traceId", traceId);
        writeSessionReceivedLog(traceId, userId, rootSessionId, agentCode, Map.of("source", "WEB"));
        return agentCoordinator.dispatch(agentCode, request);
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
        String agentCode = resolveDefaultAgentCode();
        String traceId = UUID.randomUUID().toString();
        AgentDispatchRequest request = new AgentDispatchRequest();
        request.setUserId(user.getId());
        request.setSessionId(buildPlatformRootSession(provider, chatId, threadId, platformUserId));
        request.setMessage(message);
        request.getContext().put("source", "PLATFORM");
        request.getContext().put("provider", provider == null ? "" : provider.toUpperCase());
        request.getContext().put("platformUserId", platformUserId);
        request.getContext().put("tenantKey", tenantKey == null ? "" : tenantKey);
        request.getContext().put("profileSnapshotJson", profileSnapshotJson == null ? "" : profileSnapshotJson);
        request.getContext().put("traceId", traceId);

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("source", "PLATFORM");
        if (provider != null && !provider.isBlank()) {
            metadata.put("provider", provider.toUpperCase());
        }
        writeSessionReceivedLog(traceId, user.getId(), request.getSessionId(), agentCode, metadata);
        return agentCoordinator.dispatch(agentCode, request);
    }

    private void writeSessionReceivedLog(String traceId, Long userId, String sessionId,
                                         String agentCode, Map<String, Object> metadata) {
        AgentBehaviorLog logEntry = agentBehaviorLogService.buildLog(
                traceId,
                userId,
                sessionId,
                agentCode,
                "SESSION_RECEIVED",
                "SUCCESS",
                "Conversation received by main agent",
                metadata
        );
        agentBehaviorLogService.write(logEntry);
    }

    private String buildPlatformRootSession(String provider, String chatId, String threadId, String platformUserId) {
        String normalizedProvider = provider == null ? "unknown" : provider.trim().toLowerCase();
        String anchor = firstNonBlank(chatId, threadId, platformUserId);
        String suffix = threadId != null && !threadId.isBlank() && !threadId.equals(anchor) ? "::" + threadId : "";
        return "platform::" + normalizedProvider + "::" + anchor + suffix;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "unknown";
    }
}
