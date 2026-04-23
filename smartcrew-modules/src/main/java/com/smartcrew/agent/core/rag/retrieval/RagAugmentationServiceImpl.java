package com.smartcrew.agent.core.rag.retrieval;

import com.smartcrew.agent.api.rag.domain.entity.KnowledgeBase;
import com.smartcrew.agent.api.rag.domain.vo.RagAugmentationResult;
import com.smartcrew.agent.api.rag.domain.vo.RagRetrievedChunk;
import com.smartcrew.agent.api.rag.service.EmbeddingService;
import com.smartcrew.agent.api.rag.service.KnowledgeBaseService;
import com.smartcrew.agent.api.rag.service.RagAugmentationService;
import com.smartcrew.agent.api.rag.service.VectorStoreService;
import com.smartcrew.agent.common.config.SmartCrewProperties;
import com.smartcrew.agent.common.util.StringUtils;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Agent 运行时检索增强服务实现。
 */
@Service
@ConditionalOnProperty(prefix = "smartcrew.rag", name = "enabled", havingValue = "true")
public class RagAugmentationServiceImpl implements RagAugmentationService {

    private static final Logger log = LoggerFactory.getLogger(RagAugmentationServiceImpl.class);

    private final KnowledgeBaseService knowledgeBaseService;
    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;
    private final SmartCrewProperties properties;

    public RagAugmentationServiceImpl(KnowledgeBaseService knowledgeBaseService,
                                      EmbeddingService embeddingService,
                                      VectorStoreService vectorStoreService,
                                      SmartCrewProperties properties) {
        this.knowledgeBaseService = knowledgeBaseService;
        this.embeddingService = embeddingService;
        this.vectorStoreService = vectorStoreService;
        this.properties = properties;
    }

    @Override
    public RagAugmentationResult augment(String agentCode, String query, String traceId) {
        SmartCrewProperties.Retrieval retrieval = properties.getRag().getRetrieval();
        if (retrieval == null || !retrieval.isEnabled()) {
            return emptyResult(retrieval, List.of(), false);
        }
        if (StringUtils.isBlank(agentCode) || StringUtils.isBlank(query)) {
            return emptyResult(retrieval, List.of(), false);
        }

        List<KnowledgeBase> knowledgeBases = knowledgeBaseService.findByAgentCode(agentCode).stream()
                .filter(this::isRetrievableKnowledgeBase)
                .toList();
        List<String> knowledgeBaseCodes = knowledgeBases.stream()
                .map(KnowledgeBase::getBaseCode)
                .filter(StringUtils::isNotBlank)
                .toList();
        if (knowledgeBases.isEmpty()) {
            log.debug("RAG 检索跳过，未找到可用知识库，traceId: {}, agentCode: {}", traceId, agentCode);
            return emptyResult(retrieval, knowledgeBaseCodes, false);
        }

        Embedding queryEmbedding;
        try {
            queryEmbedding = embeddingService.embed(query);
        } catch (Exception exception) {
            log.warn("RAG 查询向量化失败，已降级为普通回答，traceId: {}, agentCode: {}", traceId, agentCode, exception);
            return emptyResult(retrieval, knowledgeBaseCodes, true);
        }

        boolean degraded = false;
        List<RagRetrievedChunk> retrievedChunks = new ArrayList<>();
        for (KnowledgeBase knowledgeBase : knowledgeBases) {
            try {
                List<EmbeddingMatch<TextSegment>> matches = vectorStoreService.search(
                        knowledgeBase.getCollectionName(),
                        queryEmbedding,
                        retrieval.getPerKnowledgeBaseTopK(),
                        retrieval.getMinScore()
                );
                retrievedChunks.addAll(convertMatches(knowledgeBase, matches));
            } catch (Exception exception) {
                degraded = true;
                log.warn("RAG 检索单库失败，已跳过该知识库，traceId: {}, agentCode: {}, baseCode: {}",
                        traceId,
                        agentCode,
                        knowledgeBase.getBaseCode(),
                        exception);
            }
        }

        // 此处将retrievedChunks去重、排序、截断（已根据相关度分数进行倒序排序，从分数低的开始截出）
        List<RagRetrievedChunk> finalChunks = truncateChunks(
                deduplicateAndSort(retrievedChunks),
                retrieval
        );
        // 构建RAG提示词
        String promptBlock = buildPromptBlock(finalChunks, retrieval.getMaxContextChars());
        Double topScore = finalChunks.isEmpty() ? null : finalChunks.get(0).getScore();// 获取最高相关度分数

        log.info("RAG 检索完成，traceId: {}, agentCode: {}, knowledgeBases: {}, hitCount: {}, topScore: {}, degraded: {}",
                traceId,
                agentCode,
                knowledgeBaseCodes,
                finalChunks.size(),
                topScore,
                degraded);

        return RagAugmentationResult.builder()
                .enabled(true)
                .degraded(degraded)
                .knowledgeBaseCodes(new ArrayList<>(knowledgeBaseCodes))
                .chunks(new ArrayList<>(finalChunks))
                .promptBlock(promptBlock)
                .hitCount(finalChunks.size())
                .topScore(topScore)
                .build();
    }

