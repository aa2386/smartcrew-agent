package com.smartcrew.agent.api.collaboration.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 协作日志列表视图对象。
 */
@Data
public class AgentCollaborationLogVo {

    /**
     * 协作链路 ID。
     */
    private String traceId;

    /**
     * 根会话 ID。
     */
    private String rootSessionId;

    /**
     * 用户 ID。
     */
    private Long userId;

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
     * 开始时间。
     */
    private LocalDateTime startTime;

    /**
     * 耗时毫秒。
     */
    private Long durationMs;
}
