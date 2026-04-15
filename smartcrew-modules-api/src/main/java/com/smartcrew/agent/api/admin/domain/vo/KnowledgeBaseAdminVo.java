package com.smartcrew.agent.api.admin.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 后台知识库视图对象。
 */
@Data
public class KnowledgeBaseAdminVo {

    private Long id;

    private String baseCode;

    private String baseName;

    private String description;

    private String embeddingModel;

    private String collectionName;

    private Boolean enabled;

    private Long documentCount;

    private Long chunkCount;

    private Long agentCount;

    private Long processingDocumentCount;

    private Boolean hasDocuments;

    private Boolean collectionNameEditable;

    private Boolean embeddingModelEditable;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
