package com.smartcrew.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcrew.agent.api.agent.domain.request.AgentRegisterRequest;
import com.smartcrew.agent.api.agent.service.AgentDefinitionService;
import com.smartcrew.agent.api.rag.mapper.DocumentChunkMapper;
import com.smartcrew.agent.api.rag.mapper.KnowledgeDocumentMapper;
import com.smartcrew.agent.api.rag.service.EmbeddingService;
import com.smartcrew.agent.api.rag.service.VectorStoreService;
import com.smartcrew.agent.common.exception.ServiceException;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 知识库后台管理集成测试。
 */
@ActiveProfiles("test")
@SpringBootTest(properties = {
        "smartcrew.rag.enabled=true",
        "smartcrew.rag.embedding.api-key=test-key",
        "smartcrew.rag.vector-store.type=chroma",
        "smartcrew.rag.document.upload-path=target/test-rag-admin-uploads"
})
@AutoConfigureMockMvc
class AdminKnowledgeBaseControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AgentDefinitionService agentDefinitionService;

    @Autowired
    private KnowledgeDocumentMapper knowledgeDocumentMapper;

    @Autowired
    private DocumentChunkMapper documentChunkMapper;

    @MockBean
    private EmbeddingService embeddingService;

    @MockBean
    private VectorStoreService vectorStoreService;

    @MockBean(name = "ragDocumentTaskExecutor")
    private TaskExecutor ragDocumentTaskExecutor;

    @BeforeEach
    void setUp() {
        Mockito.reset(embeddingService, vectorStoreService, ragDocumentTaskExecutor);
        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(ragDocumentTaskExecutor).execute(any(Runnable.class));
    }

    @Test
    void shouldManageKnowledgeBaseUploadChunksAndBindings() throws Exception {
        when(embeddingService.embedAll(anyList())).thenAnswer(invocation -> {
            List<TextSegment> segments = invocation.getArgument(0);
            return segments.stream()
                    .map(item -> Embedding.from(new float[]{1.0F, 2.0F, 3.0F}))
                    .toList();
        });
        when(vectorStoreService.addAll(anyString(), anyList(), anyList())).thenAnswer(invocation -> {
            List<TextSegment> segments = invocation.getArgument(2);
            return IntStream.range(0, segments.size())
                    .mapToObj(index -> "vec-" + index)
                    .toList();
        });

        String baseCode = "kb-" + UUID.randomUUID();
        registerAgent("agent-" + UUID.randomUUID());
        registerAgent("agent-" + UUID.randomUUID());

        MvcResult createResult = mockMvc.perform(post("/api/admin/knowledge-bases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "baseCode", baseCode,
                                "baseName", "知识库管理测试",
                                "description", "用于验证知识库后台管理",
                                "enabled", true
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.baseCode").value(baseCode))
                .andReturn();
        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString()).path("data");
        assertThat(created.path("collectionName").asText()).startsWith("kb_");

        List<String> agentCodes = agentDefinitionService.listAll().stream()
                .map(item -> item.getAgentCode())
                .filter(code -> code.startsWith("agent-"))
                .sorted()
                .toList();
        mockMvc.perform(put("/api/admin/knowledge-bases/" + baseCode + "/agent-bindings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("agentCodes", agentCodes))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.boundAgents.length()").value(agentCodes.size()));

        MockMultipartFile file = new MockMultipartFile(
                "files",
                "rag-admin-guide.md",
                MediaType.TEXT_MARKDOWN_VALUE,
                "第一段介绍知识库。\n\n第二段介绍文档切片。".getBytes(StandardCharsets.UTF_8)
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/api/admin/knowledge-bases/" + baseCode + "/documents")
                        .file(file)
                        .with(request -> {
                            request.setMethod("POST");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("completed"))
                .andReturn();
        JsonNode uploadedDocument = objectMapper.readTree(uploadResult.getResponse().getContentAsString()).path("data").get(0);
        String documentCode = uploadedDocument.path("documentCode").asText();

        mockMvc.perform(get("/api/admin/knowledge-bases/" + baseCode + "/documents")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.rows[0].documentCode").value(documentCode))
                .andExpect(jsonPath("$.rows[0].chunkCount").isNumber());

        mockMvc.perform(get("/api/admin/knowledge-bases/" + baseCode + "/documents/" + documentCode + "/chunks")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(documentChunkMapper.selectByDocumentId(
                        knowledgeDocumentMapper.selectByStatus("completed").stream()
                                .filter(item -> documentCode.equals(item.getDocumentCode()))
                                .findFirst()
                                .orElseThrow()
                                .getId()
                ).size()))
                .andExpect(jsonPath("$.rows[0].vectorId").isNotEmpty())
                .andExpect(jsonPath("$.rows[0].contentPreview").isNotEmpty());

        mockMvc.perform(delete("/api/admin/knowledge-bases/" + baseCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("知识库下仍存在文档，无法删除"));
    }

    @Test
    void shouldMarkDocumentFailedWhenReprocessFails() throws Exception {
        when(embeddingService.embedAll(anyList()))
                .thenAnswer(invocation -> {
                    List<TextSegment> segments = invocation.getArgument(0);
                    return segments.stream()
                            .map(item -> Embedding.from(new float[]{1.0F, 2.0F, 3.0F}))
                            .toList();
                })
                .thenThrow(new ServiceException("mock reprocess error"));
        when(vectorStoreService.addAll(anyString(), anyList(), anyList())).thenAnswer(invocation -> {
            List<TextSegment> segments = invocation.getArgument(2);
            return IntStream.range(0, segments.size())
                    .mapToObj(index -> "vec-" + index)
                    .toList();
        });

        String baseCode = "kb-" + UUID.randomUUID();
        mockMvc.perform(post("/api/admin/knowledge-bases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "baseCode", baseCode,
                                "baseName", "重处理测试知识库",
                                "description", "用于验证失败状态回写",
                                "enabled", true
                        ))))
                .andExpect(status().isOk());

        MockMultipartFile file = new MockMultipartFile(
                "files",
                "failure-case.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "用于重处理失败场景".getBytes(StandardCharsets.UTF_8)
        );
        MvcResult uploadResult = mockMvc.perform(multipart("/api/admin/knowledge-bases/" + baseCode + "/documents")
                        .file(file)
                        .with(request -> {
                            request.setMethod("POST");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andReturn();
        String documentCode = objectMapper.readTree(uploadResult.getResponse().getContentAsString())
                .path("data").get(0).path("documentCode").asText();

        mockMvc.perform(post("/api/admin/knowledge-bases/" + baseCode + "/documents/" + documentCode + "/reprocess"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("failed"))
                .andExpect(jsonPath("$.data.errorMessage").value(org.hamcrest.Matchers.containsString("mock reprocess error")));

        mockMvc.perform(get("/api/admin/knowledge-bases/" + baseCode + "/documents")
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .param("status", "failed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.rows[0].status").value("failed"));
    }

    private void registerAgent(String agentCode) {
        AgentRegisterRequest request = new AgentRegisterRequest();
        request.setAgentCode(agentCode);
        request.setAgentName(agentCode);
        request.setAgentType("STUB");
        request.setDescription("知识库绑定测试 Agent");
        request.setStrategyType("REACT");
        request.setSystemPrompt("test");
        request.setEnabled(true);
        request.setConfigJson("{}");
        agentDefinitionService.register(request);
    }
}
