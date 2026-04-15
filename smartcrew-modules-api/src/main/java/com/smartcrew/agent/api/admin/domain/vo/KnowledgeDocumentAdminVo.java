package com.smartcrew.agent.api.admin.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 后台知识文档视图对象。
 */
@Data
public class KnowledgeDocumentAdminVo {

    private Long id;

    private Long baseId;

    private String baseCode;

    private String documentCode;

    private String documentName;

    private String filePath;

    private String fileType;

    private Long fileSize;

    private String status;

    private Integer chunkCount;

    private String errorMessage;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
