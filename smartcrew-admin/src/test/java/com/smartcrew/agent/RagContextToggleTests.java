package com.smartcrew.agent;

import com.smartcrew.agent.api.rag.service.DocumentLoaderService;
import com.smartcrew.agent.api.rag.service.DocumentSplitterService;
import com.smartcrew.agent.api.rag.service.EmbeddingService;
import com.smartcrew.agent.api.rag.service.KnowledgeBaseService;
import com.smartcrew.agent.api.rag.service.KnowledgeDocumentService;
import com.smartcrew.agent.api.rag.service.RagAugmentationService;
import com.smartcrew.agent.api.rag.service.VectorStoreService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RAG 关闭时的容器行为测试。
 */
@ActiveProfiles("test")
@SpringBootTest
class RagContextToggleTests {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void shouldNotRegisterRagBeansWhenRagDisabled() {
        assertThat(applicationContext.getBeansOfType(DocumentLoaderService.class)).isEmpty();
        assertThat(applicationContext.getBeansOfType(DocumentSplitterService.class)).isEmpty();
        assertThat(applicationContext.getBeansOfType(EmbeddingService.class)).isEmpty();
        assertThat(applicationContext.getBeansOfType(VectorStoreService.class)).isEmpty();
        assertThat(applicationContext.getBeansOfType(KnowledgeBaseService.class)).isEmpty();
        assertThat(applicationContext.getBeansOfType(KnowledgeDocumentService.class)).isEmpty();
        assertThat(applicationContext.getBeansOfType(RagAugmentationService.class)).isEmpty();
    }
}
