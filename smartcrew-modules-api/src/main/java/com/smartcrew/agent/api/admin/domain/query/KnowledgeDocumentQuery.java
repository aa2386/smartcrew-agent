package com.smartcrew.agent.api.admin.domain.query;

import com.smartcrew.agent.core.page.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 知识文档分页查询参数。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class KnowledgeDocumentQuery extends PageQuery {

    /**
     * 关键字。
     */
    private String keyword;

    /**
     * 文档状态。
     */
    private String status;

    /**
     * 文档类型。
     */
    private String fileType;
}
