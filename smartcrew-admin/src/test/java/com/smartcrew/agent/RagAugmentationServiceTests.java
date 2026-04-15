package com.smartcrew.agent;

import com.smartcrew.agent.api.agent.domain.model.AgentDispatchCommand;
import com.smartcrew.agent.api.llm.domain.request.LlmChatRequest;
import com.smartcrew.agent.api.llm.domain.vo.LlmChatResponse;
import com.smartcrew.agent.api.llm.service.LlmClient;
import com.smartcrew.agent.api.rag.domain.entity.AgentKnowledgeBinding;
import com.smartcrew.agent.api.rag.domain.entity.KnowledgeBase;
import com.smartcrew.agent.api.rag.domain.vo.RagAugmentationResult;
import com.smartcrew.agent.api.rag.mapper.AgentKnowledgeBindingMapper;
import com.smartcrew.agent.api.rag.service.EmbeddingService;
import com.smartcrew.agent.api.rag.service.KnowledgeBaseService;
import com.smartcrew.agent.api.rag.service.RagAugmentationService;
import com.smartcrew.agent.api.rag.service.VectorStoreService;
import com.smartcrew.agent.core.agent.InitialAgent;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * RAG 运行时检索增强测试。
 */
@ActiveProfiles("test")
@SpringBootTest(properties = {
        "smartcrew.rag.enabled=true",
        "smartcrew.rag.embedding.api-key=test-key",
        "smartcrew.rag.retrieval.enabled=true",
        "smartcrew.rag.retrieval.top-k=2",
        "smartcrew.rag.retrieval.per-knowledge-base-top-k=2",
        "smartcrew.rag.retrieval.max-context-chunks=2",
        "smartcrew.rag.retrieval.max-context-chars=200"
})
class RagAugmentationServiceTests {

    @Autowired
    private RagAugmentationService ragAugmentationService;

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @Autowired
    private AgentKnowledgeBindingMapper agentKnowledgeBindingMapper;

    @Autowired
    private InitialAgent initialAgent;

    @MockBean
    private EmbeddingService embeddingService;

    @MockBean
    private VectorStoreService vectorStoreService;

    @MockBean
    private LlmClient llmClient;

    @BeforeEach
    void setUp() {
        Mockito.reset(embeddingService, vectorStoreService, llmClient);
        agentKnowledgeBindingMapper.deleteByAgentCode("initial-agent");
        when(embeddingService.embed(anyString())).thenReturn(Embedding.from(new float[]{1.0F, 2.0F, 3.0F}));
    }

    @Test
    void shouldReturnEmptyResultWhenNoKnowledgeBaseBound() {
        RagAugmentationResult result = ragAugmentationService.augment("unbound-agent", "什么是 RAG", "trace-empty");

        assertThat(result.isEnabled()).isTrue();
        assertThat(result.getHitCount()).isZero();
        assertThat(result.getChunks()).isEmpty();
        assertThat(result.getPromptBlock()).isBlank();
    }

    @Test
    void shouldMergeHitsAcrossKnowledgeBasesByGlobalTopK() {
        KnowledgeBase kbOne = createKnowledgeBase("知识库一");
        KnowledgeBase kbTwo = createKnowledgeBase("知识库二");
        bindKnowledgeBase("initial-agent", kbOne);
        bindKnowledgeBase("initial-agent", kbTwo);

        when(vectorStoreService.search(anyString(), any(Embedding.class), anyInt(), anyDouble())).thenAnswer(invocation -> {
            String namespace = invocation.getArgument(0);
            if (namespace.equals(kbOne.getCollectionName())) {
                return List.of(
                        createMatch(0.81D, "vec-a", kbOne, "doc-a", "文档A", 0, "知识库一的第一段内容"),
                        createMatch(0.72D, "vec-b", kbOne, "doc-b", "文档B", 1, "知识库一的第二段内容")
                );
            }
            return List.of(
                    createMatch(0.95D, "vec-c", kbTwo, "doc-c", "文档C", 0, "知识库二的高分内容")
            );
        });

        RagAugmentationResult result = ragAugmentationService.augment("initial-agent", "请介绍 RAG", "trace-merge");

        assertThat(result.getKnowledgeBaseCodes()).contains(kbOne.getBaseCode(), kbTwo.getBaseCode());
        assertThat(result.getHitCount()).isEqualTo(2);
        assertThat(result.getChunks()).hasSize(2);
        assertThat(result.getChunks().get(0).getDocumentName()).isEqualTo("文档C");
        assertThat(result.getPromptBlock()).contains("知识库回答规则", "文档C", "知识库二的高分内容");
    }

