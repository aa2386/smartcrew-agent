package com.smartcrew.agent.api.admin.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 后台知识库视图对象。
 */
@Data
public class KnowledgeBaseAdminVo {

    /**
     * 知识库主键 ID。
     */
    private Long id;

    /**
     * 知识库编码。
     */
    private String baseCode;

    /**
     * 知识库名称。
     */
    private String baseName;

    /**
     * 知识库描述。
     */
    private String description;

    /**
     * 嵌入模型名称。
     */
    private String embeddingModel;

    /**
     * 向量命名空间或集合名称。
     */
    private String collectionName;

    /**
     * 是否启用。
     */
    private Boolean enabled;

    /**
     * 文档数量。
     */
    private Long documentCount;

    /**
     * 切片数量。
     */
    private Long chunkCount;

    /**
     * 已绑定 Agent 数量。
     */
    private Long agentCount;

    /**
     * 处理中文档数量。
     */
    private Long processingDocumentCount;

    /**
     * 是否已存在文档。
     */
    private Boolean hasDocuments;

    /**
     * 是否允许编辑集合名称。
     */
    private Boolean collectionNameEditable;

    /**
     * 是否允许编辑嵌入模型。
     */
    private Boolean embeddingModelEditable;

    /**
     * 创建时间。
     */
    private LocalDateTime createTime;

    /**
     * 更新时间。
     */
    private LocalDateTime updateTime;
}
