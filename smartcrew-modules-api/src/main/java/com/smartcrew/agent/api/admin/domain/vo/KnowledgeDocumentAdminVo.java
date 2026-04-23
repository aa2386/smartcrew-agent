package com.smartcrew.agent.api.admin.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 后台知识文档视图对象。
 */
@Data
public class KnowledgeDocumentAdminVo {

    /**
     * 文档主键 ID。
     */
    private Long id;

    /**
     * 所属知识库 ID。
     */
    private Long baseId;

    /**
     * 知识库编码。
     */
    private String baseCode;

    /**
     * 文档编码。
     */
    private String documentCode;

    /**
     * 文档名称。
     */
    private String documentName;

    /**
     * 文件存储路径。
     */
    private String filePath;

    /**
     * 文件类型。
     */
    private String fileType;

    /**
     * 文件大小，单位为字节。
     */
    private Long fileSize;

    /**
     * 文档处理状态。
     */
    private String status;

    /**
     * 已生成切片数量。
     */
    private Integer chunkCount;

    /**
     * 处理失败时的错误信息。
     */
    private String errorMessage;

    /**
     * 创建时间。
     */
    private LocalDateTime createTime;

    /**
     * 更新时间。
     */
    private LocalDateTime updateTime;
}
