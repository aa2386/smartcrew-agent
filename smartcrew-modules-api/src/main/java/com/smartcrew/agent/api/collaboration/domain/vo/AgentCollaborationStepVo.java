package com.smartcrew.agent.api.collaboration.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 协作步骤详情视图对象。
 */
@Data
public class AgentCollaborationStepVo {

    /**
     * 主键 ID。
     */
    private Long id;

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
     * 来源标识。
     */
    private String source;

    /**
     * Agent 编码。
     */
    private String agentCode;

    /**
     * 步骤类型。
     */
    private String stepType;

    /**
     * 步骤名称。
     */
    private String stepName;

    /**
     * 父步骤 ID。
     */
    private Long parentStepId;

    /**
     * 处理状态。
     */
    private String status;

    /**
     * 输入摘要。
     */
    private String inputSnapshot;

    /**
     * 输出摘要。
     */
    private String outputSnapshot;

    /**
     * 决策摘要。
     */
    private String decisionSnapshot;

    /**
     * 错误信息。
     */
    private String errorMessage;

    /**
     * 开始时间。
     */
    private LocalDateTime startTime;

    /**
     * 结束时间。
     */
    private LocalDateTime endTime;

    /**
     * 耗时毫秒。
     */
    private Long durationMs;
}
