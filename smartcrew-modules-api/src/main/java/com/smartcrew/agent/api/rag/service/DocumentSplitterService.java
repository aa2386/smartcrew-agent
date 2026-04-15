package com.smartcrew.agent.api.rag.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.segment.TextSegment;

import java.util.List;

/**
 * 文档分割服务接口。
 */
public interface DocumentSplitterService {

    /**
     * 使用默认配置分割文档。
     *
     * @param document 文档对象。
     * @return 切片列表。
     */
    List<TextSegment> split(Document document);

    /**
     * 指定参数分割文档。
     *
     * @param document 文档对象。
     * @param maxChunkSize 单个切片最大大小。
     * @param overlapSize 切片重叠大小。
     * @return 切片列表。
     */
    List<TextSegment> split(Document document, int maxChunkSize, int overlapSize);
}
