package com.smartcrew.agent.api.rag.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.smartcrew.agent.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 知识文档实体，用于追踪文档处理状态与元数据。
 */
@Data
@TableName("knowledge_document")
@EqualsAndHashCode(callSuper = true)
public class KnowledgeDocument extends BaseEntity {

    /**
     * 主键 ID。
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 知识库 ID。
     */
    private Long baseId;
    /**
     * 文档编码。
     */
    private String documentCode;
    /**
     * 文档名称。
     */
    private String documentName;
    /**
     * 存储路径。
     */
    private String filePath;
    /**
     * 文件类型。
     */
    private String fileType;
    /**
     * 文件大小。
     */
    private Long fileSize;
    /**
     * 处理状态。
     */
    private String status;
    /**
     * 切片数量。
     */
    private Integer chunkCount;
    /**
     * 错误信息。
     */
    private String errorMessage;
}
