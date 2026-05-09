package com.smartcrew.agent.api.experience.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 经验命中日志视图对象。
 */
@Data
public class AgentExperienceHitLogVo {

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
     * 应用阶段。
     */
    private String appliedStage;

    /**
     * 应用摘要。
     */
    private String appliedSnapshot;

    /**
     * 是否成功。
     */
    private Boolean successFlag;

    /**
     * 创建时间。
     */
    private LocalDateTime createTime;
}
