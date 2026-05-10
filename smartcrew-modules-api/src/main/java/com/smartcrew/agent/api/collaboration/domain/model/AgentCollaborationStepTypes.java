package com.smartcrew.agent.api.collaboration.domain.model;

/**
 * 协作步骤类型常量。
 */
public final class AgentCollaborationStepTypes {

    /** 调度分发 */
    public static final String DISPATCH = "DISPATCH";
    /** 记忆读取 */
    public static final String MEMORY_READ = "MEMORY_READ";
    /** 决策规划 */
    public static final String DECISION = "DECISION";
    /** 执行阶段 */
    public static final String EXECUTION = "EXECUTION";
    /** 工具调用 */
    public static final String TOOL_CALL = "TOOL_CALL";
    /** 记忆写回 */
    public static final String MEMORY_WRITE = "MEMORY_WRITE";
    /** 最终响应 */
    public static final String FINAL_RESPONSE = "FINAL_RESPONSE";

    private AgentCollaborationStepTypes() {
    }
}
