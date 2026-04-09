package com.smartcrew.agent.controller.web;

import com.smartcrew.agent.api.chat.domain.request.SendChatMessageRequest;
import com.smartcrew.agent.api.chat.domain.vo.ChatMessageVo;
import com.smartcrew.agent.api.chat.domain.vo.ChatSessionVo;
import com.smartcrew.agent.api.chat.service.ConversationGatewayService;
import com.smartcrew.agent.api.chat.service.ConversationQueryService;
import com.smartcrew.agent.common.auth.AuthContextHolder;
import com.smartcrew.agent.common.domain.R;
import com.smartcrew.agent.common.exception.ServiceException;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Web 端聊天控制器。
 */
@RestController
@RequestMapping("/api/web/chat")
@ConditionalOnProperty(prefix = "smartcrew.api.web", name = "enabled", havingValue = "true", matchIfMissing = true)
public class WebChatController {

    /**
     * 会话查询服务。
     */
    private final ConversationQueryService conversationQueryService;

    /**
     * 会话网关服务。
     */
    private final ConversationGatewayService conversationGatewayService;

    public WebChatController(ConversationQueryService conversationQueryService,
                             ConversationGatewayService conversationGatewayService) {
        this.conversationQueryService = conversationQueryService;
        this.conversationGatewayService = conversationGatewayService;
    }

    @PostMapping("/sessions")
    public R<ChatSessionVo> createSession() {
        return R.ok(conversationQueryService.createWebSession(currentUserId()));
    }

    @GetMapping("/sessions")
    public R<List<ChatSessionVo>> listSessions() {
        return R.ok(conversationQueryService.listWebSessions(currentUserId()));
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public R<List<ChatMessageVo>> listMessages(@PathVariable("sessionId") String sessionId) {
        return R.ok(conversationQueryService.listSessionMessages(currentUserId(), sessionId));
    }

    @PostMapping("/sessions/{sessionId}/messages")
    public R<ChatMessageVo> sendMessage(@PathVariable("sessionId") String sessionId,
                                        @Valid @RequestBody SendChatMessageRequest request) {
        var response = conversationGatewayService.chatFromWeb(currentUserId(), sessionId, request.getMessage());
        return R.ok(ChatMessageVo.builder()
                .sessionId(sessionId)
                .role("assistant")
                .content(response.getMessage())
                .traceId(response.getTraceId())
                .createTime(LocalDateTime.now())
                .build());
    }

    /**
     * 获取当前登录用户 ID。
     */
    private Long currentUserId() {
        var user = AuthContextHolder.get();
        if (user == null) {
            throw new ServiceException(401, "\u5f53\u524d\u672a\u767b\u5f55");
        }
        return user.getUserId();
    }
}
