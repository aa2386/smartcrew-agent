package com.smartcrew.agent.api.rag.service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;

import java.util.List;

/**
 * 嵌入服务接口。
 */
public interface EmbeddingService {

    /**
     * 对单条文本进行向量化。
     *
     * @param text 文本内容。
     * @return 向量结果。
     */
    Embedding embed(String text);

    /**
     * 对批量切片进行向量化。
     *
     * @param segments 切片列表。
     * @return 向量列表。
     */
    List<Embedding> embedAll(List<TextSegment> segments);

    /**
     * 返回当前使用的嵌入模型名称。
     *
     * @return 模型名称。
     */
    String modelName();
}
