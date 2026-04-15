package com.smartcrew.agent.controller.admin;

import com.smartcrew.agent.api.chat.domain.vo.ChatMessageVo;
import com.smartcrew.agent.api.chat.domain.vo.ChatSessionVo;
import com.smartcrew.agent.api.chat.service.ConversationQueryService;
import com.smartcrew.agent.common.domain.R;
import com.smartcrew.agent.core.page.PageQuery;
import com.smartcrew.agent.core.page.TableDataInfo;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台消息审计控制器。
 */
@RestController
@RequestMapping("/api/admin/conversations")
@ConditionalOnProperty(prefix = "smartcrew.api.admin", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AdminConversationController {

    /**
     * 会话查询服务。
     */
    private final ConversationQueryService conversationQueryService;

    public AdminConversationController(ConversationQueryService conversationQueryService) {
        this.conversationQueryService = conversationQueryService;
    }

    /**
     * 查询会话分页列表。
     */
    @GetMapping("/sessions")
    public TableDataInfo<ChatSessionVo> listSessions(PageQuery pageQuery,
                                                     @RequestParam(value = "userId", required = false) Long userId,
                                                     @RequestParam(value = "provider", required = false) String provider,
                                                     @RequestParam(value = "keyword", required = false) String keyword) {
        if (pageQuery.hasPaging()) {
            return TableDataInfo.build(conversationQueryService.listSessionsPage(pageQuery, userId, provider, keyword));
        }
        return TableDataInfo.build(conversationQueryService.listAllSessions(userId, provider, keyword));
    }

    /**
     * 查询消息列表。
     */
    @GetMapping("/messages")
    public R<java.util.List<ChatMessageVo>> listMessages(@RequestParam(value = "userId", required = false) Long userId,
                                                         @RequestParam(value = "sessionId", required = false) String sessionId) {
        return R.ok(conversationQueryService.listMessages(userId, sessionId));
    }
}
