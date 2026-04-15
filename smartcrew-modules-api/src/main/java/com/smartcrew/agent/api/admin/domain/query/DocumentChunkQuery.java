package com.smartcrew.agent.api.admin.domain.query;

import com.smartcrew.agent.core.page.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文档切片分页查询参数。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DocumentChunkQuery extends PageQuery {

    /**
     * 关键字。
     */
    private String keyword;
}
