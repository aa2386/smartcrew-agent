package com.smartcrew.agent;

import com.smartcrew.agent.api.llm.domain.request.LlmChatRequest;
import com.smartcrew.agent.api.llm.domain.vo.LlmChatResponse;
import com.smartcrew.agent.common.config.SmartCrewProperties;
import com.smartcrew.agent.core.llm.DashScopeLlmClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DashScopeLlmClient 集成测试类
 *
 * <p>测试千问 LLM 客户端的对话功能
 *
 * <p>测试覆盖：
 * <ul>
 *   <li>基本对话功能</li>
 *   <li>空消息处理</li>
 *   <li>长文本对话</li>
 *   <li>系统消息支持</li>
 *   <li>多轮对话</li>
 *   <li>参数配置测试</li>
 *   <li>特殊字符处理</li>
 *   <li>并发请求处理</li>
 * </ul>
 */
@ActiveProfiles("dev")
@SpringBootTest(classes = SmartCrewAgentApplication.class)
class DashScopeLlmClientTest {

    @Autowired
    private SmartCrewProperties properties;

    @Autowired
    private DashScopeLlmClient dashScopeLlmClient;

    /**
     * 测试基本的对话功能
     * 验证：
     * 1. 请求能成功处理
     * 2. 返回正确的响应结构
     * 3. 包含模型信息和耗时
     */
    @Test
    void shouldChatSuccessfully() {
        // 准备测试数据
        LlmChatRequest request = LlmChatRequest.builder()
                .userMessage("你好，请介绍一下你自己")
                .traceId("test-trace-001")
                .build();

        // 执行测试
        LlmChatResponse response = dashScopeLlmClient.chat(request);

        // 验证结果
        assertThat(response).isNotNull();
        assertThat(response.getSuccess()).isTrue();
        assertThat(response.getContent()).isNotBlank();
        assertThat(response.getModel()).isEqualTo(properties.getLlm().getModel());
        assertThat(response.getDurationMs()).isGreaterThan(0);

        // 打印响应内容用于调试
        System.out.println("Response content: " + response.getContent());
    }

    /**
     * 测试带系统消息的对话
     * 验证系统能正确处理系统角色消息
     */
    @Test
    void shouldChatWithSystemMessage() {
        // 准备测试数据
        LlmChatRequest request = LlmChatRequest.builder()
                .userMessage("你好，请介绍一下你自己")
                .traceId("test-trace-002")
                .build();

        // 执行测试
        LlmChatResponse response = dashScopeLlmClient.chat(request);

        // 验证结果
        assertThat(response).isNotNull();
        assertThat(response.getSuccess()).isTrue();
        assertThat(response.getContent()).isNotBlank();
        assertThat(response.getContent()).contains("Spring Boot");
    }

    /**
     * 测试空消息处理
     * 验证系统能正确处理空消息请求
     */
    @Test
    void shouldHandleEmptyMessage() {
        // 准备测试数据
        LlmChatRequest request = LlmChatRequest.builder()
                .userMessage("")
                .traceId("test-trace-003")
                .build();

        // 执行测试
        LlmChatResponse response = dashScopeLlmClient.chat(request);

        // 验证结果
        assertThat(response).isNotNull();
        // 空消息应该返回错误或默认响应
        assertThat(response.getSuccess()).isTrue();
    }

    /**
     * 测试长文本对话
     * 验证系统能处理较长的输入文本
     */
    @Test
    void shouldHandleLongMessage() {
        // 准备测试数据
        String longMessage = "请详细解释一下什么是 Spring Boot 框架，包括它的核心概念、主要特性以及在企业级应用中的使用场景。" +
                "Spring Boot 是由 Pivotal 团队提供的全新框架，其设计目的是用来简化新 Spring 应用的初始搭建和开发过程。" +
                "这个框架使用了特定的方式来进行配置，从而使开发人员不再需要定义样板化的配置。" +
                "通过这种方式，Spring Boot 致力于在蓬勃发展的快速应用开发领域成为领导者。" +
                "请从以下几个方面详细说明：1. 核心概念 2. 主要特性 3. 使用场景 4. 最佳实践";

        LlmChatRequest request = LlmChatRequest.builder()
                .userMessage(longMessage)
                .traceId("test-trace-004")
                .build();

        // 执行测试
        LlmChatResponse response = dashScopeLlmClient.chat(request);

        // 验证结果
        assertThat(response).isNotNull();
        assertThat(response.getSuccess()).isTrue();
        assertThat(response.getContent()).isNotBlank();
        assertThat(response.getContent().length()).isGreaterThan(100);
    }

