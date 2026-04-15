package com.smartcrew.agent.api.rag.service;

import com.smartcrew.agent.api.rag.domain.entity.KnowledgeDocument;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;

/**
 * 知识文档服务接口。
 */
public interface KnowledgeDocumentService {

    /**
     * 上传知识文档。
     *
     * @param baseId 知识库 ID。
     * @param originalFilename 原始文件名。
     * @param inputStream 文件输入流。
     * @param fileSize 文件大小。
     * @return 文档记录。
     */
    KnowledgeDocument upload(Long baseId, String originalFilename, InputStream inputStream, long fileSize);

    /**
     * 按文档 ID 执行文档处理。
     *
     * @param documentId 文档 ID。
     * @return 处理后的文档记录。
     */
    KnowledgeDocument processDocument(Long documentId);

    /**
     * 删除文档和关联切片。
     *
     * @param documentId 文档 ID。
     */
    void deleteDocument(Long documentId);

    /**
     * 按 ID 查询文档。
     *
     * @param documentId 文档 ID。
     * @return 匹配结果。
     */
    Optional<KnowledgeDocument> findById(Long documentId);

    /**
     * 按知识库 ID 查询文档列表。
     *
     * @param baseId 知识库 ID。
     * @return 文档列表。
     */
    List<KnowledgeDocument> findByBaseId(Long baseId);

    /**
     * 查询待处理文档列表。
     *
     * @return 文档列表。
     */
    List<KnowledgeDocument> findPendingDocuments();
}
