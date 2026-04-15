package com.smartcrew.agent.core.rag.store;

import com.smartcrew.agent.api.rag.service.VectorStoreService;
import com.smartcrew.agent.common.config.SmartCrewProperties;
import com.smartcrew.agent.common.exception.ServiceException;
import com.smartcrew.agent.common.util.StringUtils;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 远程 Chroma 向量存储实现。
 */
@Service
@ConditionalOnProperty(prefix = "smartcrew.rag", name = "enabled", havingValue = "true")
public class ChromaVectorStoreServiceImpl implements VectorStoreService {

    private static final Logger log = LoggerFactory.getLogger(ChromaVectorStoreServiceImpl.class);

    private final SmartCrewProperties properties;
    private final Map<String, ChromaEmbeddingStore> storeCache = new ConcurrentHashMap<>();

    public ChromaVectorStoreServiceImpl(SmartCrewProperties properties) {
        this.properties = properties;
        String vectorStoreType = properties.getRag().getVectorStore().getType();
        if (StringUtils.isNotBlank(vectorStoreType) && !"chroma".equalsIgnoreCase(vectorStoreType)) {
            throw new IllegalArgumentException("当前仅支持 chroma 向量存储实现");
        }
    }

    @Override
    public String add(String namespace, Embedding embedding, TextSegment segment) {
        return getStore(namespace).add(embedding, segment);
    }

    @Override
    public List<String> addAll(String namespace, List<Embedding> embeddings, List<TextSegment> segments) {
        if (embeddings == null || embeddings.isEmpty()) {
            return List.of();
        }
        return getStore(namespace).addAll(embeddings, segments);
    }

    @Override
    public void remove(String namespace, String id) {
        if (StringUtils.isBlank(id)) {
            return;
        }
        getStore(namespace).remove(id);
    }

    @Override
    public void removeAll(String namespace, List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        getStore(namespace).removeAll(ids);
    }

    @Override
    public List<EmbeddingMatch<TextSegment>> search(String namespace, Embedding queryEmbedding, int maxResults) {
        return getStore(namespace).findRelevant(queryEmbedding, maxResults);
    }

    @Override
    public List<EmbeddingMatch<TextSegment>> search(String namespace, Embedding queryEmbedding, int maxResults, double minScore) {
        return getStore(namespace).findRelevant(queryEmbedding, maxResults, minScore);
    }

    /* 获取指定命名空间对应的向量存储实例。 */
    private ChromaEmbeddingStore getStore(String namespace) {
        String normalizedNamespace = normalizeNamespace(namespace);
        return storeCache.computeIfAbsent(normalizedNamespace, this::createStore);
    }

    /* 创建指定命名空间的 Chroma 存储实例。 */
    private ChromaEmbeddingStore createStore(String namespace) {
        SmartCrewProperties.Chroma chroma = properties.getRag().getVectorStore().getChroma();
        log.info("初始化 Chroma 向量命名空间: {}", namespace);
        return ChromaEmbeddingStore.builder()
                .baseUrl(chroma.getBaseUrl())
                .collectionName(namespace)
                .timeout(Duration.ofSeconds(chroma.getTimeoutSeconds()))
                .build();
    }

    /* 规范化向量命名空间。 */
    private String normalizeNamespace(String namespace) {
        if (StringUtils.isBlank(namespace)) {
            throw new ServiceException("向量命名空间不能为空");
        }
        return namespace.trim();
    }
}
