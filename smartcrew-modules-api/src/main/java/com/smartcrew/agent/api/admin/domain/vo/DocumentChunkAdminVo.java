package com.smartcrew.agent.api.admin.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 后台文档切片视图对象。
 */
@Data
public class DocumentChunkAdminVo {

    private Long id;

    private Long documentId;

    private String documentCode;

    private String documentName;

    private Integer chunkIndex;

    private String content;

    private String contentPreview;

    private String vectorId;

    private Integer tokenCount;

    private String metadata;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
