package com.smartcrew.agent.api.experience.domain.query;

import com.smartcrew.agent.core.page.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 经验池分页查询参数。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AgentExperiencePoolQuery extends PageQuery {

    /**
     * 关键字。
     */
    private String keyword;

    /**
     * 作用域类型。
     */
    private String scopeType;

    /**
     * 经验类型。
     */
    private String experienceType;

    /**
     * 是否启用。
     */
    private Boolean enabled;
}
