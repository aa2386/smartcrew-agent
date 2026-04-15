package com.smartcrew.agent;

import com.smartcrew.agent.api.rag.service.DocumentLoaderService;
import com.smartcrew.agent.api.rag.service.DocumentSplitterService;
import com.smartcrew.agent.api.rag.service.EmbeddingService;
import com.smartcrew.agent.api.rag.service.RagAugmentationService;
import com.smartcrew.agent.api.rag.service.VectorStoreService;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.segment.TextSegment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RAG 基础组件上下文与基本能力测试。
 */
@ActiveProfiles("test")
@SpringBootTest(properties = {
        "smartcrew.rag.enabled=true",
        "smartcrew.rag.embedding.api-key=test-key",
        "smartcrew.rag.vector-store.type=chroma",
        "smartcrew.rag.vector-store.chroma.base-url=http://localhost:8000"
})
class RagInfrastructureContextTests {

    @Autowired
    private DocumentLoaderService documentLoaderService;

    @Autowired
    private DocumentSplitterService documentSplitterService;

    @Autowired
    private EmbeddingService embeddingService;

    @Autowired
    private VectorStoreService vectorStoreService;

    @Autowired
    private RagAugmentationService ragAugmentationService;

    @TempDir
    Path tempDir;

    @Test
    void shouldRegisterRagBeansWhenRagEnabled() {
        assertThat(documentLoaderService).isNotNull();
        assertThat(documentSplitterService).isNotNull();
        assertThat(embeddingService).isNotNull();
        assertThat(vectorStoreService).isNotNull();
        assertThat(ragAugmentationService).isNotNull();
        assertThat(embeddingService.modelName()).isEqualTo("text-embedding-v3");
    }

    @Test
    void shouldLoadMarkdownDocument() throws Exception {
        Path markdownFile = tempDir.resolve("rag-intro.md");
        Files.writeString(markdownFile, "# 标题\n\n这是一个 RAG 文档。", StandardCharsets.UTF_8);

        Document document = documentLoaderService.loadDocument(markdownFile);

        assertThat(document.text()).contains("RAG");
        assertThat(document.metadata().getString(Document.FILE_NAME)).contains("rag-intro.md");
    }

    @Test
    void shouldSplitDocumentIntoSegments() {
        Document document = Document.from("第一段介绍 RAG。\n\n第二段介绍向量检索。");

        List<TextSegment> segments = documentSplitterService.split(document, 20, 0);

        assertThat(segments).isNotEmpty();
        assertThat(segments.get(0).text()).contains("RAG");
    }
}
