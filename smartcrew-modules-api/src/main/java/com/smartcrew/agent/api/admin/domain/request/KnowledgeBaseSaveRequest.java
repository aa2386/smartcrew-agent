package com.smartcrew.agent.api.admin.domain.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 知识库创建或更新请求。
 */
@Data
public class KnowledgeBaseSaveRequest {

    /**
     * 知识库编码。
     */
    @NotBlank
    private String baseCode;

    /**
     * 知识库名称。
     */
    @NotBlank
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
     * 向量命名空间。
     */
    private String collectionName;

    /**
     * 是否启用。
     */
    private Boolean enabled = true;
}
