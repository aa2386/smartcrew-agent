package com.smartcrew.agent.core.rag.service;

import cn.hutool.core.util.IdUtil;
import com.smartcrew.agent.api.rag.domain.entity.DocumentChunk;
import com.smartcrew.agent.api.rag.domain.entity.KnowledgeBase;
import com.smartcrew.agent.api.rag.domain.entity.KnowledgeDocument;
import com.smartcrew.agent.api.rag.mapper.DocumentChunkMapper;
import com.smartcrew.agent.api.rag.mapper.KnowledgeDocumentMapper;
import com.smartcrew.agent.api.rag.service.DocumentLoaderService;
import com.smartcrew.agent.api.rag.service.DocumentSplitterService;
import com.smartcrew.agent.api.rag.service.EmbeddingService;
import com.smartcrew.agent.api.rag.service.KnowledgeBaseService;
import com.smartcrew.agent.api.rag.service.KnowledgeDocumentService;
import com.smartcrew.agent.api.rag.service.VectorStoreService;
import com.smartcrew.agent.common.config.SmartCrewProperties;
import com.smartcrew.agent.common.exception.ServiceException;
import com.smartcrew.agent.common.util.JsonUtils;
import com.smartcrew.agent.common.util.StringUtils;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * 知识文档服务实现，负责文件落盘、切分、向量化与持久化编排。
 */
