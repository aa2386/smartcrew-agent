package com.smartcrew.agent.core.config;

import com.smartcrew.agent.common.config.SmartCrewProperties;
import com.smartcrew.agent.common.util.LogUtils;
import com.smartcrew.agent.common.util.StringUtils;
import com.smartcrew.agent.core.llm.client.DashScopeLlmClient;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.community.model.dashscope.QwenStreamingChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 大模型配置类，根据配置属性初始化 LangChain4j 对话模型客户端。
 *
 * <p>仅当 {@code smartcrew.llm.enabled=true} 时激活，当前支持 DashScope（通义千问）提供商，
 * 提供同步与流式两种对话模型 Bean，并在启动时完成客户端初始化。</p>
 *
 * @see SmartCrewProperties
 * @see DashScopeLlmClient
 */
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "smartcrew.llm", name = "enabled", havingValue = "true")
public class LlmConfig {

    private static final Logger log = LoggerFactory.getLogger(LlmConfig.class);

    private final SmartCrewProperties properties;
    private final DashScopeLlmClient dashScopeLlmClient;

    /**
     * 创建同步对话模型 Bean，当容器中不存在 {@link ChatLanguageModel} 时生效。
     *
     * @return 基于通义千问的同步对话模型实例
     * @throws IllegalStateException 当提供商非 dashscope 时抛出
     * @throws IllegalArgumentException 当 API Key 或模型名称未配置时抛出
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
        return builder.build();
    }

    /**
     * 创建流式对话模型 Bean，当容器中不存在 {@link StreamingChatLanguageModel} 时生效。
     *
     * @return 基于通义千问的流式对话模型实例
     * @throws IllegalStateException 当提供商非 dashscope 时抛出
     * @throws IllegalArgumentException 当 API Key 或模型名称未配置时抛出
     */
    @Bean
    @ConditionalOnMissingBean(StreamingChatLanguageModel.class)
    public StreamingChatLanguageModel streamingChatLanguageModel() {
        SmartCrewProperties.Llm llmConfig = requireDashScope();
        QwenStreamingChatModel.QwenStreamingChatModelBuilder builder = QwenStreamingChatModel.builder()
                .apiKey(llmConfig.getApiKey())
                .modelName(llmConfig.getModel())
                .temperature(0.7F)
                .maxTokens(2048);
        if (StringUtils.isNotBlank(llmConfig.getBaseUrl())) {
            builder.baseUrl(llmConfig.getBaseUrl());
        }
        return builder.build();
    }

    /**
     * 应用启动后初始化大模型客户端，根据配置的提供商执行对应的初始化逻辑。
     */
    @PostConstruct
    public void initializeLlmClients() {
        SmartCrewProperties.Llm llmConfig = properties.getLlm();
        String provider = llmConfig.getProvider();
        LogUtils.info(log, "开始初始化大模型客户端，provider: {}", provider);
        if ("dashscope".equalsIgnoreCase(provider)) {
            dashScopeLlmClient.initializeModel();
            LogUtils.info(log, "DashScope 客户端初始化成功");
            return;
        }
        LogUtils.warn(log, "暂不支持当前大模型提供商，provider: {}", provider);
    }

    /**
     * 校验并返回 DashScope 大模型配置，确保提供商、API Key 和模型名称均已正确配置。
     *
     * @return DashScope 大模型配置对象
     * @throws IllegalStateException 当提供商非 dashscope 时抛出
     * @throws IllegalArgumentException 当 API Key 或模型名称未配置时抛出
     */
    private SmartCrewProperties.Llm requireDashScope() {
        SmartCrewProperties.Llm llmConfig = properties.getLlm();
        if (!"dashscope".equalsIgnoreCase(llmConfig.getProvider())) {
            throw new IllegalStateException("当前仅支持 dashscope provider");
        }
        if (StringUtils.isBlank(llmConfig.getApiKey())) {
            throw new IllegalArgumentException("DashScope 的 API Key 未配置");
        }
        if (StringUtils.isBlank(llmConfig.getModel())) {
            throw new IllegalArgumentException("DashScope 的模型名称未配置");
        }
        return llmConfig;
    }
}
