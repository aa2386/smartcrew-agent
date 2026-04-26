package com.smartcrew.agent.core.config;

import com.smartcrew.agent.common.config.SmartCrewProperties;
import com.smartcrew.agent.common.util.StringUtils;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 大模型配置。
 * 当前主链路统一使用 LangChain4j AI Service，并由这里提供唯一的底层对话模型 Bean。
 */
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "smartcrew.llm", name = "enabled", havingValue = "true")
public class LlmConfig {

    private static final Logger log = LoggerFactory.getLogger(LlmConfig.class);

    private final SmartCrewProperties properties;

    /**
     * 创建 DashScope 对话模型，供所有 Agent 和决策服务复用。
     */
    @Bean
    @ConditionalOnMissingBean(ChatLanguageModel.class)
    public ChatLanguageModel chatLanguageModel() {
        SmartCrewProperties.Llm llmConfig = requireDashScope();
        QwenChatModel.QwenChatModelBuilder builder = QwenChatModel.builder()
                .apiKey(llmConfig.getApiKey())
                .modelName(llmConfig.getModel())
                .temperature(0.7F)
                .maxTokens(2048);
        if (StringUtils.isNotBlank(llmConfig.getBaseUrl())) {
            builder.baseUrl(llmConfig.getBaseUrl());
        }
        log.info("大模型对话模型已启用，provider: DashScope，model: {}", llmConfig.getModel());
        return builder.build();
    }

    /**
     * 校验 DashScope 配置，避免模型 Bean 在缺少关键参数时启动到半可用状态。
     */
    private SmartCrewProperties.Llm requireDashScope() {
        SmartCrewProperties.Llm llmConfig = properties.getLlm();
        if (!"dashscope".equalsIgnoreCase(llmConfig.getProvider())) {
            throw new IllegalStateException("当前仅支持 dashscope provider");
        }
        if (StringUtils.isBlank(llmConfig.getApiKey())) {
            throw new IllegalArgumentException("DashScope API Key 未配置");
        }
        if (StringUtils.isBlank(llmConfig.getModel())) {
            throw new IllegalArgumentException("DashScope 模型名称未配置");
        }
        return llmConfig;
    }
}
