package com.smartcrew.agent.api.experience.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 经验召回视图对象。
 */
@Data
public class AgentExperienceRecallVo {

    /**
     * 经验编码。
     */
    private String experienceCode;

    /**
     * 作用域类型。
     */
    private String scopeType;

    /**
     * 经验类型。
     */
    private String experienceType;

    /**
     * 经验标题。
     */
    private String title;

    /**
     * 触发模式。
     */
    private String triggerPattern;

    /**
     * 策略摘要。
     */
    private String strategySummary;

    /**
     * 推荐 Agent 编码。
     */
    private String recommendedAgentCode;

    /**
     * 推荐工具编码列表。
     */
    private List<String> recommendedToolCodes;

    /**
     * 成功样例。
     */
    private String successSample;

    /**
     * 失败规避建议。
     */
    private String failureAvoidance;

    /**
     * 质量分。
     */
    private BigDecimal qualityScore;

    /**
     * 命中次数。
     */
    private Integer hitCount;

    /**
     * 成功次数。
     */
    private Integer successCount;

    /**
     * 最近使用时间。
     */
    private LocalDateTime lastUsedAt;

    /**
     * 是否启用。
     */
    private Boolean enabled;
}
