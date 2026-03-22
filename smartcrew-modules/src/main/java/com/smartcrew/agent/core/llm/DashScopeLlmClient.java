package com.smartcrew.agent.core.llm;

import com.smartcrew.agent.api.llm.domain.request.LlmChatRequest;
import com.smartcrew.agent.api.llm.domain.vo.LlmChatResponse;
import com.smartcrew.agent.api.llm.service.LlmClient;
import com.smartcrew.agent.common.config.SmartCrewProperties;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 千问（DashScope）LLM 客户端实现，基于 LangChain4j 框架。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashScopeLlmClient implements LlmClient {
    private final SmartCrewProperties properties;
    private ChatLanguageModel chatModel;

    @Override
    public LlmChatResponse chat(LlmChatRequest request) {
        long startTime = System.currentTimeMillis();
        String traceId = request.getTraceId() != null ? request.getTraceId() : "unknown";

        try {
            log.info("[LLM] Starting chat request, traceId: {}, message: {}", traceId, request.getUserMessage());

            String response = chatModel.chat("你好");

            long duration = System.currentTimeMillis() - startTime;
            log.info("[LLM] Chat completed, traceId: {}, duration: {}ms", traceId, duration);

            return LlmChatResponse.builder()
                    .content(response)
                    .model(properties.getLlm().getModel())
                    .success(true)
                    .durationMs(duration)
                    .build();

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[LLM] Chat failed, traceId: {}, duration: {}ms, error: {}",
                    traceId, duration, e.getMessage(), e);

            return LlmChatResponse.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .durationMs(duration)
                    .build();
        }
    }

    @Override
    public String getClientId() {
        return "dashscope-client";
    }

    /**
     * 初始化 ChatLanguageModel 实例。
     * 此方法应在配置加载后调用。
     */
    public void initializeModel() {
        SmartCrewProperties.Llm llmConfig = properties.getLlm();

        this.chatModel = QwenChatModel.builder()
                .apiKey(llmConfig.getApiKey())
                .modelName(llmConfig.getModel())
                .temperature(0.7F)
                .build();

        log.info("[LLM] DashScope client initialized with model: {}", llmConfig.getModel());
    }
}
