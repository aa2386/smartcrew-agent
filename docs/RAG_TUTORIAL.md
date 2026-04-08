# RAG（检索增强生成）教程

本教程面向 RAG 初学者，从基础概念开始，逐步讲解如何在本项目中实现完整的 RAG 能力。

---

## 目录

1. [RAG 基础概念](#1-rag-基础概念)
2. [RAG 核心组件详解](#2-rag-核心组件详解)
3. [数据库表设计](#3-数据库表设计)
4. [项目依赖配置](#4-项目依赖配置)
5. [文档处理实现](#5-文档处理实现)
6. [向量存储实现](#6-向量存储实现)
7. [检索服务实现](#7-检索服务实现)
8. [集成到 InitialAgent](#8-集成到-initialagent)
9. [完整使用示例](#9-完整使用示例)
10. [进阶优化](#10-进阶优化)

---

## 1. RAG 基础概念

### 1.1 什么是 RAG

RAG（Retrieval-Augmented Generation，检索增强生成）是一种结合了**信息检索**和**文本生成**的技术架构。

简单来说，RAG 让大模型在回答问题之前，先去"查阅资料"，然后基于查阅到的内容生成回答。

### 1.2 为什么需要 RAG

大模型（LLM）存在以下局限性：

| 问题 | 说明 |
|------|------|
| **知识截止** | 模型的知识有截止日期，无法回答最新信息 |
| **幻觉问题** | 对于不熟悉的领域，可能编造错误信息 |
| **领域知识不足** | 企业内部文档、专业知识无法直接获取 |
| **无法引用来源** | 无法告诉用户答案来自哪里 |

RAG 通过引入外部知识库，有效解决了这些问题：

```
用户问题 → 检索相关文档 → 将文档作为上下文 → LLM 基于上下文生成回答
```

### 1.3 RAG vs 微调 vs 长上下文

| 方案 | 适用场景 | 优点 | 缺点 |
|------|----------|------|------|
| **RAG** | 需要实时更新知识、需要引用来源 | 知识可更新、可追溯、成本较低 | 需要维护知识库、检索质量影响效果 |
| **微调** | 需要改变模型行为、特定任务优化 | 模型行为可定制 | 成本高、知识仍会过时、不易更新 |
| **长上下文** | 单次对话需要大量上下文 | 实现简单 | 成本高、仍有长度限制、不适合大规模知识 |

**推荐策略**：优先使用 RAG，必要时结合微调。

### 1.4 RAG 的核心流程

```
┌─────────────────────────────────────────────────────────────────┐
│                        离线索引阶段                              │
├─────────────────────────────────────────────────────────────────┤
│  文档 ──→ 文档加载 ──→ 文档分割 ──→ 向量化 ──→ 存入向量数据库     │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                        在线检索阶段                              │
├─────────────────────────────────────────────────────────────────┤
│  用户问题 ──→ 向量化 ──→ 向量检索 ──→ 获取相关文档 ──→ 构建提示词 │
│      └───────────────────────────────────────────────────────→ LLM 生成回答
└─────────────────────────────────────────────────────────────────┘
```

---

## 2. RAG 核心组件详解

### 2.1 文档加载器（Document Loader）

**作用**：将不同格式的文档加载为统一的文档对象。

常见文档类型：
- 文本文件（.txt, .md）
- PDF 文档
- Word 文档
- HTML 网页
- JSON 数据

**LangChain4j 提供的加载器**：

```java
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;

import java.nio.file.Path;

public class DocumentLoaderExample {

    public Document loadTextFile(Path filePath) {
        DocumentParser parser = new TextDocumentParser();
        return FileSystemDocumentLoader.loadDocument(filePath, parser);
    }

    public Document loadPdfFile(Path filePath) {
        DocumentParser parser = new ApachePdfBoxDocumentParser();
        return FileSystemDocumentLoader.loadDocument(filePath, parser);
    }
}
```

### 2.2 文档分割器（Document Splitter）

**作用**：将长文档切分成适当大小的片段，便于向量化存储和检索。

**为什么需要分割**：
- 向量模型有输入长度限制
- 检索时需要精确匹配，大块文档会引入噪音
- 生成时上下文窗口有限

**常见分割策略**：

| 策略 | 说明 | 适用场景 |
|------|------|----------|
| 按字符数分割 | 固定字符数切分 | 通用场景 |
| 按段落分割 | 按换行符或段落标记切分 | 结构化文档 |
| 按句子分割 | 按句号等标点切分 | 需要语义完整性 |
| 递归分割 | 多级分割，优先按段落，其次按句子 | 复杂文档 |

**LangChain4j 分割器示例**：

```java
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.document.splitter.DocumentBySentenceSplitter;
import dev.langchain4j.data.document.splitter.DocumentByRegexSplitter;
import dev.langchain4j.data.segment.TextSegment;

import java.util.List;

public class DocumentSplitterExample {

    public List<TextSegment> splitByParagraph(Document document) {
        DocumentSplitter splitter = new DocumentByParagraphSplitter(
            200,    
            50      
        );
        return splitter.split(document);
    }

    public List<TextSegment> splitBySentence(Document document) {
        DocumentSplitter splitter = new DocumentBySentenceSplitter(
            150,
            30
        );
        return splitter.split(document);
    }

    public List<TextSegment> splitByCustomRegex(Document document) {
        DocumentSplitter splitter = new DocumentByRegexSplitter(
            "\\n\\n",  
            200,
            50
        );
        return splitter.split(document);
    }
}
```

### 2.3 嵌入模型（Embedding Model）

**作用**：将文本转换为向量（一串数字），使计算机能够理解文本的语义相似性。

**向量是什么**：
```
文本: "如何使用 RAG 技术？"
向量: [0.123, -0.456, 0.789, ..., 0.234]  // 通常是 768 或 1536 维
```

**语义相似性**：
- 语义相近的文本，向量距离更近
- "如何使用 RAG？" 和 "RAG 怎么用？" 向量距离很近
- "如何使用 RAG？" 和 "今天天气不错" 向量距离很远

**常见嵌入模型**：

| 模型 | 提供商 | 维度 | 特点 |
|------|--------|------|------|
| text-embedding-v3 | 阿里云 DashScope | 1024 | 中文效果好，本项目推荐 |
| text-embedding-ada-002 | OpenAI | 1536 | 英文效果好 |
| bge-large-zh | BGE | 1024 | 开源，中文效果好 |

**LangChain4j 嵌入模型使用**：

```java
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.community.model.dashscope.QwenEmbeddingModel;

public class EmbeddingModelExample {

    private final EmbeddingModel embeddingModel;

    public EmbeddingModelExample(String apiKey) {
        this.embeddingModel = QwenEmbeddingModel.builder()
                .apiKey(apiKey)
                .modelName("text-embedding-v3")
                .build();
    }

    public Embedding embed(String text) {
        TextSegment segment = TextSegment.from(text);
        return embeddingModel.embed(segment).content();
    }

    public List<Embedding> embedAll(List<String> texts) {
        List<TextSegment> segments = texts.stream()
                .map(TextSegment::from)
                .toList();
        return embeddingModel.embedAll(segments).content();
    }
}
```

### 2.4 向量存储（Vector Store）

**作用**：存储文档片段的向量，并支持相似性检索。

**核心操作**：
1. **添加向量**：将文档片段及其向量存入数据库
2. **相似性搜索**：根据查询向量找到最相似的文档片段

**常见向量数据库**：

| 数据库 | 类型 | 特点 | 适用场景 |
|--------|------|------|----------|
| **Milvus** | 专用向量数据库 | 高性能、分布式 | 大规模生产环境 |
| **PgVector** | PostgreSQL 扩展 | 易部署、SQL 友好 | 中小规模、已有 PostgreSQL |
| **Chroma** | 轻量级向量数据库 | 简单易用 | 开发测试、小规模应用 |
| **Elasticsearch** | 搜索引擎 | 支持混合检索 | 已有 ES 基础设施 |
| **In-Memory** | 内存存储 | 最简单 | 测试、原型验证 |

**本项目推荐方案**：
- 开发阶段：使用内存向量存储或 Chroma
- 生产阶段：使用 Milvus 或 PgVector

**LangChain4j 向量存储示例**：

```java
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import java.util.List;

public class VectorStoreExample {

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;

    public VectorStoreExample(EmbeddingModel embeddingModel) {
        this.embeddingStore = new InMemoryEmbeddingStore<>();
        this.embeddingModel = embeddingModel;
    }

    public String addDocument(String content, String documentId) {
        TextSegment segment = TextSegment.from(content);
        Embedding embedding = embeddingModel.embed(segment).content();
        return embeddingStore.add(embedding, segment);
    }

    public List<EmbeddingMatch<TextSegment>> search(String query, int maxResults) {
        Embedding queryEmbedding = embeddingModel.embed(query).content();
        return embeddingStore.findRelevant(queryEmbedding, maxResults);
    }
}
```

### 2.5 内容检索器（Content Retriever）

**作用**：封装检索逻辑，将用户问题转换为检索结果。

**LangChain4j 检索器**：

```java
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.Query;

import java.util.List;

public class ContentRetrieverExample {

    private final ContentRetriever contentRetriever;

    public ContentRetrieverExample(EmbeddingStore<TextSegment> store, 
                                    EmbeddingModel model) {
        this.contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(store)
                .embeddingModel(model)
                .maxResults(3)          
                .minScore(0.7)          
                .build();
    }

    public List<Content> retrieve(String question) {
        Query query = Query.from(question);
        return contentRetriever.retrieve(query);
    }
}
```

---

## 3. 数据库表设计

### 3.1 RAG 相关表结构

在 `sql/init-smartcrew-agent.sql` 中添加以下表：

```sql
-- 知识库表
CREATE TABLE IF NOT EXISTS knowledge_base (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
    base_code VARCHAR(64) NOT NULL COMMENT '知识库编码',
    base_name VARCHAR(128) NOT NULL COMMENT '知识库名称',
    description VARCHAR(512) NULL COMMENT '描述信息',
    embedding_model VARCHAR(128) NOT NULL COMMENT '嵌入模型名称',
    vector_store_type VARCHAR(32) NOT NULL COMMENT '向量存储类型',
    enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    create_dept BIGINT NULL COMMENT '创建部门',
    create_by BIGINT NULL COMMENT '创建人',
    create_time DATETIME NULL COMMENT '创建时间',
    update_by BIGINT NULL COMMENT '更新人',
    update_time DATETIME NULL COMMENT '更新时间',
    remark VARCHAR(255) NULL COMMENT '备注信息',
    CONSTRAINT uk_base_code UNIQUE (base_code)
) COMMENT='知识库表';

-- 知识文档表
CREATE TABLE IF NOT EXISTS knowledge_document (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
    base_id BIGINT NOT NULL COMMENT '知识库 ID',
    document_code VARCHAR(64) NOT NULL COMMENT '文档编码',
    document_name VARCHAR(256) NOT NULL COMMENT '文档名称',
    file_path VARCHAR(512) NULL COMMENT '文件路径',
    file_type VARCHAR(32) NULL COMMENT '文件类型',
    file_size BIGINT NULL COMMENT '文件大小(字节)',
    status VARCHAR(32) NOT NULL DEFAULT 'pending' COMMENT '处理状态: pending/processing/completed/failed',
    chunk_count INT NOT NULL DEFAULT 0 COMMENT '切片数量',
    error_message VARCHAR(512) NULL COMMENT '错误信息',
    create_dept BIGINT NULL COMMENT '创建部门',
    create_by BIGINT NULL COMMENT '创建人',
    create_time DATETIME NULL COMMENT '创建时间',
    update_by BIGINT NULL COMMENT '更新人',
    update_time DATETIME NULL COMMENT '更新时间',
    remark VARCHAR(255) NULL COMMENT '备注信息',
    CONSTRAINT uk_document_code UNIQUE (document_code),
    INDEX idx_base_id (base_id)
) COMMENT='知识文档表';

-- 文档切片表
CREATE TABLE IF NOT EXISTS document_chunk (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
    document_id BIGINT NOT NULL COMMENT '文档 ID',
    chunk_index INT NOT NULL COMMENT '切片序号',
    content TEXT NOT NULL COMMENT '切片内容',
    vector_id VARCHAR(128) NULL COMMENT '向量存储 ID',
    token_count INT NULL COMMENT 'Token 数量',
    metadata JSON NULL COMMENT '元数据(JSON格式)',
    create_dept BIGINT NULL COMMENT '创建部门',
    create_by BIGINT NULL COMMENT '创建人',
    create_time DATETIME NULL COMMENT '创建时间',
    update_by BIGINT NULL COMMENT '更新人',
    update_time DATETIME NULL COMMENT '更新时间',
    remark VARCHAR(255) NULL COMMENT '备注信息',
    INDEX idx_document_id (document_id),
    INDEX idx_vector_id (vector_id)
) COMMENT='文档切片表';

-- Agent 知识库绑定表
CREATE TABLE IF NOT EXISTS agent_knowledge_binding (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
    agent_code VARCHAR(64) NOT NULL COMMENT 'Agent 编码',
    base_code VARCHAR(64) NOT NULL COMMENT '知识库编码',
    create_dept BIGINT NULL COMMENT '创建部门',
    create_by BIGINT NULL COMMENT '创建人',
    create_time DATETIME NULL COMMENT '创建时间',
    update_by BIGINT NULL COMMENT '更新人',
    update_time DATETIME NULL COMMENT '更新时间',
    remark VARCHAR(255) NULL COMMENT '备注信息',
    CONSTRAINT uk_agent_knowledge UNIQUE (agent_code, base_code)
) COMMENT='Agent 知识库绑定表';
```

### 3.2 表关系说明

```
┌─────────────────┐     ┌─────────────────────┐     ┌────────────────┐
│ knowledge_base  │────<│ knowledge_document  │────<│ document_chunk │
└─────────────────┘     └─────────────────────┘     └────────────────┘
        │
        │
        ▼
┌──────────────────────────┐     ┌─────────────────┐
│ agent_knowledge_binding  │────>│ agent_definition│
└──────────────────────────┘     └─────────────────┘
```

---

## 4. 项目依赖配置

### 4.1 添加 Maven 依赖

在 `smartcrew-modules/pom.xml` 中添加：

```xml
<dependencies>
    <!-- LangChain4j 核心依赖（已有） -->
    <dependency>
        <groupId>dev.langchain4j</groupId>
        <artifactId>langchain4j</artifactId>
    </dependency>
    
    <!-- LangChain4j DashScope 支持（已有） -->
    <dependency>
        <groupId>dev.langchain4j</groupId>
        <artifactId>langchain4j-community-dashscope</artifactId>
    </dependency>
    
    <!-- LangChain4j PDF 解析 -->
    <dependency>
        <groupId>dev.langchain4j</groupId>
        <artifactId>langchain4j-document-parser-apache-pdfbox</artifactId>
    </dependency>
    
    <!-- LangChain4j EasyOCR 解析（支持图片） -->
    <dependency>
        <groupId>dev.langchain4j</groupId>
        <artifactId>langchain4j-document-parser-apache-tika</artifactId>
    </dependency>
    
    <!-- 向量存储：Milvus（生产推荐） -->
    <dependency>
        <groupId>dev.langchain4j</groupId>
        <artifactId>langchain4j-milvus</artifactId>
    </dependency>
    
    <!-- 向量存储：PgVector（可选） -->
    <!--
    <dependency>
        <groupId>dev.langchain4j</groupId>
        <artifactId>langchain4j-pgvector</artifactId>
    </dependency>
    -->
</dependencies>
```

### 4.2 配置文件

在 `application.yml` 中添加 RAG 相关配置：

```yaml
smartcrew:
  rag:
    enabled: true
    embedding:
      provider: dashscope
      model: text-embedding-v3
    vector-store:
      type: in-memory    # 开发环境使用内存存储
      # type: milvus     # 生产环境使用 Milvus
      milvus:
        host: localhost
        port: 19530
        database: default
        collection: smartcrew_knowledge
    document:
      splitter:
        type: paragraph
        max-chunk-size: 200
        overlap-size: 50
      upload-path: ./uploads/knowledge
```

---

## 5. 文档处理实现

### 5.1 领域实体类

```java
package com.smartcrew.agent.api.rag.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("knowledge_base")
public class KnowledgeBase {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String baseCode;
    private String baseName;
    private String description;
    private String embeddingModel;
    private String vectorStoreType;
    private Boolean enabled;
    
    @TableField(fill = FieldFill.INSERT)
    private Long createDept;
    
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.UPDATE)
    private Long updateBy;
    
    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;
    
    private String remark;
}
```

```java
package com.smartcrew.agent.api.rag.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("knowledge_document")
public class KnowledgeDocument {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long baseId;
    private String documentCode;
    private String documentName;
    private String filePath;
    private String fileType;
    private Long fileSize;
    private String status;
    private Integer chunkCount;
    private String errorMessage;
    
    @TableField(fill = FieldFill.INSERT)
    private Long createDept;
    
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.UPDATE)
    private Long updateBy;
    
    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;
    
    private String remark;
}
```

```java
package com.smartcrew.agent.api.rag.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("document_chunk")
public class DocumentChunk {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long documentId;
    private Integer chunkIndex;
    private String content;
    private String vectorId;
    private Integer tokenCount;
    private String metadata;
    
    @TableField(fill = FieldFill.INSERT)
    private Long createDept;
    
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.UPDATE)
    private Long updateBy;
    
    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;
    
    private String remark;
}
```

### 5.2 文档加载服务

```java
package com.smartcrew.agent.api.rag.service;

import dev.langchain4j.data.document.Document;
import java.nio.file.Path;

public interface DocumentLoaderService {

    Document loadDocument(Path filePath);
    
    Document loadDocument(Path filePath, String fileType);
    
    boolean supports(String fileType);
}
```

```java
package com.smartcrew.agent.core.rag.loader;

import com.smartcrew.agent.api.rag.service.DocumentLoaderService;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class DocumentLoaderServiceImpl implements DocumentLoaderService {

    private final Map<String, DocumentParser> parserMap = new HashMap<>();

    public DocumentLoaderServiceImpl() {
        parserMap.put("txt", new TextDocumentParser());
        parserMap.put("md", new TextDocumentParser());
        parserMap.put("pdf", new ApachePdfBoxDocumentParser());
        parserMap.put("docx", new ApacheTikaDocumentParser());
        parserMap.put("doc", new ApacheTikaDocumentParser());
        parserMap.put("html", new ApacheTikaDocumentParser());
    }

    @Override
    public Document loadDocument(Path filePath) {
        String fileName = filePath.getFileName().toString();
        String extension = getFileExtension(fileName);
        return loadDocument(filePath, extension);
    }

    @Override
    public Document loadDocument(Path filePath, String fileType) {
        DocumentParser parser = parserMap.get(fileType.toLowerCase());
        if (parser == null) {
            parser = new ApacheTikaDocumentParser();
        }
        return FileSystemDocumentLoader.loadDocument(filePath, parser);
    }

    @Override
    public boolean supports(String fileType) {
        return parserMap.containsKey(fileType.toLowerCase());
    }

    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0) {
            return fileName.substring(lastDot + 1).toLowerCase();
        }
        return "txt";
    }
}
```

### 5.3 文档分割服务

```java
package com.smartcrew.agent.api.rag.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.segment.TextSegment;
import java.util.List;

public interface DocumentSplitterService {

    List<TextSegment> split(Document document);
    
    List<TextSegment> split(Document document, int maxChunkSize, int overlapSize);
}
```

```java
package com.smartcrew.agent.core.rag.splitter;

import com.smartcrew.agent.api.rag.service.DocumentSplitterService;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.document.splitter.DocumentBySentenceSplitter;
import dev.langchain4j.data.segment.TextSegment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class DocumentSplitterServiceImpl implements DocumentSplitterService {

    @Value("${smartcrew.rag.document.splitter.max-chunk-size:200}")
    private int defaultMaxChunkSize;

    @Value("${smartcrew.rag.document.splitter.overlap-size:50}")
    private int defaultOverlapSize;

    @Value("${smartcrew.rag.document.splitter.type:paragraph}")
    private String splitterType;

    @Override
    public List<TextSegment> split(Document document) {
        return split(document, defaultMaxChunkSize, defaultOverlapSize);
    }

    @Override
    public List<TextSegment> split(Document document, int maxChunkSize, int overlapSize) {
        DocumentSplitter splitter = createSplitter(maxChunkSize, overlapSize);
        List<TextSegment> segments = splitter.split(document);
        log.info("文档分割完成，生成 {} 个切片", segments.size());
        return segments;
    }

    private DocumentSplitter createSplitter(int maxChunkSize, int overlapSize) {
        return switch (splitterType.toLowerCase()) {
            case "sentence" -> new DocumentBySentenceSplitter(maxChunkSize, overlapSize);
            case "paragraph" -> new DocumentByParagraphSplitter(maxChunkSize, overlapSize);
            default -> new DocumentByParagraphSplitter(maxChunkSize, overlapSize);
        };
    }
}
```

---

## 6. 向量存储实现

### 6.1 嵌入模型配置

```java
package com.smartcrew.agent.core.rag.config;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.community.model.dashscope.QwenEmbeddingModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class EmbeddingModelConfig {

    @Value("${smartcrew.rag.embedding.model:text-embedding-v3}")
    private String embeddingModel;

    @Value("${dashscope.api-key}")
    private String apiKey;

    @Bean
    @ConditionalOnProperty(name = "smartcrew.rag.enabled", havingValue = "true")
    public EmbeddingModel embeddingModel() {
        log.info("初始化嵌入模型: {}", embeddingModel);
        return QwenEmbeddingModel.builder()
                .apiKey(apiKey)
                .modelName(embeddingModel)
                .build();
    }
}
```

### 6.2 向量存储配置

```java
package com.smartcrew.agent.api.rag.service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import java.util.List;

public interface VectorStoreService {

    String add(Embedding embedding, TextSegment segment);
    
    List<String> addAll(List<Embedding> embeddings, List<TextSegment> segments);
    
    void remove(String id);
    
    void removeAll(List<String> ids);
    
    List<EmbeddingMatch<TextSegment>> search(Embedding queryEmbedding, int maxResults);
    
    List<EmbeddingMatch<TextSegment>> search(Embedding queryEmbedding, int maxResults, double minScore);
}
```

```java
package com.smartcrew.agent.core.rag.store;

import com.smartcrew.agent.api.rag.service.VectorStoreService;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@ConditionalOnProperty(name = "smartcrew.rag.vector-store.type", havingValue = "in-memory", matchIfMissing = true)
public class InMemoryVectorStoreServiceImpl implements VectorStoreService {

    private final EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

    @Override
    public String add(Embedding embedding, TextSegment segment) {
        String id = embeddingStore.add(embedding, segment);
        log.debug("添加向量到内存存储，ID: {}", id);
        return id;
    }

    @Override
    public List<String> addAll(List<Embedding> embeddings, List<TextSegment> segments) {
        List<String> ids = embeddingStore.addAll(embeddings, segments);
        log.info("批量添加 {} 个向量到内存存储", ids.size());
        return ids;
    }

    @Override
    public void remove(String id) {
        embeddingStore.remove(id);
        log.debug("从内存存储移除向量，ID: {}", id);
    }

    @Override
    public void removeAll(List<String> ids) {
        embeddingStore.removeAll(ids);
        log.info("从内存存储批量移除 {} 个向量", ids.size());
    }

    @Override
    public List<EmbeddingMatch<TextSegment>> search(Embedding queryEmbedding, int maxResults) {
        return embeddingStore.findRelevant(queryEmbedding, maxResults);
    }

    @Override
    public List<EmbeddingMatch<TextSegment>> search(Embedding queryEmbedding, int maxResults, double minScore) {
        return embeddingStore.findRelevant(queryEmbedding, maxResults, minScore);
    }
}
```

### 6.3 Milvus 向量存储实现（生产环境）

```java
package com.smartcrew.agent.core.rag.store;

import com.smartcrew.agent.api.rag.service.VectorStoreService;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;
import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.List;

@Slf4j
@Service
@ConditionalOnProperty(name = "smartcrew.rag.vector-store.type", havingValue = "milvus")
public class MilvusVectorStoreServiceImpl implements VectorStoreService {

    @Value("${smartcrew.rag.vector-store.milvus.host:localhost}")
    private String host;

    @Value("${smartcrew.rag.vector-store.milvus.port:19530}")
    private int port;

    @Value("${smartcrew.rag.vector-store.milvus.database:default}")
    private String database;

    @Value("${smartcrew.rag.vector-store.milvus.collection:smartcrew_knowledge}")
    private String collection;

    private MilvusEmbeddingStore embeddingStore;

    @PostConstruct
    public void init() {
        log.info("初始化 Milvus 向量存储，host: {}, port: {}, collection: {}", host, port, collection);
        this.embeddingStore = MilvusEmbeddingStore.builder()
                .host(host)
                .port(port)
                .databaseName(database)
                .collectionName(collection)
                .dimension(1024)
                .build();
    }

    @Override
    public String add(Embedding embedding, TextSegment segment) {
        String id = embeddingStore.add(embedding, segment);
        log.debug("添加向量到 Milvus，ID: {}", id);
        return id;
    }

    @Override
    public List<String> addAll(List<Embedding> embeddings, List<TextSegment> segments) {
        List<String> ids = embeddingStore.addAll(embeddings, segments);
        log.info("批量添加 {} 个向量到 Milvus", ids.size());
        return ids;
    }

    @Override
    public void remove(String id) {
        embeddingStore.remove(id);
        log.debug("从 Milvus 移除向量，ID: {}", id);
    }

    @Override
    public void removeAll(List<String> ids) {
        embeddingStore.removeAll(ids);
        log.info("从 Milvus 批量移除 {} 个向量", ids.size());
    }

    @Override
    public List<EmbeddingMatch<TextSegment>> search(Embedding queryEmbedding, int maxResults) {
        return embeddingStore.findRelevant(queryEmbedding, maxResults);
    }

    @Override
    public List<EmbeddingMatch<TextSegment>> search(Embedding queryEmbedding, int maxResults, double minScore) {
        return embeddingStore.findRelevant(queryEmbedding, maxResults, minScore);
    }
}
```

---

## 7. 检索服务实现

### 7.1 知识库服务

```java
package com.smartcrew.agent.api.rag.service;

import com.smartcrew.agent.api.rag.domain.entity.KnowledgeBase;
import java.util.List;
import java.util.Optional;

public interface KnowledgeBaseService {

    KnowledgeBase create(KnowledgeBase knowledgeBase);
    
    KnowledgeBase update(KnowledgeBase knowledgeBase);
    
    void delete(String baseCode);
    
    Optional<KnowledgeBase> findByCode(String baseCode);
    
    List<KnowledgeBase> findAll();
    
    List<KnowledgeBase> findByAgentCode(String agentCode);
}
```

```java
package com.smartcrew.agent.core.rag.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartcrew.agent.api.rag.domain.entity.KnowledgeBase;
import com.smartcrew.agent.api.rag.mapper.KnowledgeBaseMapper;
import com.smartcrew.agent.api.rag.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    private final KnowledgeBaseMapper knowledgeBaseMapper;

    @Override
    public KnowledgeBase create(KnowledgeBase knowledgeBase) {
        knowledgeBaseMapper.insert(knowledgeBase);
        log.info("创建知识库: {}", knowledgeBase.getBaseCode());
        return knowledgeBase;
    }

    @Override
    public KnowledgeBase update(KnowledgeBase knowledgeBase) {
        knowledgeBaseMapper.updateById(knowledgeBase);
        log.info("更新知识库: {}", knowledgeBase.getBaseCode());
        return knowledgeBase;
    }

    @Override
    public void delete(String baseCode) {
        knowledgeBaseMapper.delete(new LambdaQueryWrapper<KnowledgeBase>()
                .eq(KnowledgeBase::getBaseCode, baseCode));
        log.info("删除知识库: {}", baseCode);
    }

    @Override
    public Optional<KnowledgeBase> findByCode(String baseCode) {
        return Optional.ofNullable(knowledgeBaseMapper.selectOne(
                new LambdaQueryWrapper<KnowledgeBase>()
                        .eq(KnowledgeBase::getBaseCode, baseCode)));
    }

    @Override
    public List<KnowledgeBase> findAll() {
        return knowledgeBaseMapper.selectList(new LambdaQueryWrapper<>());
    }

    @Override
    public List<KnowledgeBase> findByAgentCode(String agentCode) {
        return knowledgeBaseMapper.selectByAgentCode(agentCode);
    }
}
```

### 7.2 文档处理服务

```java
package com.smartcrew.agent.api.rag.service;

import com.smartcrew.agent.api.rag.domain.entity.KnowledgeDocument;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public interface KnowledgeDocumentService {

    KnowledgeDocument upload(Long baseId, MultipartFile file);
    
    KnowledgeDocument processDocument(Long documentId);
    
    void deleteDocument(Long documentId);
    
    Optional<KnowledgeDocument> findById(Long documentId);
    
    List<KnowledgeDocument> findByBaseId(Long baseId);
    
    List<KnowledgeDocument> findPendingDocuments();
}
```

```java
package com.smartcrew.agent.core.rag.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.smartcrew.agent.api.rag.domain.entity.DocumentChunk;
import com.smartcrew.agent.api.rag.domain.entity.KnowledgeDocument;
import com.smartcrew.agent.api.rag.mapper.DocumentChunkMapper;
import com.smartcrew.agent.api.rag.mapper.KnowledgeDocumentMapper;
import com.smartcrew.agent.api.rag.service.DocumentLoaderService;
import com.smartcrew.agent.api.rag.service.DocumentSplitterService;
import com.smartcrew.agent.api.rag.service.KnowledgeDocumentService;
import com.smartcrew.agent.api.rag.service.VectorStoreService;
import com.smartcrew.agent.common.util.LogUtils;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeDocumentServiceImpl implements KnowledgeDocumentService {

    private final KnowledgeDocumentMapper documentMapper;
    private final DocumentChunkMapper chunkMapper;
    private final DocumentLoaderService documentLoader;
    private final DocumentSplitterService documentSplitter;
    private final VectorStoreService vectorStoreService;
    private final EmbeddingModel embeddingModel;

    @Value("${smartcrew.rag.document.upload-path:./uploads/knowledge}")
    private String uploadPath;

    @Override
    @Transactional
    public KnowledgeDocument upload(Long baseId, MultipartFile file) {
        String documentCode = IdUtil.fastSimpleUUID();
        String originalFilename = file.getOriginalFilename();
        String fileType = getFileExtension(originalFilename);
        
        String filePath = saveFile(file, documentCode, fileType);
        
        KnowledgeDocument document = new KnowledgeDocument();
        document.setBaseId(baseId);
        document.setDocumentCode(documentCode);
        document.setDocumentName(originalFilename);
        document.setFilePath(filePath);
        document.setFileType(fileType);
        document.setFileSize(file.getSize());
        document.setStatus("pending");
        document.setCreateTime(LocalDateTime.now());
        
        documentMapper.insert(document);
        LogUtils.info(log, "上传文档成功: {}", documentCode);
        
        return document;
    }

    @Override
    @Transactional
    public KnowledgeDocument processDocument(Long documentId) {
        KnowledgeDocument document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new IllegalArgumentException("文档不存在: " + documentId);
        }

        document.setStatus("processing");
        documentMapper.updateById(document);

        try {
            Path filePath = Path.of(document.getFilePath());
            Document doc = documentLoader.loadDocument(filePath);
            
            List<TextSegment> segments = documentSplitter.split(doc);
            
            List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
            
            List<String> vectorIds = vectorStoreService.addAll(embeddings, segments);
            
            saveChunks(documentId, segments, vectorIds);
            
            document.setStatus("completed");
            document.setChunkCount(segments.size());
            documentMapper.updateById(document);
            
            LogUtils.info(log, "文档处理完成: {}, 切片数: {}", document.getDocumentCode(), segments.size());
            return document;
            
        } catch (Exception e) {
            document.setStatus("failed");
            document.setErrorMessage(e.getMessage());
            documentMapper.updateById(document);
            LogUtils.error(log, "文档处理失败: {}", e.getMessage(), e);
            throw new RuntimeException("文档处理失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void deleteDocument(Long documentId) {
        KnowledgeDocument document = documentMapper.selectById(documentId);
        if (document == null) {
            return;
        }

        List<DocumentChunk> chunks = chunkMapper.selectByDocumentId(documentId);
        List<String> vectorIds = chunks.stream()
                .map(DocumentChunk::getVectorId)
                .toList();
        
        if (!vectorIds.isEmpty()) {
            vectorStoreService.removeAll(vectorIds);
        }
        
        chunkMapper.deleteByDocumentId(documentId);
        
        if (document.getFilePath() != null) {
            FileUtil.del(document.getFilePath());
        }
        
        documentMapper.deleteById(documentId);
        LogUtils.info(log, "删除文档: {}", document.getDocumentCode());
    }

    @Override
    public Optional<KnowledgeDocument> findById(Long documentId) {
        return Optional.ofNullable(documentMapper.selectById(documentId));
    }

    @Override
    public List<KnowledgeDocument> findByBaseId(Long baseId) {
        return documentMapper.selectByBaseId(baseId);
    }

    @Override
    public List<KnowledgeDocument> findPendingDocuments() {
        return documentMapper.selectPendingDocuments();
    }

    private String saveFile(MultipartFile file, String documentCode, String fileType) {
        String dirPath = uploadPath + File.separator + documentCode.substring(0, 2);
        FileUtil.mkdir(dirPath);
        
        String filePath = dirPath + File.separator + documentCode + "." + fileType;
        try {
            file.transferTo(new File(filePath));
        } catch (IOException e) {
            throw new RuntimeException("文件保存失败: " + e.getMessage(), e);
        }
        return filePath;
    }

    private void saveChunks(Long documentId, List<TextSegment> segments, List<String> vectorIds) {
        for (int i = 0; i < segments.size(); i++) {
            TextSegment segment = segments.get(i);
            String vectorId = vectorIds.get(i);
            
            DocumentChunk chunk = new DocumentChunk();
            chunk.setDocumentId(documentId);
            chunk.setChunkIndex(i);
            chunk.setContent(segment.text());
            chunk.setVectorId(vectorId);
            chunk.setCreateTime(LocalDateTime.now());
            
            chunkMapper.insert(chunk);
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null) {
            return "txt";
        }
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0) {
            return filename.substring(lastDot + 1).toLowerCase();
        }
        return "txt";
    }
}
```

### 7.3 检索服务

```java
package com.smartcrew.agent.api.rag.service;

import dev.langchain4j.rag.content.Content;
import java.util.List;

public interface RetrievalService {

    List<Content> retrieve(String query, int topK);
    
    List<Content> retrieve(String query, String baseCode, int topK);
    
    List<Content> retrieve(String query, List<String> baseCodes, int topK);
}
```

```java
package com.smartcrew.agent.core.rag.service;

import com.smartcrew.agent.api.rag.service.RetrievalService;
import com.smartcrew.agent.api.rag.service.VectorStoreService;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.Content;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RetrievalServiceImpl implements RetrievalService {

    private final VectorStoreService vectorStoreService;
    private final EmbeddingModel embeddingModel;

    @Override
    public List<Content> retrieve(String query, int topK) {
        return doRetrieve(query, topK, 0.7);
    }

    @Override
    public List<Content> retrieve(String query, String baseCode, int topK) {
        return doRetrieve(query, topK, 0.7);
    }

    @Override
    public List<Content> retrieve(String query, List<String> baseCodes, int topK) {
        return doRetrieve(query, topK, 0.7);
    }

    private List<Content> doRetrieve(String query, int topK, double minScore) {
        log.debug("开始检索，查询: {}, topK: {}, minScore: {}", query, topK, minScore);
        
        Embedding queryEmbedding = embeddingModel.embed(query).content();
        
        List<dev.langchain4j.store.embedding.EmbeddingMatch<TextSegment>> matches = 
                vectorStoreService.search(queryEmbedding, topK, minScore);
        
        List<Content> contents = matches.stream()
                .map(match -> Content.from(match.embedded()))
                .toList();
        
        log.debug("检索完成，返回 {} 条结果", contents.size());
        return contents;
    }
}
```

### 7.4 RAG 增强服务

```java
package com.smartcrew.agent.api.rag.service;

public interface RagAugmentationService {

    String buildRagContext(String query);
    
    String buildRagContext(String query, String agentCode);
    
    String buildRagContext(String query, int topK);
}
```

```java
package com.smartcrew.agent.core.rag.service;

import com.smartcrew.agent.api.rag.service.RagAugmentationService;
import com.smartcrew.agent.api.rag.service.RetrievalService;
import dev.langchain4j.rag.content.Content;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagAugmentationServiceImpl implements RagAugmentationService {

    private final RetrievalService retrievalService;

    @Override
    public String buildRagContext(String query) {
        return buildRagContext(query, 3);
    }

    @Override
    public String buildRagContext(String query, String agentCode) {
        return buildRagContext(query, 3);
    }

    @Override
    public String buildRagContext(String query, int topK) {
        List<Content> contents = retrievalService.retrieve(query, topK);
        
        if (contents.isEmpty()) {
            log.debug("未找到相关内容，查询: {}", query);
            return "";
        }
        
        StringBuilder context = new StringBuilder();
        context.append("以下是与问题相关的参考资料，请基于这些资料回答问题：\n\n");
        
        for (int i = 0; i < contents.size(); i++) {
            context.append("【参考资料 ").append(i + 1).append("】\n");
            context.append(contents.get(i).textSegment().text());
            context.append("\n\n");
        }
        
        log.debug("构建 RAG 上下文完成，参考文档数: {}", contents.size());
        return context.toString();
    }
}
```

---

## 8. 集成到 InitialAgent

### 8.1 修改 InitialAgent

```java
package com.smartcrew.agent.core.agent;

import com.smartcrew.agent.api.agent.domain.request.AgentDispatchCommand;
import com.smartcrew.agent.api.agent.domain.vo.AgentDispatchResponse;
import com.smartcrew.agent.api.agent.service.Agent;
import com.smartcrew.agent.api.llm.domain.request.LlmChatRequest;
import com.smartcrew.agent.api.llm.domain.vo.LlmChatResponse;
import com.smartcrew.agent.api.llm.service.LlmClient;
import com.smartcrew.agent.api.rag.service.RagAugmentationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InitialAgent implements Agent {

    private final LlmClient llmClient;
    private final RagAugmentationService ragAugmentationService;
    private final InitialAgentPromptService promptService;

    @Override
    public String code() {
        return "initial-agent";
    }

    @Override
    public String name() {
        return "Initial Agent";
    }

    @Override
    public boolean supports(String capability) {
        return "chat".equalsIgnoreCase(capability)
                || "orchestrate".equalsIgnoreCase(capability)
                || "rag".equalsIgnoreCase(capability);
    }

    @Override
    public AgentDispatchResponse handle(AgentDispatchCommand command) {
        String llmSessionId = code() + "::" + command.getSessionId();

        String basePrompt = promptService.buildSystemPrompt(command.getUserId(), "default");
        
        String ragContext = ragAugmentationService.buildRagContext(command.getMessage());
        
        String systemPrompt = buildSystemPrompt(basePrompt, ragContext);

        LlmChatRequest request = LlmChatRequest.builder()
                .userId(command.getUserId())
                .sessionId(llmSessionId)
                .userMessage(command.getMessage())
                .systemPrompt(systemPrompt)
                .traceId(command.getTraceId())
                .build();

        LlmChatResponse response = llmClient.chat(request);
        
        if (!Boolean.TRUE.equals(response.getSuccess())) {
            return AgentDispatchResponse.builder()
                    .traceId(command.getTraceId())
                    .agentCode(code())
                    .accepted(false)
                    .message("暂时无法处理你的请求，请稍后再试。")
                    .build();
        }

        return AgentDispatchResponse.builder()
                .traceId(command.getTraceId())
                .agentCode(code())
                .accepted(true)
                .message(response.getContent())
                .build();
    }

    private String buildSystemPrompt(String basePrompt, String ragContext) {
        if (ragContext == null || ragContext.isBlank()) {
            return basePrompt;
        }
        
        return basePrompt + "\n\n" + ragContext;
    }
}
```

### 8.2 RAG 专用提示词模板

在 `prompt_template` 表中插入：

```sql
INSERT INTO prompt_template (template_name, template_content, category, create_time) 
VALUES (
    'Initial Agent RAG 提示词',
    '你是 SmartCrew 的智能助手，负责回答用户问题。

## 回答原则

1. 优先使用提供的参考资料回答问题
2. 如果参考资料中没有相关信息，请明确告知用户
3. 回答要准确、简洁、有条理
4. 如果不确定，不要编造信息

## 参考资料

{rag_context}

请基于以上参考资料，回答用户的问题。',
    'initial-agent-rag',
    NOW()
);
```

### 8.3 支持 RAG 的提示词服务

```java
package com.smartcrew.agent.core.agent.service;

import com.smartcrew.agent.api.prompt.domain.entity.PromptTemplate;
import com.smartcrew.agent.api.prompt.service.PromptTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class InitialAgentPromptServiceImpl implements InitialAgentPromptService {

    private static final String DEFAULT_PROMPT =
            "你是 SmartCrew 的初始 Agent，负责直接与用户交流，并在必要时协调其他能力。";

    private final PromptTemplateService promptTemplateService;

    @Override
    public String buildSystemPrompt(Long userId, String scene) {
        String category = resolveCategory(scene);
        Optional<PromptTemplate> template = promptTemplateService.findByCategory(category);
        return template.map(PromptTemplate::getTemplateContent).orElse(DEFAULT_PROMPT);
    }

    public String buildRagPrompt(String basePrompt, String ragContext) {
        if (ragContext == null || ragContext.isBlank()) {
            return basePrompt;
        }
        
        return basePrompt.replace("{rag_context}", ragContext);
    }

    private String resolveCategory(String scene) {
        if ("rag".equalsIgnoreCase(scene)) {
            return "initial-agent-rag";
        }
        if ("routing".equalsIgnoreCase(scene)) {
            return "initial-agent-routing";
        }
        if ("tool".equalsIgnoreCase(scene)) {
            return "initial-agent-tool";
        }
        return "initial-agent";
    }
}
```

---

## 9. 完整使用示例

### 9.1 创建知识库

```java
@RestController
@RequestMapping("/api/v1/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeBaseService knowledgeBaseService;
    private final KnowledgeDocumentService documentService;

    @PostMapping("/bases")
    public Result<KnowledgeBase> createBase(@RequestBody KnowledgeBaseCreateRequest request) {
        KnowledgeBase base = new KnowledgeBase();
        base.setBaseCode(request.getBaseCode());
        base.setBaseName(request.getBaseName());
        base.setDescription(request.getDescription());
        base.setEmbeddingModel("text-embedding-v3");
        base.setVectorStoreType("in-memory");
        base.setEnabled(true);
        
        return Result.success(knowledgeBaseService.create(base));
    }
}
```

### 9.2 上传文档

```java
@PostMapping("/documents/upload")
public Result<KnowledgeDocument> uploadDocument(
        @RequestParam Long baseId,
        @RequestParam MultipartFile file) {
    
    KnowledgeDocument document = documentService.upload(baseId, file);
    
    documentService.processDocument(document.getId());
    
    return Result.success(document);
}
```

### 9.3 查询测试

```java
@Test
void testRagRetrieval() {
    String query = "如何使用 RAG 技术？";
    
    List<Content> results = retrievalService.retrieve(query, 3);
    
    assertThat(results).isNotEmpty();
    
    for (Content content : results) {
        System.out.println("相关内容: " + content.textSegment().text());
    }
}
```

### 9.4 完整对话测试

```java
@Test
void testRagChat() {
    AgentDispatchCommand command = AgentDispatchCommand.builder()
            .userId(1001L)
            .sessionId("test-session-001")
            .message("请介绍一下 RAG 技术的原理")
            .traceId(UUID.randomUUID().toString())
            .build();

    AgentDispatchResponse response = initialAgent.handle(command);
    
    assertThat(response.getAccepted()).isTrue();
    assertThat(response.getMessage()).isNotEmpty();
    
    System.out.println("回答: " + response.getMessage());
}
```

---

## 10. 进阶优化

### 10.1 混合检索

结合关键词检索和向量检索，提高召回率：

```java
@Service
public class HybridRetrievalServiceImpl implements RetrievalService {

    private final VectorStoreService vectorStoreService;
    private final EmbeddingModel embeddingModel;
    private final DocumentChunkMapper chunkMapper;

    @Override
    public List<Content> retrieve(String query, int topK) {
        List<Content> vectorResults = vectorSearch(query, topK);
        
        List<Content> keywordResults = keywordSearch(query, topK);
        
        return mergeAndRerank(vectorResults, keywordResults, topK);
    }

    private List<Content> vectorSearch(String query, int topK) {
        Embedding queryEmbedding = embeddingModel.embed(query).content();
        return vectorStoreService.search(queryEmbedding, topK).stream()
                .map(match -> Content.from(match.embedded()))
                .toList();
    }

    private List<Content> keywordSearch(String query, int topK) {
        List<DocumentChunk> chunks = chunkMapper.searchByKeyword(query, topK);
        return chunks.stream()
                .map(chunk -> Content.from(TextSegment.from(chunk.getContent())))
                .toList();
    }

    private List<Content> mergeAndRerank(List<Content> vectorResults, 
                                          List<Content> keywordResults, 
                                          int topK) {
        Set<String> seen = new HashSet<>();
        List<Content> merged = new ArrayList<>();
        
        for (Content content : vectorResults) {
            String text = content.textSegment().text();
            if (!seen.contains(text)) {
                merged.add(content);
                seen.add(text);
            }
        }
        
        for (Content content : keywordResults) {
            String text = content.textSegment().text();
            if (!seen.contains(text)) {
                merged.add(content);
                seen.add(text);
            }
        }
        
        return merged.stream().limit(topK).toList();
    }
}
```

### 10.2 重排序

使用重排序模型对检索结果进行二次排序：

```java
@Service
public class RerankingRetrievalServiceImpl implements RetrievalService {

    private final RetrievalService baseRetrievalService;
    private final ChatLanguageModel rerankModel;

    @Override
    public List<Content> retrieve(String query, int topK) {
        List<Content> candidates = baseRetrievalService.retrieve(query, topK * 2);
        
        return rerank(query, candidates, topK);
    }

    private List<Content> rerank(String query, List<Content> candidates, int topK) {
        String prompt = buildRerankPrompt(query, candidates);
        
        String response = rerankModel.generate(prompt);
        
        return parseRerankResult(response, candidates, topK);
    }

    private String buildRerankPrompt(String query, List<Content> candidates) {
        StringBuilder sb = new StringBuilder();
        sb.append("请对以下文档片段与查询问题的相关性进行评分（0-10分）：\n\n");
        sb.append("查询问题：").append(query).append("\n\n");
        
        for (int i = 0; i < candidates.size(); i++) {
            sb.append("文档").append(i + 1).append("：\n");
            sb.append(candidates.get(i).textSegment().text()).append("\n\n");
        }
        
        sb.append("请按格式返回：文档编号:分数，例如：1:8, 2:5, 3:9");
        return sb.toString();
    }

    private List<Content> parseRerankResult(String response, 
                                             List<Content> candidates, 
                                             int topK) {
        return candidates.stream().limit(topK).toList();
    }
}
```

### 10.3 引用来源

在回答中标注信息来源：

```java
@Service
public class SourceCitedRagServiceImpl implements RagAugmentationService {

    private final RetrievalService retrievalService;

    @Override
    public String buildRagContext(String query) {
        List<Content> contents = retrievalService.retrieve(query, 3);
        
        if (contents.isEmpty()) {
            return "";
        }
        
        StringBuilder context = new StringBuilder();
        context.append("以下是相关参考资料：\n\n");
        
        for (int i = 0; i < contents.size(); i++) {
            Content content = contents.get(i);
            String source = content.textSegment().metadata("source");
            String docName = content.textSegment().metadata("document_name");
            
            context.append("【来源 ").append(i + 1);
            if (docName != null) {
                context.append(" - ").append(docName);
            }
            context.append("】\n");
            context.append(content.textSegment().text());
            context.append("\n\n");
        }
        
        return context.toString();
    }
}
```

### 10.4 增量更新

支持文档的增量更新：

```java
@Service
public class IncrementalDocumentServiceImpl {

    private final KnowledgeDocumentService documentService;
    private final DocumentChunkMapper chunkMapper;
    private final VectorStoreService vectorStoreService;

    @Transactional
    public void updateDocument(Long documentId, MultipartFile newFile) {
        KnowledgeDocument oldDoc = documentService.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("文档不存在"));
        
        List<DocumentChunk> oldChunks = chunkMapper.selectByDocumentId(documentId);
        List<String> oldVectorIds = oldChunks.stream()
                .map(DocumentChunk::getVectorId)
                .toList();
        
        if (!oldVectorIds.isEmpty()) {
            vectorStoreService.removeAll(oldVectorIds);
        }
        
        chunkMapper.deleteByDocumentId(documentId);
        
        KnowledgeDocument newDoc = documentService.upload(oldDoc.getBaseId(), newFile);
        documentService.processDocument(newDoc.getId());
        
        documentService.deleteDocument(documentId);
    }
}
```

---

## 总结

本教程从 RAG 的基础概念开始，详细讲解了：

1. **RAG 核心组件**：文档加载、分割、向量化、存储、检索
2. **数据库设计**：知识库、文档、切片、绑定关系
3. **代码实现**：完整的 Java 实现代码
4. **项目集成**：如何将 RAG 集成到 InitialAgent
5. **进阶优化**：混合检索、重排序、引用来源、增量更新

按照本教程实现后，你的 Agent 将具备：
- 上传知识文档的能力
- 基于知识库回答问题的能力
- 可追溯的回答来源

下一步建议：
1. 先用内存向量存储验证完整流程
2. 再切换到 Milvus 或 PgVector 用于生产环境
3. 根据实际效果调整分割策略和检索参数
