package com.smartcrew.agent;

import com.smartcrew.agent.api.llm.domain.entity.LlmConversationMessage;
import com.smartcrew.agent.api.llm.domain.request.LlmChatRequest;
import com.smartcrew.agent.api.llm.domain.vo.LlmChatResponse;
import com.smartcrew.agent.api.llm.mapper.LlmConversationMessageMapper;
import com.smartcrew.agent.common.config.SmartCrewProperties;
import com.smartcrew.agent.common.util.StringUtils;
import com.smartcrew.agent.core.llm.DashScopeLlmClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * DashScopeLlmClient 集成测试。
 */
@ActiveProfiles("dev")
@SpringBootTest(classes = SmartCrewAgentApplication.class)
class DashScopeLlmClientTest {

    @Autowired
    private SmartCrewProperties properties;

    @Autowired
    private DashScopeLlmClient dashScopeLlmClient;

    @Autowired
    private LlmConversationMessageMapper conversationMessageMapper;

    /**
     * 验证基本对话能够成功返回。
     */
    @Test
    void shouldChatSuccessfully() {
        assumeDashScopeConfigured();

        LlmChatRequest request = LlmChatRequest.builder()
                .userId(1001L)
                .sessionId("chat-success-session")
                .userMessage("你好，请用一句话介绍你自己")
                .traceId("test-trace-001")
                .build();

        LlmChatResponse response = dashScopeLlmClient.chat(request);

        assertThat(response).isNotNull();
        assertThat(response.getSuccess()).isTrue();
        assertThat(response.getContent()).isNotBlank();
        assertThat(response.getModel()).isEqualTo(properties.getLlm().getModel());
        assertThat(response.getDurationMs()).isGreaterThanOrEqualTo(0L);
    }

    /**
     * 验证同一会话下的多轮对话能够自动带上历史上下文。
     */
    @Test
    void shouldHandleMultiTurnConversation() {
        assumeDashScopeConfigured();

        String sessionId = "multi-turn-session";
        Long userId = 2001L;

        LlmChatResponse firstResponse = dashScopeLlmClient.chat(LlmChatRequest.builder()
                .userId(userId)
                .sessionId(sessionId)
                .userMessage("我叫张三")
                .traceId("test-trace-002-1")
                .build());
        assertThat(firstResponse.getSuccess()).isTrue();

        LlmChatResponse secondResponse = dashScopeLlmClient.chat(LlmChatRequest.builder()
                .userId(userId)
                .sessionId(sessionId)
                .userMessage("我刚才告诉你的名字是什么？")
                .traceId("test-trace-002-2")
                .build());

        assertThat(secondResponse).isNotNull();
        assertThat(secondResponse.getSuccess()).isTrue();
        assertThat(secondResponse.getContent()).contains("张三");
    }

    /**
     * 验证同一用户的不同会话之间不会串上下文。
     */
    @Test
    void shouldIsolateDifferentSessions() {
        assumeDashScopeConfigured();

        Long userId = 3001L;
        dashScopeLlmClient.chat(LlmChatRequest.builder()
                .userId(userId)
                .sessionId("isolate-session-a")
                .userMessage("请记住我的代号是红队")
                .traceId("test-trace-003-1")
                .build());

        LlmChatResponse response = dashScopeLlmClient.chat(LlmChatRequest.builder()
                .userId(userId)
                .sessionId("isolate-session-b")
                .userMessage("我刚才告诉你的代号是什么？如果你不知道，请直接说不知道")
                .traceId("test-trace-003-2")
                .build());

        assertThat(response).isNotNull();
        assertThat(response.getSuccess()).isTrue();
        assertThat(response.getContent()).doesNotContain("红队");
    }

    /**
     * 验证缺少必要字段时会直接返回失败响应。
     */
    @Test
    void shouldRejectInvalidRequest() {
        LlmChatResponse response = dashScopeLlmClient.chat(LlmChatRequest.builder()
                .sessionId("invalid-session")
                .userMessage("这条请求缺少用户 ID")
                .traceId("test-trace-004")
                .build());

        assertThat(response).isNotNull();
        assertThat(response.getSuccess()).isFalse();
        assertThat(response.getErrorMessage()).contains("用户 ID");
    }

    /**
     * 验证模型不可用时会记录失败消息，便于后续审计。
     */
    @Test
    void shouldRecordFailedMessageWhenModelUnavailable() {
        assumeDashScopeConfigured();

        ReflectionTestUtils.setField(dashScopeLlmClient, "chatModel", null);
        try {
            Long userId = 4001L;
            String sessionId = "failure-record-session";
            LlmChatResponse response = dashScopeLlmClient.chat(LlmChatRequest.builder()
                    .userId(userId)
                    .sessionId(sessionId)
                    .userMessage("这是一条故障回放测试消息")
                    .traceId("test-trace-005")
                    .build());

            assertThat(response).isNotNull();
            assertThat(response.getSuccess()).isFalse();

            LlmConversationMessage latestMessage = conversationMessageMapper.selectLatestMessage(userId, sessionId);
            assertThat(latestMessage).isNotNull();
            assertThat(latestMessage.getStatus()).isEqualTo("FAILED");
            assertThat(latestMessage.getContent()).isEqualTo("这是一条故障回放测试消息");
        } finally {
            dashScopeLlmClient.initializeModel();
        }
    }

    /**
     * 仅在配置了 DashScope API Key 时执行集成测试。
     */
    private void assumeDashScopeConfigured() {
        assumeTrue(StringUtils.isNotBlank(properties.getLlm().getApiKey()), "未配置 DashScope API Key，跳过集成测试");
    }
}
