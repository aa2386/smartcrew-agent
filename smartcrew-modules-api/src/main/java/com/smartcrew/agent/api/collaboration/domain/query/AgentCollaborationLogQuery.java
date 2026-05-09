package com.smartcrew.agent.api.collaboration.domain.query;

import com.smartcrew.agent.core.page.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 协作日志分页查询参数。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AgentCollaborationLogQuery extends PageQuery {

    /**
     * 协作链路 ID。
     */
    private String traceId;

    /**
     * 根会话 ID。
     */
    private String rootSessionId;

    /**
     * Agent 编码。
     */
    private String agentCode;

    /**
     * 步骤类型。
     */
    private String stepType;

    /**
     * 处理状态。
     */
    private String status;

    /**
     * 关键字。
     */
    private String keyword;

    /**
     * 开始时间下界。
     */
    private LocalDateTime startTimeFrom;

    /**
     * 开始时间上界。
     */
    private LocalDateTime startTimeTo;
}