    @Test
    void shouldDegradeWhenSingleKnowledgeBaseSearchFails() {
        KnowledgeBase kbOne = createKnowledgeBase("异常知识库");
        KnowledgeBase kbTwo = createKnowledgeBase("正常知识库");
        bindKnowledgeBase("initial-agent", kbOne);
        bindKnowledgeBase("initial-agent", kbTwo);

        when(vectorStoreService.search(anyString(), any(Embedding.class), anyInt(), anyDouble())).thenAnswer(invocation -> {
            String namespace = invocation.getArgument(0);
            if (namespace.equals(kbOne.getCollectionName())) {
                throw new IllegalStateException("mock search error");
            }
            return List.of(createMatch(0.88D, "vec-ok", kbTwo, "doc-ok", "文档OK", 0, "保留下来的切片内容"));
        });

        RagAugmentationResult result = ragAugmentationService.augment("initial-agent", "异常场景", "trace-degrade");

        assertThat(result.isDegraded()).isTrue();
        assertThat(result.getHitCount()).isEqualTo(1);
        assertThat(result.getPromptBlock()).contains("文档OK", "保留下来的切片内容");
    }

    @Test
    void shouldAppendRagPromptIntoInitialAgentSystemPrompt() {
        KnowledgeBase knowledgeBase = createKnowledgeBase("聊天知识库");
        bindKnowledgeBase("initial-agent", knowledgeBase);
        when(vectorStoreService.search(anyString(), any(Embedding.class), anyInt(), anyDouble())).thenReturn(List.of(
                createMatch(0.93D, "vec-chat", knowledgeBase, "doc-chat", "聊天文档", 0, "这是注入到提示词中的知识片段")
        ));
        when(llmClient.chat(any(LlmChatRequest.class))).thenReturn(LlmChatResponse.builder()
                .success(Boolean.TRUE)
                .content("模型回答")
                .build());

        var response = initialAgent.handle(AgentDispatchCommand.builder()
                .agentCode("initial-agent")
                .userId(1001L)
                .sessionId("session-rag")
                .message("请根据知识库回答")
                .traceId("trace-agent")
                .build());

        ArgumentCaptor<LlmChatRequest> captor = ArgumentCaptor.forClass(LlmChatRequest.class);
        verify(llmClient).chat(captor.capture());

        assertThat(response.isAccepted()).isTrue();
        assertThat(response.getMessage()).isEqualTo("模型回答");
        assertThat(captor.getValue().getSystemPrompt()).contains("知识库回答规则", "聊天文档", "这是注入到提示词中的知识片段");
    }

    /* 创建测试知识库。 */
    private KnowledgeBase createKnowledgeBase(String baseName) {
        KnowledgeBase knowledgeBase = new KnowledgeBase();
        knowledgeBase.setBaseCode("kb-" + UUID.randomUUID());
        knowledgeBase.setBaseName(baseName);
        knowledgeBase.setDescription("用于运行时检索测试");
        knowledgeBase.setEmbeddingModel("text-embedding-v3");
        knowledgeBase.setCollectionName("collection_" + UUID.randomUUID().toString().replace("-", ""));
        knowledgeBase.setEnabled(Boolean.TRUE);
        return knowledgeBaseService.create(knowledgeBase);
    }

    /* 创建 Agent 与知识库绑定。 */
    private void bindKnowledgeBase(String agentCode, KnowledgeBase knowledgeBase) {
        AgentKnowledgeBinding binding = new AgentKnowledgeBinding();
        binding.setAgentCode(agentCode);
        binding.setBaseCode(knowledgeBase.getBaseCode());
        agentKnowledgeBindingMapper.insert(binding);
    }

    /* 构造向量检索命中结果。 */
    private EmbeddingMatch<TextSegment> createMatch(double score,
                                                    String vectorId,
                                                    KnowledgeBase knowledgeBase,
                                                    String documentCode,
                                                    String documentName,
                                                    int chunkIndex,
                                                    String content) {
        Metadata metadata = Metadata.from(Map.of(
                "base_code", knowledgeBase.getBaseCode(),
                "document_code", documentCode,
                "document_name", documentName,
                "chunk_index", chunkIndex
        ));
        TextSegment segment = TextSegment.from(content, metadata);
        return new EmbeddingMatch<>(score, vectorId, Embedding.from(new float[]{1.0F, 2.0F, 3.0F}), segment);
    }
}
