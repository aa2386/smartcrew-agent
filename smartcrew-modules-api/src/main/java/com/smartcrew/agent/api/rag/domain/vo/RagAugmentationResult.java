package com.smartcrew.agent.api.rag.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * RAG 运行时增强结果。
 */
@Data
@Builder
public class RagAugmentationResult {

    /**
     * 当前请求是否启用了检索增强。
     */
    private boolean enabled;
    /**
     * 是否发生过降级。
     */
    private boolean degraded;
    /**
     * 命中的知识库编码列表。
     */
    @Builder.Default
    private List<String> knowledgeBaseCodes = new ArrayList<>();
    /**
     * 命中的切片列表。
     */
    @Builder.Default
    private List<RagRetrievedChunk> chunks = new ArrayList<>();
    /**
     * 拼装后的提示词增强片段。
     */
    private String promptBlock;
    /**
     * 命中的切片数量。
     */
    private int hitCount;
    /**
     * 最高相关度分数。
     */
    private Double topScore;
}