    /* 构建空的检索增强结果。 */
    private RagAugmentationResult emptyResult(SmartCrewProperties.Retrieval retrieval,
                                              List<String> knowledgeBaseCodes,
                                              boolean degraded) {
        boolean enabled = retrieval != null && retrieval.isEnabled();
        return RagAugmentationResult.builder()
                .enabled(enabled)
                .degraded(degraded)
                .knowledgeBaseCodes(new ArrayList<>(knowledgeBaseCodes))
                .chunks(new ArrayList<>())
                .promptBlock("")
                .hitCount(0)
                .topScore(null)
                .build();
    }

    /* 判断知识库是否可参与运行时检索。 */
    private boolean isRetrievableKnowledgeBase(KnowledgeBase knowledgeBase) {
        if (knowledgeBase == null) {
            return false;
        }
        return !Boolean.FALSE.equals(knowledgeBase.getEnabled())
                && StringUtils.isNotBlank(knowledgeBase.getCollectionName());
    }

    /* 将向量库命中结果转换为统一切片对象。 */
    private List<RagRetrievedChunk> convertMatches(KnowledgeBase knowledgeBase,
                                                   List<EmbeddingMatch<TextSegment>> matches) {
        if (matches == null || matches.isEmpty()) {
            return List.of();
        }
        List<RagRetrievedChunk> chunks = new ArrayList<>(matches.size());
        for (EmbeddingMatch<TextSegment> match : matches) {
            TextSegment segment = match.embedded();
            Map<String, Object> metadata = segment == null || segment.metadata() == null
                    ? Map.of()
                    : new LinkedHashMap<>(segment.metadata().toMap());
            chunks.add(RagRetrievedChunk.builder()
                    .score(match.score())
                    .vectorId(match.embeddingId())
                    .knowledgeBaseCode(resolveString(metadata, "base_code", knowledgeBase.getBaseCode()))
                    .knowledgeBaseName(knowledgeBase.getBaseName())
                    .documentCode(resolveString(metadata, "document_code", null))
                    .documentName(resolveString(metadata, "document_name", null))
                    .chunkIndex(resolveInteger(metadata, "chunk_index"))
                    .content(segment == null ? "" : segment.text())
                    .metadata(metadata)
                    .build());
        }
        return chunks;
    }

    /* 按向量 ID 或文档切片键去重，并按分数倒序排序。 */
    private List<RagRetrievedChunk> deduplicateAndSort(List<RagRetrievedChunk> retrievedChunks) {
        if (retrievedChunks == null || retrievedChunks.isEmpty()) {
            return List.of();
        }
        Map<String, RagRetrievedChunk> deduplicated = new LinkedHashMap<>();
        for (RagRetrievedChunk chunk : retrievedChunks) {
            String uniqueKey = buildUniqueKey(chunk);
            RagRetrievedChunk current = deduplicated.get(uniqueKey);
            if (current == null || safeScore(chunk) > safeScore(current)) {
                deduplicated.put(uniqueKey, chunk);
            }
        }
        return deduplicated.values().stream()
                .sorted(Comparator.comparingDouble(this::safeScore).reversed())
                .toList();
    }

