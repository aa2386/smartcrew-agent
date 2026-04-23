package com.smartcrew.agent.api.admin.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 后台文档切片视图对象。
 */
@Data
public class DocumentChunkAdminVo {

    /**
     * 切片主键 ID。
     */
    private Long id;

    /**
     * 所属文档 ID。
     */
    private Long documentId;

    /**
     * 文档编码。
     */
    private String documentCode;

    /**
     * 文档名称。
     */
    private String documentName;

    /**
     * 切片序号。
     */
    private Integer chunkIndex;

    /**
     * 切片完整内容。
     */
    private String content;

    /**
     * 切片内容摘要。
     */
    private String contentPreview;

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

    /**
     * 创建时间。
     */
    private LocalDateTime createTime;

    /**
     * 更新时间。
     */
    private LocalDateTime updateTime;
}