@Service
@ConditionalOnProperty(prefix = "smartcrew.rag", name = "enabled", havingValue = "true")
public class KnowledgeDocumentServiceImpl implements KnowledgeDocumentService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeDocumentServiceImpl.class);

    private static final String STATUS_PENDING = "pending";
    private static final String STATUS_PROCESSING = "processing";
    private static final String STATUS_COMPLETED = "completed";
    private static final String STATUS_FAILED = "failed";

    private final KnowledgeDocumentMapper knowledgeDocumentMapper;
    private final DocumentChunkMapper documentChunkMapper;
    private final KnowledgeBaseService knowledgeBaseService;
    private final DocumentLoaderService documentLoaderService;
    private final DocumentSplitterService documentSplitterService;
    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;
    private final SmartCrewProperties properties;

    public KnowledgeDocumentServiceImpl(KnowledgeDocumentMapper knowledgeDocumentMapper,
                                        DocumentChunkMapper documentChunkMapper,
                                        KnowledgeBaseService knowledgeBaseService,
                                        DocumentLoaderService documentLoaderService,
                                        DocumentSplitterService documentSplitterService,
                                        EmbeddingService embeddingService,
                                        VectorStoreService vectorStoreService,
                                        SmartCrewProperties properties) {
        this.knowledgeDocumentMapper = knowledgeDocumentMapper;
        this.documentChunkMapper = documentChunkMapper;
        this.knowledgeBaseService = knowledgeBaseService;
        this.documentLoaderService = documentLoaderService;
        this.documentSplitterService = documentSplitterService;
        this.embeddingService = embeddingService;
        this.vectorStoreService = vectorStoreService;
        this.properties = properties;
    }

    @Override
    @Transactional
    public KnowledgeDocument upload(Long baseId, String originalFilename, InputStream inputStream, long fileSize) {
        if (inputStream == null) {
            throw new ServiceException("文件内容不能为空");
        }
        if (StringUtils.isBlank(originalFilename)) {
            throw new ServiceException("文件名不能为空");
        }

        KnowledgeBase knowledgeBase = requireBase(baseId);
        String documentCode = IdUtil.fastSimpleUUID();
        String fileType = resolveFileType(originalFilename);
        Path storedPath = storeFile(documentCode, originalFilename, inputStream);

        KnowledgeDocument document = new KnowledgeDocument();
        document.setBaseId(knowledgeBase.getId());
        document.setDocumentCode(documentCode);
        document.setDocumentName(originalFilename);
        document.setFilePath(storedPath.toString());
        document.setFileType(fileType);
        document.setFileSize(fileSize);
        document.setStatus(STATUS_PENDING);
        document.setChunkCount(0);
        knowledgeDocumentMapper.insert(document);
        log.info("上传知识文档成功，documentCode: {}, baseCode: {}", documentCode, knowledgeBase.getBaseCode());
        return document;
    }

    @Override
    public KnowledgeDocument processDocument(Long documentId) {
        KnowledgeDocument document = requireDocument(documentId);
        KnowledgeBase knowledgeBase = requireBase(document.getBaseId());
        String namespace = knowledgeBase.getCollectionName();
        List<String> newVectorIds = new ArrayList<>();

        markProcessing(document);
        cleanupExistingChunks(namespace, documentId);

        try {
            Document loadedDocument = documentLoaderService.loadDocument(Path.of(document.getFilePath()), document.getFileType());
            List<TextSegment> segments = documentSplitterService.split(loadedDocument);
            if (segments.isEmpty()) {
                throw new ServiceException("文档分割结果为空");
            }

            List<TextSegment> enrichedSegments = buildSegments(document, knowledgeBase, segments);
            List<Embedding> embeddings = embeddingService.embedAll(enrichedSegments);
            newVectorIds = vectorStoreService.addAll(namespace, embeddings, enrichedSegments);
            if (newVectorIds.size() != enrichedSegments.size()) {
                throw new ServiceException("向量存储返回的 ID 数量与切片数量不一致");
            }

            persistChunks(documentId, enrichedSegments, newVectorIds);
            document.setStatus(STATUS_COMPLETED);
            document.setChunkCount(enrichedSegments.size());
            document.setErrorMessage(null);
            knowledgeDocumentMapper.updateById(document);
            log.info("文档处理完成，documentCode: {}, chunkCount: {}", document.getDocumentCode(), enrichedSegments.size());
            return document;
        } catch (Exception ex) {
            if (!newVectorIds.isEmpty()) {
                vectorStoreService.removeAll(namespace, newVectorIds);
            }
            document.setStatus(STATUS_FAILED);
            document.setChunkCount(0);
            document.setErrorMessage(truncateErrorMessage(ex.getMessage()));
            knowledgeDocumentMapper.updateById(document);
            throw ex instanceof ServiceException
                    ? (ServiceException) ex
                    : new ServiceException(500, "文档处理失败: " + truncateErrorMessage(ex.getMessage()));
        }
    }

    @Override
    @Transactional
    public void deleteDocument(Long documentId) {
        KnowledgeDocument document = requireDocument(documentId);
        KnowledgeBase knowledgeBase = requireBase(document.getBaseId());
        cleanupExistingChunks(knowledgeBase.getCollectionName(), documentId);
        knowledgeDocumentMapper.deleteById(documentId);
        deleteStoredFile(document.getFilePath());
        log.info("删除知识文档成功，documentCode: {}", document.getDocumentCode());
    }

    @Override
    public Optional<KnowledgeDocument> findById(Long documentId) {
        return Optional.ofNullable(knowledgeDocumentMapper.selectById(documentId));
    }

    @Override
    public List<KnowledgeDocument> findByBaseId(Long baseId) {
        return knowledgeDocumentMapper.selectByBaseId(baseId);
    }

    @Override
    public List<KnowledgeDocument> findPendingDocuments() {
        return knowledgeDocumentMapper.selectByStatus(STATUS_PENDING);
    }

    private void markProcessing(KnowledgeDocument document) {
        document.setStatus(STATUS_PROCESSING);
        document.setErrorMessage(null);
        knowledgeDocumentMapper.updateById(document);
    }

    private void cleanupExistingChunks(String namespace, Long documentId) {
        List<DocumentChunk> existingChunks = documentChunkMapper.selectByDocumentId(documentId);
        List<String> vectorIds = existingChunks.stream()
                .map(DocumentChunk::getVectorId)
                .filter(StringUtils::isNotBlank)
                .toList();
        if (!vectorIds.isEmpty()) {
            vectorStoreService.removeAll(namespace, vectorIds);
        }
        if (!existingChunks.isEmpty()) {
            documentChunkMapper.deleteByDocumentId(documentId);
        }
    }

    private List<TextSegment> buildSegments(KnowledgeDocument document, KnowledgeBase knowledgeBase, List<TextSegment> segments) {
        List<TextSegment> enrichedSegments = new ArrayList<>(segments.size());
        for (int i = 0; i < segments.size(); i++) {
            TextSegment segment = segments.get(i);
            Map<String, Object> metadataMap = new LinkedHashMap<>(segment.metadata().toMap());
            metadataMap.put("base_id", knowledgeBase.getId());
            metadataMap.put("base_code", knowledgeBase.getBaseCode());
            metadataMap.put("document_id", document.getId());
            metadataMap.put("document_code", document.getDocumentCode());
            metadataMap.put("document_name", document.getDocumentName());
            metadataMap.put("chunk_index", i);
            Metadata metadata = Metadata.from(metadataMap);
            enrichedSegments.add(TextSegment.from(segment.text(), metadata));
        }
        return enrichedSegments;
    }

    private void persistChunks(Long documentId, List<TextSegment> segments, List<String> vectorIds) {
        for (int i = 0; i < segments.size(); i++) {
            TextSegment segment = segments.get(i);
            DocumentChunk chunk = new DocumentChunk();
            chunk.setDocumentId(documentId);
            chunk.setChunkIndex(i);
            chunk.setContent(segment.text());
            chunk.setVectorId(vectorIds.get(i));
            chunk.setMetadata(JsonUtils.toJson(segment.metadata().toMap()));
            documentChunkMapper.insert(chunk);
        }
    }

    private Path storeFile(String documentCode, String originalFilename, InputStream inputStream) {
        Path uploadDirectory = Path.of(properties.getRag().getDocument().getUploadPath()).toAbsolutePath().normalize();
        String safeFileName = sanitizeFileName(originalFilename);
        Path targetPath = uploadDirectory.resolve(documentCode + "-" + safeFileName);
        try {
            Files.createDirectories(uploadDirectory);
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            return targetPath;
        } catch (IOException exception) {
            throw new ServiceException(500, "保存文档失败");
        }
    }

    private void deleteStoredFile(String filePath) {
        if (StringUtils.isBlank(filePath)) {
            return;
        }
        try {
            Files.deleteIfExists(Path.of(filePath));
        } catch (IOException exception) {
            log.warn("删除文档物理文件失败，filePath: {}", filePath, exception);
        }
    }

    private KnowledgeBase requireBase(Long baseId) {
        KnowledgeBase knowledgeBase = knowledgeBaseService.findById(baseId)
                .orElseThrow(() -> new ServiceException(404, "知识库不存在"));
        if (Boolean.FALSE.equals(knowledgeBase.getEnabled())) {
            throw new ServiceException(400, "知识库已停用");
        }
        return knowledgeBase;
    }

    private KnowledgeDocument requireDocument(Long documentId) {
        return findById(documentId).orElseThrow(() -> new ServiceException(404, "知识文档不存在"));
    }

    private String resolveFileType(String originalFilename) {
        int lastDot = originalFilename.lastIndexOf('.');
        if (lastDot < 0 || lastDot == originalFilename.length() - 1) {
            return "txt";
        }
        return originalFilename.substring(lastDot + 1).toLowerCase(Locale.ROOT);
    }

    private String sanitizeFileName(String originalFilename) {
        return originalFilename.replaceAll("[\\\\/:*?\"<>|\\s]+", "_");
    }

    private String truncateErrorMessage(String message) {
        String resolvedMessage = StringUtils.isBlank(message) ? "未知错误" : message;
        return resolvedMessage.length() > 512 ? resolvedMessage.substring(0, 512) : resolvedMessage;
    }
}