    /* 根据阈值和上下文预算裁剪切片。 */
    private List<RagRetrievedChunk> truncateChunks(List<RagRetrievedChunk> sortedChunks,
                                                   SmartCrewProperties.Retrieval retrieval) {
        if (sortedChunks == null || sortedChunks.isEmpty()) {
            return List.of();
        }
        // 根据最大切片数量、最大上下文字符数裁剪切片。
        int maxCount = Math.min(retrieval.getTopK(), retrieval.getMaxContextChunks());
        int remainingChars = Math.max(retrieval.getMaxContextChars(), 0);
        List<RagRetrievedChunk> selected = new ArrayList<>();
        for (RagRetrievedChunk chunk : sortedChunks) {
            if (selected.size() >= maxCount || remainingChars <= 0) {
                break;
            }
            String content = defaultString(chunk.getContent());
            if (content.isBlank()) {
                continue;
            }
            String truncatedContent = content.length() <= remainingChars
                    ? content
                    : content.substring(0, remainingChars);
            selected.add(copyChunkWithContent(chunk, truncatedContent));
            remainingChars -= truncatedContent.length();
        }
        return selected;
    }

    /* 组装最终的 RAG 提示词片段。 */
    private String buildPromptBlock(List<RagRetrievedChunk> chunks, int maxContextChars) {
        if (chunks == null || chunks.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        builder.append("## 知识库回答规则").append(System.lineSeparator())
                .append("1. 优先依据下方知识库资料回答问题。").append(System.lineSeparator())
                .append("2. 如果资料不足，可谨慎补充通用知识，但不要虚构资料中不存在的确定性事实。").append(System.lineSeparator())
                .append("3. 如果资料与常识冲突，优先说明资料内容并保持谨慎。").append(System.lineSeparator())
                .append(System.lineSeparator())
                .append("## 参考资料").append(System.lineSeparator());
        for (int index = 0; index < chunks.size(); index++) {
            RagRetrievedChunk chunk = chunks.get(index);
            builder.append(index + 1)
                    .append(". [知识库:")
                    .append(defaultString(chunk.getKnowledgeBaseCode()))
                    .append("] [文档:")
                    .append(defaultString(chunk.getDocumentName()))
                    .append("] [切片:")
                    .append(chunk.getChunkIndex() == null ? "-" : chunk.getChunkIndex())
                    .append("]").append(System.lineSeparator())
                    .append(defaultString(chunk.getContent()))
                    .append(System.lineSeparator()).append(System.lineSeparator());
        }
        String promptBlock = builder.toString().trim();
        if (maxContextChars > 0 && promptBlock.length() > maxContextChars) {
            return promptBlock.substring(0, maxContextChars);
        }
        return promptBlock;
    }

    /* 复制切片并替换内容。 */
    private RagRetrievedChunk copyChunkWithContent(RagRetrievedChunk chunk, String content) {
        return RagRetrievedChunk.builder()
                .score(chunk.getScore())
                .vectorId(chunk.getVectorId())
                .knowledgeBaseCode(chunk.getKnowledgeBaseCode())
                .knowledgeBaseName(chunk.getKnowledgeBaseName())
                .documentCode(chunk.getDocumentCode())
                .documentName(chunk.getDocumentName())
                .chunkIndex(chunk.getChunkIndex())
                .content(content)
                .metadata(chunk.getMetadata())
                .build();
    }

    /* 生成切片去重键。 */
    private String buildUniqueKey(RagRetrievedChunk chunk) {
        if (StringUtils.isNotBlank(chunk.getVectorId())) {
            return "vector:" + chunk.getVectorId();
        }
        if (StringUtils.isNotBlank(chunk.getDocumentCode()) && chunk.getChunkIndex() != null) {
            return "document:" + chunk.getDocumentCode() + ":" + chunk.getChunkIndex();
        }
        return "content:" + Integer.toHexString(defaultString(chunk.getContent()).hashCode());
    }

    /* 读取字符串型元数据。 */
    private String resolveString(Map<String, Object> metadata, String key, String defaultValue) {
        Object value = metadata.get(key);
        if (value == null) {
            return defaultValue;
        }
        String resolved = String.valueOf(value).trim();
        return resolved.isEmpty() ? defaultValue : resolved;
    }

    /* 读取整数型元数据。 */
    private Integer resolveInteger(Map<String, Object> metadata, String key) {
        Object value = metadata.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && StringUtils.isNotBlank(text)) {
            try {
                return Integer.parseInt(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    /* 返回切片分数，空值按 0 处理。 */
    private double safeScore(RagRetrievedChunk chunk) {
        return chunk.getScore() == null ? 0D : chunk.getScore();
    }

    /* 返回非空字符串。 */
    private String defaultString(String value) {
        return value == null ? "" : value;
    }
}
