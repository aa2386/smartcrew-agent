package com.smartcrew.agent.api.experience.domain.query;

import com.smartcrew.agent.core.page.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 经验命中日志分页查询参数。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AgentExperienceHitLogQuery extends PageQuery {

    /**
     * 协作链路 ID。
     */
    private String traceId;

    /**
     * 经验编码。
     */
    private String experienceCode;

    /**
     * Agent 编码。
     */
    private String agentCode;

    /**
     * 是否成功。
     */
    private Boolean successFlag;
}
