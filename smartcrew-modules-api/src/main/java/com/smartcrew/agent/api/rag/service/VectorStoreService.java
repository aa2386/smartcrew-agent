package com.smartcrew.agent.api.rag.service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;

import java.util.List;

/**
 * 向量存储服务接口。
 */
public interface VectorStoreService {

    /**
     * 向指定命名空间添加单条向量。
     *
     * @param namespace 命名空间。
     * @param embedding 向量数据。
     * @param segment 原始切片。
     * @return 向量 ID。
     */
    String add(String namespace, Embedding embedding, TextSegment segment);

    /**
     * 向指定命名空间批量添加向量。
     *
     * @param namespace 命名空间。
     * @param embeddings 向量列表。
     * @param segments 原始切片列表。
     * @return 向量 ID 列表。
     */
    List<String> addAll(String namespace, List<Embedding> embeddings, List<TextSegment> segments);

    /**
     * 从指定命名空间移除单条向量。
     *
     * @param namespace 命名空间。
     * @param id 向量 ID。
     */
    void remove(String namespace, String id);

    /**
     * 从指定命名空间批量移除向量。
     *
     * @param namespace 命名空间。
     * @param ids 向量 ID 列表。
     */
    void removeAll(String namespace, List<String> ids);

    /**
     * 在指定命名空间中搜索相关切片。
     *
     * @param namespace 命名空间。
     * @param queryEmbedding 查询向量。
     * @param maxResults 返回数量。
     * @return 搜索结果。
     */
    List<EmbeddingMatch<TextSegment>> search(String namespace, Embedding queryEmbedding, int maxResults);

    /**
     * 在指定命名空间中按阈值搜索相关切片。
     *
     * @param namespace 命名空间。
     * @param queryEmbedding 查询向量。
     * @param maxResults 返回数量。
     * @param minScore 最低相似度。
     * @return 搜索结果。
     */
    List<EmbeddingMatch<TextSegment>> search(String namespace, Embedding queryEmbedding, int maxResults, double minScore);
}
