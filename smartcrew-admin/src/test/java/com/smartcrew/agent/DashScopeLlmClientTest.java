package com.smartcrew.agent;

import com.smartcrew.agent.api.llm.domain.entity.LlmConversationMessage;
import com.smartcrew.agent.api.llm.domain.request.LlmChatRequest;
import com.smartcrew.agent.api.llm.domain.vo.LlmChatResponse;
import com.smartcrew.agent.api.llm.mapper.LlmConversationMessageMapper;
import com.smartcrew.agent.api.llm.service.LlmStreamingCallback;
import com.smartcrew.agent.common.config.SmartCrewProperties;
import com.smartcrew.agent.common.util.StringUtils;
import com.smartcrew.agent.core.llm.client.DashScopeLlmClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

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
     * 验证同步 chat 方法在 DashScope 配置可用时能够正常返回内容。
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
     * 验证同一个 userId 和 sessionId 下，多轮对话会自动带上已保存的历史消息。
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
     * 验证流式 chat 方法会持续回调 onNext，并在结束时通过 onComplete 返回最终响应。
     */
    @Test
    void shouldStreamChatSuccessfully() throws InterruptedException {
        assumeDashScopeConfigured();

        AtomicReference<StringBuilder> streamedContentRef = new AtomicReference<>(new StringBuilder());
        AtomicReference<LlmChatResponse> responseRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        dashScopeLlmClient.chat(LlmChatRequest.builder()
                .userId(2101L)
                .sessionId("stream-chat-success-session")
                .userMessage("现在回答我，我是谁？")
                .traceId("test-trace-002-stream")
                .build(), new LlmStreamingCallback() {
            @Override
            public void onNext(String content) {
                streamedContentRef.get().append(content);
                System.out.println("onNext: " + content);
            }

            @Override
            public void onComplete(LlmChatResponse response) {
                responseRef.set(response);
                System.out.println("onComplete: " + response);
                latch.countDown();
            }
        });

        assertThat(latch.await(60, TimeUnit.SECONDS)).isTrue();
        assertThat(responseRef.get()).isNotNull();
        assertThat(responseRef.get().getSuccess()).isTrue();
        assertThat(responseRef.get().getContent()).isNotBlank();
        assertThat(streamedContentRef.get().toString()).isNotBlank();
    }

    /**
     * 验证同一用户的不同 sessionId 之间彼此隔离，不会串用历史上下文。
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
     * 验证同步 chat 方法在缺少必要参数时会直接返回失败结果，而不是继续调用模型。
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
     * 验证流式 chat 方法在缺少必要参数时也会走失败回调，并返回失败响应。
     */
    @Test
    void shouldRejectInvalidStreamingRequest() throws InterruptedException {
        AtomicReference<LlmChatResponse> responseRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        dashScopeLlmClient.chat(LlmChatRequest.builder()
                .sessionId("invalid-stream-session")
                .userMessage("这条流式请求缺少用户 ID")
                .traceId("test-trace-004-stream")
                .build(), new LlmStreamingCallback() {
            @Override
            public void onNext(String content) {
            }

            @Override
            public void onComplete(LlmChatResponse response) {
                responseRef.set(response);
                latch.countDown();
            }
        });

        assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue();
        assertThat(responseRef.get()).isNotNull();
        assertThat(responseRef.get().getSuccess()).isFalse();
        assertThat(responseRef.get().getErrorMessage()).contains("用户 ID");
    }

    /**
     * 验证同步模型不可用时会把当前用户消息标记为 FAILED，便于排查和审计。
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
     * 验证流式模型不可用时同样会记录失败消息，并把消息状态更新为 FAILED。
     */
    @Test
    void shouldRecordFailedStreamingMessageWhenModelUnavailable() throws InterruptedException {
        assumeDashScopeConfigured();

        ReflectionTestUtils.setField(dashScopeLlmClient, "streamingChatModel", null);
        try {
            Long userId = 5001L;
            String sessionId = "stream-failure-record-session";
            AtomicReference<LlmChatResponse> responseRef = new AtomicReference<>();
            CountDownLatch latch = new CountDownLatch(1);

            dashScopeLlmClient.chat(LlmChatRequest.builder()
                    .userId(userId)
                    .sessionId(sessionId)
                    .userMessage("这是一条流式故障回放测试消息")
                    .traceId("test-trace-006")
                    .build(), new LlmStreamingCallback() {
                @Override
                public void onNext(String content) {
                }

                @Override
                public void onComplete(LlmChatResponse response) {
                    responseRef.set(response);
                    latch.countDown();
                }
            });

            assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue();
            assertThat(responseRef.get()).isNotNull();
            assertThat(responseRef.get().getSuccess()).isFalse();

            LlmConversationMessage latestMessage = conversationMessageMapper.selectLatestMessage(userId, sessionId);
            assertThat(latestMessage).isNotNull();
            assertThat(latestMessage.getStatus()).isEqualTo("FAILED");
            assertThat(latestMessage.getContent()).isEqualTo("这是一条流式故障回放测试消息");
        } finally {
            dashScopeLlmClient.initializeModel();
        }
    }

    private void assumeDashScopeConfigured() {
        assumeTrue(StringUtils.isNotBlank(properties.getLlm().getApiKey()), "未配置 DashScope API Key，跳过集成测试");
    }
}
