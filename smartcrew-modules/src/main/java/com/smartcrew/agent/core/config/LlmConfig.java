package com.smartcrew.agent.core.config;

import com.smartcrew.agent.common.config.SmartCrewProperties;
import com.smartcrew.agent.common.util.LogUtils;
import com.smartcrew.agent.common.util.StringUtils;
import com.smartcrew.agent.core.llm.client.DashScopeLlmClient;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * 大模型配置类，负责在应用启动后初始化对应的客户端。
 */
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "smartcrew.llm", name = "enabled", havingValue = "true")
public class LlmConfig {

    private static final Logger log = LoggerFactory.getLogger(LlmConfig.class);

    private final SmartCrewProperties properties;
    private final DashScopeLlmClient dashScopeLlmClient;

    /**
     * 根据当前配置初始化大模型客户端。
     */
    @PostConstruct
    public void initializeLlmClients() {
        SmartCrewProperties.Llm llmConfig = properties.getLlm();
        String provider = llmConfig.getProvider();

        LogUtils.info(log, "开始初始化大模型客户端，provider: {}", provider);
        LogUtils.info(log, "当前配置的基础地址: {}", StringUtils.isBlank(llmConfig.getBaseUrl()) ? "未配置" : llmConfig.getBaseUrl());

        if ("dashscope".equalsIgnoreCase(provider)) {
            dashScopeLlmClient.initializeModel();
            LogUtils.info(log, "DashScope 客户端初始化成功");
            return;
        }

        LogUtils.warn(log, "暂不支持当前大模型提供商，provider: {}", provider);
    }
}
