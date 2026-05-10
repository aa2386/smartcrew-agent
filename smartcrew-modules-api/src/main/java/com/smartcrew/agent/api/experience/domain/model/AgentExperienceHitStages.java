package com.smartcrew.agent.api.experience.domain.model;

/**
 * 经验命中阶段常量。
 */
public final class AgentExperienceHitStages {

    /** 经验召回阶段 */
    public static final String RECALL = "RECALL";
    /** 决策阶段 */
    public static final String DECISION = "DECISION";
    /** 执行阶段 */
    public static final String EXECUTION = "EXECUTION";
    /** 最终响应阶段 */
    public static final String FINAL_RESPONSE = "FINAL_RESPONSE";

    private AgentExperienceHitStages() {
    }
}
