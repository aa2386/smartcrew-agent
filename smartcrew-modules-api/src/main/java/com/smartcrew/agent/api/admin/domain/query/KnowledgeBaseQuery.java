package com.smartcrew.agent.api.admin.domain.query;

import com.smartcrew.agent.core.page.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 知识库分页查询参数。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class KnowledgeBaseQuery extends PageQuery {

    /**
     * 关键字。
     */
    private String keyword;

    /**
     * 启用状态。
     */
    private Boolean enabled;
}
