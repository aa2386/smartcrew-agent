package com.smartcrew.agent.api.rag.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.smartcrew.agent.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文档切片实体，用于保存切片内容与向量关联关系。
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
     * 所属文档 ID。
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
     * 切片 Token 数量。
     */
    private Integer tokenCount;

    /**
     * 切片元数据 JSON。
     */
    private String metadata;
}
