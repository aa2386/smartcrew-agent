package com.smartcrew.agent.api.experience.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.smartcrew.agent.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 经验命中日志实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("agent_experience_hit_log")
public class AgentExperienceHitLog extends BaseEntity {

    /**
     * 主键 ID。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

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
}
