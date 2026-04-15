package com.smartcrew.agent;

import com.smartcrew.agent.api.rag.domain.entity.DocumentChunk;
import com.smartcrew.agent.api.rag.domain.entity.KnowledgeBase;
import com.smartcrew.agent.api.rag.domain.entity.KnowledgeDocument;
import com.smartcrew.agent.api.rag.mapper.DocumentChunkMapper;
import com.smartcrew.agent.api.rag.mapper.KnowledgeDocumentMapper;
import com.smartcrew.agent.api.rag.service.EmbeddingService;
import com.smartcrew.agent.api.rag.service.KnowledgeBaseService;
import com.smartcrew.agent.api.rag.service.KnowledgeDocumentService;
import com.smartcrew.agent.api.rag.service.VectorStoreService;
import com.smartcrew.agent.common.exception.ServiceException;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * 知识文档服务编排测试。
 */
@ActiveProfiles("test")
@SpringBootTest(properties = {
        "smartcrew.rag.enabled=true",
        "smartcrew.rag.embedding.api-key=test-key",
        "smartcrew.rag.vector-store.type=chroma",
        "smartcrew.rag.document.upload-path=target/test-rag-uploads"
})
class KnowledgeDocumentServiceTests {

    private static final Path UPLOAD_DIRECTORY = Path.of("target/test-rag-uploads");

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @Autowired
    private KnowledgeDocumentService knowledgeDocumentService;

    @Autowired
    private KnowledgeDocumentMapper knowledgeDocumentMapper;

    @Autowired
    private DocumentChunkMapper documentChunkMapper;

    @MockBean
    private EmbeddingService embeddingService;

    @MockBean
    private VectorStoreService vectorStoreService;

    @BeforeEach
    void setUp() {
        Mockito.reset(embeddingService, vectorStoreService);
    }

    @AfterEach
    void cleanUp() throws Exception {
        if (Files.notExists(UPLOAD_DIRECTORY)) {
            return;
        }
        try (var paths = Files.walk(UPLOAD_DIRECTORY)) {
            paths.sorted((left, right) -> right.compareTo(left))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (Exception ignored) {
                            // 测试清理失败不影响下次运行，留给后续断言处理。
                        }
                    });
        }
    }

    @Test
    void shouldProcessDocumentAndPersistChunks() {
        when(embeddingService.embedAll(anyList())).thenAnswer(invocation -> {
            List<TextSegment> segments = invocation.getArgument(0);
            return segments.stream()
                    .map(segment -> Embedding.from(new float[]{1.0F, 2.0F, 3.0F}))
                    .toList();
        });
        when(vectorStoreService.addAll(anyString(), anyList(), anyList())).thenAnswer(invocation -> {
            List<TextSegment> segments = invocation.getArgument(2);
            return java.util.stream.IntStream.range(0, segments.size())
                    .mapToObj(index -> "vec-" + index)
                    .toList();
        });

        KnowledgeBase knowledgeBase = createKnowledgeBase();
        byte[] content = "第一段介绍 RAG。\n\n第二段介绍向量数据库。".getBytes(StandardCharsets.UTF_8);
        KnowledgeDocument uploaded = knowledgeDocumentService.upload(
                knowledgeBase.getId(),
                "rag-guide.md",
                new ByteArrayInputStream(content),
                content.length
        );

        KnowledgeDocument processed = knowledgeDocumentService.processDocument(uploaded.getId());
        List<DocumentChunk> chunks = documentChunkMapper.selectByDocumentId(uploaded.getId());

        assertThat(processed.getStatus()).isEqualTo("completed");
        assertThat(processed.getChunkCount()).isEqualTo(chunks.size());
        assertThat(chunks).isNotEmpty();
        assertThat(chunks.get(0).getVectorId()).isNotBlank();
        assertThat(chunks.get(0).getMetadata()).contains("document_code");
        assertThat(Files.exists(Path.of(uploaded.getFilePath()))).isTrue();
    }

    @Test
    void shouldMarkDocumentFailedWhenEmbeddingFails() {
        when(embeddingService.embedAll(anyList())).thenThrow(new ServiceException("mock embedding error"));

        KnowledgeBase knowledgeBase = createKnowledgeBase();
        byte[] content = "用于失败场景的文档内容。".getBytes(StandardCharsets.UTF_8);
        KnowledgeDocument uploaded = knowledgeDocumentService.upload(
                knowledgeBase.getId(),
                "failure-case.txt",
                new ByteArrayInputStream(content),
                content.length
        );

        assertThatThrownBy(() -> knowledgeDocumentService.processDocument(uploaded.getId()))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("mock embedding error");

        KnowledgeDocument failedDocument = knowledgeDocumentMapper.selectById(uploaded.getId());
        List<DocumentChunk> chunks = documentChunkMapper.selectByDocumentId(uploaded.getId());

        assertThat(failedDocument.getStatus()).isEqualTo("failed");
        assertThat(failedDocument.getErrorMessage()).contains("mock embedding error");
        assertThat(chunks).isEmpty();
    }

    private KnowledgeBase createKnowledgeBase() {
        KnowledgeBase knowledgeBase = new KnowledgeBase();
        knowledgeBase.setBaseCode("kb-" + UUID.randomUUID());
        knowledgeBase.setBaseName("测试知识库");
        knowledgeBase.setDescription("用于验证 RAG 基础链路");
        knowledgeBase.setEmbeddingModel("text-embedding-v3");
        knowledgeBase.setCollectionName("collection_" + UUID.randomUUID().toString().replace("-", ""));
        knowledgeBase.setEnabled(Boolean.TRUE);
        return knowledgeBaseService.create(knowledgeBase);
    }
}
