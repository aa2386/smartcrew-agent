package com.smartcrew.agent.api.experience.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.smartcrew.agent.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 经验池实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("agent_experience_pool")
public class AgentExperiencePool extends BaseEntity {

    /**
     * 主键 ID。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

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
    @TableField("recommended_tool_codes")
    private String recommendedToolCodesJson;

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

    /**
     * 来源链路 ID。
     */
    private String sourceTraceId;
}
