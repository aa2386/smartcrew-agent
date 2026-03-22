package com.smartcrew.agent.core.config;

import com.smartcrew.agent.common.config.SmartCrewProperties;
import com.smartcrew.agent.common.util.StringUtils;
import com.smartcrew.agent.core.llm.DashScopeLlmClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * LLM 配置类，负责初始化大模型客户端。
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "smartcrew.llm", name = "enabled", havingValue = "true")
public class LlmConfig {

    private final SmartCrewProperties properties;
    private final DashScopeLlmClient dashScopeLlmClient;

    @PostConstruct
    public void initializeLlmClients() {
        SmartCrewProperties.Llm llmConfig = properties.getLlm();
        String provider = llmConfig.getProvider();

        log.info("[LLM] 开始初始化大模型客户端");
        log.info("[LLM] 供应商: {}", provider);
        log.info("[LLM] 基础 URL: {}", StringUtils.isBlank(llmConfig.getBaseUrl()) ? "未配置" : llmConfig.getBaseUrl());

        if ("dashscope".equalsIgnoreCase(provider)) {
            dashScopeLlmClient.initializeModel();
            log.info("[LLM] 千问（DashScope）客户端初始化成功");
        } else {
            log.warn("[LLM] 不支持的大模型供应商: {}", provider);
        }
    }
}
