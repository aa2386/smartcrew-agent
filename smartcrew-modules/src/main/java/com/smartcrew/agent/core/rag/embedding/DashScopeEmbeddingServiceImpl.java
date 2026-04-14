package com.smartcrew.agent.core.rag.embedding;

import com.smartcrew.agent.api.rag.service.EmbeddingService;
import com.smartcrew.agent.common.config.SmartCrewProperties;
import com.smartcrew.agent.common.exception.ServiceException;
import com.smartcrew.agent.common.util.StringUtils;
import dev.langchain4j.community.model.dashscope.QwenEmbeddingModel;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * DashScope 嵌入服务实现。
 */
@Service
@ConditionalOnProperty(prefix = "smartcrew.rag", name = "enabled", havingValue = "true")
public class DashScopeEmbeddingServiceImpl implements EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(DashScopeEmbeddingServiceImpl.class);

    private final SmartCrewProperties properties;
    private final EmbeddingModel embeddingModel;

    public DashScopeEmbeddingServiceImpl(SmartCrewProperties properties) {
        this.properties = properties;
        SmartCrewProperties.Embedding embedding = properties.getRag().getEmbedding();
        if (StringUtils.isNotBlank(embedding.getProvider())
                && !"dashscope".equalsIgnoreCase(embedding.getProvider())) {
            throw new IllegalArgumentException("当前仅支持 dashscope 嵌入提供商");
        }

        String apiKey = resolveApiKey(embedding);
        String modelName = resolveModelName(embedding);
        String baseUrl = resolveBaseUrl(embedding);
        if (StringUtils.isBlank(apiKey)) {
            throw new IllegalArgumentException("RAG 嵌入模型 API Key 未配置，无法初始化");
        }

        QwenEmbeddingModel.QwenEmbeddingModelBuilder builder = QwenEmbeddingModel.builder()
                .apiKey(apiKey)
                .modelName(modelName);
        if (StringUtils.isNotBlank(baseUrl)) {
            builder.baseUrl(baseUrl);
        }
        this.embeddingModel = builder.build();
        log.info("RAG 嵌入服务初始化完成，provider: dashscope, model: {}", modelName);
    }

    @Override
    public Embedding embed(String text) {
        if (StringUtils.isBlank(text)) {
            throw new ServiceException("嵌入文本不能为空");
        }
        return embeddingModel.embed(text).content();
    }

    @Override
    public List<Embedding> embedAll(List<TextSegment> segments) {
        if (segments == null || segments.isEmpty()) {
            throw new ServiceException("嵌入切片列表不能为空");
        }
        return embeddingModel.embedAll(segments).content();
    }

    @Override
    public String modelName() {
        return resolveModelName(properties.getRag().getEmbedding());
    }

    private String resolveApiKey(SmartCrewProperties.Embedding embedding) {
        if (StringUtils.isNotBlank(embedding.getApiKey())) {
            return embedding.getApiKey();
        }
        return properties.getLlm().getApiKey();
    }

    private String resolveModelName(SmartCrewProperties.Embedding embedding) {
        if (StringUtils.isNotBlank(embedding.getModel())) {
            return embedding.getModel();
        }
        return "text-embedding-v3";
    }

    private String resolveBaseUrl(SmartCrewProperties.Embedding embedding) {
        if (StringUtils.isNotBlank(embedding.getBaseUrl())) {
            return embedding.getBaseUrl();
        }
        return properties.getLlm().getBaseUrl();
    }
}
