package com.smartcrew.agent.api.rag.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.smartcrew.agent.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文档切片实体，用于保存切片内容和向量关联。
 */
@Data
@TableName("document_chunk")
@EqualsAndHashCode(callSuper = true)
public class DocumentChunk extends BaseEntity {

    /**
     * 主键 ID。
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 文档 ID。
     */
    private Long documentId;
    /**
     * 切片序号。
     */
    private Integer chunkIndex;
    /**
     * 切片内容。
     */
    private String content;
    /**
     * 向量 ID。
     */
    private String vectorId;
    /**
     * Token 数量。
     */
    private Integer tokenCount;
    /**
     * 元数据 JSON。
     */
    private String metadata;
}
