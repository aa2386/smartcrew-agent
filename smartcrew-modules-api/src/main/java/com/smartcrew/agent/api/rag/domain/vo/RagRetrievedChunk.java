package com.smartcrew.agent.api.rag.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.util.Collections;
import java.util.Map;

/**
 * RAG 运行时命中的切片视图对象。
 */
@Data
@Builder
public class RagRetrievedChunk {

    /**
     * 相关度分数。
     */
    private Double score;
    /**
     * 向量 ID。
     */
    private String vectorId;
    /**
     * 知识库编码。
     */
    private String knowledgeBaseCode;
    /**
     * 知识库名称。
     */
    private String knowledgeBaseName;
    /**
     * 文档编码。
     */
    private String documentCode;
    /**
     * 文档名称。
     */
    private String documentName;
    /**
     * 切片序号。
     */
    private Integer chunkIndex;
    /**
     * 切片内容。
     */
    private String content;
    /**
     * 切片元数据。
     */
    @Builder.Default
    private Map<String, Object> metadata = Collections.emptyMap();
}