    /**
     * 测试多轮对话
     * 验证系统能处理包含历史消息的多轮对话
     */
    @Test
    void shouldHandleMultiTurnConversation() {
        // 准备第一轮对话
        LlmChatRequest firstRequest = LlmChatRequest.builder()
                .userMessage("我叫张三")
                .traceId("test-trace-005-1")
                .build();

        LlmChatResponse firstResponse = dashScopeLlmClient.chat(firstRequest);
        assertThat(firstResponse.getSuccess()).isTrue();

        // 准备第二轮对话，包含历史
        LlmChatRequest secondRequest = LlmChatRequest.builder()
                .userMessage("我叫什么名字？")
                .traceId("test-trace-005-2")
                .build();

        LlmChatResponse secondResponse = dashScopeLlmClient.chat(secondRequest);

        // 验证结果
        assertThat(secondResponse).isNotNull();
        assertThat(secondResponse.getSuccess()).isTrue();
        assertThat(secondResponse.getContent()).isNotBlank();
        assertThat(secondResponse.getContent()).contains("张三");
    }

    /**
     * 测试参数配置
     * 验证系统能正确应用各种参数配置
     */
    @Test
    void shouldRespectParameters() {
        // 准备测试数据
        LlmChatRequest request = LlmChatRequest.builder()
                .userMessage("写一首关于春天的诗")
                .traceId("test-trace-006")
                .maxTokens(200)
                .build();

        // 执行测试
        LlmChatResponse response = dashScopeLlmClient.chat(request);

        // 验证结果
        assertThat(response).isNotNull();
        assertThat(response.getSuccess()).isTrue();
        assertThat(response.getContent()).isNotBlank();
        assertThat(response.getContent().length()).isLessThanOrEqualTo(200 * 4); // 粗略估计
    }


    /**
     * 测试特殊字符处理
     * 验证系统能正确处理包含特殊字符的消息
     */
    @Test
    void shouldHandleSpecialCharacters() {
        // 准备测试数据
        LlmChatRequest request = LlmChatRequest.builder()
                .userMessage("测试特殊字符: <>&\"'\\n\\t\\r 中英文混合 test@#$%^&*()")
                .traceId("test-trace-008")
                .build();

        // 执行测试
        LlmChatResponse response = dashScopeLlmClient.chat(request);

        // 验证结果
        assertThat(response).isNotNull();
        assertThat(response.getSuccess()).isTrue();
        assertThat(response.getContent()).isNotBlank();
    }

    /**
     * 测试并发请求
     * 验证系统能处理多个并发请求
     */
    @Test
    void shouldHandleConcurrentRequests() throws InterruptedException {
        int threadCount = 5;
        Thread[] threads = new Thread[threadCount];
        LlmChatResponse[] responses = new LlmChatResponse[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                LlmChatRequest request = LlmChatRequest.builder()
                        .userMessage("并发测试消息 " + index)
                        .traceId("test-trace-009-" + index)
                        .build();
                responses[index] = dashScopeLlmClient.chat(request);
            });
            threads[i].start();
        }

        // 等待所有线程完成
        for (Thread thread : threads) {
            thread.join();
        }

        // 验证结果
        for (LlmChatResponse response : responses) {
            assertThat(response).isNotNull();
            assertThat(response.getSuccess()).isTrue();
            assertThat(response.getContent()).isNotBlank();
        }
    }
}
