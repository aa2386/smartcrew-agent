package com.smartcrew.agent.api.rag.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.smartcrew.agent.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 知识库实体，描述 RAG 知识库的基础信息。
 */
@Data
@TableName("knowledge_base")
@EqualsAndHashCode(callSuper = true)
public class KnowledgeBase extends BaseEntity {

    /**
     * 主键 ID。
     */
    @TableId(type = IdType.AUTO)
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
     * 使用的嵌入模型名称。
     */
    private String embeddingModel;

    /**
     * 向量库命名空间或集合名称。
     */
    private String collectionName;

    /**
     * 是否启用。
     */
    private Boolean enabled;
}
